import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.zxing.WriterException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    // Logging
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

        try {
            logger.info("Init barcode generator");
            BarcodeGenerator generator = new BarcodeGenerator();
            logger.info("Init firebase");
            FirebaseHandler firebase = new FirebaseHandler("key.json");
            logger.info("Setting listeners");
            firebase.setRealtimeListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    if (snapshot == null)
                        return;

                    String value = snapshot.getValue().toString();
                    logger.info("Processing new barcode for value: {}", value);

                    try {
                        firebase.uploadToStorage(value, generator.writeToByteStream(value));
                        firebase.removeQueued(snapshot.getKey());

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

            logger.info("Running! Type 'exit' to terminate the progress.");
            Scanner sc = new Scanner(System.in);
            while (!sc.nextLine().equals("exit")) ; // Prevent program from exiting
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
