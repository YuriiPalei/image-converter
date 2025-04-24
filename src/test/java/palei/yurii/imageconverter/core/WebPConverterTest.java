package palei.yurii.imageconverter.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.ImageTypeSpecifier;
import java.io.IOException;
import java.util.Locale;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import static org.junit.Assert.*;

public class WebPConverterTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void convertToWebP_success() throws Exception {
    IIORegistry registry = IIORegistry.getDefaultInstance();
    registry.registerServiceProvider(new TestWebPWriterSpi());
    ImageIO.scanForPlugins();
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    File inputFile = tempFolder.newFile("test.png");
    ImageIO.write(image, "PNG", inputFile);

    File outputFile = new File(tempFolder.getRoot(), "test.webp");
    WebPConverter.convertToWebP(inputFile, outputFile);

    assertTrue("Output file should exist", outputFile.exists());
    assertTrue("Output file should not be empty", outputFile.length() > 0);
  }

  @Test(expected = IllegalStateException.class)
  public void convertToWebP_noWriter_throws() throws Exception {
    IIORegistry registry = IIORegistry.getDefaultInstance();
    List<ImageWriterSpi> providers = new ArrayList<>();
    Iterator<ImageWriterSpi> iterator = registry.getServiceProviders(ImageWriterSpi.class, true);
    while (iterator.hasNext()) {
      ImageWriterSpi spi = iterator.next();
      for (String format : spi.getFormatNames()) {
        if ("webp".equalsIgnoreCase(format)) {
          providers.add(spi);
          registry.deregisterServiceProvider(spi);
          break;
        }
      }
    }
    try {
      BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
      File inputFile = tempFolder.newFile("test2.png");
      ImageIO.write(image, "PNG", inputFile);
      File outputFile = new File(tempFolder.getRoot(), "test2.webp");
      WebPConverter.convertToWebP(inputFile, outputFile);
    } finally {
      for (ImageWriterSpi spi : providers) {
        registry.registerServiceProvider(spi);
      }
    }
  }

  // --- Stub SPI и Writer для WebP ---
  private static class TestWebPWriterSpi extends ImageWriterSpi {
    public TestWebPWriterSpi() {
      super(
          "TestVendor",
          "1.0",
          new String[] {"webp"}, // format names
          new String[] {"webp"}, // file suffixes
          new String[] {"image/webp"}, // MIME types
          TestWebPImageWriter.class.getName(),
          new Class[] {ImageOutputStream.class},
          null, // ImageReaderSpi names
          false,
          null,
          null,
          null,
          null,
          false,
          null,
          null,
          null,
          null);
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
      return true;
    }

    @Override
    public ImageWriter createWriterInstance(Object ext) {
      return new TestWebPImageWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
      return "Stub WebP Writer for tests";
    }
  }

  private static class TestWebPImageWriter extends ImageWriter {
    protected TestWebPImageWriter(ImageWriterSpi originatingProvider) {
      super(originatingProvider);
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
      return null;
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(
        ImageTypeSpecifier imageType, ImageWriteParam param) {
      return null;
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
      return inData;
    }

    @Override
    public IIOMetadata convertImageMetadata(
        IIOMetadata imageMetadata, ImageTypeSpecifier type, ImageWriteParam param) {
      return imageMetadata;
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param)
        throws IOException {
      ImageOutputStream ios = (ImageOutputStream) getOutput();
      ios.writeByte(0);
    }
  }
}
