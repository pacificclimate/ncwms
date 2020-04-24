package uk.ac.rdg.resc.ncwms.config;

import java.util.List;
import org.joda.time.DateTime;
import org.simpleframework.xml.Root;

@Root(
   name = "dataset"
)
public class DatabaseDataset extends Dataset {
   private DatabaseDataReader dr;
   private DatabaseDataFile f;
   private List<DatabaseVariable> dv;

   public DataReader getDataReader() {
      return this.dr;
   }

   public void setDataReader(DatabaseDataReader dr) {
      this.dr = dr;
   }

   public void setDatabaseDataFile(DatabaseDataFile f) {
      this.f = f;
   }

   public DatabaseDataFile getDatabaseDataFile() {
      return this.f;
   }

   public void setDatabaseVariables(List<DatabaseVariable> dv) {
      this.dv = dv;
   }

   public List<DatabaseVariable> getDatabaseVariables() {
      return this.dv;
   }

   void loadLayers() {
      try {
         this.state = this.lastSuccessfulUpdateTime == null ? Dataset.State.LOADING : Dataset.State.UPDATING;
         this.doLoadLayers();
         this.err = null;
         this.numErrorsInARow = 0;
         this.state = Dataset.State.READY;
         this.lastSuccessfulUpdateTime = new DateTime();
         logger.debug("Loaded metadata for {}", this.id);
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

   protected void doLoadLayers() throws Exception {
      this.layers = this.dr.getAllLayers(this);
      this.appendLoadingProgress("loaded layers");
      this.readLayerConfig();
      this.appendLoadingProgress("attributes overridden");
      this.appendLoadingProgress("Finished loading metadata");
   }
}
