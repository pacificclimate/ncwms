package uk.ac.rdg.resc.ncwms.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.edal.cdm.CdmCoverageMetadata;
import uk.ac.rdg.resc.edal.cdm.ElevationAxis;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.util.CollectionUtils;

public class DatabaseDataReader extends DefaultDataReader {
   private static final Logger logger = LoggerFactory.getLogger(DefaultDataReader.class);
   private DatabaseCollection dc;

   public DatabaseDataReader(DatabaseCollection dc) {
      this.dc = dc;
   }

   public Map<String, Layer> getAllLayers(Dataset ds) throws FileNotFoundException, IOException {
      Map<String, LayerImpl> scalarLayers = CollectionUtils.newLinkedHashMap();
      String location = ds.getLocation();
      this.updateLayers(location, ds, scalarLayers);
      Map<String, Layer> allLayers = CollectionUtils.newLinkedHashMap();
      Iterator i$ = scalarLayers.values().iterator();

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
      ArrayList<TimestepInfo> tsinfo = this.dc.createTimestepInfo(((DatabaseDataset)ds).getDatabaseDataFile());

      LayerImpl layer;
      for(Iterator i$ = this.readLayerMetadata((DatabaseDataset)ds).iterator(); i$.hasNext(); layer.setTimesteps(tsinfo, location, true)) {
         CoverageMetadata lm = (CoverageMetadata)i$.next();
         String layerId = lm.getId();
         layer = (LayerImpl)layers.get(layerId);
         if (layer == null) {
            layer = new LayerImpl(lm, ds, this);
            layers.put(layerId, layer);
         }
      }

   }

   protected Collection<CoverageMetadata> readLayerMetadata(DatabaseDataset ds) throws IOException {
      ArrayList<CoverageMetadata> cmlist = new ArrayList();
      DatabaseDataFile f = ds.getDatabaseDataFile();
      HorizontalGrid hg = this.dc.createHorizontalGrid(f);
      List<DateTime> timesteps = this.dc.createTimesteps(f);
      GeographicBoundingBox bbox = this.dc.getBbox(hg);
      List<DatabaseVariable> vars = ds.getDatabaseVariables();
      Iterator i$ = vars.iterator();

      while(i$.hasNext()) {
         DatabaseVariable var = (DatabaseVariable)i$.next();
         ElevationAxis zAxis = this.dc.createZAxis(f, var);
         String variableTitle = var.title;
         cmlist.add(new CdmCoverageMetadata(var.id, var.title, var.description, var.units, bbox, hg, timesteps, zAxis));
      }

      return cmlist;
   }

   protected Collection<CoverageMetadata> readLayerMetadata(String location) throws IOException {
      ArrayList<CoverageMetadata> cmlist = new ArrayList();
      String id = this.dc.getIdForLocation(location);
      DatabaseDataFile f = this.dc.getDataFileForId(id);
      HorizontalGrid hg = this.dc.createHorizontalGrid(f);
      List<DateTime> timesteps = this.dc.createTimesteps(f);
      GeographicBoundingBox bbox = this.dc.getBbox(hg);
      List<DatabaseVariable> vars = this.dc.getVariablesForId(id);
      Iterator i$ = vars.iterator();

      while(i$.hasNext()) {
         DatabaseVariable var = (DatabaseVariable)i$.next();
         ElevationAxis zAxis = this.dc.createZAxis(f, var);
         String variableTitle = var.title;
         cmlist.add(new CdmCoverageMetadata(var.id, var.title, var.description, var.units, bbox, hg, timesteps, zAxis));
      }

      return cmlist;
   }
}
