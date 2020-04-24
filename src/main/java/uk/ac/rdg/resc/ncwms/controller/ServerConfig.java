package uk.ac.rdg.resc.ncwms.controller;

import java.io.File;
import java.util.Set;
import javax.servlet.ServletContext;
import org.joda.time.DateTime;

public interface ServerConfig {
   String getTitle();

   File getPaletteFilesLocation(ServletContext var1);

   int getMaxImageWidth();

   int getMaxImageHeight();

   String getServerAbstract();

   Set<String> getKeywords();

   DateTime getLastUpdateTime();

   String getServiceProviderUrl();

   String getContactName();

   String getContactOrganization();

   String getContactTelephone();

   String getContactEmail();
}
