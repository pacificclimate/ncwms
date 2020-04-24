package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.ncwms.wms.Layer;

public abstract class SimpleFormat extends ImageFormat {
   public final boolean requiresLegend() {
      return false;
   }

   public final void writeImage(List<BufferedImage> frames, OutputStream out, Layer layer, List<String> tValues, String zValue, BoundingBox bbox, BufferedImage legend) throws IOException {
      this.writeImage(frames, out);
   }

   public abstract void writeImage(List<BufferedImage> var1, OutputStream var2) throws IOException;
}
