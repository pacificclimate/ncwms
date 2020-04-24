package uk.ac.rdg.resc.ncwms.exceptions;

public class OperationNotSupportedException extends WmsException {
   public OperationNotSupportedException(String operation) {
      super("The operation \"" + operation + "\" is not supported by this server", "OperationNotSupported");
   }
}
