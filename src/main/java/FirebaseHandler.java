import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

        // Firebase
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

    public String uploadToStorage(String content, ByteArrayOutputStream stream) {

        // Upload to firebase storage
        String hash = Hashing.toHexString(Hashing.getSHA(content));
        String path = "barcodes/" + hash + ".png";
        Blob blob = bucket.create(path, stream.toByteArray());
        logger.debug("Uploaded to storage: {}", path);

        // Store key-value in firebase realtime
        db.child("generated-barcodes").push().child(content).setValue(path, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                logger.debug("Wrote on realtime: ({}: {})", content, path);
            }
        });

        return path;
    }

    public void removeQueued(String key) {
        // Delete from firebase realtime
        db.child("queued-barcodes").child(key).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                logger.info("Item [{}] processed!", key);
            }
        });
    }
}
