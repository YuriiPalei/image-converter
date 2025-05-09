package palei.yurii.imageconverter.core.converter;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import palei.yurii.imageconverter.model.ConversionStrategy;

public class WebPConverterTest {
  private final WebPConverter webPConverter;

  public WebPConverterTest() {
    this.webPConverter = new WebPConverter();
  }

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void convertToWebP_success() throws Exception {
    IIORegistry registry = IIORegistry.getDefaultInstance();
    registry.registerServiceProvider(new TestWebPWriterSpi());
    ImageIO.scanForPlugins();
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    File inputFile = tempFolder.newFile("test.png");
    ImageIO.write(image, "PNG", inputFile);

    var result = this.webPConverter.convert(inputFile, ConversionStrategy.REAL);

    assertTrue("Conversion should be successful", result.isSuccess());
    assertTrue("Output file should exist", result.getOutputFile().exists());
    assertTrue("Output file should not be empty", result.getOutputFile().length() > 0);
  }

  @Test
  public void convertToWebP_noWriter_returnsError() throws Exception {
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

      var result = this.webPConverter.convert(inputFile, ConversionStrategy.REAL);

      assertFalse("Conversion should fail", result.isSuccess());
      assertEquals(
          "Error message should indicate no WebP writers",
          "No WebP writers found",
          result.getErrorMessage());
    } finally {
      for (ImageWriterSpi spi : providers) {
        registry.registerServiceProvider(spi);
      }
    }
  }

  @Test
  public void supportsFormat_validFormats_returnsTrue() {
    assertTrue(webPConverter.supportsFormat("jpg"));
    assertTrue(webPConverter.supportsFormat("jpeg"));
    assertTrue(webPConverter.supportsFormat("png"));
    assertTrue(webPConverter.supportsFormat("PNG"));
    assertTrue(webPConverter.supportsFormat("JPG"));
  }

  @Test
  public void supportsFormat_invalidFormats_returnsFalse() {
    assertFalse(webPConverter.supportsFormat("webp"));
    assertFalse(webPConverter.supportsFormat("gif"));
    assertFalse(webPConverter.supportsFormat("bmp"));
    assertFalse(webPConverter.supportsFormat(null));
    assertFalse(webPConverter.supportsFormat(""));
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
    private ImageWriteParam writeParam;

    protected TestWebPImageWriter(ImageWriterSpi originatingProvider) {
      super(originatingProvider);
      writeParam = new TestWebPWriteParam();
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
    public ImageWriteParam getDefaultWriteParam() {
      return writeParam;
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param)
        throws IOException {
      ImageOutputStream ios = (ImageOutputStream) getOutput();
      ios.write(new byte[] {0x1, 0x2, 0x3, 0x4});
    }
  }

  private static class TestWebPWriteParam extends ImageWriteParam {
    public TestWebPWriteParam() {
      super();
      canWriteCompressed = true;
      compressionMode = MODE_EXPLICIT;
      compressionTypes = new String[] {"DEFAULT"};
      compressionType = compressionTypes[0];
    }
  }
}
