package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidFormatException;
import uk.ac.rdg.resc.ncwms.wms.Layer;

public abstract class ImageFormat {
   private static final Map<String, ImageFormat> formats = new LinkedHashMap();

   public static Set<String> getSupportedMimeTypes() {
      return formats.keySet();
   }

   public static ImageFormat get(String mimeType) throws InvalidFormatException {
      ImageFormat format = (ImageFormat)formats.get(mimeType);
      if (format == null) {
         throw new InvalidFormatException("The image format " + mimeType + " is not supported by this server");
      } else {
         return format;
      }
   }

   public abstract String getMimeType();

   public abstract boolean supportsMultipleFrames();

   public abstract boolean supportsFullyTransparentPixels();

   public abstract boolean supportsPartiallyTransparentPixels();

   public abstract boolean requiresLegend();

   public abstract void writeImage(List<BufferedImage> var1, OutputStream var2, Layer var3, List<String> var4, String var5, BoundingBox var6, BufferedImage var7) throws IOException;

   static {
      ImageIO.setUseCache(false);
      ImageFormat[] arr$ = new ImageFormat[]{new PngFormat(), new Png32Format(), new GifFormat(), new JpegFormat(), new KmzFormat()};
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         ImageFormat format = arr$[i$];
         formats.put(format.getMimeType(), format);
      }

   }
}
