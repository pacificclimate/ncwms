package uk.ac.rdg.resc.ncwms.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.rdg.resc.ncwms.cache.TileCache;
import uk.ac.rdg.resc.ncwms.cache.TileCacheKey;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.OperationNotSupportedException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import org.joda.time.DateTime;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;

public final class NcwmsController extends AbstractWmsController {
   private NcwmsMetadataController metadataController;
   private TileCache tileCache;
   private final AbstractWmsController.LayerFactory LAYER_FACTORY = new AbstractWmsController.LayerFactory() {
      public Layer getLayer(String layerName) throws LayerNotDefinedException {
         int slashIndex = layerName.lastIndexOf("/");
         if (slashIndex > 0) {
            String datasetId = layerName.substring(0, slashIndex);
            uk.ac.rdg.resc.ncwms.wms.Dataset ds = NcwmsController.this.getConfig().getDatasetById(datasetId);
            if (ds == null) {
               throw new LayerNotDefinedException(layerName);
            } else {
               String layerId = layerName.substring(slashIndex + 1);
               Layer layer = ds.getLayerById(layerId);
               if (layer == null) {
                  throw new LayerNotDefinedException(layerName);
               } else {
                  return layer;
               }
            }
         } else {
            throw new LayerNotDefinedException(layerName);
         }
      }
   };

   public void init() throws Exception {
      this.metadataController = new NcwmsMetadataController(this.getConfig(), this.LAYER_FACTORY);
      super.init();
   }

   protected ModelAndView dispatchWmsRequest(String request, RequestParams params, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, UsageLogEntry usageLogEntry) throws Exception {
      if (request.equals("GetCapabilities")) {
         return this.getCapabilities(params, httpServletRequest, usageLogEntry);
      } else if (request.equals("GetMap")) {
         return this.getMap(params, this.LAYER_FACTORY, httpServletResponse, usageLogEntry);
      } else if (request.equals("GetFeatureInfo")) {
         String url = params.getString("url");
         if (url != null && !url.trim().equals("")) {
            usageLogEntry.setRemoteServerUrl(url);
            NcwmsMetadataController.proxyRequest(url, httpServletRequest, httpServletResponse);
            return null;
         } else {
            return this.getFeatureInfo(params, this.LAYER_FACTORY, httpServletRequest, httpServletResponse, usageLogEntry);
         }
      } else if (request.equals("GetMetadata")) {
         return this.metadataController.handleRequest(httpServletRequest, httpServletResponse, usageLogEntry);
      } else if (request.equals("GetLegendGraphic")) {
         return this.getLegendGraphic(params, this.LAYER_FACTORY, httpServletResponse);
      } else if (request.equals("GetTransect")) {
         return this.getTransect(params, this.LAYER_FACTORY, httpServletResponse, usageLogEntry);
      } else if (request.equals("GetVerticalProfile")) {
         return this.getVerticalProfile(params, this.LAYER_FACTORY, httpServletResponse, usageLogEntry);
      } else if (request.equals("GetVerticalSection")) {
         return this.getVerticalSection(params, this.LAYER_FACTORY, httpServletResponse, usageLogEntry);
      } else {
         throw new OperationNotSupportedException(request);
      }
   }

   private ModelAndView getCapabilities(RequestParams params, HttpServletRequest httpServletRequest, UsageLogEntry usageLogEntry) throws WmsException, IOException {
      String datasetId = params.getString("dataset");
      DateTime lastUpdate;
      Object datasets;
      if (datasetId != null && !datasetId.trim().equals("")) {
         uk.ac.rdg.resc.ncwms.wms.Dataset ds = this.getConfig().getDatasetById(datasetId);
         if (ds == null) {
            throw new WmsException("There is no dataset with ID " + datasetId);
         }

         if (!ds.isReady()) {
            throw new WmsException("The dataset with ID " + datasetId + " is not ready for use");
         }

         datasets = Arrays.asList(ds);
         lastUpdate = ds.getLastUpdateTime();
      } else {
         Map<String, ? extends Dataset> allDatasets = this.getConfig().getAllDatasets();
         if (!this.getConfig().getAllowsGlobalCapabilities()) {
            throw new WmsException("Cannot create a Capabilities document that includes all datasets on this server. You must specify a dataset identifier with &amp;DATASET=");
         }

         datasets = allDatasets.values();
         lastUpdate = this.getConfig().getLastUpdateTime();
      }

      return this.getCapabilities((Collection)datasets, lastUpdate, params, httpServletRequest, usageLogEntry);
   }

   protected List<Float> readDataGrid(ScalarLayer layer, DateTime dateTime, double elevation, RegularGrid grid, UsageLogEntry usageLogEntry) throws InvalidDimensionValueException, IOException {
      LayerImpl layerImpl = (LayerImpl)layer;
      LayerImpl.FilenameAndTimeIndex fti = layerImpl.findAndCheckFilenameAndTimeIndex(dateTime);
      int zIndex = layerImpl.findAndCheckElevationIndex(elevation);
      TileCacheKey key = new TileCacheKey(fti.filename, layer, grid, fti.tIndexInFile, zIndex);
      List<Float> data = null;
      boolean cacheEnabled = this.getConfig().getCache().isEnabled();
      if (cacheEnabled) {
         data = this.tileCache.get(key);
      }

      usageLogEntry.setUsedCache(data != null);
      if (data == null) {
         data = layerImpl.readHorizontalDomain(fti, zIndex, grid);
         if (cacheEnabled) {
            this.tileCache.put(key, data);
         }
      }

      return data;
   }

   public void shutdown() {
      this.tileCache.shutdown();
   }

   private Config getConfig() {
      return (Config)this.serverConfig;
   }

   public void setTileCache(TileCache tileCache) {
      this.tileCache = tileCache;
   }
}
