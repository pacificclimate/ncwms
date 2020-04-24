package uk.ac.rdg.resc.ncwms.config;

import java.util.Comparator;

class CompareTimestepInfoByTime implements Comparator<TimestepInfo> {
   public int compare(TimestepInfo o1, TimestepInfo o2) {
      return o1.getDateTime().compareTo(o2.getDateTime());
   }

   public boolean equals(TimestepInfo o1, TimestepInfo o2) {
      return o1.getDateTime().equals(o2.getDateTime());
   }
}
