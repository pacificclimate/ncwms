package uk.ac.rdg.resc.ncwms.controller;

import java.util.List;

import uk.ac.rdg.resc.ncwms.wms.Layer;

public class TiledLayer {
   private Layer layer;
   private List<double[]> tiles;

   public TiledLayer(Layer layer, List<double[]> tiles) {
      this.layer = layer;
      this.tiles = tiles;
   }

   public Layer getLayer() {
      return this.layer;
   }

   public List<double[]> getTiles() {
      return this.tiles;
   }
}
