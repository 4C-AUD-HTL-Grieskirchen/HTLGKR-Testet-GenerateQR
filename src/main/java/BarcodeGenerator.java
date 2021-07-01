import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.okapibarcode.backend.HumanReadableLocation;
import uk.org.okapibarcode.backend.QrCode;
import uk.org.okapibarcode.output.SvgRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class BarcodeGenerator {

    // Logger
    private static final Logger logger = LoggerFactory.getLogger(BarcodeGenerator.class);

    // Config
    public static String FontName = "Monospaced";
    public static int FontSize = 24;
    public static int Width = 100;
    public static int Height = 100;

    public static ByteArrayOutputStream writeSvgToByteStream(String barcodeContents) throws IOException {
        logger.debug("Initializing new Barcode with values:");
        logger.debug("Font: {}", FontName);
        logger.debug("Size: {}", FontSize);
        logger.debug("Width: {}", Width);
        logger.debug("Height: {}", Height);
        logger.debug("Content: {}", barcodeContents);

        QrCode barcode = new QrCode();
        barcode.setFontName(FontName);
        barcode.setFontSize(FontSize);
        // barcode.setModuleWidth(Width);
        // barcode.setBarHeight(Height);
        barcode.setHumanReadableLocation(HumanReadableLocation.BOTTOM);
        barcode.setContent(barcodeContents);
        barcode.setPreferredEccLevel(QrCode.EccLevel.H);
        logger.debug("Barcode created!");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SvgRenderer renderer = new SvgRenderer(out, 1, Color.WHITE, Color.BLACK, true);
        renderer.render(barcode);
        logger.debug("Rendered new barcode!");
        return out;
    }
}
