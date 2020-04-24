package uk.ac.rdg.resc.ncwms.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;

public abstract class AbstractMetadataController {
   private static final Logger log = LoggerFactory.getLogger(AbstractMetadataController.class);
   private final AbstractWmsController.LayerFactory layerFactory;

   protected AbstractMetadataController(AbstractWmsController.LayerFactory layerFactory) {
      this.layerFactory = layerFactory;
   }

   public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response, UsageLogEntry usageLogEntry) throws MetadataException {
      try {
         String item = request.getParameter("item");
         usageLogEntry.setWmsOperation("GetMetadata:" + item);
         if (item == null) {
            throw new Exception("Must provide an ITEM parameter");
         } else if (item.equals("menu")) {
            return this.showMenu(request, usageLogEntry);
         } else if (item.equals("layerDetails")) {
            return this.showLayerDetails(request, usageLogEntry);
         } else if (item.equals("timesteps")) {
            return this.showTimesteps(request);
         } else if (item.equals("minmax")) {
            return this.showMinMax(request, usageLogEntry);
         } else if (item.equals("animationTimesteps")) {
            return this.showAnimationTimesteps(request);
         } else {
            throw new Exception("Invalid value for ITEM parameter");
         }
      } catch (Exception var5) {
         throw new MetadataException(var5);
      }
   }

   protected abstract ModelAndView showMenu(HttpServletRequest var1, UsageLogEntry var2) throws Exception;

   private ModelAndView showLayerDetails(HttpServletRequest request, UsageLogEntry usageLogEntry) throws Exception {
      Layer layer = this.getLayer(request);
      usageLogEntry.setLayer(layer);
      DateTime targetDateTime = new DateTime(layer.getChronology());
      String targetDateIso = request.getParameter("time");
      if (targetDateIso != null && !targetDateIso.trim().equals("")) {
         try {
            targetDateTime = WmsUtils.iso8601ToDateTime(targetDateIso, layer.getChronology());
         } catch (IllegalArgumentException var20) {
         }
      }

      Map<Integer, Map<Integer, List<Integer>>> datesWithData = new LinkedHashMap();
      List<DateTime> timeValues = layer.getTimeValues();
      DateTime nearestDateTime = timeValues.isEmpty() ? new DateTime(0L) : (DateTime)timeValues.get(0);
      Iterator i$ = layer.getTimeValues().iterator();

      while(i$.hasNext()) {
         DateTime dateTime = (DateTime)i$.next();
         dateTime = dateTime.withZone(DateTimeZone.UTC);
         long d1 = (new Duration(dateTime, targetDateTime)).getMillis();
         long d2 = (new Duration(nearestDateTime, targetDateTime)).getMillis();
         if (Math.abs(d1) < Math.abs(d2)) {
            nearestDateTime = dateTime;
         }

         int year = dateTime.getYear();
         Map<Integer, List<Integer>> months = (Map)datesWithData.get(year);
         if (months == null) {
            months = new LinkedHashMap();
            datesWithData.put(year, months);
         }

         int month = dateTime.getMonthOfYear() - 1;
         List<Integer> days = (List)((Map)months).get(month);
         if (days == null) {
            days = new ArrayList();
            ((Map)months).put(month, days);
         }

         int day = dateTime.getDayOfMonth();
         if (!((List)days).contains(day)) {
            ((List)days).add(day);
         }
      }

      Map<String, Object> models = new HashMap();
      models.put("layer", layer);
      models.put("datesWithData", datesWithData);
      models.put("nearestTimeIso", WmsUtils.dateTimeToISO8601(nearestDateTime));
      models.put("paletteNames", ColorPalette.getAvailablePaletteNames());
      return new ModelAndView("showLayerDetails", models);
   }

   private Layer getLayer(HttpServletRequest request) throws LayerNotDefinedException {
      String layerName = request.getParameter("layerName");
      if (layerName == null) {
         throw new LayerNotDefinedException("null");
      } else {
         return this.layerFactory.getLayer(layerName);
      }
   }

   private ModelAndView showTimesteps(HttpServletRequest request) throws Exception {
      Layer layer = this.getLayer(request);
      if (layer.getTimeValues().isEmpty()) {
         return null;
      } else {
         String dayStr = request.getParameter("day");
         if (dayStr == null) {
            throw new Exception("Must provide a value for the day parameter");
         } else {
            DateTime date = WmsUtils.iso8601ToDateTime(dayStr, layer.getChronology());
            List<DateTime> timesteps = new ArrayList();
            Iterator i$ = layer.getTimeValues().iterator();

            while(i$.hasNext()) {
               DateTime tVal = (DateTime)i$.next();
               if (onSameDay(tVal, date)) {
                  timesteps.add(tVal);
               }
            }

            log.debug("Found {} timesteps on {}", timesteps.size(), dayStr);
            return new ModelAndView("showTimesteps", "timesteps", timesteps);
         }
      }
   }

   private static boolean onSameDay(DateTime dt1, DateTime dt2) {
      dt1 = dt1.withZone(DateTimeZone.UTC);
      dt2 = dt2.withZone(DateTimeZone.UTC);
      boolean onSameDay = dt1.getYear() == dt2.getYear() && dt1.getMonthOfYear() == dt2.getMonthOfYear() && dt1.getDayOfMonth() == dt2.getDayOfMonth();
      log.debug("onSameDay({}, {}) = {}", new Object[]{dt1, dt2, onSameDay});
      return onSameDay;
   }

   private ModelAndView showMinMax(HttpServletRequest request, UsageLogEntry usageLogEntry) throws Exception {
      RequestParams params = new RequestParams(request.getParameterMap());
      GetMapDataRequest dr = new GetMapDataRequest(params, "1.1.1");
      Layer layer = this.layerFactory.getLayer(dr.getLayers()[0]);
      usageLogEntry.setLayer(layer);
      RegularGrid grid = WmsUtils.getImageGrid(dr);
      double zValue = AbstractWmsController.getElevationValue(dr.getElevationString(), layer);
      List<DateTime> timeValues = AbstractWmsController.getTimeValues(dr.getTimeString(), layer);
      DateTime tValue = timeValues.isEmpty() ? null : (DateTime)timeValues.get(0);
      List magnitudes;
      if (layer instanceof ScalarLayer) {
         magnitudes = ((ScalarLayer)layer).readHorizontalPoints(tValue, zValue, grid);
      } else {
         if (!(layer instanceof VectorLayer)) {
            throw new IllegalStateException("Invalid Layer type");
         }

         VectorLayer vecLayer = (VectorLayer)layer;
         List<Float> east = vecLayer.getEastwardComponent().readHorizontalPoints(tValue, zValue, grid);
         List<Float> north = vecLayer.getNorthwardComponent().readHorizontalPoints(tValue, zValue, grid);
         magnitudes = WmsUtils.getMagnitudes(east, north);
      }

      Range<Float> valueRange = Ranges.findMinMax(magnitudes);
      return new ModelAndView("showMinMax", "valueRange", valueRange);
   }

   private ModelAndView showAnimationTimesteps(HttpServletRequest request) throws Exception {
      Layer layer = this.getLayer(request);
      String startStr = request.getParameter("start");
      String endStr = request.getParameter("end");
      if (startStr != null && endStr != null) {
         int startIndex = AbstractWmsController.findTIndex(startStr, layer);
         int endIndex = AbstractWmsController.findTIndex(endStr, layer);
         List<DateTime> tValues = layer.getTimeValues();
         Map<String, String> timeStrings = new LinkedHashMap();
         timeStrings.put("Full (" + (endIndex - startIndex + 1) + " frames)", startStr + "/" + endStr);
         addTimeString("Daily", timeStrings, tValues, startIndex, endIndex, (new Period()).withDays(1));
         addTimeString("Weekly", timeStrings, tValues, startIndex, endIndex, (new Period()).withWeeks(1));
         addTimeString("Monthly", timeStrings, tValues, startIndex, endIndex, (new Period()).withMonths(1));
         addTimeString("Bi-monthly", timeStrings, tValues, startIndex, endIndex, (new Period()).withMonths(2));
         addTimeString("Twice-yearly", timeStrings, tValues, startIndex, endIndex, (new Period()).withMonths(6));
         addTimeString("Yearly", timeStrings, tValues, startIndex, endIndex, (new Period()).withYears(1));
         return new ModelAndView("showAnimationTimesteps", "timeStrings", timeStrings);
      } else {
         throw new Exception("Must provide values for start and end");
      }
   }

   private static void addTimeString(String label, Map<String, String> timeStrings, List<DateTime> tValues, int startIndex, int endIndex, Period resolution) {
      List<DateTime> timesteps = getAnimationTimesteps(tValues, startIndex, endIndex, resolution);
      if (timesteps.size() > 1) {
         String timeString = getTimeString(timesteps);
         timeStrings.put(label + " (" + timesteps.size() + " frames)", timeString);
      }

   }

   private static List<DateTime> getAnimationTimesteps(List<DateTime> tValues, int startIndex, int endIndex, Period resolution) {
      List<DateTime> times = new ArrayList();
      times.add(tValues.get(startIndex));

      for(int i = startIndex + 1; i <= endIndex; ++i) {
         DateTime lastdt = (DateTime)times.get(times.size() - 1);
         DateTime thisdt = (DateTime)tValues.get(i);
         if (!thisdt.isBefore(lastdt.plus(resolution))) {
            times.add(thisdt);
         }
      }

      return times;
   }

   private static String getTimeString(List<DateTime> timesteps) {
      if (timesteps.size() == 0) {
         return "";
      } else {
         StringBuilder builder = new StringBuilder(WmsUtils.dateTimeToISO8601((DateTime)timesteps.get(0)));

         for(int i = 1; i < timesteps.size(); ++i) {
            builder.append("," + WmsUtils.dateTimeToISO8601((DateTime)timesteps.get(i)));
         }

         return builder.toString();
      }
   }
}
