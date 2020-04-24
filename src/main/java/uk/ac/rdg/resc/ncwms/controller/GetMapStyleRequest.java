package uk.ac.rdg.resc.ncwms.controller;

import java.awt.Color;

import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;

public class GetMapStyleRequest {
   private String[] styles;
   private String imageFormat;
   private boolean transparent;
   private Color backgroundColour;
   private int opacity;
   private int numColourBands;
   private Boolean logarithmic;
   private Range<Float> colorScaleRange;

   public GetMapStyleRequest(RequestParams params) throws WmsException {
      String stylesStr = params.getMandatoryString("styles");
      if (stylesStr.trim().isEmpty()) {
         this.styles = new String[0];
      } else {
         this.styles = stylesStr.split(",");
      }

      this.imageFormat = params.getMandatoryString("format").replaceAll(" ", "+");
      this.transparent = params.getBoolean("transparent", false);

      try {
         String bgc = params.getString("bgcolor", "0xFFFFFF");
         if (bgc.length() != 8 || !bgc.startsWith("0x")) {
            throw new Exception();
         }

         this.backgroundColour = new Color(Integer.parseInt(bgc.substring(2), 16));
      } catch (Exception var4) {
         throw new WmsException("Invalid format for BGCOLOR");
      }

      this.opacity = params.getPositiveInt("opacity", 100);
      if (this.opacity > 100) {
         this.opacity = 100;
      }

      this.colorScaleRange = getColorScaleRange(params);
      this.numColourBands = getNumColourBands(params);
      this.logarithmic = isLogScale(params);
   }

   static Range<Float> getColorScaleRange(RequestParams params) throws WmsException {
      String csr = params.getString("colorscalerange");
      if (csr != null && !csr.equalsIgnoreCase("default")) {
         if (csr.equalsIgnoreCase("auto")) {
            return Ranges.emptyRange();
         } else {
            try {
               String[] scaleEls = csr.split(",");
               if (scaleEls.length != 2) {
                  throw new Exception();
               } else {
                  float scaleMin = Float.parseFloat(scaleEls[0]);
                  float scaleMax = Float.parseFloat(scaleEls[1]);
                  if (Float.compare(scaleMin, scaleMax) > 0) {
                     throw new Exception();
                  } else {
                     return Ranges.newRange(scaleMin, scaleMax);
                  }
               }
            } catch (Exception var5) {
               throw new WmsException("Invalid format for COLORSCALERANGE");
            }
         }
      } else {
         return null;
      }
   }

   static int getNumColourBands(RequestParams params) throws WmsException {
      int numColourBands = params.getPositiveInt("numcolorbands", 254);
      if (numColourBands > 254) {
         numColourBands = 254;
      }

      return numColourBands;
   }

   static Boolean isLogScale(RequestParams params) throws WmsException {
      String logScaleStr = params.getString("logscale");
      if (logScaleStr == null) {
         return null;
      } else if (logScaleStr.equalsIgnoreCase("true")) {
         return Boolean.TRUE;
      } else if (logScaleStr.equalsIgnoreCase("false")) {
         return Boolean.FALSE;
      } else {
         throw new WmsException("The value of LOGSCALE must be TRUE or FALSE (or can be omitted");
      }
   }

   public String[] getStyles() {
      return this.styles;
   }

   public String getImageFormat() {
      return this.imageFormat;
   }

   public Boolean isScaleLogarithmic() {
      return this.logarithmic;
   }

   public boolean isTransparent() {
      return this.transparent;
   }

   public Color getBackgroundColour() {
      return this.backgroundColour;
   }

   public int getOpacity() {
      return this.opacity;
   }

   public Range<Float> getColorScaleRange() {
      return this.colorScaleRange;
   }

   public int getNumColourBands() {
      return this.numColourBands;
   }
}
