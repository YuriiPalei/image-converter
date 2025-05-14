package palei.yurii.imageconverter.core.converter.webp;

import java.io.File;
import palei.yurii.imageconverter.core.converter.AbstractWebPConverter;
import palei.yurii.imageconverter.core.converter.ImageConverter;
import palei.yurii.imageconverter.model.ConversionResult;
import palei.yurii.imageconverter.model.ConversionStrategy;

public class HybridWebPConverter extends AbstractWebPConverter {
  private ImageConverter primaryConverter;
  private final ImageConverter fallbackConverter;

  public HybridWebPConverter() {
    // Try to initialize the TwelveMonkeys converter
    try {
      primaryConverter = new TwelveMonkeysWebPConverter();
    } catch (Exception e) {
      primaryConverter = null;
    }

    // Initialize the native executor as fallback
    fallbackConverter = new NativeWebPConverter();
  }

  @Override
  public ConversionResult convert(File inputFile, ConversionStrategy strategy) {
    // Try primary converter first
    if (primaryConverter != null) {
      try {
        ConversionResult result = primaryConverter.convert(inputFile, strategy);
        if (result.isSuccess()) {
          return result;
        }
      } catch (Exception e) {
        // Primary converter failed, fall back to native
      }
    }

    // Use fallback converter
    return fallbackConverter.convert(inputFile, strategy);
  }
}
