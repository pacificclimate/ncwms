package uk.ac.rdg.resc.ncwms.usagelog;

import java.awt.Color;
import javax.servlet.http.HttpServletRequest;

import uk.ac.rdg.resc.ncwms.wms.Layer;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.ncwms.controller.GetFeatureInfoDataRequest;
import uk.ac.rdg.resc.ncwms.controller.GetFeatureInfoRequest;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.controller.GetMapRequest;
import uk.ac.rdg.resc.ncwms.controller.GetMapStyleRequest;

public class UsageLogEntry {
   private DateTime requestTime = new DateTime();
   private String clientIpAddress = null;
   private String clientHost = null;
   private String clientReferrer = null;
   private String clientUserAgent = null;
   private String httpMethod = null;
   private String wmsVersion = null;
   private String wmsOperation = null;
   private String exceptionClass = null;
   private String exceptionMessage = null;
   private String crsCode = null;
   private double[] bbox = null;
   private String elevation = null;
   private String timeStr = null;
   private Integer numTimeSteps = null;
   private Integer width = null;
   private Integer height = null;
   private String layer = null;
   private String datasetId = null;
   private String variableId = null;
   private Long timeToExtractDataMs = null;
   private Boolean usedCache = false;
   private Double featureInfoLon = null;
   private Double featureInfoLat = null;
   private Integer featureInfoPixelCol = null;
   private Integer featureInfoPixelRow = null;
   private String style = null;
   private String outputFormat = null;
   private Boolean transparent = null;
   private String backgroundColor = null;
   private String menu = null;
   private String remoteServerUrl = null;

   public UsageLogEntry(HttpServletRequest httpServletRequest) {
      this.clientIpAddress = httpServletRequest.getRemoteAddr();
      this.clientHost = httpServletRequest.getRemoteHost();
      this.clientReferrer = httpServletRequest.getHeader("Referer");
      this.clientUserAgent = httpServletRequest.getHeader("User-Agent");
      this.httpMethod = httpServletRequest.getMethod();
   }

   public void setException(Exception ex) {
      this.exceptionClass = ex.getClass().getName();
      this.exceptionMessage = ex.getMessage();
   }

   public void setGetMapRequest(GetMapRequest getMapRequest) {
      this.wmsVersion = getMapRequest.getWmsVersion();
      this.setGetMapDataRequest(getMapRequest.getDataRequest());
      GetMapStyleRequest sr = getMapRequest.getStyleRequest();
      this.outputFormat = sr.getImageFormat();
      this.transparent = sr.isTransparent();
      Color bgColor = sr.getBackgroundColour();
      this.backgroundColor = bgColor.getRed() + "," + bgColor.getGreen() + "," + bgColor.getBlue();
      this.style = sr.getStyles().length > 0 ? sr.getStyles()[0] : "";
   }

   public void setGetFeatureInfoRequest(GetFeatureInfoRequest request) {
      this.wmsVersion = request.getWmsVersion();
      this.outputFormat = request.getOutputFormat();
      GetFeatureInfoDataRequest dr = request.getDataRequest();
      this.setGetMapDataRequest(dr);
      this.featureInfoPixelCol = dr.getPixelColumn();
      this.featureInfoPixelRow = dr.getPixelRow();
   }

   private void setGetMapDataRequest(GetMapDataRequest dr) {
      this.layer = dr.getLayers()[0];
      this.crsCode = dr.getCrsCode();
      this.bbox = dr.getBbox();
      this.elevation = dr.getElevationString();
      this.width = dr.getWidth();
      this.height = dr.getHeight();
      this.timeStr = dr.getTimeString();
   }

   public void setWmsOperation(String op) {
      this.wmsOperation = op;
   }

   public void setLayer(Layer layer) {
      this.datasetId = layer.getDataset().getId();
      this.variableId = layer.getId();
   }

   public void setTimeToExtractDataMs(long timeToExtractDataMs) {
      this.timeToExtractDataMs = timeToExtractDataMs;
   }

   public void setNumTimeSteps(Integer numTimeSteps) {
      this.numTimeSteps = numTimeSteps;
   }

   public void setWmsVersion(String wmsVersion) {
      this.wmsVersion = wmsVersion;
   }

   public void setOutputFormat(String outputFormat) {
      this.outputFormat = outputFormat;
   }

   public void setFeatureInfoLocation(double lon, double lat) {
      this.featureInfoLon = lon;
      this.featureInfoLat = lat;
   }

   public void setMenu(String menu) {
      this.menu = menu;
   }

   public DateTime getRequestTime() {
      return this.requestTime;
   }

   public String getClientIpAddress() {
      return this.clientIpAddress;
   }

   public String getClientHost() {
      return this.clientHost;
   }

   public String getClientReferrer() {
      return this.clientReferrer;
   }

   public String getClientUserAgent() {
      return this.clientUserAgent;
   }

   public String getHttpMethod() {
      return this.httpMethod;
   }

   public String getExceptionClass() {
      return this.exceptionClass;
   }

   public String getExceptionMessage() {
      return this.exceptionMessage;
   }

   public String getWmsOperation() {
      return this.wmsOperation;
   }

   public String getWmsVersion() {
      return this.wmsVersion;
   }

   public String getCrs() {
      return this.crsCode;
   }

   public double[] getBbox() {
      return this.bbox;
   }

   public String getElevation() {
      return this.elevation;
   }

   public String getTimeString() {
      return this.timeStr;
   }

   public Integer getNumTimeSteps() {
      return this.numTimeSteps;
   }

   public Integer getWidth() {
      return this.width;
   }

   public Integer getHeight() {
      return this.height;
   }

   public String getLayer() {
      return this.layer;
   }

   public String getDatasetId() {
      return this.datasetId;
   }

   public String getVariableId() {
      return this.variableId;
   }

   public Long getTimeToExtractDataMs() {
      return this.timeToExtractDataMs;
   }

   public boolean isUsedCache() {
      return this.usedCache;
   }

   public void setUsedCache(boolean usedCache) {
      this.usedCache = usedCache;
   }

   public Double getFeatureInfoLon() {
      return this.featureInfoLon;
   }

   public Double getFeatureInfoLat() {
      return this.featureInfoLat;
   }

   public Integer getFeatureInfoPixelCol() {
      return this.featureInfoPixelCol;
   }

   public Integer getFeatureInfoPixelRow() {
      return this.featureInfoPixelRow;
   }

   public String getStyle() {
      return this.style;
   }

   public String getOutputFormat() {
      return this.outputFormat;
   }

   public Boolean getTransparent() {
      return this.transparent;
   }

   public String getBackgroundColor() {
      return this.backgroundColor;
   }

   public String getMenu() {
      return this.menu;
   }

   public String getRemoteServerUrl() {
      return this.remoteServerUrl;
   }

   public void setRemoteServerUrl(String remoteServerUrl) {
      this.remoteServerUrl = remoteServerUrl;
   }
}
