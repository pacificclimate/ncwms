package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Png32Format extends PngFormat {
   protected Png32Format() {
   }

   public String getMimeType() {
      return "image/png;mode=32bit";
   }

   public void writeImage(List<BufferedImage> frames, OutputStream out) throws IOException {
      List<BufferedImage> frames32bit = new ArrayList(frames.size());
      Iterator i$ = frames.iterator();

      while(i$.hasNext()) {
         BufferedImage source = (BufferedImage)i$.next();
         frames32bit.add(convertARGB(source));
      }

      super.writeImage(frames32bit, out);
   }

   private static BufferedImage convertARGB(BufferedImage source) {
      BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), 2);
      ColorConvertOp convertOp = new ColorConvertOp(source.getColorModel().getColorSpace(), dest.getColorModel().getColorSpace(), (RenderingHints)null);
      convertOp.filter(source, dest);
      return dest;
   }
}
