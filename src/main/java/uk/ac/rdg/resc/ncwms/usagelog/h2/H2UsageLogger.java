package uk.ac.rdg.resc.ncwms.usagelog.h2;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import org.h2.tools.Csv;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogger;

public class H2UsageLogger implements UsageLogger {
   private static final Logger logger = LoggerFactory.getLogger(H2UsageLogger.class);
   private static final String INSERT_COMMAND = "INSERT INTO usage_log(request_time, client_ip, client_hostname, client_referrer, client_user_agent, http_method, wms_version,wms_operation, exception_class, exception_message, crs, bbox_minx, bbox_miny, bbox_maxx, bbox_maxy, elevation, time_str, num_timesteps, image_width, image_height, layer, dataset_id, variable_id, time_to_extract_data_ms, used_cache, feature_info_lon, feature_info_lat, feature_info_col, feature_info_row, style_str, output_format, transparent, background_color, menu, remote_server_url) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
   private Connection conn;
   private DataSource dataSource;
   private File usageLogDir;

   public void init() throws Exception {
      WmsUtils.createDirectory(this.usageLogDir);
      String databasePath = (new File(this.usageLogDir, "usagelog")).getCanonicalPath();
      logger.debug("Usage logger database path = {}", databasePath);
      InputStream scriptIn = Thread.currentThread().getContextClassLoader().getResourceAsStream("/uk/ac/rdg/resc/ncwms/usagelog/h2/init.sql");
      if (scriptIn == null) {
         throw new Exception("Can't find initialization script init.sql");
      } else {
         InputStreamReader scriptReader = new InputStreamReader(scriptIn);

         try {
            Class.forName("org.h2.Driver");
            this.conn = DriverManager.getConnection("jdbc:h2:" + databasePath);
            this.conn.setAutoCommit(true);
            this.dataSource = new SingleConnectionDataSource(this.conn, true);
            RunScript.execute(this.conn, scriptReader);
         } catch (Exception var5) {
            this.close();
            throw var5;
         }

         logger.info("H2 Usage Logger initialized");
      }
   }

   public void logUsage(UsageLogEntry logEntry) {
      long startLog = System.currentTimeMillis();

      try {
         PreparedStatement ps = this.conn.prepareStatement("INSERT INTO usage_log(request_time, client_ip, client_hostname, client_referrer, client_user_agent, http_method, wms_version,wms_operation, exception_class, exception_message, crs, bbox_minx, bbox_miny, bbox_maxx, bbox_maxy, elevation, time_str, num_timesteps, image_width, image_height, layer, dataset_id, variable_id, time_to_extract_data_ms, used_cache, feature_info_lon, feature_info_lat, feature_info_col, feature_info_row, style_str, output_format, transparent, background_color, menu, remote_server_url) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
         ps.setObject(1, logEntry.getRequestTime().toDate());
         ps.setObject(2, logEntry.getClientIpAddress());
         ps.setObject(3, logEntry.getClientHost());
         ps.setObject(4, logEntry.getClientReferrer());
         ps.setObject(5, logEntry.getClientUserAgent());
         ps.setObject(6, logEntry.getHttpMethod());
         ps.setObject(7, logEntry.getWmsVersion());
         ps.setObject(8, logEntry.getWmsOperation());
         ps.setString(9, logEntry.getExceptionClass());
         ps.setString(10, logEntry.getExceptionMessage());
         ps.setString(11, logEntry.getCrs());
         ps.setObject(12, logEntry.getBbox() == null ? null : logEntry.getBbox()[0]);
         ps.setObject(13, logEntry.getBbox() == null ? null : logEntry.getBbox()[1]);
         ps.setObject(14, logEntry.getBbox() == null ? null : logEntry.getBbox()[2]);
         ps.setObject(15, logEntry.getBbox() == null ? null : logEntry.getBbox()[3]);
         ps.setString(16, logEntry.getElevation());
         ps.setString(17, logEntry.getTimeString());
         ps.setObject(18, logEntry.getNumTimeSteps());
         ps.setObject(19, logEntry.getWidth());
         ps.setObject(20, logEntry.getHeight());
         ps.setString(21, logEntry.getLayer());
         ps.setString(22, logEntry.getDatasetId());
         ps.setString(23, logEntry.getVariableId());
         ps.setObject(24, logEntry.getTimeToExtractDataMs());
         ps.setBoolean(25, logEntry.isUsedCache());
         ps.setObject(26, logEntry.getFeatureInfoLon());
         ps.setObject(27, logEntry.getFeatureInfoLat());
         ps.setObject(28, logEntry.getFeatureInfoPixelCol());
         ps.setObject(29, logEntry.getFeatureInfoPixelRow());
         ps.setString(30, logEntry.getStyle());
         ps.setString(31, logEntry.getOutputFormat());
         ps.setObject(32, logEntry.getTransparent());
         ps.setString(33, logEntry.getBackgroundColor());
         ps.setString(34, logEntry.getMenu());
         ps.setString(35, logEntry.getRemoteServerUrl());
         ps.executeUpdate();
      } catch (SQLException var8) {
         logger.error("Error writing to usage log", var8);
      } finally {
         logger.debug("Time to log: {} ms", System.currentTimeMillis() - startLog);
      }

   }

   public void writeCsv(OutputStream out) throws Exception {
      Writer writer = new OutputStreamWriter(out);
      Statement stmt = this.conn.createStatement();
      ResultSet results = stmt.executeQuery("SELECT * from usage_log");
      Csv.getInstance().write(writer, results);
   }

   public DataSource getDataSource() {
      return this.dataSource;
   }

   public void close() {
      if (this.conn != null) {
         try {
            this.conn.close();
         } catch (SQLException var2) {
            logger.error("Error closing H2 Usage Logger", var2);
         }
      }

      logger.info("H2 Usage Logger closed");
   }

   public void setUsageLogDirectory(File usageLogDir) {
      this.usageLogDir = usageLogDir;
   }
}
