package uk.ac.rdg.resc.ncwms.controller;

import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

public class GetMapDataRequest {
   protected String[] layers;
   private String crsCode;
   private double[] bbox;
   private int width;
   private int height;
   private String timeString;
   private String elevationString;

   public GetMapDataRequest(RequestParams params, String version) throws WmsException {
      this.layers = params.getMandatoryString("layers").split(",");
      this.init(params, version);
   }

   protected GetMapDataRequest() {
   }

   protected void init(RequestParams params, String version) throws WmsException {
      if (version.equals("1.3.0")) {
         this.crsCode = params.getMandatoryString("crs");
         if (this.crsCode.equalsIgnoreCase("EPSG:4326")) {
            this.crsCode = "CRS:84";
            this.bbox = WmsUtils.parseBbox(params.getMandatoryString("bbox"), false);
         } else {
            this.bbox = WmsUtils.parseBbox(params.getMandatoryString("bbox"), true);
         }
      } else {
         this.crsCode = params.getMandatoryString("srs");
         if (this.crsCode.equalsIgnoreCase("EPSG:4326")) {
            this.crsCode = "CRS:84";
         }

         this.bbox = WmsUtils.parseBbox(params.getMandatoryString("bbox"), true);
      }

      this.width = params.getMandatoryPositiveInt("width");
      this.height = params.getMandatoryPositiveInt("height");
      this.timeString = params.getString("time");
      this.elevationString = params.getString("elevation");
   }

   public String[] getLayers() {
      return this.layers;
   }

   public String getCrsCode() {
      return this.crsCode;
   }

   public double[] getBbox() {
      return this.bbox;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public String getTimeString() {
      return this.timeString;
   }

   public String getElevationString() {
      return this.elevationString;
   }
}
