import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

        try {
            String keyFile = "key.json"; // Path to key file

            logger.debug("Initializing FirebaseHandler with key file: {}", keyFile);
            FirebaseHandler firebase = new FirebaseHandler(keyFile);
            firebase.setFirestoreListener();

            Scanner sc = new Scanner(System.in);
            System.out.println();
            System.out.println("    //   ) )                                                       //   ) )                  ");
            System.out.println("   //___/ /   ___      __      ___      ___      ___   /  ___     //         ___       __    ");
            System.out.println("  / __  (   //   ) ) //  ) ) //   ) ) //   ) ) //   ) / //___) ) //  ____  //___) ) //   ) ) ");
            System.out.println(" //    ) ) //   / / //      //       //   / / //   / / //       //    / / //       //   / /  ");
            System.out.println("//____/ / ((___( ( //      ((____   ((___/ / ((___/ / ((____   ((____/ / ((____   //   / /   ");
            System.out.println();

            logger.info("Startup complete! Type 'exit' to stop the program:");
            while (!sc.nextLine().equals("exit")) ; // Prevent program from exiting
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}