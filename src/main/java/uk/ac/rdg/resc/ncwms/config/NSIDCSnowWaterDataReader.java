package uk.ac.rdg.resc.ncwms.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import org.geotools.metadata.iso.extent.GeographicBoundingBoxImpl;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.util.Utils;

public class NSIDCSnowWaterDataReader extends DataReader {
   private static final Logger logger = LoggerFactory.getLogger(NSIDCSnowWaterDataReader.class);
   private static final int ROWS = 721;
   private static final int COLS = 721;
   private static final double RE_KM = 6371.228D;
   private static final double CELL_KM = 25.067525D;
   private static final GeographicBoundingBox BBOX = new GeographicBoundingBoxImpl(-180.0D, 180.0D, 0.0D, 90.0D);

   protected Collection<CoverageMetadata> readLayerMetadata(String location) throws IOException {
      String filename = (new File(location)).getName();

      final DateTime timestep;
      try {
         DateFormat df = new SimpleDateFormat("'NL'yyyyMM'.v01.NSIDC8'");
         timestep = new DateTime(df.parse(filename).getTime());
      } catch (Exception var5) {
         logger.error("Error parsing filepath " + location, var5);
         throw new IOException("Error parsing filepath " + location);
      }

      CoverageMetadata lm = new CoverageMetadata() {
         public String getId() {
            return "swe";
         }

         public String getTitle() {
            return "snow_water_equivalent";
         }

         public String getDescription() {
            return "Snow Water Equivalent (SWE)";
         }

         public String getUnits() {
            return "mm";
         }

         public GeographicBoundingBox getGeographicBoundingBox() {
            return NSIDCSnowWaterDataReader.BBOX;
         }

         public HorizontalGrid getHorizontalGrid() {
            return null;
         }

         public Chronology getChronology() {
            return timestep.getChronology();
         }

         public List<DateTime> getTimeValues() {
            return Arrays.asList(timestep);
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
      return Arrays.asList(lm);
   }

   public List<Float> read(String filename, Layer layer, int tIndex, int zIndex, Domain<HorizontalPosition> domain) throws IOException {
      logger.debug("Reading data from " + filename);
      List<Float> picData = WmsUtils.nullArrayList(domain.getDomainObjects().size());
      FileInputStream fin = null;
      ByteBuffer data = null;

      try {
         fin = new FileInputStream(filename);
         data = ByteBuffer.allocate(1039682);
         data.order(ByteOrder.LITTLE_ENDIAN);
         fin.getChannel().read(data);
      } finally {
         try {
            if (fin != null) {
               fin.close();
            }
         } catch (IOException var18) {
         }

      }

      int picIndex = 0;

      for(Iterator i$ = domain.getDomainObjects().iterator(); i$.hasNext(); ++picIndex) {
         HorizontalPosition point = (HorizontalPosition)i$.next();
         LonLatPosition lonLat = Utils.transformToWgs84LonLat(point);
         if (lonLat.getLatitude() >= 0.0D && lonLat.getLatitude() <= 90.0D) {
            int dataIndex = latLonToIndex(lonLat.getLatitude(), lonLat.getLongitude());
            short val = data.getShort(dataIndex * 2);
            if (val > 0) {
               picData.set(picIndex, (float)val);
            }
         }
      }

      return picData;
   }

   private static int latLonToIndex(double lat, double lon) {
      double Rg = 254.16262674516133D;
      double r0 = 360.0D;
      double s0 = 360.0D;
      double phi = Math.toRadians(lat);
      double lam = Math.toRadians(lon);
      double rho = 2.0D * Rg * Math.sin(0.7853981633974483D - phi / 2.0D);
      int col = (int)Math.round(r0 + rho * Math.sin(lam));
      int row = (int)Math.round(s0 + rho * Math.cos(lam));
      int index = row * 721 + col;
      return index;
   }
}
