package uk.ac.rdg.resc.ncwms.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import org.joda.time.DateTime;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;

@Root(
   name = "dataset"
)
public class Dataset implements uk.ac.rdg.resc.ncwms.wms.Dataset {
   protected static final Logger logger = LoggerFactory.getLogger(Dataset.class);
   @Attribute(
      name = "id"
   )
   protected String id;
   @Attribute(
      name = "location"
   )
   private String location;
   @Attribute(
      name = "queryable",
      required = false
   )
   private boolean queryable = true;
   @Attribute(
      name = "dataReaderClass",
      required = false
   )
   private String dataReaderClass = "";
   @Attribute(
      name = "copyrightStatement",
      required = false
   )
   private String copyrightStatement = "";
   @Attribute(
      name = "moreInfo",
      required = false
   )
   private String moreInfo = "";
   @Attribute(
      name = "disabled",
      required = false
   )
   private boolean disabled = false;
   @Attribute(
      name = "title"
   )
   private String title;
   @Attribute(
      name = "updateInterval",
      required = false
   )
   private int updateInterval = -1;
   @ElementList(
      name = "variables",
      type = Variable.class,
      required = false
   )
   private ArrayList<Variable> variableList = new ArrayList();
   private Config config;
   protected Dataset.State state;
   protected Exception err;
   protected int numErrorsInARow;
   private List<String> loadingProgress;
   private Map<String, Variable> variables;
   protected DateTime lastSuccessfulUpdateTime;
   protected DateTime lastFailedUpdateTime;
   protected Map<String, Layer> layers;

   public Dataset() {
      this.state = Dataset.State.NEEDS_REFRESH;
      this.numErrorsInARow = 0;
      this.loadingProgress = new ArrayList();
      this.variables = new LinkedHashMap();
      this.lastSuccessfulUpdateTime = null;
      this.lastFailedUpdateTime = null;
      this.layers = Collections.emptyMap();
   }

   @Validate
   public void validate() throws PersistenceException {
      List<String> varIds = new ArrayList();
      Iterator i$ = this.variableList.iterator();

      while(i$.hasNext()) {
         Variable var = (Variable)i$.next();
         String varId = var.getId();
         if (varIds.contains(varId)) {
            throw new PersistenceException("Duplicate variable id %s", new Object[]{varId});
         }

         varIds.add(varId);
      }

   }

   @Commit
   public void build() {
      Iterator i$ = this.variableList.iterator();

      while(i$.hasNext()) {
         Variable var = (Variable)i$.next();
         var.setDataset(this);
         this.variables.put(var.getId(), var);
      }

   }

   public String getId() {
      return this.id;
   }

   public void setId(String id) {
      this.id = id.trim();
   }

   public String getLocation() {
      return this.location;
   }

   public void setLocation(String location) {
      this.location = location.trim();
   }

   void setConfig(Config config) {
      this.config = config;
   }

   public synchronized boolean isReady() {
      return !this.isDisabled() && (this.state == Dataset.State.READY || this.state == Dataset.State.UPDATING);
   }

   public synchronized boolean isLoading() {
      return !this.isDisabled() && (this.state == Dataset.State.NEEDS_REFRESH || this.state == Dataset.State.LOADING);
   }

   public boolean isError() {
      return this.err != null;
   }

   public Exception getException() {
      return this.err;
   }

   public Dataset.State getState() {
      return this.state;
   }

   public void setState(Dataset.State state) {
      this.state = state;
   }

   public boolean isQueryable() {
      return this.queryable;
   }

