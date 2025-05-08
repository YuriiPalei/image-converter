package palei.yurii.imageconverter.config;

public class ConversionConfig {
  private static final int DEFAULT_MAX_FILES = 5;
  private static int maxFiles = DEFAULT_MAX_FILES;

  public static int getMaxFiles() {
    return maxFiles;
  }

  public static void setMaxFiles(int value) {
    if (value <= 0) {
      throw new IllegalArgumentException("Max files must be positive");
    }
    maxFiles = value;
  }
}
