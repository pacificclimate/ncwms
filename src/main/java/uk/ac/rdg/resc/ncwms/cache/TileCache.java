package uk.ac.rdg.resc.ncwms.cache;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.config.Config;

public class TileCache {
   private static final Logger logger = LoggerFactory.getLogger(TileCache.class);
   private static final String CACHE_NAME = "tilecache";
   private static final Float[] EMPTY_FLOAT_ARRAY = new Float[0];
   private CacheManager cacheManager;
   private File cacheDirectory;
   private Config ncwmsConfig;

   public void init() {
      Configuration tileCacheConfig = new Configuration();
      DiskStoreConfiguration diskStore = new DiskStoreConfiguration();
      diskStore.setPath(this.cacheDirectory.getPath());
      tileCacheConfig.addDiskStore(diskStore);
      tileCacheConfig.addDefaultCache(new CacheConfiguration());
      this.cacheManager = new CacheManager(tileCacheConfig);
      Cache tileCache = new Cache("tilecache", this.ncwmsConfig.getCache().getMaxNumItemsInMemory(), MemoryStoreEvictionPolicy.LRU, this.ncwmsConfig.getCache().isEnableDiskStore(), "", false, (long)(this.ncwmsConfig.getCache().getElementLifetimeMinutes() * 60), 0L, this.ncwmsConfig.getCache().isEnableDiskStore(), 1000L, (RegisteredEventListeners)null, (BootstrapCacheLoader)null, this.ncwmsConfig.getCache().getMaxNumItemsOnDisk());
      this.cacheManager.addCache(tileCache);
      logger.info("Tile cache started");
   }

   public void shutdown() {
      this.cacheManager.shutdown();
      logger.info("Tile cache shut down");
   }

   public List<Float> get(TileCacheKey key) {
      Cache cache = this.cacheManager.getCache("tilecache");
      Element el = cache.get(key);
      if (el == null) {
         logger.debug("Not found in tile cache: {}", key);
         return null;
      } else {
         logger.debug("Found in tile cache");
         Float[] arr = (Float[])((Float[])el.getValue());
         return arr == null ? null : Arrays.asList(arr);
      }
   }

   public void put(TileCacheKey key, List<Float> data) {
      Float[] arr = (Float[])data.toArray(EMPTY_FLOAT_ARRAY);
      this.cacheManager.getCache("tilecache").put(new Element(key, arr));
      logger.debug("Data put into tile cache: {}", key);
   }

   public void setCacheDirectory(File cacheDirectory) {
      this.cacheDirectory = cacheDirectory;
   }

   public void setConfig(Config config) {
      this.ncwmsConfig = config;
   }
}
