package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GifFormat extends SimpleFormat {
   private static final Logger logger = LoggerFactory.getLogger(GifFormat.class);

   protected GifFormat() {
   }

   public void writeImage(List<BufferedImage> frames, OutputStream out) throws IOException {
      logger.debug("Writing GIF to output stream ...");
      AnimatedGifEncoder e = new AnimatedGifEncoder();
      e.start(out);
      if (frames.size() > 1) {
         logger.debug("Animated GIF ({} frames), setting loop count and delay", frames.size());
         e.setRepeat(0);
         e.setDelay(150);
      }

      byte[] rgbPalette = null;
      IndexColorModel icm = null;
      Iterator i$ = frames.iterator();

      while(i$.hasNext()) {
         BufferedImage frame = (BufferedImage)i$.next();
         if (rgbPalette == null) {
            e.setSize(frame.getWidth(), frame.getHeight());
            icm = (IndexColorModel)frame.getColorModel();
            rgbPalette = getRGBPalette(icm);
         }

         byte[] indices = ((DataBufferByte)frame.getRaster().getDataBuffer()).getData();
         e.addFrame(rgbPalette, indices, icm.getTransparentPixel());
      }

      e.finish();
      logger.debug("  ... written.");
   }

   private static byte[] getRGBPalette(IndexColorModel icm) {
      byte[] reds = new byte[icm.getMapSize()];
      byte[] greens = new byte[icm.getMapSize()];
      byte[] blues = new byte[icm.getMapSize()];
      icm.getReds(reds);
      icm.getGreens(greens);
      icm.getBlues(blues);
      byte[] palette = new byte[768];

      for(int i = 0; i < icm.getMapSize(); ++i) {
         palette[i * 3] = reds[i];
         palette[i * 3 + 1] = greens[i];
         palette[i * 3 + 2] = blues[i];
      }

      return palette;
   }

   public String getMimeType() {
      return "image/gif";
   }

   public boolean supportsMultipleFrames() {
      return true;
   }

   public boolean supportsFullyTransparentPixels() {
      return true;
   }

   public boolean supportsPartiallyTransparentPixels() {
      return false;
   }
}
