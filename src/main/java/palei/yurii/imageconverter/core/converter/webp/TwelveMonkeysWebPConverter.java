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

package palei.yurii.imageconverter.core.converter.webp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import palei.yurii.imageconverter.core.converter.AbstractWebPConverter;
import palei.yurii.imageconverter.model.ConversionResult;
import palei.yurii.imageconverter.model.ConversionStrategy;

/** WebP converter implementation using TwelveMonkeys ImageIO library. */
public class TwelveMonkeysWebPConverter extends AbstractWebPConverter {
  private static final Logger LOGGER = Logger.getLogger(TwelveMonkeysWebPConverter.class.getName());

  static {
    // Ensure ImageIO is properly initialized
    ImageIO.scanForPlugins();
    LOGGER.info("Available writers: " + Arrays.toString(ImageIO.getWriterFormatNames()));
    LOGGER.info("Available readers: " + Arrays.toString(ImageIO.getReaderFormatNames()));
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
      BufferedImage inputImage = ImageIO.read(inputFile);
      if (inputImage == null) {
        return new ConversionResult(
            false,
            inputFile,
            null,
            inputFile.length(),
            0L,
            "Failed to read input image, format may not be supported");
      }

      Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(FORMAT);
      if (!writers.hasNext()) {
        return new ConversionResult(
            false, inputFile, null, inputFile.length(), 0L, "No WebP writers found");
      }

      ImageWriter writer = writers.next();

      try (FileImageOutputStream output = new FileImageOutputStream(outputFile)) {
        writer.setOutput(output);

        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        if (writeParam.canWriteCompressed()) {
          try {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(0.8f); // 80% quality
          } catch (UnsupportedOperationException e) {
            // Some writers might not support compression even if canWriteCompressed is true
            LOGGER.warning("Compression settings not supported: " + e.getMessage());
          }
        }

        writer.write(null, new IIOImage(inputImage, null, null), writeParam);
        writer.dispose(); // Clean up resources

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
      }
    } catch (IOException e) {
      LOGGER.severe("Error during WebP conversion: " + e.getMessage());
      e.printStackTrace();

      // Clean up partial output if it exists
      if (outputFile.exists()) {
        outputFile.delete();
      }

      return new ConversionResult(
          false, inputFile, null, inputFile.length(), 0L, "Conversion failed: " + e.getMessage());
    }
  }
}
