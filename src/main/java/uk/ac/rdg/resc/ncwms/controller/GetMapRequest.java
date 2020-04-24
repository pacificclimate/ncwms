package uk.ac.rdg.resc.ncwms.controller;

import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

public class GetMapRequest {
   private String wmsVersion;
   private GetMapDataRequest dataRequest;
   private GetMapStyleRequest styleRequest;

   public GetMapRequest(RequestParams params) throws WmsException {
      this.wmsVersion = params.getMandatoryWmsVersion();
      if (!WmsUtils.SUPPORTED_VERSIONS.contains(this.wmsVersion)) {
         throw new WmsException("VERSION " + this.wmsVersion + " not supported");
      } else {
         this.dataRequest = new GetMapDataRequest(params, this.wmsVersion);
         this.styleRequest = new GetMapStyleRequest(params);
         if (this.styleRequest.getStyles().length != this.dataRequest.getLayers().length && this.styleRequest.getStyles().length != 0) {
            throw new WmsException("You must request exactly one STYLE per layer, or use the default style for each layer with STYLES=");
         }
      }
   }

   public GetMapDataRequest getDataRequest() {
      return this.dataRequest;
   }

   public GetMapStyleRequest getStyleRequest() {
      return this.styleRequest;
   }

   public String getWmsVersion() {
      return this.wmsVersion;
   }
}
