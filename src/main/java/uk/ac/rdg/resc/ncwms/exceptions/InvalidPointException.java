package uk.ac.rdg.resc.ncwms.exceptions;

public class InvalidPointException extends WmsException {
   public InvalidPointException(String i_or_j) {
      super("The GetFeatureInfo request contains an invalid value for " + i_or_j.toUpperCase());
   }
}
