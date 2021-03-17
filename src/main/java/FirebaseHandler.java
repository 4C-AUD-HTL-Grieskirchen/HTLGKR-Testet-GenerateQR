import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Bucket;
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
import java.util.HashMap;
import java.util.Map;

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

    public String uploadToStorage(String filename, ByteArrayOutputStream stream) {
        String hash = Hashing.toHexString(Hashing.getSHA(filename));
        String path = "barcodes/" + hash + ".svg";
        logger.debug("Uploading {} to cloud storage...", path);
        bucket.create(path, stream.toByteArray());
        return path;
    }

    // FIRESTORE

    public void setFirestoreListener(EventListener<QuerySnapshot> snapshotEventListener) {
        firestore.collection("Anmeldungen")
                .addSnapshotListener(snapshotEventListener);
    }

    public void setFirestoreListener() {
        setFirestoreListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirestoreException error) {
                if (error != null || value == null)
                    return;

                for (DocumentSnapshot doc : value) {
                    logger.debug("Processing document with id: {}", doc.getId());
                    if (doc.contains("barcodelocation")) {
                        logger.debug("Skipping {}... (barcodelocation already defined)");
                        continue;
                    }

                    try {
                        String content = doc.get("barcodecontent").toString();
                        if (content == null)
                            continue;

                        String path = uploadToStorage(content, BarcodeGenerator.writeSvgToByteStream(content));
                        writeReferenceOnFirestore(path, doc.getId());
                        logger.info("Generated a new barcode for {}! (id: {}, barcode: {})", doc.get("name"), doc.getId(), content);

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
        ApiFuture<WriteResult> writeResult = firestore.collection("Anmeldungen")
                .document(doc)
                .set(update, SetOptions.merge());
    }
}