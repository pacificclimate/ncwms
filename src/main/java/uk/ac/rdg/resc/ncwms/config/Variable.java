package uk.ac.rdg.resc.ncwms.config;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;

@Root(
   name = "variable"
)
public class Variable {
   @Attribute(
      name = "id"
   )
   private String id;
   @Attribute(
      name = "title",
      required = false
   )
   private String title = null;
   @Attribute(
      name = "colorScaleRange",
      required = false
   )
   private String colorScaleRangeStr = null;
   @Attribute(
      name = "palette",
      required = false
   )
   private String paletteName = "rainbow";
   @Attribute(
      name = "scaling",
      required = false
   )
   private String scaling = "linear";
   @Attribute(
      name = "numColorBands",
      required = false
   )
   private int numColorBands = 254;
   private Dataset dataset;
   private Range<Float> colorScaleRange = null;
   private boolean logScaling = false;

   @Validate
   public void validate() throws PersistenceException {
      if (this.colorScaleRangeStr != null) {
         try {
            this.colorScaleRange = parseColorScaleRangeString(this.colorScaleRangeStr);
            this.colorScaleRangeStr = formatColorScaleRange(this.colorScaleRange);
         } catch (Exception var3) {
            throw new PersistenceException("Invalid colorScaleRange attribute for variable " + this.id, new Object[0]);
         }
      }

      try {
         this.setScaling(this.scaling);
      } catch (IllegalArgumentException var2) {
         throw new PersistenceException(var2.getMessage(), new Object[0]);
      }

      if (this.numColorBands > 254) {
         this.numColorBands = 254;
      }

   }

   private static Range<Float> parseColorScaleRangeString(String colorScaleRangeStr) throws Exception {
      colorScaleRangeStr = colorScaleRangeStr.trim();
      String[] els = colorScaleRangeStr.split(" ");
      if (els.length == 2) {
         return parseColorScaleRangeStrings(els[0].replace(',', '.'), els[1].replace(',', '.'));
      } else {
         if (els.length == 1) {
            els = colorScaleRangeStr.split(",");
            if (els.length == 2) {
               return parseColorScaleRangeStrings(els[0], els[1]);
            }

            if (els.length == 4) {
               return parseColorScaleRangeStrings(els[0] + "." + els[1], els[2] + "." + els[3]);
            }
         }

         throw new Exception();
      }
   }

   private static Range<Float> parseColorScaleRangeStrings(String minStr, String maxStr) {
      float min = Float.parseFloat(minStr);
      float max = Float.parseFloat(maxStr);
      if (max < min) {
         max = min;
      }

      return Ranges.newRange(min, max);
   }

   private static String formatColorScaleRange(Range<Float> colorScaleRange) {
      return Float.toString((Float)colorScaleRange.getMinimum()) + " " + Float.toString((Float)colorScaleRange.getMaximum());
   }

   public String getId() {
      return this.id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public Dataset getDataset() {
      return this.dataset;
   }

   void setDataset(Dataset dataset) {
      this.dataset = dataset;
   }

   public Range<Float> getColorScaleRange() {
      return this.colorScaleRange;
   }

   public void setColorScaleRange(Range<Float> colorScaleRange) {
      this.colorScaleRange = colorScaleRange;
      this.colorScaleRangeStr = colorScaleRange == null ? null : formatColorScaleRange(colorScaleRange);
   }

   public String getPaletteName() {
      return this.paletteName;
   }

   public void setPaletteName(String paletteName) {
      this.paletteName = paletteName;
   }

   public boolean isLogScaling() {
      return this.logScaling;
   }

   public int getNumColorBands() {
      return this.numColorBands;
   }

   public void setNumColorBands(int numColorBands) {
      if (numColorBands < 0) {
         this.numColorBands = 5;
      } else if (numColorBands > 254) {
         this.numColorBands = 254;
      } else {
         this.numColorBands = numColorBands;
      }

   }

   public void setScaling(String scaling) {
      if (scaling.equalsIgnoreCase("linear")) {
         this.logScaling = false;
         this.scaling = scaling;
      } else {
         if (!scaling.equalsIgnoreCase("logarithmic")) {
            throw new IllegalArgumentException("Scaling must be \"linear\" or \"logarithmic\"");
         }

         this.logScaling = true;
         this.scaling = scaling;
      }

   }
}
