package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.imageio.ImageIO;

public class PngFormat extends SimpleFormat {
   protected PngFormat() {
   }

   public String getMimeType() {
      return "image/png";
   }

   public boolean supportsMultipleFrames() {
      return false;
   }

   public boolean supportsFullyTransparentPixels() {
      return true;
   }

   public boolean supportsPartiallyTransparentPixels() {
      return true;
   }

   public void writeImage(List<BufferedImage> frames, OutputStream out) throws IOException {
      if (frames.size() > 1) {
         throw new IllegalArgumentException("Cannot render animations in PNG format");
      } else {
         ImageIO.write((RenderedImage)frames.get(0), "png", out);
      }
   }
}
