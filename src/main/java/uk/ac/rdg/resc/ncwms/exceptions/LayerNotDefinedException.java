package uk.ac.rdg.resc.ncwms.exceptions;

public class LayerNotDefinedException extends WmsException {
   public LayerNotDefinedException(String layerName) {
      super("The layer \"" + layerName + "\" is not provided by this server", "LayerNotDefined");
   }
}
