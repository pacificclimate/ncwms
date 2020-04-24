package uk.ac.rdg.resc.ncwms.security;

import java.io.Serializable;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataAccessException;

public class Users implements UserDetailsService, Serializable {
   private Users.AdminUser adminUser = new Users.AdminUser();

   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
      if (username.equals("admin")) {
         return this.adminUser;
      } else {
         throw new UsernameNotFoundException(username);
      }
   }

   public void setAdminPassword(String password) {
      this.adminUser.password = password;
   }

   private class AdminUser implements UserDetails {
      private String password;

      private AdminUser() {
         this.password = null;
      }

      public boolean isEnabled() {
         return this.password != null;
      }

      public boolean isCredentialsNonExpired() {
         return true;
      }

      public boolean isAccountNonLocked() {
         return true;
      }

      public boolean isAccountNonExpired() {
         return true;
      }

      public String getUsername() {
         return "admin";
      }

      public String getPassword() {
         return this.password;
      }

      public GrantedAuthority[] getAuthorities() {
         return new GrantedAuthority[]{new GrantedAuthority() {
            public String getAuthority() {
               return "ROLE_ADMIN";
            }
         }};
      }

      // $FF: synthetic method
      AdminUser(Object x1) {
         this();
      }
   }
}
