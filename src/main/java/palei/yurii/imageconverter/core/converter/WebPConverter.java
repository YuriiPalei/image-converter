/*
 * Copyright 2024 Yurii Palei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file also uses the TwelveMonkeys ImageIO library which is licensed under the BSD 3-Clause License.
 */

package palei.yurii.imageconverter.core.converter;

import palei.yurii.imageconverter.model.ConversionResult;
import palei.yurii.imageconverter.model.ConversionStrategy;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

public class WebPConverter implements ImageConverter {
  private static final String FORMAT = "webp";
  private static final List<String> SUPPORTED_FORMATS = List.of("jpg", "jpeg", "png");

  static {
    ImageIO.scanForPlugins();
    System.out.println(
        "Available image writers: " + Arrays.toString(ImageIO.getWriterFormatNames()));
    System.out.println(
        "Available image readers: " + Arrays.toString(ImageIO.getReaderFormatNames()));
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
      System.out.println("Reading input file: " + inputFile.getAbsolutePath());
      BufferedImage image = ImageIO.read(inputFile);
      if (image == null) {
        System.err.println("Failed to read input image");
        return new ConversionResult(
            false, inputFile, null, inputFile.length(), 0L, "Failed to read input image");
      }
      System.out.println(
          "Successfully read input image: " + image.getWidth() + "x" + image.getHeight());

      System.out.println("Writing image to output file: " + outputFile.getAbsolutePath());
      boolean success = ImageIO.write(image, FORMAT, outputFile);

      if (!success) {
        System.err.println(
            "Failed to write image. Available writers: "
                + Arrays.toString(ImageIO.getWriterFormatNames()));
        return new ConversionResult(
            false, inputFile, null, inputFile.length(), 0L, "Failed to write image");
      }

      if (!outputFile.exists() || outputFile.length() == 0) {
        System.err.println("Output file was not created or is empty");
        return new ConversionResult(
            false,
            inputFile,
            null,
            inputFile.length(),
            0L,
            "Output file was not created or is empty");
      }

      System.out.println(
          "Conversion successful. Output file size: " + outputFile.length() + " bytes");
      var conversionResult =
          new ConversionResult(
              true, inputFile, outputFile, inputFile.length(), outputFile.length(), null);

      if (strategy == ConversionStrategy.SIMULATION) {
        outputFile.delete();
      }

      return conversionResult;
    } catch (IOException e) {
      System.err.println("Conversion failed with error: " + e.getMessage());
      e.printStackTrace();
      if (outputFile.exists()) {
        outputFile.delete();
      }
      return new ConversionResult(
          false, inputFile, null, inputFile.length(), 0L, "Conversion failed: " + e.getMessage());
    }
  }

  @Override
  public boolean supportsFormat(String format) {
    if (format == null) return false;
    format = format.toLowerCase();
    return SUPPORTED_FORMATS.contains(format);
  }

  @Override
  public String getTargetFormat() {
    return FORMAT;
  }

  @Override
  public List<String> getSupportedFormats() {
    return SUPPORTED_FORMATS;
  }

  private String getFileExtension(File file) {
    String name = file.getName();
    int pos = name.lastIndexOf(".");
    return pos > 0 ? name.substring(pos + 1).toLowerCase() : "";
  }

  private String getFileNameWithoutExtension(File file) {
    String name = file.getName();
    int pos = name.lastIndexOf(".");
    return pos > 0 ? name.substring(0, pos) : name;
  }
}
