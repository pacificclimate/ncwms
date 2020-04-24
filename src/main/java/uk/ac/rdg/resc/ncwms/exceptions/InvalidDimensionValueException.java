package uk.ac.rdg.resc.ncwms.exceptions;

public class InvalidDimensionValueException extends WmsException {
   public InvalidDimensionValueException(String dimName, String value) {
      super("The value \"" + value + "\" is not valid for the " + dimName.toUpperCase() + " dimension", "InvalidDimensionValue");
   }
}
