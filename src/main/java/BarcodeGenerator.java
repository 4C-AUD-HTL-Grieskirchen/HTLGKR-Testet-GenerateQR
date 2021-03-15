import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code39Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class BarcodeGenerator {

    // Logging
    private final Logger logger = LoggerFactory.getLogger(BarcodeGenerator.class);

    // Config
    public BarcodeFormat format;

    // Dimensions
    public int width;
    public int height;

    private Code39Writer writer;

    public BarcodeGenerator() {
        writer = new Code39Writer();
        format = BarcodeFormat.CODE_39;
        width = 1024;
        height = 200;
    }

    public ByteArrayOutputStream writeToByteStream(String barcodeContents) throws WriterException, IOException {
        logger.debug("Generating new barcode: {}", barcodeContents);

        // Generate barcode
        BitMatrix matrix = writer.encode(barcodeContents, format, width, height);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", stream);

        return stream;
    }
}
