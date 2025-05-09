package palei.yurii.imageconverter.service;

import palei.yurii.imageconverter.model.ConversionResult;
import palei.yurii.imageconverter.model.ConversionStrategy;

import java.io.File;
import java.util.List;

public interface ConversionService extends AutoCloseable {
  List<ConversionResult> convertFiles(
      List<File> files, String targetFormat, ConversionStrategy strategy);

  boolean isFormatSupported(String format);

  List<String> getSupportedFormats();
}
