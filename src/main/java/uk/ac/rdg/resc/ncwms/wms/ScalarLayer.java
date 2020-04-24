package uk.ac.rdg.resc.ncwms.wms;

import java.io.IOException;
import java.util.List;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;

public interface ScalarLayer extends Layer {
   Float readSinglePoint(DateTime var1, double var2, HorizontalPosition var4) throws InvalidDimensionValueException, IOException;

   List<Float> readHorizontalPoints(DateTime var1, double var2, Domain<HorizontalPosition> var4) throws InvalidDimensionValueException, IOException;

   List<List<Float>> readVerticalSection(DateTime var1, List<Double> var2, Domain<HorizontalPosition> var3) throws InvalidDimensionValueException, IOException;

   List<Float> readTimeseries(List<DateTime> var1, double var2, HorizontalPosition var4) throws InvalidDimensionValueException, IOException;
}
