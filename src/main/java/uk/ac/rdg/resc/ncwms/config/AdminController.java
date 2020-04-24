package uk.ac.rdg.resc.ncwms.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.usagelog.h2.H2UsageLogger;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import uk.ac.rdg.resc.edal.util.Ranges;

public class AdminController extends MultiActionController {
   private Config config;
   private H2UsageLogger usageLogger;

   public ModelAndView displayAdminPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
      return new ModelAndView("admin", "config", this.config);
   }

   public ModelAndView displayDatasetStatusPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
      return new ModelAndView("admin_datasetStatus", "dataset", this.getDataset(request));
   }

   public ModelAndView displayLoadingPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
      return new ModelAndView("admin_loading", "dataset", this.getDataset(request));
   }

   private Dataset getDataset(HttpServletRequest request) throws Exception {
      String datasetId = request.getParameter("dataset");
      if (datasetId == null) {
         throw new Exception("Must provide a dataset id");
      } else {
         Dataset dataset = (Dataset)this.config.getAllDatasets().get(datasetId);
         if (dataset == null) {
            throw new Exception("There is no dataset with id " + datasetId);
         } else {
            return dataset;
         }
      }
   }

   public ModelAndView displayUsagePage(HttpServletRequest request, HttpServletResponse response) throws Exception {
      return new ModelAndView("admin_usage", "usageLogger", this.usageLogger);
   }

   public void downloadUsageLog(HttpServletRequest request, HttpServletResponse response) throws Exception {
      response.setContentType("application/excel");
      response.setHeader("Content-Disposition", "inline; filename=usageLog.csv");
      this.usageLogger.writeCsv(response.getOutputStream());
   }

   public void updateConfig(HttpServletRequest request, HttpServletResponse response) throws Exception {
      Contact contact = this.config.getContact();
      Server server = this.config.getServer();
      if (request.getParameter("contact.name") != null) {
         contact.setName(request.getParameter("contact.name"));
         contact.setOrg(request.getParameter("contact.org"));
         contact.setTel(request.getParameter("contact.tel"));
         contact.setEmail(request.getParameter("contact.email"));
         server.setTitle(request.getParameter("server.title"));
         server.setServerAbstract(request.getParameter("server.abstract"));
         server.setKeywords(request.getParameter("server.keywords"));
         server.setUrl(request.getParameter("server.url"));
         server.setMaxImageWidth(Integer.parseInt(request.getParameter("server.maximagewidth")));
         server.setMaxImageHeight(Integer.parseInt(request.getParameter("server.maximageheight")));
         server.setAllowFeatureInfo(request.getParameter("server.allowfeatureinfo") != null);
         server.setAllowGlobalCapabilities(request.getParameter("server.allowglobalcapabilities") != null);
         List<Dataset> datasetsToRemove = new ArrayList();
         Map<Dataset, String> changedIds = new HashMap();
         Iterator i$ = this.config.getAllDatasets().values().iterator();

         Dataset ds;
         while(i$.hasNext()) {
            ds = (Dataset)i$.next();
            boolean refreshDataset = false;
            if (request.getParameter("dataset." + ds.getId() + ".remove") != null) {
               datasetsToRemove.add(ds);
            } else {
               ds.setTitle(request.getParameter("dataset." + ds.getId() + ".title"));
               String newLocation = request.getParameter("dataset." + ds.getId() + ".location");
               if (!newLocation.trim().equals(ds.getLocation().trim())) {
                  refreshDataset = true;
               }

               ds.setLocation(newLocation);
               String newDataReaderClass = request.getParameter("dataset." + ds.getId() + ".reader");
               if (!newDataReaderClass.trim().equals(ds.getDataReaderClass().trim())) {
                  refreshDataset = true;
               }

               ds.setDataReaderClass(newDataReaderClass);
               boolean disabled = request.getParameter("dataset." + ds.getId() + ".disabled") != null;
               if (!disabled && ds.isDisabled()) {
                  refreshDataset = true;
               }

               ds.setDisabled(disabled);
               ds.setQueryable(request.getParameter("dataset." + ds.getId() + ".queryable") != null);
               ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset." + ds.getId() + ".updateinterval")));
               ds.setMoreInfo(request.getParameter("dataset." + ds.getId() + ".moreinfo"));
               ds.setCopyrightStatement(request.getParameter("dataset." + ds.getId() + ".copyright"));
               if (request.getParameter("dataset." + ds.getId() + ".refresh") != null) {
                  refreshDataset = true;
               }

               String newId = request.getParameter("dataset." + ds.getId() + ".id").trim();
               if (!newId.equals(ds.getId())) {
                  changedIds.put(ds, newId);
               }
            }

            if (refreshDataset) {
               ds.forceRefresh();
            }
         }

         i$ = datasetsToRemove.iterator();

         while(i$.hasNext()) {
            ds = (Dataset)i$.next();
            this.config.removeDataset(ds);
         }

         i$ = changedIds.keySet().iterator();

         while(i$.hasNext()) {
            ds = (Dataset)i$.next();
            this.config.changeDatasetId(ds, (String)changedIds.get(ds));
            ds.forceRefresh();
         }

         for(int i = 0; request.getParameter("dataset.new" + i + ".id") != null; ++i) {
            if (!request.getParameter("dataset.new" + i + ".id").trim().equals("")) {
               ds = new Dataset();
               ds.setId(request.getParameter("dataset.new" + i + ".id"));
               ds.setTitle(request.getParameter("dataset.new" + i + ".title"));
               ds.setLocation(request.getParameter("dataset.new" + i + ".location"));
               ds.setDataReaderClass(request.getParameter("dataset.new" + i + ".reader"));
               ds.setDisabled(request.getParameter("dataset.new" + i + ".disabled") != null);
               ds.setQueryable(request.getParameter("dataset.new" + i + ".queryable") != null);
               ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset.new" + i + ".updateinterval")));
               ds.setMoreInfo(request.getParameter("dataset.new" + i + ".moreinfo"));
               ds.setCopyrightStatement(request.getParameter("dataset.new" + i + ".copyright"));
               this.config.addDataset(ds);
            }
         }

         this.config.getCache().setEnabled(request.getParameter("cache.enable") != null);
         this.config.getCache().setElementLifetimeMinutes(Integer.parseInt(request.getParameter("cache.elementLifetime")));
         this.config.getCache().setMaxNumItemsInMemory(Integer.parseInt(request.getParameter("cache.maxNumItemsInMemory")));
         this.config.getCache().setEnableDiskStore(request.getParameter("cache.enableDiskStore") != null);
         this.config.getCache().setMaxNumItemsOnDisk(Integer.parseInt(request.getParameter("cache.maxNumItemsOnDisk")));
         String newThreddsCatalogLocation = request.getParameter("thredds.catalog.location");
         if (!this.config.getThreddsCatalogLocation().trim().equals(newThreddsCatalogLocation)) {
            this.config.setThreddsCatalogLocation(newThreddsCatalogLocation);
         }

         this.config.save();
      }

      response.sendRedirect("index.jsp");
   }

   public ModelAndView displayEditVariablesPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
      String datasetID = request.getParameter("dataset");
      if (datasetID == null) {
         throw new Exception("Must specify a dataset id");
      } else {
         Dataset ds = (Dataset)this.config.getAllDatasets().get(datasetID);
         if (ds == null) {
            throw new Exception("Must specify a valid dataset id");
         } else if (!ds.isReady()) {
            throw new Exception("Dataset must be ready before its variables can be edited");
         } else {
            Map<String, Object> models = new HashMap();
            models.put("dataset", ds);
            models.put("paletteNames", ColorPalette.getAvailablePaletteNames());
            return new ModelAndView("editVariables", models);
         }
      }
   }

   public void updateVariables(HttpServletRequest request, HttpServletResponse response) throws Exception {
      if (request.getParameter("save") != null) {
         Dataset ds = (Dataset)this.config.getAllDatasets().get(request.getParameter("dataset.id"));
         Iterator i$ = ds.getLayers().iterator();

         while(i$.hasNext()) {
            Layer layer = (Layer)i$.next();
            String newTitle = request.getParameter(layer.getId() + ".title").trim();
            float min = Float.parseFloat(request.getParameter(layer.getId() + ".scaleMin").trim());
            float max = Float.parseFloat(request.getParameter(layer.getId() + ".scaleMax").trim());
            Variable var = (Variable)ds.getVariables().get(layer.getId());
            var.setTitle(newTitle);
            var.setColorScaleRange(Ranges.newRange(min, max));
            var.setPaletteName(request.getParameter(layer.getId() + ".palette"));
            var.setNumColorBands(Integer.parseInt(request.getParameter(layer.getId() + ".numColorBands")));
            var.setScaling(request.getParameter(layer.getId() + ".scaling"));
         }

         this.config.save();
      }

      response.sendRedirect("index.jsp");
   }

   public void setConfig(Config config) {
      this.config = config;
   }

   public void setUsageLogger(H2UsageLogger usageLogger) {
      this.usageLogger = usageLogger;
   }
}
