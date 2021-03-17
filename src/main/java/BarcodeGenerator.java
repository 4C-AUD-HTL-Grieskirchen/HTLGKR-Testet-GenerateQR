import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.okapibarcode.backend.Code3Of9;
import uk.org.okapibarcode.backend.HumanReadableLocation;
import uk.org.okapibarcode.output.SvgRenderer;

import java.awt.*;
import java.io.*;

public class BarcodeGenerator {

    // Logger
    private static final Logger logger = LoggerFactory.getLogger(BarcodeGenerator.class);

    // Config
    public static String FontName = "Monospaced";
    public static int FontSize = 24;
    public static int Width = 2;
    public static int Height = 100;

    public static ByteArrayOutputStream writeSvgToByteStream(String barcodeContents) throws IOException {
        logger.debug("Initializing new Barcode with values:");
        logger.debug("Font: {}", FontName);
        logger.debug("Size: {}", FontSize);
        logger.debug("Width: {}", Width);
        logger.debug("Height: {}", Height);
        logger.debug("Content: {}", barcodeContents);

        Code3Of9 barcode = new Code3Of9();
        barcode.setFontName(FontName);
        barcode.setFontSize(FontSize);
        barcode.setModuleWidth(Width);
        barcode.setBarHeight(Height);
        barcode.setHumanReadableLocation(HumanReadableLocation.BOTTOM);
        barcode.setContent(barcodeContents);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SvgRenderer renderer = new SvgRenderer(out, 1, Color.WHITE, Color.BLACK, true);
        renderer.render(barcode);
        logger.debug("Rendered new barcode!");
        return out;
    }
}