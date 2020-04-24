package uk.ac.rdg.resc.ncwms.exceptions;

public class InvalidFormatException extends WmsException {
   public InvalidFormatException(String message) {
      super(message, "InvalidFormat");
   }
}
