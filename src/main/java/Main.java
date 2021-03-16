import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class Main {

    // Logging
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

        try {
            logger.info("Init barcode generator...");
            BarcodeGenerator generator = new BarcodeGenerator();
            logger.info("Init firebase...");
            FirebaseHandler firebase = new FirebaseHandler("key.json");
            logger.info("Setting listeners...");
            firebase.setFirestoreListener(generator);

            logger.info("Running! Type 'exit' to terminate the progress.");
            Scanner sc = new Scanner(System.in);
            while (!sc.nextLine().equals("exit")) ; // Prevent program from exiting
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
