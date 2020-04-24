package uk.ac.rdg.resc.ncwms.exceptions;

public class WmsException extends Exception {
   private String code = null;

   public WmsException(String message) {
      super(message);
   }

   public WmsException(String message, String code) {
      super(message);
      this.code = code;
   }

   public String getCode() {
      return this.code;
   }
}
