package uk.ac.rdg.resc.ncwms.controller;

class WmsVersion implements Comparable<WmsVersion> {
   private Integer value;
   private String str;
   private int hashCode;
   public static final WmsVersion VERSION_1_1_1 = new WmsVersion("1.1.1");
   public static final WmsVersion VERSION_1_3_0 = new WmsVersion("1.3.0");

   public WmsVersion(String versionStr) {
      String[] els = versionStr.split("\\.");
      if (els.length != 3) {
         throw new IllegalArgumentException(versionStr + " is not a valid WMS version number");
      } else {
         int x;
         int y;
         int z;
         try {
            x = Integer.parseInt(els[0]);
            y = Integer.parseInt(els[1]);
            z = Integer.parseInt(els[2]);
         } catch (NumberFormatException var7) {
            throw new IllegalArgumentException(versionStr + " is not a valid WMS version number");
         }

         if (y <= 99 && z <= 99) {
            this.str = x + "." + y + "." + z;
            this.value = 10000 * x + 100 * y + z;
            this.hashCode = 7 + 79 * this.value.hashCode();
         } else {
            throw new IllegalArgumentException(versionStr + " is not a valid WMS version number");
         }
      }
   }

   public int compareTo(WmsVersion otherVersion) {
      return this.value.compareTo(otherVersion.value);
   }

   public String toString() {
      return this.str;
   }

   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else if (obj instanceof WmsVersion) {
         WmsVersion other = (WmsVersion)obj;
         return this.value.equals(other.value);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.hashCode;
   }
}
