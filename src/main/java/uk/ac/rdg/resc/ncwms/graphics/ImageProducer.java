package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;

public final class ImageProducer {
   private static final Logger logger = LoggerFactory.getLogger(ImageProducer.class);
   private ImageProducer.Style style;
   private int picWidth;
   private int picHeight;
   private boolean transparent;
   private int opacity;
   private int numColourBands;
   private boolean logarithmic;
   private Color bgColor;
   private ColorPalette colorPalette;
   private Range<Float> scaleRange;
   private float arrowLength;
   private List<BufferedImage> renderedFrames;
   private List<ImageProducer.Components> frameData;
   private List<String> labels;

   private ImageProducer() {
      this.arrowLength = 10.0F;
      this.renderedFrames = new ArrayList();
   }

   public BufferedImage getLegend(Layer layer) {
      return this.colorPalette.createLegend(this.numColourBands, layer.getTitle(), layer.getUnits(), this.logarithmic, this.scaleRange);
   }

   public int getPicWidth() {
      return this.picWidth;
   }

   public int getPicHeight() {
      return this.picHeight;
   }

   public boolean isTransparent() {
      return this.transparent;
   }

   public void addFrame(List<Float> data, String label) {
      this.addFrame(data, (List)null, label);
   }

   public void addFrame(List<Float> xData, List<Float> yData, String label) {
      logger.debug("Adding frame with label {}", label);
      ImageProducer.Components comps = new ImageProducer.Components(xData, yData);
      if (this.scaleRange.isEmpty()) {
         logger.debug("Auto-scaling, so caching frame");
         if (this.frameData == null) {
            this.frameData = new ArrayList();
            this.labels = new ArrayList();
         }

         this.frameData.add(comps);
         this.labels.add(label);
      } else {
         logger.debug("Scale is set, so rendering image");
         this.renderedFrames.add(this.createImage(comps, label));
      }

   }

   public IndexColorModel getColorModel() {
      return this.colorPalette.getColorModel(this.numColourBands, this.opacity, this.bgColor, this.transparent);
   }

   private BufferedImage createImage(ImageProducer.Components comps, String label) {
      byte[] pixels = new byte[this.picWidth * this.picHeight];
      List<Float> magnitudes = comps.getMagnitudes();

      for(int i = 0; i < pixels.length; ++i) {
         int dataIndex = this.getDataIndex(i);
         pixels[i] = (byte)this.getColourIndex((Float)magnitudes.get(dataIndex));
      }

      ColorModel colorModel = this.getColorModel();
      DataBuffer buf = new DataBufferByte(pixels, pixels.length);
      SampleModel sampleModel = colorModel.createCompatibleSampleModel(this.picWidth, this.picHeight);
      WritableRaster raster = Raster.createWritableRaster(sampleModel, buf, (Point)null);
      BufferedImage image = new BufferedImage(colorModel, raster, false, (Hashtable)null);
      Graphics2D g;
      if (label != null && !label.equals("")) {
         g = (Graphics2D)image.getGraphics();
         g.setPaint(new Color(0, 0, 143));
         g.fillRect(1, image.getHeight() - 19, image.getWidth() - 1, 18);
         g.setPaint(new Color(255, 151, 0));
         g.drawString(label, 10, image.getHeight() - 5);
      }

      if (this.style == ImageProducer.Style.VECTOR) {
         g = image.createGraphics();
         g.setColor(Color.BLACK);
         logger.debug("Drawing vectors, length = {} pixels", this.arrowLength);

         for(int i = 0; i < this.picWidth; i = (int)((double)i + Math.ceil((double)this.arrowLength * 1.2D))) {
            for(int j = 0; j < this.picHeight; j = (int)((double)j + Math.ceil((double)this.arrowLength * 1.2D))) {
               int dataIndex = this.getDataIndex(i, j);
               Float eastVal = (Float)comps.x.get(dataIndex);
               Float northVal = (Float)comps.y.get(dataIndex);
               if (eastVal != null && northVal != null) {
                  double angle = Math.atan2(northVal.doubleValue(), eastVal.doubleValue());
                  double iEnd = (double)i + (double)this.arrowLength * Math.cos(angle);
                  double jEnd = (double)j - (double)this.arrowLength * Math.sin(angle);
                  g.fillOval(i - 2, j - 2, 4, 4);
                  g.setStroke(new BasicStroke(1.0F));
                  g.drawLine(i, j, (int)Math.round(iEnd), (int)Math.round(jEnd));
               }
            }
         }
      }

      return image;
   }

   private int getDataIndex(int imageIndex) {
      int imageI = imageIndex % this.picWidth;
      int imageJ = imageIndex / this.picWidth;
      return this.getDataIndex(imageI, imageJ);
   }

   private int getDataIndex(int imageI, int imageJ) {
      int dataJ = this.picHeight - imageJ - 1;
      int dataIndex = dataJ * this.picWidth + imageI;
      return dataIndex;
   }

   public int getColourIndex(Float value) {
      if (value == null) {
         return this.numColourBands;
      } else if (!this.scaleRange.contains(value)) {
         return this.numColourBands + 1;
      } else {
         float scaleMin = (Float)this.scaleRange.getMinimum();
         float scaleMax = (Float)this.scaleRange.getMaximum();
         double min = this.logarithmic ? Math.log((double)scaleMin) : (double)scaleMin;
         double max = this.logarithmic ? Math.log((double)scaleMax) : (double)scaleMax;
         double val = this.logarithmic ? Math.log((double)value) : (double)value;
         double frac = (val - min) / (max - min);
         int index = (int)(frac * (double)this.numColourBands);
         if (index == this.numColourBands) {
            --index;
         }

         return index;
      }
   }

