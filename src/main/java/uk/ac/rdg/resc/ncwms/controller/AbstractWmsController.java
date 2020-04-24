package uk.ac.rdg.resc.ncwms.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import uk.ac.rdg.resc.edal.geometry.impl.HorizontalPositionImpl;
import uk.ac.rdg.resc.edal.geometry.impl.LineString;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.ui.RectangleInsets;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.domain.impl.HorizontalDomain;
import uk.ac.rdg.resc.edal.coverage.grid.GridCoordinates;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;
import uk.ac.rdg.resc.edal.util.Utils;
import uk.ac.rdg.resc.ncwms.exceptions.CurrentUpdateSequence;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidFormatException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidUpdateSequence;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.StyleNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.Wms1_1_1Exception;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.graphics.ImageFormat;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer;
import uk.ac.rdg.resc.ncwms.graphics.KmzFormat;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogger;

public abstract class AbstractWmsController extends AbstractController {
   private static final Logger log = LoggerFactory.getLogger(AbstractWmsController.class);
   private static final int LAYER_LIMIT = 1;
   private static final String FEATURE_INFO_XML_FORMAT = "text/xml";
   private static final String FEATURE_INFO_PNG_FORMAT = "image/png";
   protected ServerConfig serverConfig;
   protected UsageLogger usageLogger;

   public void init() throws Exception {
      File paletteLocationDir = this.serverConfig.getPaletteFilesLocation(this.getServletContext());
      if (paletteLocationDir != null && paletteLocationDir.exists() && paletteLocationDir.isDirectory()) {
         ColorPalette.loadPalettes(paletteLocationDir);
      } else {
         log.info("Directory of palette files does not exist or is not a directory");
      }

   }

   protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
      UsageLogEntry usageLogEntry = new UsageLogEntry(httpServletRequest);
      boolean logUsage = true;
      RequestParams params = new RequestParams(httpServletRequest.getParameterMap());

      String wmsVersion;
      try {
         try {
            String request = params.getMandatoryString("request");
            usageLogEntry.setWmsOperation(request);
            ModelAndView var19 = this.dispatchWmsRequest(request, params, httpServletRequest, httpServletResponse, usageLogEntry);
            return var19;
         } catch (WmsException var14) {
            usageLogEntry.setException(var14);
            wmsVersion = params.getWmsVersion();
            if (wmsVersion != null && wmsVersion.equals("1.1.1")) {
               throw new Wms1_1_1Exception(var14);
            }

            throw var14;
         } catch (SocketException var15) {
            wmsVersion = null;
            return wmsVersion;
         } catch (IOException var16) {
            if (!var16.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")) {
               throw var16;
            }
         } catch (Exception var17) {
            usageLogEntry.setException(var17);
            var17.printStackTrace();
            throw var17;
         }

         wmsVersion = null;
      } finally {
         if (logUsage && this.usageLogger != null) {
            this.usageLogger.logUsage(usageLogEntry);
         }

      }

