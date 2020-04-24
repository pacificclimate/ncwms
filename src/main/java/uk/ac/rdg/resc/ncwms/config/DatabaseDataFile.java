package uk.ac.rdg.resc.ncwms.config;

public class DatabaseDataFile {
   public int data_file_id;
   public String filename;
   public String unique_id;
   public String calendar;
   public int time_set_id;
   public DatabaseGridInfo gi;

   public DatabaseDataFile(int data_file_id, String filename, String unique_id, String calendar, int time_set_id, DatabaseGridInfo gi) {
      this.data_file_id = data_file_id;
      this.filename = filename;
      this.unique_id = unique_id;
      this.calendar = calendar;
      this.time_set_id = time_set_id;
      this.gi = gi;
   }
}
