package uk.ac.rdg.resc.ncwms.config;

import java.io.File;
import java.util.Properties;

import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

public class NcwmsContext implements ApplicationContextAware {
   private static final String LOG_FILE_DIR_NAME = "logs";
   private static final String LOG_FILE_NAME = "ncWMS.log";
   private File workingDirectory = new File(System.getProperty("user.home"), ".ncWMS");
   private ApplicationContext applicationContext;

   public void init() throws Exception {
      WmsUtils.createDirectory(this.workingDirectory);
      File logDirectory = new File(this.workingDirectory, "logs");
      WmsUtils.createDirectory(logDirectory);
      File logFile = new File(logDirectory, "ncWMS.log");
      Properties logProps = new Properties();
      Resource logConfig = this.applicationContext.getResource("/WEB-INF/conf/log4j.properties");
      logProps.load(logConfig.getInputStream());
      logProps.put("log4j.appender.R.File", logFile.getPath());
      PropertyConfigurator.configure(logProps);
   }

   public File getWorkingDirectory() {
      return this.workingDirectory;
   }

   public void setWorkingDirectory(File workingDirectory) {
      if (!workingDirectory.isAbsolute()) {
         throw new IllegalArgumentException("The working directory must be an absolute path");
      } else {
         this.workingDirectory = workingDirectory;
      }
   }

   public Properties getProperties() {
      Properties props = new Properties();
      props.setProperty("ncwms.workingDirectory", this.workingDirectory.getPath());
      return props;
   }

   public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      this.applicationContext = applicationContext;
   }
}
