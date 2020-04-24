package uk.ac.rdg.resc.ncwms.config;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.ncwms.wms.AbstractScalarLayer;
import org.joda.time.DateTime;

abstract class AbstractTimeAggregatedLayer extends AbstractScalarLayer {
   protected AbstractTimeAggregatedLayer.TimestepInfoAggregate timesteps = new AbstractTimeAggregatedLayer.TimestepInfoAggregate();
   private final List<DateTime> dateTimes = new AbstractList<DateTime>() {
      public DateTime get(int index) {
         return AbstractTimeAggregatedLayer.this.timesteps.getDateTime(index);
      }

      public int size() {
         return AbstractTimeAggregatedLayer.this.timesteps.size();
      }
   };

   public AbstractTimeAggregatedLayer(CoverageMetadata lm) {
      super(lm);
   }

   public List<DateTime> getTimeValues() {
      return this.dateTimes;
   }

   void addTimestepInfo(DateTime dt, String filename, int indexInFile) {
      this.timesteps.addTimestep(dt, filename, indexInFile);
   }

   void addTimesteps(List<DateTime> dts, String filename) {
      ArrayList<TimestepInfo> timesteps = new ArrayList(dts.size());
      int i = 0;

      for(Iterator i$ = dts.iterator(); i$.hasNext(); ++i) {
         DateTime dt = (DateTime)i$.next();
         TimestepInfo tInfo = new TimestepInfo(dt, -1, i);
         timesteps.add(tInfo);
      }

      this.timesteps.addTimesteps(timesteps, filename);
   }

   void setTimesteps(ArrayList<TimestepInfo> timesteps, String filename, boolean sorted) {
      this.timesteps.clear();
      this.timesteps.setTimesteps(timesteps, filename, sorted);
   }

   class TimestepInfoAggregate {
      private ArrayList<TimestepInfo> timesteps = new ArrayList();
      private ArrayList<String> filenames = new ArrayList();
      private HashMap<String, Integer> filenameMap = new HashMap();
      private boolean sorted = true;

      public TimestepInfoAggregate() {
      }

      private void sortAndUnique() {
         if (!this.sorted) {
            Collections.sort(this.timesteps, new CompareTimestepInfoByTime());
            int lastTSIdx = 0;

            for(int i = 1; i < this.timesteps.size(); ++i) {
               TimestepInfo curTS = (TimestepInfo)this.timesteps.get(i);
               TimestepInfo lastTS = (TimestepInfo)this.timesteps.get(lastTSIdx);
               if (lastTS.getDateTime() == curTS.getDateTime()) {
                  if (curTS.getIndexInFile() < lastTS.getIndexInFile()) {
                     this.timesteps.remove(i);
                  } else {
                     this.timesteps.remove(lastTSIdx);
                     lastTSIdx = i;
                  }

                  --i;
               } else {
                  lastTSIdx = i;
               }
            }

            this.sorted = true;
         }

      }

      public void setTimesteps(ArrayList<TimestepInfo> timesteps, String filename, boolean sorted) {
         this.sorted = sorted;
         this.filenameMap.clear();
         this.filenames.clear();
         this.timesteps = timesteps;
         this.filenameMap.put(filename, 0);
         this.filenames.add(filename);
      }

      public int getFilenameIdx(String filename) {
         if (!this.filenameMap.containsKey(filename)) {
            this.filenames.add(filename);
            this.filenameMap.put(filename, this.filenames.size() - 1);
         }

         return (Integer)this.filenameMap.get(filename);
      }

      public void addTimestep(DateTime timestep, String filename, int indexInFile) {
         this.sorted = false;
         int filenameIdx = this.getFilenameIdx(filename);
         this.timesteps.add(new TimestepInfo(timestep, filenameIdx, indexInFile));
      }

      public void addTimesteps(ArrayList<TimestepInfo> timesteps, String filename) {
         this.sorted = false;
         int filenameIdx = this.getFilenameIdx(filename);
         int curSize = this.timesteps.size();
         this.timesteps.addAll(timesteps);
         int newSize = this.timesteps.size();

         for(int i = curSize; i < newSize; ++i) {
            ((TimestepInfo)this.timesteps.get(i)).setFilenameIdx(filenameIdx);
         }

      }

      public void clear() {
         this.filenames.clear();
         this.filenameMap.clear();
         this.timesteps.clear();
         this.sorted = true;
      }

      public int getIndexInFile(int idx) {
         this.sortAndUnique();
         return ((TimestepInfo)this.timesteps.get(idx)).getIndexInFile();
      }

      public DateTime getDateTime(int idx) {
         this.sortAndUnique();
         return ((TimestepInfo)this.timesteps.get(idx)).getDateTime();
      }

      public String getFilename(int idx) {
         this.sortAndUnique();
         return this.filenames.size() == 1 ? (String)this.filenames.get(0) : (String)this.filenames.get(((TimestepInfo)this.timesteps.get(idx)).getFilenameIdx());
      }

      public int size() {
         return this.timesteps.size();
      }
   }
}
