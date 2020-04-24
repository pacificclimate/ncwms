package uk.ac.rdg.resc.ncwms.exceptions;

public class Wms1_1_1Exception extends Exception {
   private WmsException wmse;

   public Wms1_1_1Exception(WmsException wmse) {
      this.wmse = wmse;
   }

   public String getCode() {
      return this.wmse instanceof InvalidCrsException ? "InvalidSRS" : this.wmse.getCode();
   }

   public String getMessage() {
      return this.wmse.getMessage();
   }
}
