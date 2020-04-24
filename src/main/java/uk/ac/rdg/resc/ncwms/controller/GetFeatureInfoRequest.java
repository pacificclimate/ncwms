package uk.ac.rdg.resc.ncwms.controller;

import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

public class GetFeatureInfoRequest {
   private String wmsVersion;
   private GetFeatureInfoDataRequest dataRequest;
   private String outputFormat;

   public GetFeatureInfoRequest(RequestParams params) throws WmsException {
      this.wmsVersion = params.getMandatoryWmsVersion();
      if (!WmsUtils.SUPPORTED_VERSIONS.contains(this.wmsVersion)) {
         throw new WmsException("VERSION " + this.wmsVersion + " not supported");
      } else {
         this.dataRequest = new GetFeatureInfoDataRequest(params, this.wmsVersion);
         this.outputFormat = params.getMandatoryString("info_format");
      }
   }

   public GetFeatureInfoDataRequest getDataRequest() {
      return this.dataRequest;
   }

   public String getOutputFormat() {
      return this.outputFormat;
   }

   public String getWmsVersion() {
      return this.wmsVersion;
   }
}
