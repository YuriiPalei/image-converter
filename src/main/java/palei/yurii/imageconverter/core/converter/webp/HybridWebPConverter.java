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
    try {
      primaryConverter = new NativeWebPConverter();
    } catch (Exception e) {
      primaryConverter = null;
    }

    fallbackConverter = new TwelveMonkeysWebPConverter();
  }

  @Override
  public ConversionResult convert(File inputFile, ConversionStrategy strategy) {
    if (primaryConverter != null) {
      System.out.println("Using Native converter");
      try {
        ConversionResult result = primaryConverter.convert(inputFile, strategy);
        if (result.isSuccess()) {
          return result;
        }
      } catch (Exception e) {
        // Primary converter failed, fall back to native
      }
    }

    System.out.println("Using TwelveMonkeys converter");
    return fallbackConverter.convert(inputFile, strategy);
  }
}
