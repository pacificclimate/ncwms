package uk.ac.rdg.resc.ncwms.config;

import org.joda.time.DateTime;

class TimestepInfo {
   private DateTime timestep;
   private int filenameIdx;
   private int indexInFile;

   public TimestepInfo(DateTime timestep, int filenameIdx, int indexInFile) {
      if (timestep == null) {
         throw new NullPointerException();
      } else if (indexInFile < 0) {
         throw new IllegalArgumentException("indexInFile must be >= 0");
      } else {
         this.timestep = timestep;
         this.filenameIdx = filenameIdx;
         this.indexInFile = indexInFile;
      }
   }

   public int getFilenameIdx() {
      return this.filenameIdx;
   }

   public void setFilenameIdx(int idx) {
      this.filenameIdx = idx;
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
      } else if (!(obj instanceof TimestepInfo)) {
         return false;
      } else {
         TimestepInfo otherTstep = (TimestepInfo)obj;
         return this.timestep.isEqual(otherTstep.timestep) && this.indexInFile == otherTstep.indexInFile && this.filenameIdx == otherTstep.filenameIdx;
      }
   }

   public int hashCode() {
      int hash = 3;
      int hash = 41 * hash + this.timestep.hashCode();
      hash = 41 * hash + this.filenameIdx;
      hash = 41 * hash + this.indexInFile;
      return hash;
   }
}
