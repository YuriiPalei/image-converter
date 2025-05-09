package palei.yurii.imageconverter.model;

import lombok.Getter;

import java.io.File;

@Getter
public class ConversionResult {
  private final boolean success;
  private final File inputFile;
  private final File outputFile;
  private final long convertedSize;
  private final String errorMessage;
  private final double reductionPercentage;

  public ConversionResult(
      boolean success,
      File inputFile,
      File outputFile,
      long originalSize,
      long convertedSize,
      String errorMessage) {
    this.success = success;
    this.inputFile = inputFile;
    this.outputFile = outputFile;
    this.convertedSize = convertedSize;
    this.errorMessage = errorMessage;

    if (success && originalSize > 0) {
      this.reductionPercentage = ((originalSize - convertedSize) / (double) originalSize) * 100.0;
    } else {
      this.reductionPercentage = 0.0;
    }
  }
}
