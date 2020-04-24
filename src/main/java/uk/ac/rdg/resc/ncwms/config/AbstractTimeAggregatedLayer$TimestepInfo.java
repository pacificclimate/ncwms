package uk.ac.rdg.resc.ncwms.config;

import org.joda.time.DateTime;

public class AbstractTimeAggregatedLayer$TimestepInfo {
   private DateTime timestep;
   private String filename;
   private int indexInFile;

   public AbstractTimeAggregatedLayer$TimestepInfo(DateTime timestep, String filename, int indexInFile) {
      if (timestep != null && filename != null) {
         if (indexInFile < 0) {
            throw new IllegalArgumentException("indexInFile must be >= 0");
         } else {
            this.timestep = timestep;
            this.filename = filename;
            this.indexInFile = indexInFile;
         }
      } else {
         throw new NullPointerException();
      }
   }

   public String getFilename() {
      return this.filename;
   }

   public int getIndexInFile() {
      return this.indexInFile;
   }

   public DateTime getDateTime() {
      return this.timestep;
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (!(obj instanceof AbstractTimeAggregatedLayer$TimestepInfo)) {
         return false;
      } else {
         AbstractTimeAggregatedLayer$TimestepInfo otherTstep = (AbstractTimeAggregatedLayer$TimestepInfo)obj;
         return this.timestep.isEqual(otherTstep.timestep) && this.indexInFile == otherTstep.indexInFile && this.filename.equals(otherTstep.filename);
      }
   }

   public int hashCode() {
      int hash = 3;
      int hash = 41 * hash + this.timestep.hashCode();
      hash = 41 * hash + this.filename.hashCode();
      hash = 41 * hash + this.indexInFile;
      return hash;
   }
}
