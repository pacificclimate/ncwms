package uk.ac.rdg.resc.ncwms.exceptions;

public class LayerNotQueryableException extends WmsException {
   public LayerNotQueryableException(String layer) {
      super("The layer " + layer + " is not queryable", "LayerNotQueryable");
   }
}
