package uk.ac.rdg.resc.ncwms.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;

import uk.ac.rdg.resc.ncwms.controller.ServerConfig;
import uk.ac.rdg.resc.ncwms.security.Users;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import org.joda.time.DateTime;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.core.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.io.RandomAccessFile;
import uk.ac.rdg.resc.edal.util.Utils;

@Root(
   name = "config"
)
public class Config implements ServerConfig, ApplicationContextAware {
   private static final Logger logger = LoggerFactory.getLogger(Config.class);
   private static final Serializer PERSISTER = new Persister();
   @ElementList(
      name = "datasets",
      type = Dataset.class
   )
   private ArrayList<Dataset> datasetList = new ArrayList();
   @ElementList(
      name = "databasecollections",
      type = DatabaseCollection.class,
      required = false
   )
   private ArrayList<DatabaseCollection> dbCollectionList = new ArrayList();
   @Element(
      name = "threddsCatalog",
      required = false
   )
   private String threddsCatalogLocation = " ";
   @Element(
      name = "contact",
      required = false
   )
   private Contact contact = new Contact();
   @Element(
      name = "server"
   )
   private Server server = new Server();
   @Element(
      name = "cache",
      required = false
   )
   private Cache cache = new Cache();
   private DateTime lastUpdateTime;
   private File configFile;
   private File configBackup;
   private NcwmsCredentialsProvider credentialsProvider;
   private Map<String, Dataset> datasets = new LinkedHashMap();
   private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
   private Map<String, ScheduledFuture<?>> futures = new HashMap();

   private Config() {
   }

   public static Config readConfig(File configFile) throws Exception {
      Config config;
      if (configFile.exists()) {
         config = (Config)PERSISTER.read(Config.class, configFile);
         config.configFile = configFile;
         logger.debug("Loaded configuration from {}", configFile.getPath());
      } else {
         config = new Config();
         config.configFile = configFile;
         config.save();
         logger.debug("Created new configuration object and saved to {}", configFile.getPath());
      }

      config.lastUpdateTime = new DateTime();
      NetcdfDataset.initNetcdfFileCache(50, 500, 500, 300);
      logger.debug("NetcdfDatasetCache initialized");
      if (logger.isDebugEnabled()) {
         RandomAccessFile.setDebugLeaks(true);
      }

      Iterator i$ = config.datasets.values().iterator();

      while(i$.hasNext()) {
         Dataset ds = (Dataset)i$.next();
         ds.setConfig(config);
         config.scheduleReloading(ds);
      }

      return config;
   }

   public synchronized void save() throws Exception {
      if (this.configFile == null) {
         throw new IllegalStateException("No location set for config file");
      } else {
         if (this.configBackup == null) {
            String backupName = this.configFile.getAbsolutePath() + ".bak";
            this.configBackup = new File(backupName);
         }

         if (this.configFile.exists()) {
            this.configBackup.delete();
            Utils.copyFile(this.configFile, this.configBackup);
         }

         PERSISTER.write(this, this.configFile);
         logger.debug("Config information saved to {}", this.configFile.getPath());
      }
   }

   @Validate
   public void validate() throws PersistenceException {
      List<String> dsIds = new ArrayList();
      Iterator i$ = this.datasetList.iterator();

      while(i$.hasNext()) {
         Dataset ds = (Dataset)i$.next();
         String dsId = ds.getId();
         if (dsIds.contains(dsId)) {
            throw new PersistenceException("Duplicate dataset id %s", new Object[]{dsId});
         }

         dsIds.add(dsId);
      }

   }

   @Commit
   public void build() {
      Iterator i$ = this.datasetList.iterator();

      while(i$.hasNext()) {
         Dataset ds = (Dataset)i$.next();
         this.datasets.put(ds.getId(), ds);
      }

      i$ = this.dbCollectionList.iterator();

      while(i$.hasNext()) {
         DatabaseCollection dc = (DatabaseCollection)i$.next();
         dc.setConfig(this);

         try {
            dc.makeConnection();
         } catch (Exception var4) {
            logger.warn("Couldn't open database: {} {}", dc.getJdbcUrl(), var4.toString());
         }
      }

   }

   void setLastUpdateTime(DateTime date) {
      if (date.isAfter(this.lastUpdateTime)) {
         this.lastUpdateTime = date;
      }

   }

   private void scheduleReloading(final Dataset ds) {
      Runnable reloader = new Runnable() {
         public void run() {
            ds.loadLayers();
            Config.logger.debug("num RAFs open = {}", RandomAccessFile.getOpenFiles().size());
         }
      };
      ScheduledFuture<?> future = this.scheduler.scheduleWithFixedDelay(reloader, 0L, 1L, TimeUnit.SECONDS);
      this.futures.put(ds.getId(), future);
      logger.debug("Scheduled auto-reloading of dataset {}", ds.getId());
   }

   public DateTime getLastUpdateTime() {
      return this.lastUpdateTime;
   }

   public Server getServer() {
      return this.server;
   }

   public void setServer(Server server) {
      this.server = server;
   }

   public Cache getCache() {
      return this.cache;
   }

   public void setCache(Cache cache) {
      this.cache = cache;
   }

