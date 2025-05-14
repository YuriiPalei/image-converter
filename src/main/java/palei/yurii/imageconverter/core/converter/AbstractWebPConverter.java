package palei.yurii.imageconverter.core.converter;

import java.io.File;
import java.util.List;

public abstract class AbstractWebPConverter implements ImageConverter {
  protected static String FORMAT = "webp";
  protected static List<String> SUPPORTED_FORMATS = List.of("jpg", "jpeg", "png");

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

  protected String getFileExtension(File file) {
    String name = file.getName();
    int pos = name.lastIndexOf(".");
    return pos > 0 ? name.substring(pos + 1).toLowerCase() : "";
  }

  protected String getFileNameWithoutExtension(File file) {
    String name = file.getName();
    int pos = name.lastIndexOf(".");
    return pos > 0 ? name.substring(0, pos) : name;
  }
}
