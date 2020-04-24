package uk.ac.rdg.resc.ncwms.controller;

import uk.ac.rdg.resc.ncwms.exceptions.InvalidPointException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

public class GetFeatureInfoDataRequest extends GetMapDataRequest {
   private int pixelColumn;
   private int pixelRow;
   private int featureCount;

   public GetFeatureInfoDataRequest(RequestParams params, String version) throws WmsException {
      this.layers = params.getMandatoryString("query_layers").split(",");
      this.init(params, version);
      String featureCountStr = params.getString("feature_count");
      if (featureCountStr == null) {
         this.featureCount = 1;
      }

      try {
         this.featureCount = Integer.parseInt(featureCountStr);
         if (this.featureCount <= 0) {
            this.featureCount = 1;
         }
      } catch (NumberFormatException var5) {
         this.featureCount = 1;
      }

      this.pixelColumn = params.getMandatoryPositiveInt(version.equals("1.3.0") ? "i" : "x");
      if (this.pixelColumn > this.getWidth() - 1) {
         throw new InvalidPointException("i (or x)");
      } else {
         this.pixelRow = params.getMandatoryPositiveInt(version.equals("1.3.0") ? "j" : "y");
         if (this.pixelRow > this.getHeight() - 1) {
            throw new InvalidPointException("j (or y)");
         }
      }
   }

   public int getPixelColumn() {
      return this.pixelColumn;
   }

   public int getPixelRow() {
      return this.pixelRow;
   }

   public int getFeatureCount() {
      return this.featureCount;
   }
}
