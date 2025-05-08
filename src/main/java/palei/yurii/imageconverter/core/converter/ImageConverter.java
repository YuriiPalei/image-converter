package palei.yurii.imageconverter.core.converter;

import java.io.File;
import java.util.List;

public interface ImageConverter {
  ConversionResult convert(File inputFile);

  boolean supportsFormat(String format);

  String getTargetFormat();

  List<String> getSupportedFormats();
}
