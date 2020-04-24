package uk.ac.rdg.resc.ncwms.config;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(
   name = "cache"
)
public class Cache {
   @Attribute(
      name = "enabled",
      required = false
   )
   private boolean enabled = false;
   @Element(
      name = "elementLifetimeMinutes",
      required = false
   )
   private int elementLifetimeMinutes = 1440;
   @Element(
      name = "maxNumItemsInMemory",
      required = false
   )
   private int maxNumItemsInMemory = 200;
   @Element(
      name = "enableDiskStore",
      required = false
   )
   private boolean enableDiskStore = true;
   @Element(
      name = "maxNumItemsOnDisk",
      required = false
   )
   private int maxNumItemsOnDisk = 2000;

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public int getElementLifetimeMinutes() {
      return this.elementLifetimeMinutes;
   }

   public void setElementLifetimeMinutes(int elementLifetimeMinutes) {
      this.elementLifetimeMinutes = elementLifetimeMinutes;
   }

   public int getMaxNumItemsInMemory() {
      return this.maxNumItemsInMemory;
   }

   public void setMaxNumItemsInMemory(int maxNumItemsInMemory) {
      this.maxNumItemsInMemory = maxNumItemsInMemory;
   }

   public boolean isEnableDiskStore() {
      return this.enableDiskStore;
   }

   public void setEnableDiskStore(boolean enableDiskStore) {
      this.enableDiskStore = enableDiskStore;
   }

   public int getMaxNumItemsOnDisk() {
      return this.maxNumItemsOnDisk;
   }

   public void setMaxNumItemsOnDisk(int maxNumItemsOnDisk) {
      this.maxNumItemsOnDisk = maxNumItemsOnDisk;
   }
}
