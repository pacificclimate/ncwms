package uk.ac.rdg.resc.ncwms.wms;

import java.io.IOException;
import java.util.List;

import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;

public class SimpleVectorLayer implements VectorLayer {
   private final String id;
   private final ScalarLayer east;
   private final ScalarLayer north;

   public SimpleVectorLayer(String id, ScalarLayer east, ScalarLayer north) {
      this.id = id;
      this.east = east;
      this.north = north;
   }

   public ScalarLayer getEastwardComponent() {
      return this.east;
   }

   public ScalarLayer getNorthwardComponent() {
      return this.north;
   }

   public String getId() {
      return this.id;
   }

   public String getLayerAbstract() {
      return "Automatically-generated vector field, composed of the fields " + this.east.getTitle() + " and " + this.north.getTitle();
   }

   public String getName() {
      return WmsUtils.createUniqueLayerName(this.getDataset().getId(), this.getId());
   }

   public String getTitle() {
      return this.id;
   }

   public Dataset getDataset() {
      return this.east.getDataset();
   }

   public String getUnits() {
      return this.east.getUnits();
   }

   public boolean isQueryable() {
      return this.east.isQueryable();
   }

   public GeographicBoundingBox getGeographicBoundingBox() {
      return this.east.getGeographicBoundingBox();
   }

   public HorizontalGrid getHorizontalGrid() {
      return this.east.getHorizontalGrid();
   }

   public Chronology getChronology() {
      return this.east.getChronology();
   }

   public List<DateTime> getTimeValues() {
      return this.east.getTimeValues();
   }

   public DateTime getCurrentTimeValue() {
      return this.east.getCurrentTimeValue();
   }

   public DateTime getDefaultTimeValue() {
      return this.east.getDefaultTimeValue();
   }

   public List<Double> getElevationValues() {
      return this.east.getElevationValues();
   }

   public double getDefaultElevationValue() {
      return this.east.getDefaultElevationValue();
   }

   public String getElevationUnits() {
      return this.east.getElevationUnits();
   }

   public boolean isElevationPositive() {
      return this.east.isElevationPositive();
   }

   public boolean isElevationPressure() {
      return this.east.isElevationPressure();
   }

   public ColorPalette getDefaultColorPalette() {
      return ColorPalette.get((String)null);
   }

   public boolean isLogScaling() {
      return this.east.isLogScaling();
   }

   public int getDefaultNumColorBands() {
      return this.east.getDefaultNumColorBands();
   }

   public Range<Float> getApproxValueRange() {
      try {
         return WmsUtils.estimateValueRange(this);
      } catch (IOException var2) {
         return Ranges.newRange(-50.0F, 50.0F);
      }
   }
}
