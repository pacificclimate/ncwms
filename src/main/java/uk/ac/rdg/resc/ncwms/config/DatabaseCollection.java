package uk.ac.rdg.resc.ncwms.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import uk.ac.rdg.resc.edal.cdm.ElevationAxis;
import uk.ac.rdg.resc.edal.cdm.ProjectedGrid;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.geotools.metadata.iso.extent.GeographicBoundingBoxImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ogc.WKTParser;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.ReferenceableAxis;
import uk.ac.rdg.resc.edal.coverage.grid.RegularAxis;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RectilinearGridImpl;
import uk.ac.rdg.resc.edal.coverage.grid.impl.ReferenceableAxisImpl;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularAxisImpl;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.util.Ranges;

@Root(
   name = "databasecollection"
)
public class DatabaseCollection {
   private static final Logger logger = LoggerFactory.getLogger(DatabaseCollection.class);
   @Attribute(
      name = "jdbcUrl"
   )
   private String jdbcUrl;
   @Attribute(
      name = "username"
   )
   private String username;
   @Attribute(
      name = "password"
   )
   private String password;
   @Attribute(
      name = "driverClassName"
   )
   private String driverClassName;
   @Attribute(
      name = "ensemble"
   )
   private String ensemble;
   @Attribute(
      name = "maxActiveConnections"
   )
   private String maxActive;
   @Attribute(
      name = "maxIdleConnections"
   )
   private String maxIdle;
   private static final int maxTSCacheEntries = 100;
   private static final int maxSmallItemCacheEntries = 1000;
   private static final int maxSmallItemLifetime = 1200;
   private Exception err;
   private Config config;
   private DataSource ds;
   private DatabaseCollection.ResultCache<Integer, HorizontalGrid> horizGridCache;
   private DatabaseCollection.ResultCache<Integer, ArrayList<DateTime>> timesetCache;
   private DatabaseCollection.ResultCache<String, DatabaseDataFile> DBDataFileCache;
   private DatabaseCollection.ResultCache<String, ArrayList<DatabaseVariable>> DBVariableCache;
   private DatabaseCollection.ResultCache<Integer, ArrayList<TimestepInfo>> tsInfoCache;

   public DatabaseCollection() {
      this.jdbcUrl = this.ensemble = this.username = this.password = this.driverClassName = "";
      this.maxActive = this.maxIdle = "100";
   }

   public DatabaseCollection(String jdbcUrl, String ensemble, String username, String password, String driverClassName) {
      this.jdbcUrl = jdbcUrl;
      this.ensemble = ensemble;
      this.username = username;
      this.password = password;
      this.driverClassName = driverClassName;
   }

   public void setConfig(Config config) {
      this.config = config;
   }

   public void setEnsemble(String ensemble) {
      this.ensemble = ensemble;
   }

   public String getEnsemble() {
      return this.ensemble;
   }

   public void setJdbcUrl(String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
   }

