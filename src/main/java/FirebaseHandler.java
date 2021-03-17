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

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHandler {

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
                if (error != null)
                    return;

                if (value != null && value.exists()) {
                    Map<String, Object> data = value.getData();

                    data.forEach((key, val) -> {
                        try {
                            String v = val.toString();
                            String path = uploadToStorage(v, barcode.writeToByteStream(v));
                            writeReferenceOnFirestore(v, path);
                            removeQueuedFromFirestore(key);

                        } catch (IOException | WriterException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    public void removeQueuedFromFirestore(String key) {
        Map<String, Object> update = new HashMap<>();
        update.put(key, FieldValue.delete());

        ApiFuture<WriteResult> writeResult = firestore.collection("barcodes")
                .document("queued")
                .update(update);
    }

    public void writeReferenceOnFirestore(String content, String path) {
        Map<String, Object> update = new HashMap<>();
        update.put(content, path);
        ApiFuture<WriteResult> writeResult = firestore.collection("barcodes")
                .document("generated")
                .set(update, SetOptions.merge());
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

                try {
                    String path = uploadToStorage(value, barcode.writeToByteStream(value));
                    writeReferenceOnRealtime(value, path);
                    removeQueuedFromRealtime(snapshot.getKey());

                } catch (IOException | WriterException ex) {
                    ex.printStackTrace();
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
            }
        });
    }

    @Deprecated
    public void removeQueuedFromRealtime(String key) {
        db.child("queued-barcodes").child(key).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
            }
        });
    }
}