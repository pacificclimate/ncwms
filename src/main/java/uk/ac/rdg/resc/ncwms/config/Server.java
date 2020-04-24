package uk.ac.rdg.resc.ncwms.config;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(
   name = "server"
)
public class Server {
   @Element(
      name = "title"
   )
   private String title = "My ncWMS server";
   @Element(
      name = "allowFeatureInfo",
      required = false
   )
   private boolean allowFeatureInfo = true;
   @Element(
      name = "maxImageWidth",
      required = false
   )
   private int maxImageWidth = 1024;
   @Element(
      name = "maxImageHeight",
      required = false
   )
   private int maxImageHeight = 1024;
   @Element(
      name = "abstract",
      required = false
   )
   private String abstr = " ";
   @Element(
      name = "keywords",
      required = false
   )
   private String keywords = " ";
   @Element(
      name = "url",
      required = false
   )
   private String url = " ";
   @Element(
      name = "adminpassword"
   )
   private String adminPassword = "ncWMS";
   @Element(
      name = "allowglobalcapabilities",
      required = false
   )
   private boolean allowGlobalCapabilities = true;

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String title) {
      this.title = Config.checkEmpty(title);
   }

   public boolean isAllowFeatureInfo() {
      return this.allowFeatureInfo;
   }

   public void setAllowFeatureInfo(boolean allowFeatureInfo) {
      this.allowFeatureInfo = allowFeatureInfo;
   }

   public boolean isAllowGlobalCapabilities() {
      return this.allowGlobalCapabilities;
   }

   public void setAllowGlobalCapabilities(boolean allowGlobalCapabilities) {
      this.allowGlobalCapabilities = allowGlobalCapabilities;
   }

   public int getMaxImageWidth() {
      return this.maxImageWidth;
   }

   public void setMaxImageWidth(int maxImageWidth) {
      this.maxImageWidth = maxImageWidth;
   }

   public int getMaxImageHeight() {
      return this.maxImageHeight;
   }

   public void setMaxImageHeight(int maxImageHeight) {
      this.maxImageHeight = maxImageHeight;
   }

   public String getServerAbstract() {
      return this.abstr;
   }

   public void setServerAbstract(String abstr) {
      this.abstr = Config.checkEmpty(abstr);
   }

   public String getKeywords() {
      return this.keywords;
   }

   public void setKeywords(String keywords) {
      this.keywords = Config.checkEmpty(keywords);
   }

   public String getUrl() {
      return this.url;
   }

   public void setUrl(String url) {
      this.url = Config.checkEmpty(url);
   }

   public String getAdminPassword() {
      return this.adminPassword;
   }

   public void setAdminPassword(String adminPassword) {
      this.adminPassword = adminPassword;
   }
}
