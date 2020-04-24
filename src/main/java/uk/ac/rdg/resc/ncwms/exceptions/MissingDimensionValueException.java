package uk.ac.rdg.resc.ncwms.exceptions;

public class MissingDimensionValueException extends WmsException {
   public MissingDimensionValueException(String dimName) {
      super("You must provide a value for the " + dimName.toUpperCase() + " dimension", "MissingDimensionValue");
   }
}
