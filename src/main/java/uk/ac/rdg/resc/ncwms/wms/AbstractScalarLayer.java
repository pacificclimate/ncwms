package uk.ac.rdg.resc.ncwms.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;

public abstract class AbstractScalarLayer implements ScalarLayer {
   private static final Set<String> PRESSURE_UNITS = CollectionUtils.setOf("Pa", "hPa", "bar", "millibar", "decibar", "atmosphere", "atm", "pascal");
   private static final Comparator<Double> ABSOLUTE_VALUE_COMPARATOR = new Comparator<Double>() {
      public int compare(Double d1, Double d2) {
         return Double.compare(Math.abs(d1), Math.abs(d2));
      }
   };
   private final CoverageMetadata cm;

   public AbstractScalarLayer(CoverageMetadata cm) {
      if (cm == null) {
         throw new NullPointerException("CoverageMetadata can't be null");
      } else {
         this.cm = cm;
      }
   }

   public String getId() {
      return this.cm.getId();
   }

   public String getName() {
      return WmsUtils.createUniqueLayerName(this.getDataset().getId(), this.getId());
   }

   public String getTitle() {
      return this.cm.getTitle();
   }

   public String getLayerAbstract() {
      return this.cm.getDescription();
   }

   public String getUnits() {
      return this.cm.getUnits();
   }

   public String getElevationUnits() {
      return this.cm.getElevationUnits();
   }

   public List<Double> getElevationValues() {
      return this.cm.getElevationValues();
   }

   public boolean isElevationPositive() {
      return this.cm.isElevationPositive();
   }

   public boolean isElevationPressure() {
      return this.cm.isElevationPressure();
   }

   public GeographicBoundingBox getGeographicBoundingBox() {
      return this.cm.getGeographicBoundingBox();
   }

   public HorizontalGrid getHorizontalGrid() {
      return this.cm.getHorizontalGrid();
   }

   public List<DateTime> getTimeValues() {
      return this.cm.getTimeValues();
   }

   protected boolean hasTimeAxis() {
      return !this.getTimeValues().isEmpty();
   }

   protected boolean hasElevationAxis() {
      return !this.getElevationValues().isEmpty();
   }

   public DateTime getCurrentTimeValue() {
      int currentTimeIndex = this.getCurrentTimeIndex();
      return currentTimeIndex < 0 ? null : (DateTime)this.getTimeValues().get(currentTimeIndex);
   }

   public DateTime getDefaultTimeValue() {
      return this.getCurrentTimeValue();
   }

   public Chronology getChronology() {
      return this.hasTimeAxis() ? ((DateTime)this.getTimeValues().get(0)).getChronology() : null;
   }

   protected int getCurrentTimeIndex() {
      if (this.getTimeValues().size() == 0) {
         return -1;
      } else {
         int index = WmsUtils.findTimeIndex(this.getTimeValues(), new DateTime());
         if (index >= 0) {
            return index;
         } else {
            int insertionPoint = -(index + 1);
            return insertionPoint > 0 ? insertionPoint - 1 : 0;
         }
      }
   }

   public int findAndCheckTimeIndex(DateTime target) throws InvalidDimensionValueException {
      if (!this.hasTimeAxis()) {
         return -1;
      } else {
         int index = WmsUtils.findTimeIndex(this.getTimeValues(), target);
         if (index >= 0) {
            return index;
         } else {
            throw new InvalidDimensionValueException("time", WmsUtils.dateTimeToISO8601(target));
         }
      }
   }

   public double getDefaultElevationValue() {
      if (!this.hasElevationAxis()) {
         return Double.NaN;
      } else {
         return PRESSURE_UNITS.contains(this.getElevationUnits()) ? (Double)Collections.max(this.getElevationValues()) : (Double)Collections.min(this.getElevationValues(), ABSOLUTE_VALUE_COMPARATOR);
      }
   }

   protected int findElevationIndex(double targetVal) {
      List<Double> zVals = this.getElevationValues();

      for(int i = 0; i < zVals.size(); ++i) {
         if ((Double)zVals.get(i) == targetVal || Math.abs(((Double)zVals.get(i) - targetVal) / targetVal) < 1.0E-5D) {
            return i;
         }
      }

      return -1;
   }

   public int findAndCheckElevationIndex(double targetVal) throws InvalidDimensionValueException {
      if (!this.hasElevationAxis()) {
         return -1;
      } else {
         int index = this.findElevationIndex(targetVal);
         if (index >= 0) {
            return index;
         } else {
            throw new InvalidDimensionValueException("elevation", "" + targetVal);
         }
      }
   }

   public boolean isLogScaling() {
      return false;
   }

   public ColorPalette getDefaultColorPalette() {
      return ColorPalette.get((String)null);
   }

   public List<Float> readHorizontalPoints(DateTime time, double elevation, Domain<HorizontalPosition> domain) throws InvalidDimensionValueException, IOException {
      List<? extends HorizontalPosition> points = domain.getDomainObjects();
      List<Float> vals = new ArrayList(points.size());
      Iterator i$ = points.iterator();

      while(i$.hasNext()) {
         HorizontalPosition xy = (HorizontalPosition)i$.next();
         vals.add(this.readSinglePoint(time, elevation, xy));
      }

      return vals;
   }

   public List<Float> readTimeseries(List<DateTime> times, double elevation, HorizontalPosition xy) throws InvalidDimensionValueException, IOException {
      List<Float> vals = new ArrayList(times.size());
      Iterator i$ = times.iterator();

      while(i$.hasNext()) {
         DateTime time = (DateTime)i$.next();
         vals.add(this.readSinglePoint(time, elevation, xy));
      }

      return vals;
   }
}
