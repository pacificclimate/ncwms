package uk.ac.rdg.resc.ncwms.config;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import uk.ac.rdg.resc.edal.cdm.CdmUtils;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;

public class DefaultDataReader extends DataReader {
   private static final Logger logger = LoggerFactory.getLogger(DefaultDataReader.class);

   protected Collection<CoverageMetadata> readLayerMetadata(String location) throws IOException {
      NetcdfDataset nc = null;

      Collection var3;
      try {
         nc = openDataset(location);
         var3 = CdmUtils.readCoverageMetadata(CdmUtils.getGridDataset(nc));
      } finally {
         closeDataset(nc);
      }

      return var3;
   }

   public List<Float> read(String filename, Layer layer, int tIndex, int zIndex, Domain<HorizontalPosition> domain) throws IOException {
      NetcdfDataset nc = null;

      List var7;
      try {
         nc = openDataset(filename);
         var7 = CdmUtils.readHorizontalPoints(nc, layer.getId(), layer.getHorizontalGrid(), tIndex, zIndex, domain);
      } finally {
         closeDataset(nc);
      }

      return var7;
   }

   public List<List<Float>> readVerticalSection(String filename, Layer layer, int tIndex, List<Integer> zIndices, Domain<HorizontalPosition> domain) throws IOException {
      NetcdfDataset nc = null;

      List var7;
      try {
         nc = openDataset(filename);
         var7 = CdmUtils.readVerticalSection(nc, layer.getId(), layer.getHorizontalGrid(), tIndex, zIndices, domain);
      } finally {
         closeDataset(nc);
      }

      return var7;
   }

   public List<Float> readTimeseries(String filename, Layer layer, List<Integer> tIndices, int zIndex, HorizontalPosition xy) throws IOException {
      NetcdfDataset nc = null;

      List var7;
      try {
         nc = openDataset(filename);
         var7 = CdmUtils.readTimeseries(nc, layer.getId(), layer.getHorizontalGrid(), tIndices, zIndex, xy);
      } finally {
         closeDataset(nc);
      }

      return var7;
   }

   private static NetcdfDataset openDataset(String location) throws IOException {
      boolean usedCache = false;
      long start = System.nanoTime();
      NetcdfDataset nc;
      if (WmsUtils.isNcmlAggregation(location)) {
         nc = NetcdfDataset.acquireDataset(location, (CancelTask)null);
         usedCache = true;
      } else {
         nc = NetcdfDataset.openDataset(location);
      }

      long openedDS = System.nanoTime();
      String verb = usedCache ? "Acquired" : "Opened";
      logger.debug(verb + " NetcdfDataset in {} milliseconds", (double)(openedDS - start) / 1000000.0D);
      return nc;
   }

   private static void closeDataset(NetcdfDataset nc) {
      if (nc != null) {
         try {
            nc.close();
            logger.debug("NetCDF file closed");
         } catch (IOException var2) {
            logger.error("IOException closing " + nc.getLocation(), var2);
         }

      }
   }
}