   public Contact getContact() {
      return this.contact;
   }

   public void setContact(Contact contact) {
      this.contact = contact;
   }

   public Map<String, Dataset> getAllDatasets() {
      if (this.dbCollectionList.size() <= 0) {
         return Collections.unmodifiableMap(this.datasets);
      } else {
         LinkedHashMap<String, Dataset> out = new LinkedHashMap();
         out.putAll(this.datasets);
         Iterator i$ = this.dbCollectionList.iterator();

         while(i$.hasNext()) {
            DatabaseCollection dc = (DatabaseCollection)i$.next();
            out.putAll(dc.getAllDatasets());
         }

         return Collections.unmodifiableMap(out);
      }
   }

   public Dataset getDatasetById(String datasetId) {
      if (this.datasets.containsKey(datasetId)) {
         return (Dataset)this.datasets.get(datasetId);
      } else {
         Iterator i$ = this.dbCollectionList.iterator();

         Dataset d;
         do {
            if (!i$.hasNext()) {
               return null;
            }

            DatabaseCollection dc = (DatabaseCollection)i$.next();
            d = dc.getDatasetById(datasetId);
         } while(d == null);

         return d;
      }
   }

   public synchronized void addDataset(Dataset ds) {
      ds.setConfig(this);
      this.datasetList.add(ds);
      this.datasets.put(ds.getId(), ds);
      this.scheduleReloading(ds);
   }

   public synchronized void removeDataset(Dataset ds) {
      this.datasetList.remove(ds);
      this.datasets.remove(ds.getId());
      ScheduledFuture<?> future = (ScheduledFuture)this.futures.remove(ds.getId());
      if (future != null) {
         future.cancel(true);
      }

   }

   public synchronized void changeDatasetId(Dataset ds, String newId) {
      String oldId = ds.getId();
      this.datasets.remove(oldId);
      ScheduledFuture<?> future = (ScheduledFuture)this.futures.remove(oldId);
      ds.setId(newId);
      this.datasets.put(newId, ds);
      this.futures.put(newId, future);
      logger.debug("Changed dataset with ID {} to {}", oldId, newId);
   }

   static String checkEmpty(String s) {
      if (s == null) {
         return " ";
      } else {
         s = s.trim();
         return s.equals("") ? " " : s;
      }
   }

   void updateCredentialsProvider(Dataset ds) {
      logger.debug("Called updateCredentialsProvider, {}", ds.getLocation());
      if (WmsUtils.isOpendapLocation(ds.getLocation())) {
         String newLoc = "http" + ds.getLocation().substring(4);

         try {
            URL url = new URL(newLoc);
            String userInfo = url.getUserInfo();
            logger.debug("user info = {}", userInfo);
            if (userInfo != null) {
               this.credentialsProvider.addCredentials(url.getHost(), url.getPort() >= 0 ? url.getPort() : url.getDefaultPort(), userInfo);
            }

            ds.setLocation("dods" + newLoc.substring(4));
         } catch (MalformedURLException var5) {
            logger.warn(newLoc + " is not a valid url");
         }
      }

   }

   public void shutdown() {
      this.scheduler.shutdownNow();
      NetcdfDataset.shutdown();
      logger.info("Cleaned up Config object");
   }

   public String getTitle() {
      return this.server.getTitle();
   }

   public String getServerAbstract() {
      return this.server.getServerAbstract();
   }

   public int getMaxImageWidth() {
      return this.server.getMaxImageWidth();
   }

   public int getMaxImageHeight() {
      return this.server.getMaxImageHeight();
   }

   public Set<String> getKeywords() {
      String[] keysArray = this.server.getKeywords().split(",");
      Set<String> keywords = new LinkedHashSet(keysArray.length);
      String[] arr$ = keysArray;
      int len$ = keysArray.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String keyword = arr$[i$];
         keywords.add(keyword);
      }

      return keywords;
   }

   public boolean getAllowsGlobalCapabilities() {
      return this.server.isAllowGlobalCapabilities();
   }

   public String getServiceProviderUrl() {
      return this.server.getUrl();
   }

   public String getContactName() {
      return this.contact.getName();
   }

   public String getContactEmail() {
      return this.contact.getEmail();
   }

   public String getContactOrganization() {
      return this.contact.getOrg();
   }

   public String getContactTelephone() {
      return this.contact.getTel();
   }

   public String getThreddsCatalogLocation() {
      return this.threddsCatalogLocation;
   }

   public void setThreddsCatalogLocation(String threddsCatalogLocation) {
      this.threddsCatalogLocation = checkEmpty(threddsCatalogLocation);
   }

   public void setCredentialsProvider(NcwmsCredentialsProvider credentialsProvider) {
      this.credentialsProvider = credentialsProvider;
   }

   public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      Users users = (Users)applicationContext.getBean("users");
      if (users == null) {
         logger.error("Could not retrieve Users object from application context");
      } else {
         logger.debug("Setting admin password in Users object");
         users.setAdminPassword(this.server.getAdminPassword());
      }

   }

   public File getPaletteFilesLocation(ServletContext context) {
      return new File(context.getRealPath("/WEB-INF/conf/palettes"));
   }
}
