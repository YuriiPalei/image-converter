package palei.yurii.imageconverter.service;

import palei.yurii.imageconverter.core.converter.ConversionResult;
import java.io.File;
import java.util.List;

public interface ConversionService extends AutoCloseable {
  List<ConversionResult> convertFiles(List<File> files, String targetFormat);

  boolean isFormatSupported(String format);

  List<String> getSupportedFormats();
}
