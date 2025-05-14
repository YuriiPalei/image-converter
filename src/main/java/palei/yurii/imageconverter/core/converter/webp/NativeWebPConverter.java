package palei.yurii.imageconverter.core.converter.webp;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import palei.yurii.imageconverter.core.converter.AbstractWebPConverter;
import palei.yurii.imageconverter.model.ConversionResult;
import palei.yurii.imageconverter.model.ConversionStrategy;

public class NativeWebPConverter extends AbstractWebPConverter {
  private final String cwebpPath;

  public NativeWebPConverter() {
    cwebpPath = determineExecutablePath();
  }

  @Override
  public ConversionResult convert(File inputFile, ConversionStrategy strategy) {
    if (!supportsFormat(getFileExtension(inputFile))) {
      return new ConversionResult(
          false,
          inputFile,
          null,
          inputFile.length(),
          0L,
          "Unsupported input format: " + getFileExtension(inputFile));
    }

    File outputFile =
        new File(inputFile.getParent(), getFileNameWithoutExtension(inputFile) + "." + FORMAT);

    try {
      ProcessBuilder pb =
          new ProcessBuilder(
              cwebpPath,
              "-q",
              "80", // Quality setting (0-100)
              inputFile.getAbsolutePath(),
              "-o",
              outputFile.getAbsolutePath());

      Process process = pb.start();
      int exitCode = process.waitFor();

      if (exitCode != 0) {
        // Read error output
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
          String errorOutput = reader.lines().collect(Collectors.joining("\n"));
          return new ConversionResult(
              false,
              inputFile,
              null,
              inputFile.length(),
              0L,
              "cwebp process failed with exit code " + exitCode + ": " + errorOutput);
        }
      }

      if (!outputFile.exists() || outputFile.length() == 0) {
        return new ConversionResult(
            false,
            inputFile,
            null,
            inputFile.length(),
            0L,
            "Output file was not created or is empty");
      }

      var result =
          new ConversionResult(
              true, inputFile, outputFile, inputFile.length(), outputFile.length(), null);

      if (strategy == ConversionStrategy.SIMULATION) {
        outputFile.delete();
      }

      return result;

    } catch (IOException | InterruptedException e) {
      return new ConversionResult(
          false,
          inputFile,
          null,
          inputFile.length(),
          0L,
          "Error executing cwebp: " + e.getMessage());
    }
  }

  private String determineExecutablePath() {
    String osName = System.getProperty("os.name").toLowerCase();
    String osArch = System.getProperty("os.arch").toLowerCase();
    String resourcePath = getResourcePath(osName, osArch);

    // Extract executable to a temporary file
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IOException("❌ Resource not found: " + resourcePath);
      }

      File tempDir = new File(System.getProperty("java.io.tmpdir"), "webp-converter");
      tempDir.mkdirs();

      String executableName = new File(resourcePath).getName();
      File executableFile = new File(tempDir, executableName);

      Files.copy(is, executableFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      executableFile.setExecutable(true);

      return executableFile.getAbsolutePath();
    } catch (IOException e) {
      throw new RuntimeException("Failed to extract WebP executable", e);
    }
  }

  private static @NotNull String getResourcePath(String osName, String osArch) {
    String resourcePath;

    if (osName.contains("mac")) {
      if (osArch.contains("aarch64") || osArch.contains("arm64")) {
        resourcePath = "/custom-native/mac/arm64/cwebp";
      } else {
        resourcePath = "/custom-native/mac/x86_64/cwebp";
      }
    } else if (osName.contains("win")) {
      resourcePath = "/custom-native/windows/cwebp.exe";
    } else {
      // Linux or other
      if (osArch.contains("aarch64") || osArch.contains("arm64")) {
        resourcePath = "/custom-native/linux/arm64/cwebp";
      } else {
        resourcePath = "/custom-native/linux/x86_64/cwebp";
      }
    }
    return resourcePath;
  }
}
