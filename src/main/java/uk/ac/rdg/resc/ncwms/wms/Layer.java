package uk.ac.rdg.resc.ncwms.wms;

import java.util.List;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;

public interface Layer {
   Dataset getDataset();

   String getId();

   String getTitle();

   String getLayerAbstract();

   String getName();

   String getUnits();

   boolean isQueryable();

   GeographicBoundingBox getGeographicBoundingBox();

   HorizontalGrid getHorizontalGrid();

   Chronology getChronology();

   List<DateTime> getTimeValues();

   DateTime getCurrentTimeValue();

   DateTime getDefaultTimeValue();

   List<Double> getElevationValues();

   double getDefaultElevationValue();

   String getElevationUnits();

   boolean isElevationPositive();

   boolean isElevationPressure();

   Range<Float> getApproxValueRange();

   boolean isLogScaling();

   ColorPalette getDefaultColorPalette();

   int getDefaultNumColorBands();
}
