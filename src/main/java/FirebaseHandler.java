import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FirebaseHandler {

    // Logger
    private static final Logger logger = LoggerFactory.getLogger(FirebaseHandler.class);

    // Config
    public String projectId;
    public String bucketAddress;
    public String databaseURL;

    private Bucket bucket;
    private FileInputStream service;
    private Firestore firestore;

    public FirebaseHandler(String serviceFilePath) throws IOException {
        projectId = "htlgkr-testet";
        bucketAddress = "htlgkr-testet.appspot.com";
        databaseURL = "https://htlgkr-testet-default-rtdb.firebaseio.com/";
        service = new FileInputStream(serviceFilePath);

        initFirebase();
    }

    public FirebaseHandler(String projectId, String bucketAddress, String databaseURL, String serviceFilePath) throws IOException {
        this.projectId = projectId;
        this.bucketAddress = bucketAddress;
        this.databaseURL = databaseURL;
        this.service = new FileInputStream(serviceFilePath);

        initFirebase();
    }

    private void initFirebase() throws IOException {
        logger.debug("Initializing Firebase with values:");
        logger.debug("DatabaseURL: {}", databaseURL);
        logger.debug("StorageBucket: {}", bucketAddress);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(service))
                .setDatabaseUrl(databaseURL)
                .setStorageBucket(bucketAddress)
                .build();
        FirebaseApp.initializeApp(options);

        firestore = FirestoreClient.getFirestore();
        bucket = StorageClient.getInstance().bucket();
    }

    // CLOUD STORAGE

    public String uploadToStorage(String filename, String type, ByteArrayOutputStream stream) {
        String hash = Hashing.toHexString(Hashing.getSHA(filename));
        String path = "barcodes/" + hash;
        logger.debug("Uploading {} to cloud storage...", path);
        bucket.create(path + "." + type, stream.toByteArray());
        return path;
    }

    // FIRESTORE

    public void setFirestoreListener(EventListener<QuerySnapshot> snapshotEventListener) {
        firestore.collection("Registrations")
                .addSnapshotListener(snapshotEventListener);
    }

    public void setFirestoreListener() {
        setFirestoreListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirestoreException error) {
                if (error != null || value == null)
                    return;

                for (DocumentSnapshot doc : value) {
                    String content = doc.getId();

                    if (doc.contains("barcodelocation") && doc.getString("barcodelocation").length() != 0) {
                        logger.debug("[{}] Skipped!", content);
                        continue;
                    }
                    logger.debug("[{}] Processing...", content);

                    try {
                        String path = uploadToStorage(content, "svg", BarcodeGenerator.writeSvgToByteStream(content));
                        writeReferenceOnFirestore(path, doc.getId());
                        logger.info("[{}] Generated new QR-Code", content);

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    public void writeReferenceOnFirestore(String path, String doc) {
        Map<String, Object> update = new HashMap<>();
        update.put("barcodelocation", path);
        ApiFuture<WriteResult> writeResult = firestore.collection("Registrations")
                .document(doc)
                .set(update, SetOptions.merge());
    }
}
