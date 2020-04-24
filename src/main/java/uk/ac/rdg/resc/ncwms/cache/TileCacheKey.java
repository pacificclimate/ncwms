package uk.ac.rdg.resc.ncwms.cache;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import org.geotools.referencing.CRS;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.util.Utils;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;

public class TileCacheKey implements Serializable {
   private String layerId;
   private String crsCode;
   private double[] bbox;
   private int width;
   private int height;
   private String filepath;
   private long lastModified = 0L;
   private long fileSize = 0L;
   private int tIndex;
   private int zIndex;
   private long datasetLastModified = 0L;
   private String str;
   private int hashCode;

   public TileCacheKey(String filepath, Layer layer, RegularGrid grid, int tIndex, int zIndex) {
      this.layerId = layer.getId();
      this.setGrid(grid);
      this.filepath = filepath;
      File f = new File(filepath);
      if (f.exists()) {
         if (!f.isFile()) {
            throw new IllegalArgumentException(filepath + " exists but is not a valid file on this server");
         }

         this.lastModified = f.lastModified();
         this.fileSize = f.length();
      }

      if (WmsUtils.isOpendapLocation(filepath) || WmsUtils.isNcmlAggregation(filepath)) {
         this.datasetLastModified = layer.getDataset().getLastUpdateTime().getMillis();
      }

      this.tIndex = tIndex;
      this.zIndex = zIndex;
      StringBuffer buf = new StringBuffer();
      buf.append(this.layerId);
      buf.append(",");
      buf.append(this.crsCode);
      buf.append(",{");
      double[] arr$ = this.bbox;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         double bboxVal = arr$[i$];
         buf.append(bboxVal);
         buf.append(",");
      }

      buf.append("},");
      buf.append(this.width);
      buf.append(",");
      buf.append(this.height);
      buf.append(",");
      buf.append(this.filepath);
      buf.append(",");
      buf.append(this.lastModified);
      buf.append(",");
      buf.append(this.fileSize);
      buf.append(",");
      buf.append(this.tIndex);
      buf.append(",");
      buf.append(this.zIndex);
      buf.append(",");
      buf.append(this.datasetLastModified);
      this.str = buf.toString();
      this.hashCode = this.str.hashCode();
   }

   public int hashCode() {
      return this.hashCode;
   }

   public String toString() {
      return this.str;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof TileCacheKey)) {
         return false;
      } else {
         TileCacheKey other = (TileCacheKey)o;
         return this.tIndex == other.tIndex && this.zIndex == other.zIndex && this.fileSize == other.fileSize && this.lastModified == other.lastModified && this.datasetLastModified == other.datasetLastModified && this.width == other.width && this.height == other.height && this.crsCode.equals(other.crsCode) && this.filepath.equals(other.filepath) && this.layerId.equals(other.layerId) && Arrays.equals(this.bbox, other.bbox);
      }
   }

   private void setGrid(RegularGrid grid) {
      this.width = grid.getXAxis().getSize();
      this.height = grid.getYAxis().getSize();
      BoundingBox boundingBox = grid.getExtent();
      this.bbox = new double[]{boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMaxX(), boundingBox.getMaxY()};
      if (Utils.isWgs84LonLat(grid.getCoordinateReferenceSystem())) {
         this.crsCode = "CRS:841";
         this.bbox[0] = Utils.constrainLongitude180(this.bbox[0]);
         this.bbox[2] = Utils.constrainLongitude180(this.bbox[2]);
      } else {
         try {
            this.crsCode = CRS.lookupIdentifier(grid.getCoordinateReferenceSystem(), true);
         } catch (Exception var4) {
         }
      }

   }
}