   public List<BufferedImage> getRenderedFrames() {
      this.setScale();
      if (this.frameData != null) {
         logger.debug("Rendering image frames...");

         for(int i = 0; i < this.frameData.size(); ++i) {
            logger.debug("    ... rendering frame {}", i);
            ImageProducer.Components comps = (ImageProducer.Components)this.frameData.get(i);
            this.renderedFrames.add(this.createImage(comps, (String)this.labels.get(i)));
         }
      }

      return this.renderedFrames;
   }

   private void setScale() {
      if (this.scaleRange.isEmpty()) {
         Float scaleMin = null;
         Float scaleMax = null;
         logger.debug("Setting the scale automatically");
         Iterator i$ = this.frameData.iterator();

         while(true) {
            Range range;
            do {
               do {
                  if (!i$.hasNext()) {
                     this.scaleRange = Ranges.newRange(scaleMin, scaleMax);
                     return;
                  }

                  ImageProducer.Components comps = (ImageProducer.Components)i$.next();
                  range = Ranges.findMinMax(comps.x);
               } while(range.isEmpty());

               if (scaleMin == null || ((Float)range.getMinimum()).compareTo(scaleMin) < 0) {
                  scaleMin = (Float)range.getMinimum();
               }
            } while(scaleMax != null && ((Float)range.getMaximum()).compareTo(scaleMax) <= 0);

            scaleMax = (Float)range.getMaximum();
         }
      }
   }

   public int getOpacity() {
      return this.opacity;
   }

   // $FF: synthetic method
   ImageProducer(Object x0) {
      this();
   }

   public static final class Builder {
      private int picWidth = -1;
      private int picHeight = -1;
      private boolean transparent = false;
      private int opacity = 100;
      private int numColourBands = 254;
      private Boolean logarithmic = null;
      private Color bgColor;
      private Range<Float> scaleRange;
      private ImageProducer.Style style;
      private ColorPalette colorPalette;

      public Builder() {
         this.bgColor = Color.WHITE;
         this.scaleRange = null;
         this.style = null;
         this.colorPalette = null;
      }

      public ImageProducer.Builder style(ImageProducer.Style style) {
         this.style = style;
         return this;
      }

      public ImageProducer.Builder palette(ColorPalette palette) {
         this.colorPalette = palette;
         return this;
      }

      public ImageProducer.Builder width(int width) {
         if (width < 0) {
            throw new IllegalArgumentException();
         } else {
            this.picWidth = width;
            return this;
         }
      }

      public ImageProducer.Builder height(int height) {
         if (height < 0) {
            throw new IllegalArgumentException();
         } else {
            this.picHeight = height;
            return this;
         }
      }

      public ImageProducer.Builder transparent(boolean transparent) {
         this.transparent = transparent;
         return this;
      }

      public ImageProducer.Builder opacity(int opacity) {
         if (opacity >= 0 && opacity <= 100) {
            this.opacity = opacity;
            return this;
         } else {
            throw new IllegalArgumentException();
         }
      }

      public ImageProducer.Builder colourScaleRange(Range<Float> scaleRange) {
         this.scaleRange = scaleRange;
         return this;
      }

      public ImageProducer.Builder numColourBands(int numColourBands) {
         if (numColourBands >= 0 && numColourBands <= 254) {
            this.numColourBands = numColourBands;
            return this;
         } else {
            throw new IllegalArgumentException();
         }
      }

      public ImageProducer.Builder logarithmic(Boolean logarithmic) {
         this.logarithmic = logarithmic;
         return this;
      }

      public ImageProducer.Builder backgroundColour(Color bgColor) {
         if (bgColor != null) {
            this.bgColor = bgColor;
         }

         return this;
      }

      public ImageProducer build() {
         if (this.picWidth >= 0 && this.picHeight >= 0) {
            ImageProducer ip = new ImageProducer();
            ip.picWidth = this.picWidth;
            ip.picHeight = this.picHeight;
            ip.opacity = this.opacity;
            ip.transparent = this.transparent;
            ip.bgColor = this.bgColor;
            ip.numColourBands = this.numColourBands;
            ip.style = this.style == null ? ImageProducer.Style.BOXFILL : this.style;
            ip.colorPalette = this.colorPalette == null ? ColorPalette.get((String)null) : this.colorPalette;
            ip.logarithmic = this.logarithmic == null ? false : this.logarithmic;
            Range<Float> emptyRange = Ranges.emptyRange();
            ip.scaleRange = this.scaleRange == null ? emptyRange : this.scaleRange;
            return ip;
         } else {
            throw new IllegalStateException("picture width and height must be >= 0");
         }
      }
   }

   private static final class Components {
      private final List<Float> x;
      private final List<Float> y;

      public Components(List<Float> x, List<Float> y) {
         this.x = x;
         this.y = y;
      }

      public Components(List<Float> x) {
         this(x, (List)null);
      }

      public List<Float> getMagnitudes() {
         return this.y == null ? this.x : WmsUtils.getMagnitudes(this.x, this.y);
      }
   }

   public static enum Style {
      BOXFILL,
      VECTOR;
   }
}
