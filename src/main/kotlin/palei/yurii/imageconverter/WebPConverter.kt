import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream

object WebPConverter {
    @Throws(Exception::class)
    fun convertToWebP(inputFile: File, outputFile: File) {
        val image: BufferedImage = ImageIO.read(inputFile)

        val writers: Iterator<ImageWriter> = ImageIO.getImageWritersByFormatName("webp")
        if (!writers.hasNext()) {
            throw IllegalStateException("No WebP writers found")
        }

        val writer: ImageWriter = writers.next()
        val ios: ImageOutputStream = ImageIO.createImageOutputStream(outputFile)
        writer.output = ios

        writer.write(null, javax.imageio.IIOImage(image, null, null), null)

        ios.close()
        writer.dispose()
    }
}