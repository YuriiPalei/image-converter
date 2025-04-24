package palei.yurii.imageconverter.core;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class WebPConverter {
  private WebPConverter() {
    // Private constructor to prevent instantiation
  }

  public static void convertToWebP(File inputFile, File outputFile) throws Exception {
    BufferedImage image = ImageIO.read(inputFile);

    var writers = ImageIO.getImageWritersByFormatName("webp");
    if (!writers.hasNext()) {
      throw new IllegalStateException("No WebP writers found");
    }

    ImageWriter writer = writers.next();
    ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile);
    writer.setOutput(ios);

    writer.write(null, new IIOImage(image, null, null), null);

    ios.close();
    writer.dispose();
  }
}
