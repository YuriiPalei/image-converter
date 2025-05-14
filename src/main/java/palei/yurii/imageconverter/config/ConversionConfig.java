package palei.yurii.imageconverter.config;

import lombok.Getter;

public class ConversionConfig {
  private static final int DEFAULT_MAX_FILES = 5;
  @Getter private static int maxFiles = DEFAULT_MAX_FILES;
}
