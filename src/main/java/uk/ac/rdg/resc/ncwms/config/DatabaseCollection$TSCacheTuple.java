package uk.ac.rdg.resc.ncwms.config;

import java.util.ArrayList;
import java.util.Date;
import org.joda.time.DateTime;

class DatabaseCollection$TSCacheTuple implements Comparable {
   private ArrayList<DateTime> dt;
   private long firstAccess;
   private int cacheHits;
   // $FF: synthetic field
   final DatabaseCollection this$0;

   public DatabaseCollection$TSCacheTuple(DatabaseCollection var1, ArrayList dt) {
      this.this$0 = var1;
      this.dt = dt;
      this.cacheHits = 1;
      this.firstAccess = (new Date()).getTime();
   }

   public ArrayList<DateTime> getDT() {
      ++this.cacheHits;
      return this.dt;
   }

   public float getCacheScore() {
      long curTime = (new Date()).getTime();
      return (float)(curTime - this.firstAccess) / (float)this.cacheHits;
   }

   public int compareTo(Object o) {
      return (int)Math.signum(this.getCacheScore() - ((DatabaseCollection$TSCacheTuple)o).getCacheScore());
   }
}
