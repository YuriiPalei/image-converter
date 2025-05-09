package palei.yurii.imageconverter.core.converter;

import palei.yurii.imageconverter.model.ConversionResult;
import palei.yurii.imageconverter.model.ConversionStrategy;

import java.io.File;
import java.util.List;

public interface ImageConverter {
  ConversionResult convert(File inputFile, ConversionStrategy strategy);

  boolean supportsFormat(String format);

  String getTargetFormat();

  List<String> getSupportedFormats();
}
