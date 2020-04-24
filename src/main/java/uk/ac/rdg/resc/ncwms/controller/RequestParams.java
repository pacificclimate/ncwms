package uk.ac.rdg.resc.ncwms.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

public class RequestParams {
   private Map<String, String> paramMap = new HashMap();

   public RequestParams(Map<?, ?> httpRequestParamMap) {
      Map<String, String[]> httpParamMap = httpRequestParamMap;
      Iterator i$ = httpRequestParamMap.keySet().iterator();

      while(i$.hasNext()) {
         String name = (String)i$.next();
         String[] values = (String[])httpParamMap.get(name);

         assert values.length >= 1;

         try {
            String key = URLDecoder.decode(name.trim(), "UTF-8").toLowerCase();
            String value = URLDecoder.decode(values[0].trim(), "UTF-8");
            this.paramMap.put(key, value);
         } catch (UnsupportedEncodingException var8) {
            throw new AssertionError(var8);
         }
      }

   }

   public String getString(String paramName) {
      return (String)this.paramMap.get(paramName.toLowerCase());
   }

   public String getMandatoryString(String paramName) throws WmsException {
      String value = this.getString(paramName);
      if (value == null) {
         throw new WmsException("Must provide a value for parameter " + paramName.toUpperCase());
      } else {
         return value;
      }
   }

   public String getWmsVersion() {
      String version = this.getString("version");
      if (version == null) {
         version = this.getString("wmtver");
      }

      return version;
   }

   public String getMandatoryWmsVersion() throws WmsException {
      String version = this.getWmsVersion();
      if (version == null) {
         throw new WmsException("Must provide a value for VERSION");
      } else {
         return version;
      }
   }

   public int getPositiveInt(String paramName, int defaultValue) throws WmsException {
      String value = this.getString(paramName);
      return value == null ? defaultValue : parsePositiveInt(paramName, value);
   }

   public int getMandatoryPositiveInt(String paramName) throws WmsException {
      String value = this.getString(paramName);
      if (value == null) {
         throw new WmsException("Must provide a value for parameter " + paramName.toUpperCase());
      } else {
         return parsePositiveInt(paramName, value);
      }
   }

   private static int parsePositiveInt(String paramName, String value) throws WmsException {
      try {
         int i = Integer.parseInt(value);
         if (i < 0) {
            throw new WmsException("Parameter " + paramName.toUpperCase() + " must be a valid positive integer");
         } else {
            return i;
         }
      } catch (NumberFormatException var3) {
         throw new WmsException("Parameter " + paramName.toUpperCase() + " must be a valid positive integer");
      }
   }

   public String getString(String paramName, String defaultValue) {
      String value = this.getString(paramName);
      return value == null ? defaultValue : value;
   }

   public boolean getBoolean(String paramName, boolean defaultValue) throws WmsException {
      String value = this.getString(paramName);
      if (value == null) {
         return defaultValue;
      } else {
         value = value.trim();
         if ("true".equalsIgnoreCase(value)) {
            return true;
         } else if ("false".equalsIgnoreCase(value)) {
            return false;
         } else {
            throw new WmsException("Invalid boolean value for parameter " + paramName);
         }
      }
   }

   public float getFloat(String paramName, float defaultValue) throws WmsException {
      String value = this.getString(paramName);
      if (value == null) {
         return defaultValue;
      } else {
         try {
            return Float.parseFloat(value);
         } catch (NumberFormatException var5) {
            throw new WmsException("Parameter " + paramName.toUpperCase() + " must be a valid floating-point number");
         }
      }
   }
}
