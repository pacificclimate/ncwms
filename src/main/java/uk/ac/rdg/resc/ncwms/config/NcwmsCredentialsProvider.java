package uk.ac.rdg.resc.ncwms.config;

import java.util.HashMap;
import java.util.Map;
import opendap.dap.DConnect2;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.net.HttpClientManager;
import ucar.unidata.io.http.HTTPRandomAccessFile;

public class NcwmsCredentialsProvider implements CredentialsProvider {
   private static final Logger logger = LoggerFactory.getLogger(NcwmsCredentialsProvider.class);
   private Map<String, Credentials> creds = new HashMap();

   public void init() {
      HttpClient client = HttpClientManager.init(this, (String)null);
      DConnect2.setHttpClient(client);
      HTTPRandomAccessFile.setHttpClient(client);
      logger.debug("NcwmsCredentialsProvider initialized");
   }

   public void addCredentials(String host, int port, String usernamePassword) {
      logger.debug("Adding credentials for {}:{} - {}", new Object[]{host, port, usernamePassword});
      this.creds.put(host + ":" + port, new UsernamePasswordCredentials(usernamePassword));
   }

   public Credentials getCredentials(AuthScheme authScheme, String host, int port, boolean proxy) throws CredentialsNotAvailableException {
      Credentials cred = (Credentials)this.creds.get(host + ":" + port);
      if (cred == null) {
         logger.debug("No credentials available for ({},{})", host, port);
         throw new CredentialsNotAvailableException();
      } else {
         logger.debug("Returning credentials for ({},{})", host, port);
         return cred;
      }
   }
}
