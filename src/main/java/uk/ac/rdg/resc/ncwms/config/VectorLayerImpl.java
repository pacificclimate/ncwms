package uk.ac.rdg.resc.ncwms.config;

import java.util.List;

import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.util.Range;

final class VectorLayerImpl implements VectorLayer {
   private final Dataset ds;
   private final VectorLayer wrappedLayer;

   public VectorLayerImpl(Dataset ds, VectorLayer vecLayer) {
      this.ds = ds;
      this.wrappedLayer = vecLayer;
   }

   public Dataset getDataset() {
      return this.ds;
   }

   private Variable getVariable() {
      return (Variable)this.ds.getVariables().get(this.getId());
   }

   public String getTitle() {
      Variable var = this.getVariable();
      return var != null && var.getTitle() != null ? var.getTitle() : this.wrappedLayer.getTitle();
   }

   public Range<Float> getApproxValueRange() {
      return this.getVariable().getColorScaleRange();
   }

   public boolean isLogScaling() {
      return this.getVariable().isLogScaling();
   }

   public ColorPalette getDefaultColorPalette() {
      return ColorPalette.get(this.getVariable().getPaletteName());
   }

   public int getDefaultNumColorBands() {
      return this.getVariable().getNumColorBands();
   }

   public ScalarLayer getEastwardComponent() {
      return this.wrappedLayer.getEastwardComponent();
   }

   public ScalarLayer getNorthwardComponent() {
      return this.wrappedLayer.getNorthwardComponent();
   }

   public String getId() {
      return this.wrappedLayer.getId();
   }

   public String getLayerAbstract() {
      return this.wrappedLayer.getLayerAbstract();
   }

   public String getName() {
      return this.wrappedLayer.getName();
   }

   public String getUnits() {
      return this.wrappedLayer.getUnits();
   }

   public boolean isQueryable() {
      return this.wrappedLayer.isQueryable();
   }

   public GeographicBoundingBox getGeographicBoundingBox() {
      return this.wrappedLayer.getGeographicBoundingBox();
   }

   public HorizontalGrid getHorizontalGrid() {
      return this.wrappedLayer.getHorizontalGrid();
   }

   public Chronology getChronology() {
      return this.wrappedLayer.getChronology();
   }

   public List<DateTime> getTimeValues() {
      return this.wrappedLayer.getTimeValues();
   }

   public DateTime getCurrentTimeValue() {
      return this.wrappedLayer.getCurrentTimeValue();
   }

   public DateTime getDefaultTimeValue() {
      return this.wrappedLayer.getDefaultTimeValue();
   }

   public List<Double> getElevationValues() {
      return this.wrappedLayer.getElevationValues();
   }

   public double getDefaultElevationValue() {
      return this.wrappedLayer.getDefaultElevationValue();
   }

   public String getElevationUnits() {
      return this.wrappedLayer.getElevationUnits();
   }

   public boolean isElevationPositive() {
      return this.wrappedLayer.isElevationPositive();
   }

   public boolean isElevationPressure() {
      return this.wrappedLayer.isElevationPressure();
   }
}