      return wmsVersion;
   }

   protected abstract ModelAndView dispatchWmsRequest(String var1, RequestParams var2, HttpServletRequest var3, HttpServletResponse var4, UsageLogEntry var5) throws Exception;

   protected ModelAndView getCapabilities(Collection<? extends Dataset> datasets, DateTime lastUpdateTime, RequestParams params, HttpServletRequest httpServletRequest, UsageLogEntry usageLogEntry) throws WmsException, IOException {
      String service = params.getMandatoryString("service");
      if (!service.equals("WMS")) {
         throw new WmsException("The value of the SERVICE parameter must be \"WMS\"");
      } else {
         String versionStr = params.getWmsVersion();
         usageLogEntry.setWmsVersion(versionStr);
         String format = params.getString("format");
         usageLogEntry.setOutputFormat(format);
         String updateSeqStr = params.getString("updatesequence");
         if (updateSeqStr != null) {
            DateTime updateSequence;
            try {
               updateSequence = WmsUtils.iso8601ToDateTime(updateSeqStr, ISOChronology.getInstanceUTC());
            } catch (IllegalArgumentException var14) {
               throw new InvalidUpdateSequence(updateSeqStr + " is not a valid ISO date-time");
            }

            if (updateSequence.isEqual(lastUpdateTime)) {
               throw new CurrentUpdateSequence(updateSeqStr);
            }

            if (updateSequence.isAfter(lastUpdateTime)) {
               throw new InvalidUpdateSequence(updateSeqStr + " is later than the current server updatesequence value");
            }
         }

         boolean verboseTimes = params.getBoolean("verbose", false);
         Map<String, Object> models = new HashMap();
         models.put("config", this.serverConfig);
         models.put("datasets", datasets);
         models.put("lastUpdate", lastUpdateTime == null ? new DateTime() : lastUpdateTime);
         models.put("wmsBaseUrl", httpServletRequest.getRequestURL().toString());
         String[] supportedCrsCodes = new String[]{"EPSG:4326", "CRS:84", "EPSG:41001", "EPSG:27700", "EPSG:3408", "EPSG:3409", "EPSG:3857", "EPSG:32661", "EPSG:32761"};
         models.put("supportedCrsCodes", supportedCrsCodes);
         models.put("supportedImageFormats", ImageFormat.getSupportedMimeTypes());
         models.put("layerLimit", 1);
         models.put("featureInfoFormats", new String[]{"image/png", "text/xml"});
         models.put("legendWidth", 110);
         models.put("legendHeight", 264);
         models.put("paletteNames", ColorPalette.getAvailablePaletteNames());
         models.put("verboseTimes", verboseTimes);
         WmsVersion wmsVersion = versionStr == null ? WmsVersion.VERSION_1_3_0 : new WmsVersion(versionStr);
         return wmsVersion.compareTo(WmsVersion.VERSION_1_3_0) >= 0 ? new ModelAndView("capabilities_xml", models) : new ModelAndView("capabilities_xml_1_1_1", models);
      }
   }

   protected ModelAndView getMap(RequestParams params, AbstractWmsController.LayerFactory layerFactory, HttpServletResponse httpServletResponse, UsageLogEntry usageLogEntry) throws WmsException, Exception {
      GetMapRequest getMapRequest = new GetMapRequest(params);
      usageLogEntry.setGetMapRequest(getMapRequest);
      GetMapStyleRequest styleRequest = getMapRequest.getStyleRequest();
      String mimeType = styleRequest.getImageFormat();
      ImageFormat imageFormat = ImageFormat.get(mimeType);
      GetMapDataRequest dr = getMapRequest.getDataRequest();
      if (dr.getHeight() <= this.serverConfig.getMaxImageHeight() && dr.getWidth() <= this.serverConfig.getMaxImageWidth()) {
         String layerName = getLayerName(dr);
         Layer layer = layerFactory.getLayer(layerName);
         usageLogEntry.setLayer(layer);
         RegularGrid grid = WmsUtils.getImageGrid(dr);
         Range<Float> scaleRange = styleRequest.getColorScaleRange();
         if (scaleRange == null) {
            scaleRange = layer.getApproxValueRange();
         }

         Boolean logScale = styleRequest.isScaleLogarithmic();
         if (logScale == null) {
            logScale = layer.isLogScaling();
         }

         ImageProducer.Style style = layer instanceof VectorLayer ? ImageProducer.Style.VECTOR : ImageProducer.Style.BOXFILL;
         ColorPalette palette = layer.getDefaultColorPalette();
         String[] styles = styleRequest.getStyles();
         if (styles.length > 0) {
            String[] styleStrEls = styles[0].split("/");
            String styleType = styleStrEls[0];
            if (styleType.equalsIgnoreCase("boxfill")) {
               style = ImageProducer.Style.BOXFILL;
            } else {
               if (!styleType.equalsIgnoreCase("vector")) {
                  throw new StyleNotDefinedException("The style " + styles[0] + " is not supported by this server");
               }

               style = ImageProducer.Style.VECTOR;
            }

            String paletteName = null;
            if (styleStrEls.length > 1) {
               paletteName = styleStrEls[1];
            }

            palette = ColorPalette.get(paletteName);
            if (palette == null) {
               throw new StyleNotDefinedException("There is no palette with the name " + paletteName);
            }
         }

         ImageProducer imageProducer = (new ImageProducer.Builder()).width(dr.getWidth()).height(dr.getHeight()).style(style).palette(palette).colourScaleRange(scaleRange).backgroundColour(styleRequest.getBackgroundColour()).transparent(styleRequest.isTransparent()).logarithmic(logScale).opacity(styleRequest.getOpacity()).numColourBands(styleRequest.getNumColourBands()).build();
         if (imageProducer.isTransparent() && !imageFormat.supportsFullyTransparentPixels()) {
            throw new WmsException("The image format " + mimeType + " does not support fully-transparent pixels");
         } else if (imageProducer.getOpacity() < 100 && !imageFormat.supportsPartiallyTransparentPixels()) {
            throw new WmsException("The image format " + mimeType + " does not support partially-transparent pixels");
         } else {
            double zValue = getElevationValue(dr.getElevationString(), layer);
            List<String> tValueStrings = new ArrayList();
            List<DateTime> timeValues = getTimeValues(dr.getTimeString(), layer);
            if (timeValues.size() > 1 && !imageFormat.supportsMultipleFrames()) {
               throw new WmsException("The image format " + mimeType + " does not support multiple frames");
            } else {
               usageLogEntry.setNumTimeSteps(timeValues.size());
               long beforeExtractData = System.currentTimeMillis();
               if (timeValues.isEmpty()) {
                  timeValues = Arrays.asList((DateTime)null);
               }

               Iterator i$ = timeValues.iterator();

               while(i$.hasNext()) {
                  DateTime timeValue = (DateTime)i$.next();
                  String tValueStr = "";
                  if (timeValues.size() > 1 && timeValue != null) {
                     tValueStr = WmsUtils.dateTimeToISO8601(timeValue);
                  }

                  tValueStrings.add(tValueStr);
                  if (layer instanceof ScalarLayer) {
                     List<Float> data = this.readDataGrid((ScalarLayer)layer, timeValue, zValue, grid, usageLogEntry);
                     imageProducer.addFrame(data, tValueStr);
                  } else {
                     if (!(layer instanceof VectorLayer)) {
                        throw new IllegalStateException("Unrecognized layer type");
                     }

                     VectorLayer vecLayer = (VectorLayer)layer;
                     List<Float> eastData = this.readDataGrid(vecLayer.getEastwardComponent(), timeValue, zValue, grid, usageLogEntry);
                     List<Float> northData = this.readDataGrid(vecLayer.getNorthwardComponent(), timeValue, zValue, grid, usageLogEntry);
                     imageProducer.addFrame(eastData, northData, tValueStr);
                  }
               }

               long timeToExtractData = System.currentTimeMillis() - beforeExtractData;
               usageLogEntry.setTimeToExtractDataMs(timeToExtractData);
               BufferedImage legend = imageFormat.requiresLegend() ? imageProducer.getLegend(layer) : null;
               httpServletResponse.setStatus(200);
               httpServletResponse.setContentType(mimeType);
               if (imageFormat instanceof KmzFormat) {
                  httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + layer.getDataset().getId() + "_" + layer.getId() + ".kmz");
               }

               imageFormat.writeImage(imageProducer.getRenderedFrames(), httpServletResponse.getOutputStream(), layer, tValueStrings, dr.getElevationString(), grid.getExtent(), legend);
               return null;
            }
         }
      } else {
         throw new WmsException("Requested image size exceeds the maximum of " + this.serverConfig.getMaxImageWidth() + "x" + this.serverConfig.getMaxImageHeight());
      }
   }

   private static String getLayerName(GetMapDataRequest getMapDataRequest) throws WmsException {
      String[] layers = getMapDataRequest.getLayers();
      if (layers.length == 0) {
         throw new WmsException("Must provide a value for the LAYERS parameter");
      } else if (layers.length > 1) {
         throw new WmsException("You may only create a map from 1 layer(s) at a time");
      } else {
         return layers[0];
      }
   }

   protected ModelAndView getFeatureInfo(RequestParams params, AbstractWmsController.LayerFactory layerFactory, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, UsageLogEntry usageLogEntry) throws WmsException, Exception {
      GetFeatureInfoRequest request = new GetFeatureInfoRequest(params);
      usageLogEntry.setGetFeatureInfoRequest(request);
      GetFeatureInfoDataRequest dr = request.getDataRequest();
      if (!request.getOutputFormat().equals("text/xml") && !request.getOutputFormat().equals("image/png")) {
         throw new InvalidFormatException("The output format " + request.getOutputFormat() + " is not valid for GetFeatureInfo");
      } else {
         String layerName = getLayerName(dr);
         Layer layer = layerFactory.getLayer(layerName);
         usageLogEntry.setLayer(layer);
         RegularGrid grid = WmsUtils.getImageGrid(dr);
         int j = dr.getHeight() - dr.getPixelRow() - 1;
         HorizontalPosition pos = grid.transformCoordinates(dr.getPixelColumn(), j);
         LonLatPosition lonLat = Utils.transformToWgs84LonLat(pos);
         HorizontalGrid horizGrid = layer.getHorizontalGrid();
         GridCoordinates gridCoords = horizGrid.findNearestGridPoint(pos);
         LonLatPosition gridCellCentre = null;
         if (gridCoords != null) {
            HorizontalPosition gridCellCentrePos = (HorizontalPosition)horizGrid.transformCoordinates(gridCoords);
            gridCellCentre = Utils.transformToWgs84LonLat(gridCellCentrePos);
         }

         double zValue = getElevationValue(dr.getElevationString(), layer);
         List<DateTime> tValues = getTimeValues(dr.getTimeString(), layer);
         usageLogEntry.setNumTimeSteps(tValues.size());
         List tsData;
         if (layer instanceof ScalarLayer) {
            ScalarLayer scalLayer = (ScalarLayer)layer;
            if (tValues.isEmpty()) {
               Float val = scalLayer.readSinglePoint((DateTime)null, zValue, pos);
               tsData = Arrays.asList(val);
            } else {
               tsData = scalLayer.readTimeseries(tValues, zValue, pos);
            }
         } else {
            if (!(layer instanceof VectorLayer)) {
               throw new IllegalStateException("Unrecognized layer type");
            }

            VectorLayer vecLayer = (VectorLayer)layer;
            ScalarLayer eastComp = vecLayer.getEastwardComponent();
            ScalarLayer northComp = vecLayer.getNorthwardComponent();
            if (tValues.isEmpty()) {
               Float eastVal = eastComp.readSinglePoint((DateTime)null, zValue, pos);
               Float northVal = northComp.readSinglePoint((DateTime)null, zValue, pos);
               if (eastVal != null && northVal != null) {
                  tsData = Arrays.asList((float)Math.sqrt((double)(eastVal * eastVal + northVal * northVal)));
               } else {
                  tsData = Arrays.asList((Float)null);
               }
            } else {
               List<Float> tsDataEast = eastComp.readTimeseries(tValues, zValue, pos);
               List<Float> tsDataNorth = northComp.readTimeseries(tValues, zValue, pos);
               tsData = WmsUtils.getMagnitudes(tsDataEast, tsDataNorth);
            }
         }

         if (!tValues.isEmpty() && tValues.size() != tsData.size()) {
            throw new IllegalStateException("Internal error: timeseries length inconsistency");
         } else {
            Map<DateTime, Float> featureData = new LinkedHashMap();
            if (tValues.isEmpty()) {
               featureData.put((Object)null, tsData.get(0));
            } else {
               for(int i = 0; i < tValues.size(); ++i) {
                  featureData.put(tValues.get(i), tsData.get(i));
               }
            }

            if (request.getOutputFormat().equals("text/xml")) {
               Map<String, Object> models = new HashMap();
               models.put("longitude", lonLat.getLongitude());
               models.put("latitude", lonLat.getLatitude());
               models.put("gridCoords", gridCoords);
               models.put("gridCentre", gridCellCentre);
               models.put("data", featureData);
               return new ModelAndView("showFeatureInfo_xml", models);
            } else {
               JFreeChart chart = Charting.createTimeseriesPlot(layer, lonLat, featureData);
               httpServletResponse.setContentType("image/png");
               ChartUtilities.writeChartAsPNG(httpServletResponse.getOutputStream(), chart, 400, 300);
               return null;
            }
         }
      }
   }

   protected ModelAndView getLegendGraphic(RequestParams params, AbstractWmsController.LayerFactory layerFactory, HttpServletResponse httpServletResponse) throws Exception {
      int numColourBands = GetMapStyleRequest.getNumColourBands(params);
      String paletteName = params.getString("palette");
      String colorBarOnly = params.getString("colorbaronly", "false");
      BufferedImage legend;
      ColorPalette palette;
      if (colorBarOnly.equalsIgnoreCase("true")) {
         int width = params.getPositiveInt("width", 50);
         int height = params.getPositiveInt("height", 200);
         palette = ColorPalette.get(paletteName);
         legend = palette.createColorBar(width, height, numColourBands);
      } else {
         String layerName = params.getMandatoryString("layer");
         Layer layer = layerFactory.getLayer(layerName);
         palette = paletteName == null ? layer.getDefaultColorPalette() : ColorPalette.get(paletteName);
         Boolean isLogScale = GetMapStyleRequest.isLogScale(params);
         boolean logarithmic = isLogScale == null ? layer.isLogScaling() : isLogScale;
         Range<Float> colorScaleRange = GetMapStyleRequest.getColorScaleRange(params);
         if (colorScaleRange == null) {
            colorScaleRange = layer.getApproxValueRange();
         } else if (colorScaleRange.isEmpty()) {
            throw new WmsException("Cannot automatically create a colour scale for a legend graphic.  Use COLORSCALERANGE=default or specify the scale extremes explicitly.");
         }

         legend = palette.createLegend(numColourBands, layer.getTitle(), layer.getUnits(), logarithmic, colorScaleRange);
      }

      httpServletResponse.setContentType("image/png");
      ImageIO.write(legend, "png", httpServletResponse.getOutputStream());
      return null;
   }

   protected ModelAndView getTransect(RequestParams params, AbstractWmsController.LayerFactory layerFactory, HttpServletResponse response, UsageLogEntry usageLogEntry) throws Exception {
      String layerStr = params.getMandatoryString("layer");
      Layer layer = layerFactory.getLayer(layerStr);
      String crsCode = params.getMandatoryString("crs");
      String lineString = params.getMandatoryString("linestring");
      String outputFormat = params.getMandatoryString("format");
      List<DateTime> tValues = getTimeValues(params.getString("time"), layer);
      DateTime tValue = tValues.isEmpty() ? null : (DateTime)tValues.get(0);
      double zValue = getElevationValue(params.getString("elevation"), layer);
      if (!outputFormat.equals("image/png") && !outputFormat.equals("text/xml")) {
         throw new InvalidFormatException(outputFormat);
      } else {
         usageLogEntry.setLayer(layer);
         usageLogEntry.setOutputFormat(outputFormat);
         usageLogEntry.setWmsOperation("GetTransect");
         CoordinateReferenceSystem crs = WmsUtils.getCrs(crsCode);
         LineString transect = new LineString(lineString, crsCode, params.getWmsVersion());
         log.debug("Got {} control points", transect.getControlPoints().size());
         Domain<HorizontalPosition> transectDomain = getOptimalTransectDomain(layer, transect);
         log.debug("Using transect consisting of {} points", transectDomain.getDomainObjects().size());
         List transectData;
         List points;
         if (layer instanceof ScalarLayer) {
            transectData = ((ScalarLayer)layer).readHorizontalPoints(tValue, zValue, transectDomain);
         } else {
            if (!(layer instanceof VectorLayer)) {
               throw new IllegalStateException("Unrecognized layer type");
            }

            VectorLayer vecLayer = (VectorLayer)layer;
            points = vecLayer.getEastwardComponent().readHorizontalPoints(tValue, zValue, transectDomain);
            List<Float> tsDataNorth = vecLayer.getNorthwardComponent().readHorizontalPoints(tValue, zValue, transectDomain);
            transectData = WmsUtils.getMagnitudes(points, tsDataNorth);
         }

         log.debug("Transect: Got {} dataValues", transectData.size());
         response.setContentType(outputFormat);
         if (outputFormat.equals("image/png")) {
            JFreeChart chart = Charting.createTransectPlot(layer, transect, transectData);
            int width = 400;
            int height = 300;
            if (layer.getElevationValues().size() > 1) {
               JFreeChart verticalSectionChart = createVerticalSectionChart(params, layer, tValue, transect, transectDomain);
               CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis("distance along path (arbitrary units)"));
               plot.setGap(20.0D);
               plot.add(chart.getXYPlot(), 1);
               plot.add(verticalSectionChart.getXYPlot(), 1);
               plot.setOrientation(PlotOrientation.VERTICAL);
               String title = WmsUtils.removeDuplicatedWhiteSpace(layer.getTitle()) + " (" + layer.getUnits() + ")" + " at " + zValue + layer.getElevationUnits();
               chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
               RectangleInsets r = new RectangleInsets(0.0D, 10.0D, 0.0D, 0.0D);
               chart.setPadding(r);
               chart.addSubtitle(verticalSectionChart.getSubtitle(0));
               height = 600;
            }

            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
         } else if (outputFormat.equals("text/xml")) {
            Map<HorizontalPosition, Float> dataPoints = new LinkedHashMap();
            points = transectDomain.getDomainObjects();

            for(int i = 0; i < points.size(); ++i) {
               dataPoints.put(points.get(i), transectData.get(i));
            }

            Map<String, Object> models = new HashMap();
            models.put("crs", crsCode);
            models.put("layer", layer);
            models.put("linestring", lineString);
            models.put("data", dataPoints);
            return new ModelAndView("showTransect_xml", models);
         }

         return null;
      }
   }

   protected ModelAndView getVerticalProfile(RequestParams params, AbstractWmsController.LayerFactory layerFactory, HttpServletResponse response, UsageLogEntry usageLogEntry) throws WmsException, IOException {
      String layerStr = params.getMandatoryString("layer");
      Layer layer = layerFactory.getLayer(layerStr);
      String crsCode = params.getMandatoryString("crs");
      String point = params.getMandatoryString("point");
      List<DateTime> tValues = getTimeValues(params.getString("time"), layer);
      DateTime tValue = tValues.isEmpty() ? null : (DateTime)tValues.get(0);
      String outputFormat = params.getMandatoryString("format");
      if (!"image/png".equals(outputFormat) && !"image/jpeg".equals(outputFormat) && !"image/jpg".equals(outputFormat)) {
         throw new InvalidFormatException(outputFormat + " is not a valid output format");
      } else {
         usageLogEntry.setLayer(layer);
         usageLogEntry.setOutputFormat(outputFormat);
         usageLogEntry.setWmsOperation("GetVerticalProfile");
         CoordinateReferenceSystem crs = WmsUtils.getCrs(crsCode);
         String[] coords = point.trim().split(" +");
         if (coords.length != 2) {
            throw new WmsException("Invalid POINT format");
         } else {
            int lonIndex = 0;
            int latIndex = 1;
            if (crsCode.equalsIgnoreCase("EPSG:4326") && params.getWmsVersion().equalsIgnoreCase("1.3.0")) {
               latIndex = 0;
               lonIndex = 1;
            }

            double x;
            double y;
            try {
               x = Double.parseDouble(coords[lonIndex]);
               y = Double.parseDouble(coords[latIndex]);
            } catch (NumberFormatException var31) {
               throw new WmsException("Invalid POINT format");
            }

            HorizontalPosition pos = new HorizontalPositionImpl(x, y, crs);
            Domain<HorizontalPosition> domain = new HorizontalDomain(pos);
            List<Double> zValues = new ArrayList();
            List<Float> profileData = new ArrayList();
            List sectionDataEast;
            if (layer instanceof ScalarLayer) {
               ScalarLayer scalarLayer = (ScalarLayer)layer;
               sectionDataEast = scalarLayer.readVerticalSection(tValue, layer.getElevationValues(), domain);
               int i = 0;

               for(Iterator i$ = layer.getElevationValues().iterator(); i$.hasNext(); ++i) {
                  Double zValue = (Double)i$.next();
                  Float d = (Float)((List)sectionDataEast.get(i)).get(0);
                  if (d != null) {
                     profileData.add(d);
                     zValues.add(zValue);
                  }
               }
            } else {
               if (!(layer instanceof VectorLayer)) {
                  throw new UnsupportedOperationException("Unsupported layer type");
               }

               VectorLayer vecLayer = (VectorLayer)layer;
               sectionDataEast = vecLayer.getEastwardComponent().readVerticalSection(tValue, layer.getElevationValues(), domain);
               List<List<Float>> sectionDataNorth = vecLayer.getNorthwardComponent().readVerticalSection(tValue, layer.getElevationValues(), domain);
               int i = 0;

               for(Iterator i$ = layer.getElevationValues().iterator(); i$.hasNext(); ++i) {
                  Double zValue = (Double)i$.next();
                  Float mag = (Float)WmsUtils.getMagnitudes((List)sectionDataEast.get(i), (List)sectionDataNorth.get(i)).get(0);
                  if (mag != null) {
                     profileData.add(mag);
                     zValues.add(zValue);
                  }
               }
            }

            JFreeChart chart = Charting.createVerticalProfilePlot(layer, pos, zValues, profileData, tValue);
            response.setContentType(outputFormat);
            int width = 500;
            int height = 400;
            if ("image/png".equals(outputFormat)) {
               ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
            } else {
               ChartUtilities.writeChartAsJPEG(response.getOutputStream(), chart, width, height);
            }

            return null;
         }
      }
   }

   private static JFreeChart createVerticalSectionChart(RequestParams params, Layer layer, DateTime tValue, LineString lineString, Domain<HorizontalPosition> transectDomain) throws WmsException, InvalidDimensionValueException, IOException {
      int numColourBands = GetMapStyleRequest.getNumColourBands(params);
      Range<Float> scaleRange = GetMapStyleRequest.getColorScaleRange(params);
      if (scaleRange == null) {
         scaleRange = layer.getApproxValueRange();
      }

      Boolean logScale = GetMapStyleRequest.isLogScale(params);
      if (logScale == null) {
         logScale = layer.isLogScaling();
      }

      String paletteName = params.getString("palette");
      ColorPalette palette = paletteName == null ? layer.getDefaultColorPalette() : ColorPalette.get(paletteName);
      List<Double> zValues = new ArrayList();
      List<List<Float>> sectionData = new ArrayList();
      List sectionDataEast;
      if (layer instanceof ScalarLayer) {
         ScalarLayer scalarLayer = (ScalarLayer)layer;
         sectionDataEast = scalarLayer.readVerticalSection(tValue, layer.getElevationValues(), transectDomain);
         int i = 0;

         for(Iterator i$ = layer.getElevationValues().iterator(); i$.hasNext(); ++i) {
            Double zValue = (Double)i$.next();
            List<Float> d = (List)sectionDataEast.get(i);
            if (!allNull(d)) {
               sectionData.add(d);
               zValues.add(zValue);
            }
         }
      } else {
         if (!(layer instanceof VectorLayer)) {
            throw new UnsupportedOperationException("Unsupported layer type");
         }

         VectorLayer vecLayer = (VectorLayer)layer;
         sectionDataEast = vecLayer.getEastwardComponent().readVerticalSection(tValue, layer.getElevationValues(), transectDomain);
         List<List<Float>> sectionDataNorth = vecLayer.getNorthwardComponent().readVerticalSection(tValue, layer.getElevationValues(), transectDomain);
         int i = 0;

         for(Iterator i$ = layer.getElevationValues().iterator(); i$.hasNext(); ++i) {
            Double zValue = (Double)i$.next();
            List<Float> mags = WmsUtils.getMagnitudes((List)sectionDataEast.get(i), (List)sectionDataNorth.get(i));
            if (!allNull(mags)) {
               sectionData.add(mags);
               zValues.add(zValue);
            }
         }
      }

      float max = Float.NEGATIVE_INFINITY;
      float min = Float.POSITIVE_INFINITY;
      if (scaleRange.isEmpty()) {
         Range minMax;
         for(Iterator i$ = sectionData.iterator(); i$.hasNext(); min = Math.min(min, (Float)minMax.getMinimum())) {
            List<Float> data = (List)i$.next();
            minMax = Ranges.findMinMax(data);
            max = Math.max(max, (Float)minMax.getMaximum());
         }

         scaleRange = Ranges.newRange(min, max);
      }

      double zValue = getElevationValue(params.getString("elevation"), layer);
      return Charting.createVerticalSectionChart(layer, lineString, zValues, sectionData, scaleRange, palette, numColourBands, logScale, zValue);
   }

   protected ModelAndView getVerticalSection(RequestParams params, AbstractWmsController.LayerFactory layerFactory, HttpServletResponse response, UsageLogEntry usageLogEntry) throws Exception {
      String layerStr = params.getMandatoryString("layer");
      Layer layer = layerFactory.getLayer(layerStr);
      String crsCode = params.getMandatoryString("crs");
      String lineStr = params.getMandatoryString("linestring");
      List<DateTime> tValues = getTimeValues(params.getString("time"), layer);
      DateTime tValue = tValues.isEmpty() ? null : (DateTime)tValues.get(0);
      usageLogEntry.setLayer(layer);
      String outputFormat = params.getMandatoryString("format");
      if (!"image/png".equals(outputFormat) && !"image/jpeg".equals(outputFormat) && !"image/jpg".equals(outputFormat)) {
         throw new InvalidFormatException(outputFormat + " is not a valid output format");
      } else {
         usageLogEntry.setOutputFormat(outputFormat);
         usageLogEntry.setWmsOperation("GetVerticalSection");
         CoordinateReferenceSystem crs = WmsUtils.getCrs(crsCode);
         LineString lineString = new LineString(lineStr, crsCode, params.getMandatoryWmsVersion());
         log.debug("Got {} control points", lineString.getControlPoints().size());
         Domain<HorizontalPosition> transectDomain = getOptimalTransectDomain(layer, lineString);
         log.debug("Using transect consisting of {} points", transectDomain.getDomainObjects().size());
         JFreeChart chart = createVerticalSectionChart(params, layer, tValue, lineString, transectDomain);
         response.setContentType(outputFormat);
         int width = 500;
         int height = 400;
         if ("image/png".equals(outputFormat)) {
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
         } else {
            ChartUtilities.writeChartAsJPEG(response.getOutputStream(), chart, width, height);
         }

         return null;
      }
   }

   private static Domain<HorizontalPosition> getOptimalTransectDomain(Layer layer, LineString transect) throws Exception {
      int numTransectPoints = 500;
      int lastNumUniqueGridPointsSampled = -1;
      HorizontalDomain pointList = null;

      while(true) {
         List<HorizontalPosition> points = transect.getPointsOnPath(numTransectPoints);
         HorizontalDomain testPointList = new HorizontalDomain(points, transect.getCoordinateReferenceSystem());
         Set<GridCoordinates> gridCoords = new HashSet();
         Iterator i$ = layer.getHorizontalGrid().findNearestGridPoints(testPointList).iterator();

         while(i$.hasNext()) {
            GridCoordinates coords = (GridCoordinates)i$.next();
            gridCoords.add(coords);
         }

         int numUniqueGridPointsSampled = gridCoords.size();
         log.debug("With {} transect points, we'll sample {} grid points", numTransectPoints, numUniqueGridPointsSampled);
         if ((double)numUniqueGridPointsSampled <= (double)lastNumUniqueGridPointsSampled * 1.1D) {
            return pointList;
         }

         lastNumUniqueGridPointsSampled = numUniqueGridPointsSampled;
         numTransectPoints += 500;
         pointList = testPointList;
      }
   }

   private static boolean allNull(List<Float> data) {
      Iterator i$ = data.iterator();

      Float val;
      do {
         if (!i$.hasNext()) {
            return true;
         }

         val = (Float)i$.next();
      } while(val == null);

      return false;
   }

   static double getElevationValue(String zValue, Layer layer) throws InvalidDimensionValueException {
      if (layer.getElevationValues().isEmpty()) {
         return Double.NaN;
      } else if (zValue == null) {
         double defaultVal = layer.getDefaultElevationValue();
         if (Double.isNaN(defaultVal)) {
            throw new InvalidDimensionValueException("elevation", "null");
         } else {
            return defaultVal;
         }
      } else if (!zValue.contains(",") && !zValue.contains("/")) {
         try {
            return Double.parseDouble(zValue);
         } catch (NumberFormatException var4) {
            throw new InvalidDimensionValueException("elevation", zValue);
         }
      } else {
         throw new InvalidDimensionValueException("elevation", zValue);
      }
   }

   static List<DateTime> getTimeValues(String timeString, Layer layer) throws InvalidDimensionValueException {
      if (layer.getTimeValues().isEmpty()) {
         return Collections.emptyList();
      } else if (timeString == null) {
         DateTime defaultDateTime = layer.getDefaultTimeValue();
         if (defaultDateTime == null) {
            throw new InvalidDimensionValueException("time", timeString);
         } else {
            return Arrays.asList(defaultDateTime);
         }
      } else {
         List<DateTime> tValues = new ArrayList();
         String[] arr$ = timeString.split(",");
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String t = arr$[i$];
            String[] startStop = t.split("/");
            if (startStop.length == 1) {
               tValues.add(findTValue(startStop[0], layer));
            } else {
               if (startStop.length != 2) {
                  throw new InvalidDimensionValueException("time", t);
               }

               tValues.addAll(findTValues(startStop[0], startStop[1], layer));
            }
         }

         return tValues;
      }
   }

   static int findTIndex(String isoDateTime, Layer layer) throws InvalidDimensionValueException {
      DateTime target;
      if (isoDateTime.equals("current")) {
         target = layer.getCurrentTimeValue();
      } else {
         try {
            target = WmsUtils.iso8601ToDateTime(isoDateTime, layer.getChronology());
         } catch (IllegalArgumentException var4) {
            throw new InvalidDimensionValueException("time", isoDateTime);
         }
      }

      int index = WmsUtils.findTimeIndex(layer.getTimeValues(), target);
      if (index < 0) {
         throw new InvalidDimensionValueException("time", isoDateTime);
      } else {
         return index;
      }
   }

   private static DateTime findTValue(String isoDateTime, Layer layer) throws InvalidDimensionValueException {
      return (DateTime)layer.getTimeValues().get(findTIndex(isoDateTime, layer));
   }

   private static List<DateTime> findTValues(String isoDateTimeStart, String isoDateTimeEnd, Layer layer) throws InvalidDimensionValueException {
      int startIndex = findTIndex(isoDateTimeStart, layer);
      int endIndex = findTIndex(isoDateTimeEnd, layer);
      if (startIndex > endIndex) {
         throw new InvalidDimensionValueException("time", isoDateTimeStart + "/" + isoDateTimeEnd);
      } else {
         List<DateTime> layerTValues = layer.getTimeValues();
         List<DateTime> tValues = new ArrayList();

         for(int i = startIndex; i <= endIndex; ++i) {
            tValues.add(layerTValues.get(i));
         }

         return tValues;
      }
   }

   protected List<Float> readDataGrid(ScalarLayer layer, DateTime dateTime, double elevation, RegularGrid grid, UsageLogEntry usageLogEntry) throws InvalidDimensionValueException, IOException {
      return layer.readHorizontalPoints(dateTime, elevation, grid);
   }

   public void shutdown() {
   }

   public void setServerConfig(ServerConfig serverConfig) {
      this.serverConfig = serverConfig;
   }

   public void setUsageLogger(UsageLogger usageLogger) {
      this.usageLogger = usageLogger;
   }

   public interface LayerFactory {
      Layer getLayer(String var1) throws LayerNotDefinedException;
   }
}
