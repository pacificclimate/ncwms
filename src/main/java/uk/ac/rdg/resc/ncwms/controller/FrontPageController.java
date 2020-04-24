package uk.ac.rdg.resc.ncwms.controller;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import uk.ac.rdg.resc.ncwms.graphics.ImageFormat;

public class FrontPageController extends AbstractController {
   private ServerConfig config;

   protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
      Map<String, Object> models = new HashMap();
      models.put("config", this.config);
      models.put("supportedImageFormats", ImageFormat.getSupportedMimeTypes());
      return new ModelAndView("index", models);
   }

   public void setConfig(ServerConfig config) {
      this.config = config;
   }
}
