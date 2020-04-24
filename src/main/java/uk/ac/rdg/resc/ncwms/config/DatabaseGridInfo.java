package uk.ac.rdg.resc.ncwms.config;

public class DatabaseGridInfo {
   public int grid_id;
   public String srtext;
   public double xc_res;
   public double yc_res;
   public double xc_center_left;
   public double yc_center_upper;
   public int xc_size;
   public int yc_size;
   public String xc_dim_name;
   public String yc_dim_name;
   public String xc_units;
   public String yc_units;
   public boolean evenly_spaced_y;

   public DatabaseGridInfo(int grid_id, String srtext, double xc_res, double yc_res, double xc_center_left, double yc_center_upper, int xc_size, int yc_size, String xc_dim_name, String yc_dim_name, String xc_units, String yc_units, boolean evenly_spaced_y) {
      this.grid_id = grid_id;
      this.srtext = srtext;
      this.xc_res = xc_res;
      this.yc_res = yc_res;
      this.xc_center_left = xc_center_left;
      this.yc_center_upper = yc_center_upper;
      this.xc_size = xc_size;
      this.yc_size = yc_size;
      this.xc_dim_name = xc_dim_name;
      this.yc_dim_name = yc_dim_name;
      this.xc_units = xc_units;
      this.yc_units = yc_units;
      this.evenly_spaced_y = evenly_spaced_y;
   }
}
