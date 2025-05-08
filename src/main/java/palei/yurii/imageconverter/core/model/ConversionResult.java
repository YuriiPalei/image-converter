package palei.yurii.imageconverter.core.model;

import java.io.File;

public class ConversionResult {
  private final File inputFile;
  private final File outputFile;
  private final boolean success;
  private final String error;
  private final long originalSize;
  private final long convertedSize;

  public ConversionResult(
      File inputFile,
      File outputFile,
      boolean success,
      String error,
      long originalSize,
      long convertedSize) {
    this.inputFile = inputFile;
    this.outputFile = outputFile;
    this.success = success;
    this.error = error;
    this.originalSize = originalSize;
    this.convertedSize = convertedSize;
  }

  public File getInputFile() {
    return inputFile;
  }

  public File getOutputFile() {
    return outputFile;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getError() {
    return error;
  }

  public long getOriginalSize() {
    return originalSize;
  }

  public long getConvertedSize() {
    return convertedSize;
  }

  public double getReductionPercentage() {
    if (originalSize == 0) return 0;
    return ((originalSize - convertedSize) / (double) originalSize) * 100;
  }
}
