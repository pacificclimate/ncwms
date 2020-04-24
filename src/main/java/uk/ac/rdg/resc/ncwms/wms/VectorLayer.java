package uk.ac.rdg.resc.ncwms.wms;

public interface VectorLayer extends Layer {
   ScalarLayer getEastwardComponent();

   ScalarLayer getNorthwardComponent();
}
