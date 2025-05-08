package palei.yurii.imageconverter.core.converter;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class WebPConverter implements ImageConverter {
  private static final String FORMAT = "webp";
  private static final List<String> SUPPORTED_FORMATS = List.of("jpg", "jpeg", "png");

  @Override
  public ConversionResult convert(File inputFile) {
    if (!supportsFormat(getFileExtension(inputFile))) {
      return new ConversionResult(
          false, inputFile, null, 0, "Unsupported input format: " + getFileExtension(inputFile));
    }

    File outputFile =
        new File(inputFile.getParent(), getFileNameWithoutExtension(inputFile) + "." + FORMAT);

    try {
      BufferedImage image = ImageIO.read(inputFile);
      if (image == null) {
        return new ConversionResult(false, inputFile, null, 0, "Failed to read input image");
      }

      var writers = ImageIO.getImageWritersByFormatName(FORMAT);
      if (!writers.hasNext()) {
        return new ConversionResult(false, inputFile, null, 0, "No WebP writers found");
      }

      var writer = writers.next();
      var param = writer.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionType("DEFAULT");
      param.setCompressionQuality(0.8f);

      var ios = ImageIO.createImageOutputStream(outputFile);
      writer.setOutput(ios);
      writer.write(null, new IIOImage(image, null, null), param);

      ios.close();
      writer.dispose();

      return new ConversionResult(true, inputFile, outputFile, outputFile.length(), null);
    } catch (IOException e) {
      return new ConversionResult(
          false, inputFile, null, 0, "Conversion failed: " + e.getMessage());
    }
  }

  @Override
  public boolean supportsFormat(String format) {
    if (format == null) return false;
    format = format.toLowerCase();
    return SUPPORTED_FORMATS.contains(format);
  }

  @Override
  public String getTargetFormat() {
    return FORMAT;
  }

  @Override
  public List<String> getSupportedFormats() {
    return SUPPORTED_FORMATS;
  }

  private String getFileExtension(File file) {
    String name = file.getName();
    int pos = name.lastIndexOf(".");
    return pos > 0 ? name.substring(pos + 1).toLowerCase() : "";
  }

  private String getFileNameWithoutExtension(File file) {
    String name = file.getName();
    int pos = name.lastIndexOf(".");
    return pos > 0 ? name.substring(0, pos) : name;
  }
}
