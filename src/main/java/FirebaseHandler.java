import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import com.google.firebase.database.*;
import com.google.zxing.WriterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class FirebaseHandler {

    // Logging
    private final Logger logger = LoggerFactory.getLogger(FirebaseHandler.class);

    // Config
    public String projectId;
    public String bucketAddress;
    public String databaseURL;

    private Bucket bucket;
    private FileInputStream service;
    private DatabaseReference db;

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
        logger.debug("Initializing Firebase with values: {}; {}; {};", projectId, bucketAddress, databaseURL);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(service))
                .setDatabaseUrl(databaseURL)
                .setStorageBucket(bucketAddress)
                .build();
        FirebaseApp.initializeApp(options);

        db = FirebaseDatabase.getInstance().getReference();
        bucket = StorageClient.getInstance().bucket();
    }

    public void setRealtimeListener(ChildEventListener eventListener) {
        db.child("queued-barcodes").addChildEventListener(eventListener);
    }

    public void setRealtimeListenerDefault(BarcodeGenerator barcode) {
        setRealtimeListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                if (snapshot == null)
                    return;

                String value = snapshot.getValue().toString();
                logger.info("Processing new barcode for value: {}", value);

                try {
                    String path = uploadToStorage(value, barcode.writeToByteStream(value));
                    writeReferenceOnRealtime(value, path);
                    removeQueued(snapshot.getKey());

                } catch (IOException | WriterException ex) {
                    logger.error(ex.getMessage());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    public String uploadToStorage(String filename, ByteArrayOutputStream stream) {
        String hash = Hashing.toHexString(Hashing.getSHA(filename));
        String path = "barcodes/" + hash + ".png";
        bucket.create(path, stream.toByteArray());
        logger.debug("Uploaded to storage: {}", path);

        return path;
    }

    public void writeReferenceOnRealtime(String content, String path) {
        db.child("generated-barcodes").push().child(content).setValue(path, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                logger.debug("Wrote on realtime: ({}: {})", content, path);
            }
        });
    }

    public void removeQueued(String key) {
        db.child("queued-barcodes").child(key).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                logger.info("Item [{}] processed!", key);
            }
        });
    }
}
