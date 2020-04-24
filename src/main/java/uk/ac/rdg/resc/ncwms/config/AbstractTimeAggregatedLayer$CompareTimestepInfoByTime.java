package uk.ac.rdg.resc.ncwms.config;

import java.util.Comparator;

class AbstractTimeAggregatedLayer$CompareTimestepInfoByTime implements Comparator<TimestepInfo> {
   // $FF: synthetic field
   final AbstractTimeAggregatedLayer this$0;

   private AbstractTimeAggregatedLayer$CompareTimestepInfoByTime(AbstractTimeAggregatedLayer var1) {
      this.this$0 = var1;
   }

   public int compare(TimestepInfo o1, TimestepInfo o2) {
      return o1.getDateTime().compareTo(o2.getDateTime());
   }

   public boolean equals(TimestepInfo o1, TimestepInfo o2) {
      return o1.getDateTime().equals(o2.getDateTime());
   }

   // $FF: synthetic method
   AbstractTimeAggregatedLayer$CompareTimestepInfoByTime(AbstractTimeAggregatedLayer x0, Object x1) {
      this(x0);
   }
}
