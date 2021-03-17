import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        try {
            BarcodeGenerator generator = new BarcodeGenerator();
            FirebaseHandler firebase = new FirebaseHandler("key.json");
            firebase.setFirestoreListener(generator);

            Scanner sc = new Scanner(System.in);
            while (!sc.nextLine().equals("exit")) ; // Prevent program from exiting
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}