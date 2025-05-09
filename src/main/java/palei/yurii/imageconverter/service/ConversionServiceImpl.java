package palei.yurii.imageconverter.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import palei.yurii.imageconverter.model.ConversionResult;
import palei.yurii.imageconverter.core.converter.ImageConverter;
import palei.yurii.imageconverter.core.converter.WebPConverter;
import palei.yurii.imageconverter.model.ConversionStrategy;

public class ConversionServiceImpl implements ConversionService {
  private final Map<String, ImageConverter> converters;
  private final ExecutorService executorService;
  private final ImageConverter webpConverter;

  public ConversionServiceImpl() {
    this.converters = new ConcurrentHashMap<>();
    this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    this.webpConverter = new WebPConverter();

    registerConverter(new WebPConverter());
  }

  private void registerConverter(ImageConverter converter) {
    converters.put(converter.getTargetFormat(), converter);
  }

  @Override
  public List<ConversionResult> convertFiles(
      List<File> files, String targetFormat, ConversionStrategy strategy) {
    List<Future<ConversionResult>> futures = new ArrayList<>();
    List<ConversionResult> results = new ArrayList<>();

    try {
      for (File file : files) {
        Future<ConversionResult> future =
            executorService.submit(
                () -> {
                  if ("webp".equalsIgnoreCase(targetFormat)) {
                    return webpConverter.convert(file, strategy);
                  }
                  return new ConversionResult(
                      false, file, null, file.length(), 0L, "Unsupported format: " + targetFormat);
                });
        futures.add(future);
      }

      for (Future<ConversionResult> future : futures) {
        try {
          results.add(future.get(30, TimeUnit.SECONDS));
        } catch (Exception e) {
          results.add(
              new ConversionResult(
                  false, null, null, 0L, 0L, "Conversion failed: " + e.getMessage()));
        }
      }
    } catch (Exception e) {
      results.add(
          new ConversionResult(false, null, null, 0L, 0L, "Service error: " + e.getMessage()));
    }

    return results;
  }

  @Override
  public boolean isFormatSupported(String format) {
    var supportedFormats = webpConverter.getSupportedFormats();
    return supportedFormats.contains(format.toLowerCase());
  }

  @Override
  public List<String> getSupportedFormats() {
    return new ArrayList<>(converters.keySet());
  }

  @Override
  public void close() {
    if (executorService != null) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
