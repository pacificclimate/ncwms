package uk.ac.rdg.resc.ncwms.config;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.rdg.resc.ncwms.graphics.ImageFormat;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class FrontPageController extends AbstractController {
   private Config config;

   protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
      Map<String, Object> models = new HashMap();
      models.put("config", this.config);
      models.put("supportedImageFormats", ImageFormat.getSupportedMimeTypes());
      return new ModelAndView("index", models);
   }

   public void setConfig(Config config) {
      this.config = config;
   }
}
