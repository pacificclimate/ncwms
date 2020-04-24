package uk.ac.rdg.resc.ncwms.config;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(
   name = "contact"
)
public class Contact {
   @Element(
      name = "name",
      required = false
   )
   private String name = " ";
   @Element(
      name = "organization",
      required = false
   )
   private String org = " ";
   @Element(
      name = "telephone",
      required = false
   )
   private String tel = " ";
   @Element(
      name = "email",
      required = false
   )
   private String email = " ";

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = Config.checkEmpty(name);
   }

   public String getOrg() {
      return this.org;
   }

   public void setOrg(String org) {
      this.org = Config.checkEmpty(org);
   }

   public String getTel() {
      return this.tel;
   }

   public void setTel(String tel) {
      this.tel = Config.checkEmpty(tel);
   }

   public String getEmail() {
      return this.email;
   }

   public void setEmail(String email) {
      this.email = Config.checkEmpty(email);
   }
}
