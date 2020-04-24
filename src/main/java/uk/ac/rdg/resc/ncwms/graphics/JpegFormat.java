package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.imageio.ImageIO;

public class JpegFormat extends SimpleFormat {
   protected JpegFormat() {
   }

   public String getMimeType() {
      return "image/jpeg";
   }

   public boolean supportsMultipleFrames() {
      return false;
   }

   public boolean supportsFullyTransparentPixels() {
      return false;
   }

   public boolean supportsPartiallyTransparentPixels() {
      return false;
   }

   public void writeImage(List<BufferedImage> frames, OutputStream out) throws IOException {
      if (frames.size() > 1) {
         throw new IllegalArgumentException("Cannot render animations in JPEG format");
      } else {
         ImageIO.write((RenderedImage)frames.get(0), "jpeg", out);
      }
   }
}
