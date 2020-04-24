package uk.ac.rdg.resc.ncwms.exceptions;

public class CurrentUpdateSequence extends WmsException {
   public CurrentUpdateSequence(String updateSequence) {
      super("The updatesequence value " + updateSequence + " is equal to the current value", "CurrentUpdateSequence");
   }
}
