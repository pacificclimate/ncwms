package uk.ac.rdg.resc.ncwms.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import org.joda.time.DateTime;

class DatabaseCollection$TSCache {
   private LinkedHashMap<Integer, DatabaseCollection$TSCacheTuple> cache;
   private int maxCacheSize;
   // $FF: synthetic field
   final DatabaseCollection this$0;

   public DatabaseCollection$TSCache(DatabaseCollection var1, int maxCacheSize) {
      this.this$0 = var1;
      this.maxCacheSize = maxCacheSize;
      this.cache = new LinkedHashMap();
   }

   public synchronized ArrayList<DateTime> get(int key) {
      DatabaseCollection$TSCacheTuple tc = (DatabaseCollection$TSCacheTuple)this.cache.get(key);
      return tc == null ? null : tc.getDT();
   }

   public synchronized void put(int key, ArrayList<DateTime> dt) {
      if (this.cache.size() >= this.maxCacheSize - 1) {
         Set<Integer> tcs = this.cache.keySet();
         Integer idx = -1;
         float minScore = Float.POSITIVE_INFINITY;
         Iterator i$ = tcs.iterator();

         while(i$.hasNext()) {
            Integer i = (Integer)i$.next();
            float score = ((DatabaseCollection$TSCacheTuple)this.cache.get(i)).getCacheScore();
            if (score < minScore) {
               idx = i;
               minScore = score;
            }
         }

         this.cache.remove(idx);
      }

      this.cache.put(key, new DatabaseCollection$TSCacheTuple(this.this$0, dt));
   }
}
