package uk.ac.rdg.resc.ncwms.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.domain.impl.HorizontalDomain;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.util.Range;

final class LayerImpl extends AbstractTimeAggregatedLayer {
   private final Dataset dataset;
   private final DataReader dataReader;

   public LayerImpl(CoverageMetadata lm, Dataset ds, DataReader dr) {
      super(lm);
      this.dataset = ds;
      this.dataReader = dr;
   }

   public String getTitle() {
      Variable var = this.getVariable();
      return var != null && var.getTitle() != null ? var.getTitle() : super.getTitle();
   }

   public Dataset getDataset() {
      return this.dataset;
   }

   public Range<Float> getApproxValueRange() {
      return this.getVariable().getColorScaleRange();
   }

   public boolean isQueryable() {
      return this.dataset.isQueryable();
   }

   public boolean isLogScaling() {
      return this.getVariable().isLogScaling();
   }

   public ColorPalette getDefaultColorPalette() {
      return ColorPalette.get(this.getVariable().getPaletteName());
   }

   public int getDefaultNumColorBands() {
      return this.getVariable().getNumColorBands();
   }

   private Variable getVariable() {
      return (Variable)this.dataset.getVariables().get(this.getId());
   }

   public List<Float> readHorizontalPoints(DateTime time, double elevation, Domain<HorizontalPosition> domain) throws InvalidDimensionValueException, IOException {
      int zIndex = this.findAndCheckElevationIndex(elevation);
      LayerImpl.FilenameAndTimeIndex fti = this.findAndCheckFilenameAndTimeIndex(time);
      return this.readHorizontalDomain(fti, zIndex, domain);
   }

   List<Float> readHorizontalDomain(LayerImpl.FilenameAndTimeIndex fti, int zIndex, Domain<HorizontalPosition> domain) throws IOException {
      return this.dataReader.read(fti.filename, this, fti.tIndexInFile, zIndex, domain);
   }

   public List<List<Float>> readVerticalSection(DateTime time, List<Double> elevations, Domain<HorizontalPosition> points) throws InvalidDimensionValueException, IOException {
      LayerImpl.FilenameAndTimeIndex fti = this.findAndCheckFilenameAndTimeIndex(time);
      Object zIndices;
      if (elevations == null) {
         zIndices = Arrays.asList(-1);
      } else {
         zIndices = new ArrayList(elevations.size());
         Iterator i$ = elevations.iterator();

         while(i$.hasNext()) {
            Double el = (Double)i$.next();
            ((List)zIndices).add(this.findAndCheckElevationIndex(el));
         }
      }

      return this.dataReader.readVerticalSection(fti.filename, this, fti.tIndexInFile, (List)zIndices, points);
   }

   LayerImpl.FilenameAndTimeIndex findAndCheckFilenameAndTimeIndex(DateTime time) throws InvalidDimensionValueException {
      int tIndex = this.findAndCheckTimeIndex(time);
      String filename;
      int tIndexInFile;
      if (tIndex < 0) {
         String location = this.dataset.getLocation();
         if (WmsUtils.isOpendapLocation(location)) {
            filename = location;
         } else {
            filename = ((File)DataReader.expandGlobExpression(location).get(0)).getPath();
         }

         tIndexInFile = tIndex;
      } else {
         filename = this.timesteps.getFilename(tIndex);
         tIndexInFile = this.timesteps.getIndexInFile(tIndex);
      }

      return new LayerImpl.FilenameAndTimeIndex(filename, tIndexInFile);
   }

   public Float readSinglePoint(DateTime time, double elevation, HorizontalPosition xy) throws InvalidDimensionValueException, IOException {
      HorizontalDomain singlePoint = new HorizontalDomain(xy);
      return (Float)this.readHorizontalPoints(time, elevation, singlePoint).get(0);
   }

   public List<Float> readTimeseries(List<DateTime> times, double elevation, HorizontalPosition xy) throws InvalidDimensionValueException, IOException {
      if (times == null) {
         throw new NullPointerException("times");
      } else {
         int zIndex = this.findAndCheckElevationIndex(elevation);
         Map<String, List<Integer>> files = new LinkedHashMap();

         LayerImpl.FilenameAndTimeIndex ft;
         Object tIndicesInFile;
         for(Iterator i$ = times.iterator(); i$.hasNext(); ((List)tIndicesInFile).add(ft.tIndexInFile)) {
            DateTime dt = (DateTime)i$.next();
            ft = this.findAndCheckFilenameAndTimeIndex(dt);
            tIndicesInFile = (List)files.get(ft.filename);
            if (tIndicesInFile == null) {
               tIndicesInFile = new ArrayList();
               files.put(ft.filename, tIndicesInFile);
            }
         }

         List<Float> data = new ArrayList();
         Iterator i$ = files.keySet().iterator();

         while(i$.hasNext()) {
            String filename = (String)i$.next();
            List<Integer> tIndicesInFile = (List)files.get(filename);
            List<Float> arr = this.dataReader.readTimeseries(filename, this, tIndicesInFile, zIndex, xy);
            data.addAll(arr);
         }

         if (data.size() != times.size()) {
            throw new AssertionError("Timeseries length inconsistency");
         } else {
            return data;
         }
      }
   }

   static class FilenameAndTimeIndex {
      String filename;
      int tIndexInFile;

      public FilenameAndTimeIndex(String filename, int tIndexInFile) {
         this.filename = filename;
         this.tIndexInFile = tIndexInFile;
      }
   }
}
