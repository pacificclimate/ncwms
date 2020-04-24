package uk.ac.rdg.resc.ncwms.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import uk.ac.rdg.resc.edal.cdm.CdmUtils;
import uk.ac.rdg.resc.edal.cdm.PixelMap;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import org.geotools.metadata.iso.extent.GeographicBoundingBoxImpl;
import org.geotools.referencing.CRS;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.RegularAxis;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularAxisImpl;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;

public final class PhaveosDataReader extends DataReader {
   private static final CoordinateReferenceSystem BNG;
   private static final double[] BNG_BBOX = new double[]{-257750.0D, -92250.0D, 692750.0D, 1268750.0D};
   private static final int NX = 3802;
   private static final int NY = 5444;
   private static final HorizontalGrid SOURCE_GRID;
   private static final GeographicBoundingBox GEO_BBOX = new GeographicBoundingBoxImpl(-14.11655D, 3.45044D, 48.72112D, 61.19144D);

   public List<Float> read(String filename, Layer layer, int tIndex, int zIndex, Domain<HorizontalPosition> targetDomain) throws IOException {
      NetcdfDataset nc = null;

      List var28;
      try {
         nc = NetcdfDataset.openDataset(filename);
         ucar.nc2.Variable var = nc.findVariable(layer.getId());
         PixelMap pm = new PixelMap(layer.getHorizontalGrid(), targetDomain);
         int iSize = pm.getMaxIIndex() - pm.getMinIIndex() + 1;
         int jSize = pm.getMaxJIndex() - pm.getMinJIndex() + 1;
         int[] origin = new int[]{pm.getMinJIndex(), pm.getMinIIndex()};
         int[] shape = new int[]{jSize, iSize};
         Array data = var.read(origin, shape);
         Index index = data.getIndex();
         index.set(new int[index.getRank()]);
         float[] arr = new float[(int)targetDomain.size()];
         Arrays.fill(arr, Float.NaN);
         Iterator i$ = pm.iterator();

         while(i$.hasNext()) {
            PixelMap.PixelMapEntry pme = (PixelMap.PixelMapEntry)i$.next();
            int i = pme.getSourceGridIIndex() - pm.getMinIIndex();
            int j = pme.getSourceGridJIndex() - pm.getMinJIndex();
            index.set(new int[]{j, i});
            float val = data.getFloat(index);
            if ("mtci_l4".equals(layer.getId())) {
               val /= 1000.0F;
            }

            int targetGridPoint;
            for(Iterator i$ = pme.getTargetGridPoints().iterator(); i$.hasNext(); arr[targetGridPoint] = val > 0.0F ? val : Float.NaN) {
               targetGridPoint = (Integer)i$.next();
            }
         }

         var28 = CdmUtils.wrap(arr);
      } catch (InvalidRangeException var26) {
         throw new RuntimeException(var26);
      } finally {
         if (nc != null) {
            nc.close();
         }

      }

      return var28;
   }

   protected Collection<CoverageMetadata> readLayerMetadata(String location) throws IOException {
      int[] expectedShape = new int[]{5444, 3802};
      List<CoverageMetadata> cms = new ArrayList();
      NetcdfDataset nc = null;

      try {
         nc = NetcdfDataset.openDataset(location);
         DateTime dt = getDateTime(location);
         Iterator i$ = nc.getVariables().iterator();

         while(i$.hasNext()) {
            ucar.nc2.Variable var = (ucar.nc2.Variable)i$.next();
            if (Arrays.equals(var.getShape(), expectedShape)) {
               CoverageMetadata cm = getCoverageMetadata(var, dt);
               cms.add(cm);
            }
         }
      } finally {
         if (nc != null) {
            nc.close();
         }

      }

      return cms;
   }

   private static DateTime getDateTime(String filename) {
      filename = (new File(filename)).getName();
      String dateStr = filename.split("_|\\.")[1];
      int year = Integer.valueOf(dateStr.substring(0, 4));
      int month = Integer.valueOf(dateStr.substring(4, 6));
      int day = Integer.valueOf(dateStr.substring(6, 8));
      return new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
   }

   private static CoverageMetadata getCoverageMetadata(final ucar.nc2.Variable var, DateTime dt) {
      final List<DateTime> times = new ArrayList(1);
      times.add(dt);
      return new CoverageMetadata() {
         public String getId() {
            return var.getName();
         }

         public String getTitle() {
            return CdmUtils.getVariableTitle(var);
         }

         public String getDescription() {
            return var.getDescription();
         }

         public String getUnits() {
            return var.getUnitsString();
         }

         public GeographicBoundingBox getGeographicBoundingBox() {
            return PhaveosDataReader.GEO_BBOX;
         }

         public HorizontalGrid getHorizontalGrid() {
            return PhaveosDataReader.SOURCE_GRID;
         }

         public Chronology getChronology() {
            return null;
         }

         public List<DateTime> getTimeValues() {
            return times;
         }

         public List<Double> getElevationValues() {
            return Collections.emptyList();
         }

         public String getElevationUnits() {
            return "";
         }

         public boolean isElevationPositive() {
            return false;
         }

         public boolean isElevationPressure() {
            return false;
         }
      };
   }

   static {
      try {
         BNG = CRS.decode("EPSG:27700");
         double xSpacing = (BNG_BBOX[2] - BNG_BBOX[0]) / 3802.0D;
         double ySpacing = (BNG_BBOX[1] - BNG_BBOX[3]) / 5444.0D;
         RegularAxis xAxis = new RegularAxisImpl("x", BNG_BBOX[0], xSpacing, 3802, false);
         RegularAxis yAxis = new RegularAxisImpl("y", BNG_BBOX[3], ySpacing, 5444, false);
         SOURCE_GRID = new RegularGridImpl(xAxis, yAxis, BNG);
      } catch (Exception var6) {
         throw new ExceptionInInitializerError(var6);
      }
   }
}
