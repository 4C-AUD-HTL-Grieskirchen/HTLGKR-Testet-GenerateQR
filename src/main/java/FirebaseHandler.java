import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import com.google.firebase.database.*;
import com.google.zxing.WriterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
        logger.debug("Initializing Firebase with values: {}; {}; {};", projectId, bucketAddress, databaseURL);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(service))
                .setDatabaseUrl(databaseURL)
                .setStorageBucket(bucketAddress)
                .build();
        FirebaseApp.initializeApp(options);

        db = FirebaseDatabase.getInstance().getReference();
        firestore = FirestoreClient.getFirestore();
        bucket = StorageClient.getInstance().bucket();
    }

    // CLOUD STORAGE

    public String uploadToStorage(String filename, ByteArrayOutputStream stream) {
        String hash = Hashing.toHexString(Hashing.getSHA(filename));
        String path = "barcodes/" + hash + ".png";
        bucket.create(path, stream.toByteArray());
        logger.debug("Uploaded to storage: {}", path);

        return path;
    }

    // FIRESTORE

    public void setFirestoreListener(EventListener<DocumentSnapshot> snapshotEventListener) {
        firestore.collection("barcodes")
                .document("queued")
                .addSnapshotListener(snapshotEventListener);
    }

    public void setFirestoreListener(BarcodeGenerator barcode) {
        setFirestoreListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirestoreException error) {
                if (error != null) {
                    logger.error(error.getMessage());
                    return;
                }

                if (value != null && value.exists()) {
                    Map<String, Object> data = value.getData();
                    logger.debug("Firestore: {}", data);

                    data.forEach((key, val) -> {
                        try {
                            String v = val.toString();
                            String path = uploadToStorage(v, barcode.writeToByteStream(v));
                            writeReferenceOnFirestore(v, path);
                            removeQueuedFromFirestore(key);

                        } catch (IOException | WriterException ex) {
                            logger.error(ex.getMessage());
                        }
                    });
                } else {
                    logger.debug("Firestore: No data found");
                }
            }
        });
    }

    public void removeQueuedFromFirestore(String key) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put(key, FieldValue.delete());

            ApiFuture<WriteResult> writeResult = firestore.collection("barcodes")
                    .document("queued")
                    .update(update);

            logger.debug("Firestore: Item [{}] processed; time: {}", key, writeResult.get().getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void writeReferenceOnFirestore(String content, String path) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put(content, path);
            ApiFuture<WriteResult> writeResult = firestore.collection("barcodes")
                    .document("generated")
                    .set(update, SetOptions.merge());

            logger.debug("Wrote on Firestore: time: {}; data: ({}: {})", writeResult.get().getUpdateTime(), content, path);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    // REALTIME DATABASE

    @Deprecated
    public void setRealtimeListener(ChildEventListener eventListener) {
        db.child("queued-barcodes").addChildEventListener(eventListener);
    }

    @Deprecated
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
                    removeQueuedFromRealtime(snapshot.getKey());

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

    @Deprecated
    public void writeReferenceOnRealtime(String content, String path) {
        db.child("generated-barcodes").child(content).setValue(path, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                logger.debug("Wrote on realtime: ({}: {})", content, path);
            }
        });
    }

    @Deprecated
    public void removeQueuedFromRealtime(String key) {
        db.child("queued-barcodes").child(key).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                logger.info("Item [{}] processed!", key);
            }
        });
    }
}
