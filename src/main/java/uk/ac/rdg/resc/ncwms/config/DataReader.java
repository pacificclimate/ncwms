package uk.ac.rdg.resc.ncwms.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.domain.impl.HorizontalDomain;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;
import org.apache.oro.io.GlobFilenameFilter;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.util.CollectionUtils;

public abstract class DataReader {
   private static Map<String, DataReader> readers = new HashMap();

   protected DataReader() {
   }

   public static DataReader forName(String dataReaderClassName) throws Exception {
      String clazz = DefaultDataReader.class.getName();
      if (dataReaderClassName != null && !dataReaderClassName.trim().equals("")) {
         clazz = dataReaderClassName;
      }

      if (!readers.containsKey(clazz)) {
         Object drObj = Class.forName(clazz).newInstance();
         readers.put(clazz, (DataReader)drObj);
      }

      return (DataReader)readers.get(clazz);
   }

   public abstract List<Float> read(String var1, Layer var2, int var3, int var4, Domain<HorizontalPosition> var5) throws IOException;

   public List<List<Float>> readVerticalSection(String filename, Layer layer, int tIndex, List<Integer> zIndices, Domain<HorizontalPosition> domain) throws IOException {
      if (zIndices == null) {
         zIndices = Arrays.asList(-1);
      }

      List<List<Float>> data = new ArrayList(zIndices.size());
      Iterator i$ = zIndices.iterator();

      while(i$.hasNext()) {
         int zIndex = (Integer)i$.next();
         data.add(this.read(filename, layer, tIndex, zIndex, domain));
      }

      return data;
   }

   public List<Float> readTimeseries(String filename, Layer layer, List<Integer> tIndices, int zIndex, HorizontalPosition xy) throws IOException {
      HorizontalDomain pointList = new HorizontalDomain(xy);
      List<Float> tsData = new ArrayList();
      Iterator i$ = tIndices.iterator();

      while(i$.hasNext()) {
         int tIndex = (Integer)i$.next();
         tsData.add(this.read(filename, layer, tIndex, zIndex, pointList).get(0));
      }

      return tsData;
   }

   public Map<String, Layer> getAllLayers(Dataset ds) throws FileNotFoundException, IOException {
      Map<String, LayerImpl> scalarLayers = CollectionUtils.newLinkedHashMap();
      String location = ds.getLocation();
      Iterator i$;
      if (WmsUtils.isOpendapLocation(location)) {
         this.updateLayers(location, ds, scalarLayers);
      } else {
         List<File> files = expandGlobExpression(location);
         if (files.isEmpty()) {
            throw new FileNotFoundException(location + " does not match any files");
         }

         i$ = files.iterator();

         while(i$.hasNext()) {
            File file = (File)i$.next();
            this.updateLayers(file.getPath(), ds, scalarLayers);
         }
      }

      Map<String, Layer> allLayers = CollectionUtils.newLinkedHashMap();
      i$ = scalarLayers.values().iterator();

      while(i$.hasNext()) {
         LayerImpl scalarLayer = (LayerImpl)i$.next();
         allLayers.put(scalarLayer.getId(), scalarLayer);
      }

      i$ = WmsUtils.findVectorLayers(scalarLayers.values()).iterator();

      while(i$.hasNext()) {
         VectorLayer vecLayer = (VectorLayer)i$.next();
         allLayers.put(vecLayer.getId(), new VectorLayerImpl(ds, vecLayer));
      }

      return allLayers;
   }

   protected void updateLayers(String location, Dataset ds, Map<String, LayerImpl> layers) throws IOException {
      CoverageMetadata lm;
      LayerImpl layer;
      for(Iterator i$ = this.readLayerMetadata(location).iterator(); i$.hasNext(); layer.addTimesteps(lm.getTimeValues(), location)) {
         lm = (CoverageMetadata)i$.next();
         String layerId = lm.getId();
         layer = (LayerImpl)layers.get(layerId);
         if (layer == null) {
            layer = new LayerImpl(lm, ds, this);
            layers.put(layerId, layer);
         }
      }

   }

   protected abstract Collection<CoverageMetadata> readLayerMetadata(String var1) throws IOException;

   public static List<File> expandGlobExpression(String globExpression) {
      File globFile = new File(globExpression);
      if (!globFile.isAbsolute()) {
         throw new IllegalArgumentException("Location must be an absolute path");
      } else {
         ArrayList pathComponents;
         File parent;
         for(pathComponents = new ArrayList(); globFile != null; globFile = parent) {
            parent = globFile.getParentFile();
            String pathComponent = parent == null ? globFile.getPath() : globFile.getName();
            pathComponents.add(0, pathComponent);
         }

         List<File> searchPaths = new ArrayList();
         searchPaths.add(new File((String)pathComponents.get(0)));

         label54:
         for(int i = 1; i < pathComponents.size(); ++i) {
            FilenameFilter globFilter = new GlobFilenameFilter((String)pathComponents.get(i));
            List<File> newSearchPaths = new ArrayList();
            Iterator i$ = searchPaths.iterator();

            while(true) {
               File dir;
               do {
                  if (!i$.hasNext()) {
                     searchPaths = newSearchPaths;
                     continue label54;
                  }

                  dir = (File)i$.next();
               } while(!dir.isDirectory());

               (new File(dir, (String)pathComponents.get(i))).list();
               File[] arr$ = dir.listFiles(globFilter);
               int len$ = arr$.length;

               for(int i$ = 0; i$ < len$; ++i$) {
                  File match = arr$[i$];
                  newSearchPaths.add(match);
               }
            }
         }

         List<File> files = new ArrayList();
         Iterator i$ = searchPaths.iterator();

         while(i$.hasNext()) {
            File path = (File)i$.next();
            if (path.isFile()) {
               files.add(path);
            }
         }

         return files;
      }
   }
}
