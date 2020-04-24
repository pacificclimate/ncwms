package uk.ac.rdg.resc.ncwms.exceptions;

public class InvalidCrsException extends WmsException {
   public InvalidCrsException(String crsCode) {
      super("The CRS " + crsCode + " is not supported by this server", "InvalidCRS");
   }
}