   public void setQueryable(boolean queryable) {
      this.queryable = queryable;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public String toString() {
      return "id: " + this.id + ", location: " + this.location;
   }

   public String getDataReaderClass() {
      return this.dataReaderClass;
   }

   void setDataReaderClass(String dataReaderClass) throws Exception {
      this.dataReaderClass = dataReaderClass;
   }

   public int getUpdateInterval() {
      return this.updateInterval;
   }

   void setUpdateInterval(int updateInterval) {
      this.updateInterval = updateInterval;
   }

   public DateTime getLastUpdateTime() {
      return this.lastSuccessfulUpdateTime;
   }

   public Layer getLayerById(String layerId) {
      return (Layer)this.layers.get(layerId);
   }

   public Collection<Layer> getLayers() {
      return this.layers.values();
   }

   public boolean isDisabled() {
      return this.disabled;
   }

   public void setDisabled(boolean disabled) {
      this.disabled = disabled;
   }

   public String getCopyrightStatement() {
      if (this.copyrightStatement != null && !this.copyrightStatement.trim().equals("")) {
         int currentYear = (new DateTime()).getYear();
         return this.copyrightStatement.replaceAll("\\$\\{year\\}", "" + currentYear);
      } else {
         return "";
      }
   }

   public void setCopyrightStatement(String copyrightStatement) {
      this.copyrightStatement = copyrightStatement;
   }

   public String getMoreInfoUrl() {
      return this.moreInfo;
   }

   public void setMoreInfo(String moreInfo) {
      this.moreInfo = moreInfo;
   }

   public List<String> getLoadingProgress() {
      return this.loadingProgress;
   }

   protected void appendLoadingProgress(String loadingProgress) {
      this.loadingProgress.add(loadingProgress);
   }

   public Map<String, Variable> getVariables() {
      return this.variables;
   }

   public void addVariable(Variable var) {
      var.setDataset(this);
      this.variableList.add(var);
      this.variables.put(var.getId(), var);
   }

   void forceRefresh() {
      this.err = null;
      this.state = Dataset.State.NEEDS_REFRESH;
   }

   void loadLayers() {
      this.loadingProgress = new ArrayList();
      Thread.currentThread().setName("load-metadata-" + this.id);
      if (this.needsRefresh()) {
         try {
            this.state = this.lastSuccessfulUpdateTime == null ? Dataset.State.LOADING : Dataset.State.UPDATING;
            this.doLoadLayers();
            this.err = null;
            this.numErrorsInARow = 0;
            this.state = Dataset.State.READY;
            this.lastSuccessfulUpdateTime = new DateTime();
            logger.debug("Loaded metadata for {}", this.id);
            this.config.setLastUpdateTime(this.lastSuccessfulUpdateTime);
            this.config.save();
         } catch (Exception var2) {
            this.state = Dataset.State.ERROR;
            ++this.numErrorsInARow;
            this.lastFailedUpdateTime = new DateTime();
            if (this.err == null || this.err.getClass() != var2.getClass()) {
               logger.error(var2.getClass().getName() + " loading metadata for dataset " + this.id, var2);
            }

            this.err = var2;
         }

      }
   }

   private boolean needsRefresh() {
      logger.debug("Last update time for dataset {} is {}", this.id, this.lastSuccessfulUpdateTime);
      logger.debug("State of dataset {} is {}", this.id, this.state);
      logger.debug("Disabled = {}", this.disabled);
      if (!this.disabled && this.state != Dataset.State.LOADING && this.state != Dataset.State.UPDATING) {
         if (this.state == Dataset.State.NEEDS_REFRESH) {
            return true;
         } else if (this.state == Dataset.State.ERROR) {
            double delaySeconds = Math.pow(2.0D, (double)this.numErrorsInARow);
            delaySeconds = Math.min(delaySeconds, 600.0D);
            boolean needsRefresh = this.lastFailedUpdateTime == null ? true : (new DateTime()).isAfter(this.lastFailedUpdateTime.plusSeconds((int)delaySeconds));
            logger.debug("delay = {} seconds, needsRefresh = {}", delaySeconds, needsRefresh);
            return needsRefresh;
         } else {
            return this.updateInterval < 0 ? false : (new DateTime()).isAfter(this.lastSuccessfulUpdateTime.plusMinutes(this.updateInterval));
         }
      } else {
         return false;
      }
   }

   protected void doLoadLayers() throws Exception {
      logger.debug("Getting data reader of type {}", this.dataReaderClass);
      DataReader dr = DataReader.forName(this.dataReaderClass);
      this.config.updateCredentialsProvider(this);
      this.layers = dr.getAllLayers(this);
      this.appendLoadingProgress("loaded layers");
      this.readLayerConfig();
      this.appendLoadingProgress("attributes overridden");
      this.appendLoadingProgress("Finished loading metadata");
   }

   protected void readLayerConfig() {
      Iterator i$ = this.getLayers().iterator();

      while(i$.hasNext()) {
         Layer layer = (Layer)i$.next();
         Variable var = (Variable)this.getVariables().get(layer.getId());
         if (var == null) {
            var = new Variable();
            var.setId(layer.getId());
            this.addVariable(var);
         }

         if (var.getTitle() == null) {
            var.setTitle(layer.getTitle());
         }

         if (var.getColorScaleRange() == null) {
            this.appendLoadingProgress("Reading min-max data for layer " + layer.getName());

            Range valueRange;
            try {
               valueRange = WmsUtils.estimateValueRange(layer);
               if (valueRange.isEmpty()) {
                  valueRange = Ranges.newRange(-50.0F, 50.0F);
               } else if (((Float)valueRange.getMinimum()).equals(valueRange.getMaximum())) {
                  valueRange = Ranges.newRange(valueRange.getMinimum(), (Float)valueRange.getMaximum() + 1.0F);
               } else {
                  float diff = (Float)valueRange.getMaximum() - (Float)valueRange.getMinimum();
                  valueRange = Ranges.newRange((Float)valueRange.getMinimum() - 0.05F * diff, (Float)valueRange.getMaximum() + 0.05F * diff);
               }
            } catch (Exception var6) {
               logger.error("Error reading min-max from layer " + layer.getId() + " in dataset " + this.id, var6);
               valueRange = Ranges.newRange(-50.0F, 50.0F);
            }

            var.setColorScaleRange(valueRange);
         }
      }

   }

   public static enum State {
      NEEDS_REFRESH,
      LOADING,
      READY,
      UPDATING,
      ERROR;
   }
}
