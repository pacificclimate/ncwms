package uk.ac.rdg.resc.ncwms.config;

public class DatabaseVariable {
   public String id;
   public String title;
   public String description;
   public String units;
   public double varRangeMin;
   public double varRangeMax;
   public String palette;
   public String scaling;
   public int numColorBands;
   public String level_units;
   public int level_set_id;

   public DatabaseVariable(String id, String title, String description, String units, double varRangeMin, double varRangeMax, String palette, String scaling, int numColorBands, String level_units, int level_set_id) {
      this.id = id;
      this.title = title;
      this.description = description;
      this.units = units;
      this.varRangeMin = varRangeMin;
      this.varRangeMax = varRangeMax;
      this.palette = palette;
      this.scaling = scaling;
      this.numColorBands = numColorBands;
      this.level_units = level_units;
      this.level_set_id = level_set_id;
   }
}