   public String getJdbcUrl() {
      return this.jdbcUrl;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getUsername() {
      return this.username;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getPassword() {
      return this.password;
   }

   public void setDriverClassName(String driverClassName) {
      this.driverClassName = driverClassName;
   }

   public String getDriverClassName() {
      return this.driverClassName;
   }

   public void makeConnection() throws Exception {
      PoolProperties p = new PoolProperties();
      p.setUrl(this.jdbcUrl);
      p.setDriverClassName(this.driverClassName);
      p.setUsername(this.username);
      p.setPassword(this.password);
      p.setMaxActive(Integer.parseInt(this.maxActive));
      p.setMaxIdle(Integer.parseInt(this.maxIdle));
      p.setValidationQuery("SELECT 1");
      this.ds = new DataSource(p);
      this.timesetCache = new DatabaseCollection.ResultCache(100);
      this.horizGridCache = new DatabaseCollection.ResultCache(1000, 1200);
      this.DBDataFileCache = new DatabaseCollection.ResultCache(1000, 1200);
      this.DBVariableCache = new DatabaseCollection.ResultCache(1000, 1200);
      this.tsInfoCache = new DatabaseCollection.ResultCache(100);
   }

   public String getIdForLocation(String location) {
      try {
         Connection con = this.ds.getConnection();
         PreparedStatement uniqueIdQuery = con.prepareStatement("SELECT unique_id FROM data_files where filename = ?");
         uniqueIdQuery.setString(1, location);
         ResultSet r = uniqueIdQuery.executeQuery();
         String result = null;
         if (r.next()) {
            result = r.getString("unique_id");
         }

         r.close();
         uniqueIdQuery.close();
         con.close();
         return result;
      } catch (Exception var6) {
         logger.warn("Failed to execute query for data_file's unique_id; check your database configuration.\n");
         return null;
      }
   }

   public List<DatabaseVariable> getVariablesForId(String unique_id) {
      ArrayList<DatabaseVariable> vars = (ArrayList)this.DBVariableCache.get(unique_id);
      if (vars != null) {
         return vars;
      } else {
         vars = new ArrayList();

         try {
            Connection con = this.ds.getConnection();
            PreparedStatement varsQuery = con.prepareStatement("SELECT variable_units, level_set_id, netcdf_variable_name, variable_long_name, variable_standard_name, range_min as range_min, range_max as range_max, level_units, level_set_id FROM data_file_variables NATURAL JOIN variable_aliases NATURAL LEFT JOIN variables NATURAL LEFT JOIN level_sets WHERE data_file_variable_id IN (SELECT data_file_variable_id from data_files natural join data_file_variables natural join ensemble_data_file_variables natural join ensembles_cur_version where unique_id = ? and ensemble_name=?)");
            varsQuery.setString(1, unique_id);
            varsQuery.setString(2, this.ensemble);
            ResultSet r = varsQuery.executeQuery();

            while(r.next()) {
               vars.add(new DatabaseVariable(r.getString("netcdf_variable_name"), r.getString("variable_standard_name"), r.getString("variable_long_name"), r.getString("variable_units"), r.getDouble("range_min"), r.getDouble("range_max"), "rainbow", "linear", 254, r.getString("level_units"), r.getInt("level_set_id")));
            }

            r.close();
            varsQuery.close();
            con.close();
         } catch (Exception var6) {
            logger.warn("Failed to execute query for variables; check your database config.\n");
         }

         this.DBVariableCache.put(unique_id, vars);
         return vars;
      }
   }

   public DatabaseDataFile getDataFileForId(String unique_id) {
      DatabaseDataFile f = (DatabaseDataFile)this.DBDataFileCache.get(unique_id);
      if (f != null) {
         return f;
      } else {
         try {
            Connection con = this.ds.getConnection();
            PreparedStatement dataFileQuery = con.prepareStatement("SELECT data_file_id, filename, unique_id, calendar, time_set_id, grid_id, srtext, grid_name, xc_grid_step, yc_grid_step, xc_origin, yc_origin, xc_count, yc_count, x_dim_name, y_dim_name, xc_units, yc_units, evenly_spaced_y from data_files NATURAL JOIN time_sets NATURAL JOIN data_file_variables NATURAL JOIN grids NATURAL LEFT JOIN spatial_ref_sys WHERE data_file_id = (SELECT data_file_id from data_files natural join ensemble_data_file_variables natural join ensembles_cur_version where unique_id = ? and ensemble_name = ? group by data_file_id) LIMIT 1");
            dataFileQuery.setString(1, unique_id);
            dataFileQuery.setString(2, this.ensemble);

            ResultSet r;
            DatabaseGridInfo gi;
            for(r = dataFileQuery.executeQuery(); r.next(); f = new DatabaseDataFile(r.getInt("data_file_id"), r.getString("filename"), r.getString("unique_id"), r.getString("calendar"), r.getInt("time_set_id"), gi)) {
               gi = new DatabaseGridInfo(r.getInt("grid_id"), r.getString("srtext"), r.getDouble("xc_grid_step"), r.getDouble("yc_grid_step"), r.getDouble("xc_origin"), r.getDouble("yc_origin"), r.getInt("xc_count"), r.getInt("yc_count"), r.getString("x_dim_name"), r.getString("y_dim_name"), r.getString("xc_units"), r.getString("yc_units"), r.getBoolean("evenly_spaced_y"));
            }

            r.close();
            dataFileQuery.close();
            con.close();
         } catch (Exception var7) {
            logger.warn("Failure when returning records; error: {}", var7.toString());
         }

         this.DBDataFileCache.put(unique_id, f);
         return f;
      }
   }

   public HorizontalGrid createHorizontalGrid(DatabaseDataFile f) {
      HorizontalGrid hg = (HorizontalGrid)this.horizGridCache.get(f.gi.grid_id);
      if (hg != null) {
         return hg;
      } else {
         ReferenceableAxis xRefAxis = this.getReferenceableAxis(f, "X");
         ReferenceableAxis yRefAxis = this.getReferenceableAxis(f, "Y");
         boolean isLatLon = f.gi.xc_units == "degrees_east" && f.gi.yc_units == "degrees_north";
         Object hg;
         if (isLatLon) {
            CoordinateReferenceSystem crs84 = DefaultGeographicCRS.WGS84;
            if (xRefAxis instanceof RegularAxis && yRefAxis instanceof RegularAxis) {
               hg = new RegularGridImpl((RegularAxis)xRefAxis, (RegularAxis)yRefAxis, crs84);
            } else {
               hg = new RectilinearGridImpl(xRefAxis, yRefAxis, crs84);
            }
         } else {
            ProjectionImpl proj = this.getProjectionImpl(f.gi.srtext);
            hg = new ProjectedGrid(xRefAxis, yRefAxis, this.getLatLonRect(xRefAxis, yRefAxis, proj), proj);
         }

         this.horizGridCache.put(f.gi.grid_id, hg);
         return (HorizontalGrid)hg;
      }
   }

   public List<DateTime> createTimesteps(DatabaseDataFile f) {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      ArrayList<DateTime> timevals = (ArrayList)this.timesetCache.get(f.time_set_id);
      if (timevals != null) {
         return timevals;
      } else {
         try {
            timevals = new ArrayList();
            Connection con = this.ds.getConnection();
            PreparedStatement tAxisQuery = con.prepareStatement("SELECT timestep, time_idx from times WHERE time_set_id = ? order by time_idx");
            tAxisQuery.setInt(1, f.time_set_id);
            ResultSet r = tAxisQuery.executeQuery();
            r.setFetchSize(5000);

            while(r.next()) {
               timevals.add(new DateTime(r.getTimestamp("timestep", cal), DateTimeZone.UTC));
            }

            r.close();
            tAxisQuery.close();
            con.close();
            this.timesetCache.put(f.time_set_id, timevals);
            return timevals;
         } catch (Exception var7) {
            logger.warn("Failed to execute query to fetch timesteps for data_file; check your database configuration.\n");
            return null;
         }
      }
   }

   public ArrayList<TimestepInfo> createTimestepInfo(DatabaseDataFile f) {
      ArrayList<TimestepInfo> timesteps = (ArrayList)this.tsInfoCache.get(f.time_set_id);
      if (timesteps != null) {
         return timesteps;
      } else {
         timesteps = new ArrayList();
         List<DateTime> ts = this.createTimesteps(f);
         int i = 0;
         timesteps.ensureCapacity(ts.size());

         for(Iterator i$ = ts.iterator(); i$.hasNext(); ++i) {
            DateTime dt = (DateTime)i$.next();
            TimestepInfo tInfo = new TimestepInfo(dt, 0, i);
            timesteps.add(tInfo);
         }

         Collections.sort(timesteps, new CompareTimestepInfoByTime());
         this.tsInfoCache.put(f.time_set_id, timesteps);
         return timesteps;
      }
   }

   public ElevationAxis createZAxis(DatabaseDataFile f, DatabaseVariable v) {
      try {
         ArrayList<Double> values = new ArrayList();
         boolean isPressure = false;
         boolean isPositive = true;
         if (v.level_units != null) {
            Connection con = this.ds.getConnection();
            PreparedStatement zAxisQuery = con.prepareStatement("SELECT vertical_level,level_idx FROM levels WHERE level_set_id = ? order by level_idx");
            zAxisQuery.setInt(1, v.level_set_id);
            ResultSet r = zAxisQuery.executeQuery();

            while(r.next()) {
               values.add(r.getDouble("pressure_level"));
            }

            r.close();
            zAxisQuery.close();
            isPositive = (Double)values.get(0) < (Double)values.get(1);
            isPressure = v.level_units == "Pa";
            con.close();
         }

         return new ElevationAxis(values, v.level_units, isPositive, isPressure);
      } catch (Exception var9) {
         logger.warn("Failed to fetch Z axis for variable. Check your database configuration.\n");
         return null;
      }
   }

   private LatLonRect getLatLonRect(ReferenceableAxis xRefAxis, ReferenceableAxis yRefAxis, ProjectionImpl proj) {
      Envelope xEnv = xRefAxis.getExtent();
      Envelope yEnv = yRefAxis.getExtent();
      if (proj.isLatLon()) {
         LatLonPoint ul = new LatLonPointImpl(yEnv.getMaximum(0), xEnv.getMinimum(0));
         LatLonPoint lr = new LatLonPointImpl(yEnv.getMinimum(0), xEnv.getMaximum(0));
         return new LatLonRect(ul, lr);
      } else {
         double[][] points = new double[][]{{xEnv.getMinimum(0), xEnv.getMaximum(0), xEnv.getMinimum(0), xEnv.getMaximum(0)}, {yEnv.getMinimum(0), yEnv.getMinimum(0), yEnv.getMaximum(0), yEnv.getMaximum(0)}};
         double[][] llPoints = proj.projToLatLon(points);
         double xMax = -1.0E38D;
         double xMin = 1.0E38D;
         double yMax = -1.0E38D;
         double yMin = 1.0E38D;

         int idx;
         double y;
         for(idx = 0; idx < llPoints[0].length; ++idx) {
            y = llPoints[0][idx];
            if (y > xMax) {
               xMax = y;
            }

            if (y < xMin) {
               xMin = y;
            }
         }

         for(idx = 0; idx < llPoints[1].length; ++idx) {
            y = llPoints[1][idx];
            if (y > yMax) {
               yMax = y;
            }

            if (y < yMin) {
               yMin = y;
            }
         }

         LatLonPoint ul = new LatLonPointImpl(yMax, xMin);
         LatLonPoint lr = new LatLonPointImpl(yMin, xMax);
         return new LatLonRect(ul, lr);
      }
   }

   public GeographicBoundingBox getBbox(HorizontalGrid hg) {
      try {
         return new GeographicBoundingBoxImpl(hg.getExtent());
      } catch (Exception var3) {
         logger.error("Error setting bounding box; defaulting to world: ", var3);
         return new GeographicBoundingBoxImpl(-180.0D, 180.0D, -90.0D, 90.0D);
      }
   }

   private ReferenceableAxis getReferenceableAxis(DatabaseDataFile f, String axis) {
      if (axis == "X") {
         return new RegularAxisImpl(f.gi.xc_dim_name, f.gi.xc_center_left, f.gi.xc_res, f.gi.xc_size, f.gi.xc_units == "degrees_east");
      } else if (axis != "Y") {
         return null;
      } else if (f.gi.evenly_spaced_y) {
         return new RegularAxisImpl(f.gi.yc_dim_name, f.gi.yc_center_upper, f.gi.yc_res, f.gi.yc_size, false);
      } else {
         try {
            Connection con = this.ds.getConnection();
            ArrayList<Double> yc = new ArrayList();
            PreparedStatement ycAxisQuery;
            if (f.gi.yc_res > 0.0D) {
               ycAxisQuery = con.prepareStatement("SELECT y_center FROM y_cell_bounds WHERE grid_id = ? ORDER BY y_center ASC");
            } else {
               ycAxisQuery = con.prepareStatement("SELECT y_center FROM y_cell_bounds WHERE grid_id = ? ORDER BY y_center DESC");
            }

            ycAxisQuery.setInt(1, f.gi.grid_id);
            ResultSet r = ycAxisQuery.executeQuery();

            while(r.next()) {
               yc.add(r.getDouble("y_center"));
            }

            r.close();
            ycAxisQuery.close();
            con.close();
            double[] yc_array = new double[yc.size()];

            for(int i = 0; i < yc.size(); ++i) {
               yc_array[i] = (Double)yc.get(i);
            }

            return new ReferenceableAxisImpl(f.gi.yc_dim_name, yc_array, false);
         } catch (Exception var9) {
            logger.warn("Failed to fetch Y coordinates for variable: {}", var9.toString());
            return null;
         }
      }
   }

   private ProjectionImpl getProjectionImpl(String proj) {
      ProjectionImpl projImpl = null;

      try {
         WKTParser wp = new WKTParser(proj);
         projImpl = WKTParser.convertWKTToProjection(wp);
      } catch (Exception var4) {
         logger.warn("Couldn't parse WKT string into a Projection.\n");
      }

      return projImpl;
   }

   private Dataset getDataset(String unique_id) {
      DatabaseDataFile f = this.getDataFileForId(unique_id);
      if (f == null) {
         return null;
      } else {
         DatabaseDataset d = new DatabaseDataset();
         d.setId(unique_id);
         d.setLocation(f.filename);
         d.setTitle(unique_id);
         d.setConfig(this.config);
         d.setDatabaseDataFile(f);

         try {
            DatabaseDataReader dr = new DatabaseDataReader(this);
            d.setDataReader(dr);
         } catch (Exception var8) {
            logger.warn("Something weird happened when setting the data reader. Code is screwed.\n");
            return null;
         }

         List<DatabaseVariable> vars = this.getVariablesForId(unique_id);
         d.setDatabaseVariables(vars);
         Iterator i$ = vars.iterator();

         while(i$.hasNext()) {
            DatabaseVariable v = (DatabaseVariable)i$.next();
            d.addVariable(this.createVariable(v.id, v.title, v.varRangeMin, v.varRangeMax, v.palette, v.scaling, v.numColorBands));
         }

         try {
            d.loadLayers();
            return d;
         } catch (Exception var7) {
            logger.warn("Something bad happened when trying to load layers. Error: {}", var7.toString());
            var7.printStackTrace();
            return null;
         }
      }
   }

   private Variable createVariable(String id, String title, double varRangeMin, double varRangeMax, String palette, String scaling, int numColorBands) {
      Variable v = new Variable();
      v.setId(id);
      v.setTitle(title);
      v.setColorScaleRange(Ranges.newRange((float)varRangeMin, (float)varRangeMax));
      v.setPaletteName(palette);
      v.setScaling(scaling);
      v.setNumColorBands(numColorBands);
      return v;
   }

   public Map<String, Dataset> getAllDatasets() {
      LinkedHashMap<String, Dataset> datasets = new LinkedHashMap();
      ArrayList dsIDs = new ArrayList();

      try {
         Connection con = this.ds.getConnection();
         PreparedStatement uniqueIdQuery = con.prepareStatement("select unique_id from ensemble_data_file_variables natural join data_file_variables natural join data_files where ensemble_id=(select ensemble_id from ensembles_cur_version where ensemble_name=?) group by unique_id");
         uniqueIdQuery.setString(1, this.ensemble);
         ResultSet r = uniqueIdQuery.executeQuery();

         while(r.next()) {
            dsIDs.add(r.getString("unique_id"));
         }

         r.close();
         uniqueIdQuery.close();
         con.close();
         Iterator i$ = dsIDs.iterator();

         while(i$.hasNext()) {
            String id = (String)i$.next();
            datasets.put(id, this.getDataset(id));
         }

         return datasets;
      } catch (Exception var8) {
         logger.warn("Something weird happened when querying for unique IDs for ensemble: {}\n", var8.toString());
         return null;
      }
   }

   public Dataset getDatasetById(String datasetId) {
      return this.getDataset(datasetId);
   }

   private class ResultCache<K, V> {
      private LinkedHashMap<K, DatabaseCollection.ResultCacheTuple<V>> cache;
      private int maxCacheSize;
      private int maxLifetime;
      private Timer reaperTimer;

      public ResultCache(int maxCacheSize) {
         this.maxCacheSize = maxCacheSize;
         this.cache = new LinkedHashMap();
      }

      public ResultCache(int maxCacheSize, int maxLifetime) {
         this(maxCacheSize);
         this.reaperTimer = new Timer("GRIM REAPER");
         this.reaperTimer.scheduleAtFixedRate(new DatabaseCollection.ResultCache.ReaperTask(), 0L, (long)(1000 * maxLifetime));
      }

      public synchronized V get(K key) {
         DatabaseCollection.ResultCacheTuple<V> tc = (DatabaseCollection.ResultCacheTuple)this.cache.get(key);
         return tc == null ? null : tc.get();
      }

      public synchronized void put(K key, V dat) {
         if (this.cache.size() >= this.maxCacheSize - 1) {
            Set<K> tcs = this.cache.keySet();
            K idx = null;
            float minScore = Float.POSITIVE_INFINITY;
            Iterator i$ = tcs.iterator();

            while(i$.hasNext()) {
               K i = i$.next();
               float score = ((DatabaseCollection.ResultCacheTuple)this.cache.get(i)).getCacheScore();
               if (score < minScore) {
                  idx = i;
                  minScore = score;
               }
            }

            this.cache.remove(idx);
         }

         this.cache.put(key, DatabaseCollection.this.new ResultCacheTuple(dat));
      }

      private class ReaperTask extends TimerTask {
         long curtime;

         private ReaperTask() {
            this.curtime = (new Date()).getTime();
         }

         public void run() {
            synchronized(ResultCache.this.cache) {
               Iterator i$ = ResultCache.this.cache.keySet().iterator();

               while(i$.hasNext()) {
                  K i = i$.next();
                  if (this.curtime - ((DatabaseCollection.ResultCacheTuple)ResultCache.this.cache.get(i)).firstAccess() > (long)ResultCache.this.maxLifetime) {
                     ResultCache.this.cache.remove(i);
                  }
               }

            }
         }

         // $FF: synthetic method
         ReaperTask(Object x1) {
            this();
         }
      }
   }

   private class ResultCacheTuple<T> implements Comparable {
      private T dat;
      private long firstAccess;
      private int cacheHits;

      public ResultCacheTuple(T dat) {
         this.dat = dat;
         this.cacheHits = 1;
         this.firstAccess = (new Date()).getTime();
      }

      public T get() {
         ++this.cacheHits;
         return this.dat;
      }

      public long firstAccess() {
         return this.firstAccess;
      }

      public float getCacheScore() {
         long curTime = (new Date()).getTime();
         return (float)(curTime - this.firstAccess) / (float)this.cacheHits;
      }

      public int compareTo(Object o) {
         return (int)Math.signum(this.getCacheScore() - ((DatabaseCollection.ResultCacheTuple)o).getCacheScore());
      }
   }
}
