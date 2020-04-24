package uk.ac.rdg.resc.ncwms.config;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.rdg.resc.ncwms.controller.AbstractMetadataController;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import org.springframework.web.servlet.ModelAndView;

class NcwmsMetadataController extends AbstractMetadataController {
   private final Config serverConfig;

   public NcwmsMetadataController(Config serverConfig, AbstractWmsController.LayerFactory layerFactory) {
      super(layerFactory);
      this.serverConfig = serverConfig;
   }

   public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response, UsageLogEntry usageLogEntry) throws MetadataException {
      try {
         String url = request.getParameter("url");
         if (url != null && !url.trim().equals("")) {
            usageLogEntry.setRemoteServerUrl(url);
            proxyRequest(url, request, response);
            return null;
         }
      } catch (Exception var5) {
         throw new MetadataException(var5);
      }

      return super.handleRequest(request, response, usageLogEntry);
   }

   static void proxyRequest(String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
      StringBuffer fullURL = new StringBuffer(url);
      boolean firstTime = true;
      Iterator i$ = request.getParameterMap().keySet().iterator();

      while(i$.hasNext()) {
         Object urlParamNameObj = i$.next();
         fullURL.append(firstTime ? "?" : "&");
         firstTime = false;
         String urlParamName = (String)urlParamNameObj;
         if (!urlParamName.equalsIgnoreCase("url")) {
            fullURL.append(urlParamName + "=" + request.getParameter(urlParamName));
         }
      }

      InputStream in = null;
      ServletOutputStream out = null;

      try {
         URLConnection conn = (new URL(fullURL.toString())).openConnection();
         response.setContentType(conn.getContentType());
         response.setContentLength(conn.getContentLength());
         in = conn.getInputStream();
         out = response.getOutputStream();
         byte[] buf = new byte[8192];

         int len;
         while((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
         }
      } finally {
         if (in != null) {
            in.close();
         }

         if (out != null) {
            out.close();
         }

      }

   }

   protected ModelAndView showMenu(HttpServletRequest request, UsageLogEntry usageLogEntry) throws Exception {
      Map<String, ? extends Dataset> allDatasets = this.serverConfig.getAllDatasets();
      String menu = "default";
      String menuFromRequest = request.getParameter("menu");
      if (menuFromRequest != null && !menuFromRequest.trim().equals("")) {
         menu = menuFromRequest.toLowerCase();
      }

      usageLogEntry.setMenu(menu);
      Map<String, Object> models = new HashMap();
      models.put("serverTitle", this.serverConfig.getTitle());
      models.put("datasets", allDatasets);
      return new ModelAndView(menu + "Menu", models);
   }
}
