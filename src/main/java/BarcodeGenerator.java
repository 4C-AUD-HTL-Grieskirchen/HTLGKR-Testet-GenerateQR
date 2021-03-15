//import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.*;

public class BarcodeGenerator {

    // Barcode
    private QRCodeWriter writer;
    public BarcodeFormat format = BarcodeFormat.CODE_39;

    // Dimensions
    public int width = 1024;
    public int height = 200;

    public BarcodeGenerator() {
        writer = new QRCodeWriter();
    }

    public String writeToFirebase(String barcodeContents) throws WriterException, IOException {
        // Generate barcode
        BitMatrix matrix = writer.encode(barcodeContents, format, width, height);
        OutputStream stream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "SVG", stream);

        // Upload to firebase

        return ""; // Path to final file
    }
}
