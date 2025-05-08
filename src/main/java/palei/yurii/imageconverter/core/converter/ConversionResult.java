package palei.yurii.imageconverter.core.converter;

import java.io.File;

public class ConversionResult {
  private final boolean success;
  private final File inputFile;
  private final File outputFile;
  private final long convertedSize;
  private final String errorMessage;
  private final double reductionPercentage;

  public ConversionResult(
      boolean success, File inputFile, File outputFile, long convertedSize, String errorMessage) {
    this.success = success;
    this.inputFile = inputFile;
    this.outputFile = outputFile;
    this.convertedSize = convertedSize;
    this.errorMessage = errorMessage;

    if (success && inputFile != null) {
      long originalSize = inputFile.length();
      this.reductionPercentage =
          originalSize > 0 ? ((originalSize - convertedSize) / (double) originalSize) * 100.0 : 0.0;
    } else {
      this.reductionPercentage = 0.0;
    }
  }

  public boolean isSuccess() {
    return success;
  }

  public File getInputFile() {
    return inputFile;
  }

  public File getOutputFile() {
    return outputFile;
  }

  public long getConvertedSize() {
    return convertedSize;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public double getReductionPercentage() {
    return reductionPercentage;
  }
}
