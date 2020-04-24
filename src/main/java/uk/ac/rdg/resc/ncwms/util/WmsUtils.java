package uk.ac.rdg.resc.ncwms.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.geotools.referencing.CRS;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.JulianChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.impl.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.time.AllLeapChronology;
import uk.ac.rdg.resc.edal.time.NoLeapChronology;
import uk.ac.rdg.resc.edal.time.ThreeSixtyDayChronology;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.SimpleVectorLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

public class WmsUtils {
   public static final Set<String> SUPPORTED_VERSIONS = new HashSet();
   private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER;
   private static final DateTimeFormatter ISO_DATE_TIME_PARSER;
   private static final DateTimeFormatter ISO_TIME_FORMATTER;
   private static final String EMPTY_STRING = "";
   private static final Pattern MULTIPLE_WHITESPACE;
   public static final Comparator<DateTime> DATE_TIME_COMPARATOR;

   private WmsUtils() {
      throw new AssertionError();
   }

   public static String dateTimeToISO8601(DateTime dateTime) {
      return ISO_DATE_TIME_FORMATTER.print(dateTime);
   }

   public static DateTime iso8601ToDateTime(String isoDateTime, Chronology chronology) {
      try {
         return ISO_DATE_TIME_PARSER.withChronology(chronology).parseDateTime(isoDateTime);
      } catch (RuntimeException var3) {
         throw var3;
      }
   }

   public static String formatUTCTimeOnly(DateTime dateTime) {
      return ISO_TIME_FORMATTER.print(dateTime);
   }

   public static int findTimeIndex(List<DateTime> dtList, DateTime target) {
      return Collections.binarySearch(dtList, target, DATE_TIME_COMPARATOR);
   }

   public static void createDirectory(File dir) throws Exception {
      if (dir.exists()) {
         if (!dir.isDirectory()) {
            throw new Exception(dir.getPath() + " already exists but it is a regular file");
         }
      } else {
         boolean created = dir.mkdirs();
         if (!created) {
            throw new Exception("Could not create directory " + dir.getPath());
         }
      }
   }

   public static String createUniqueLayerName(String datasetId, String layerId) {
      return datasetId + "/" + layerId;
   }

   public static double[] parseBbox(String bboxStr, boolean lonFirst) throws WmsException {
      String[] bboxEls = bboxStr.split(",");
      if (bboxEls.length != 4) {
         throw new WmsException("Invalid bounding box format: need four elements");
      } else {
         double[] bbox = new double[4];

         try {
            if (lonFirst) {
               bbox[0] = Double.parseDouble(bboxEls[0]);
               bbox[1] = Double.parseDouble(bboxEls[1]);
               bbox[2] = Double.parseDouble(bboxEls[2]);
               bbox[3] = Double.parseDouble(bboxEls[3]);
            } else {
               bbox[0] = Double.parseDouble(bboxEls[1]);
               bbox[1] = Double.parseDouble(bboxEls[0]);
               bbox[2] = Double.parseDouble(bboxEls[3]);
               bbox[3] = Double.parseDouble(bboxEls[2]);
            }
         } catch (NumberFormatException var5) {
            throw new WmsException("Invalid bounding box format: all elements must be numeric");
         }

         if (bbox[0] < bbox[2] && bbox[1] < bbox[3]) {
            return bbox;
         } else {
            throw new WmsException("Invalid bounding box format");
         }
      }
   }

   public static List<Float> getMagnitudes(List<Float> eastData, List<Float> northData) {
      if (eastData != null && northData != null) {
         if (eastData.size() != northData.size()) {
            throw new IllegalArgumentException("east and north data components must be the same length");
         } else {
            List<Float> mag = new ArrayList(eastData.size());

            for(int i = 0; i < eastData.size(); ++i) {
               Float east = (Float)eastData.get(i);
               Float north = (Float)northData.get(i);
               Float val = null;
               if (east != null && north != null) {
                  val = (float)Math.sqrt((double)(east * east + north * north));
               }

               mag.add(val);
            }

            if (mag.size() != eastData.size()) {
               throw new AssertionError();
            } else {
               return mag;
            }
         }
      } else {
         throw new NullPointerException();
      }
   }

   public static boolean isOpendapLocation(String location) {
      return location.startsWith("http://") || location.startsWith("dods://") || location.startsWith("https://");
   }

   public static boolean isNcmlAggregation(String location) {
      return location.endsWith(".xml") || location.endsWith(".ncml");
   }

   public static Range<Float> estimateValueRange(Layer layer) throws IOException {
      if (layer instanceof ScalarLayer) {
         List<Float> dataSample = readDataSample((ScalarLayer)layer);
         return Ranges.findMinMax(dataSample);
      } else if (layer instanceof VectorLayer) {
         VectorLayer vecLayer = (VectorLayer)layer;
         List<Float> eastDataSample = readDataSample(vecLayer.getEastwardComponent());
         List<Float> northDataSample = readDataSample(vecLayer.getEastwardComponent());
         List<Float> magnitudes = getMagnitudes(eastDataSample, northDataSample);
         return Ranges.findMinMax(magnitudes);
      } else {
         throw new IllegalStateException("Unrecognized layer type");
      }
   }

   private static List<Float> readDataSample(ScalarLayer layer) throws IOException {
      try {
         return layer.readHorizontalPoints(layer.getDefaultTimeValue(), layer.getDefaultElevationValue(), new RegularGridImpl(layer.getGeographicBoundingBox(), 100, 100));
      } catch (InvalidDimensionValueException var2) {
         throw new IllegalStateException(var2);
      }
   }

   public static String removeDuplicatedWhiteSpace(String theString) {
      return MULTIPLE_WHITESPACE.matcher(theString).replaceAll(" ");
   }

   public static List<VectorLayer> findVectorLayers(Collection<? extends ScalarLayer> scalarLayers) {
      Map<String, ScalarLayer[]> components = new LinkedHashMap();
      Iterator i$ = scalarLayers.iterator();

      String vectorKey;
      while(i$.hasNext()) {
         ScalarLayer layer = (ScalarLayer)i$.next();
         if (layer.getTitle().contains("eastward")) {
            vectorKey = layer.getTitle().replaceFirst("eastward_", "");
            if (!components.containsKey(vectorKey)) {
               components.put(vectorKey, new ScalarLayer[2]);
            }

            ((ScalarLayer[])components.get(vectorKey))[0] = layer;
         } else if (layer.getTitle().contains("northward")) {
            vectorKey = layer.getTitle().replaceFirst("northward_", "");
            if (!components.containsKey(vectorKey)) {
               components.put(vectorKey, new ScalarLayer[2]);
            }

            ((ScalarLayer[])components.get(vectorKey))[1] = layer;
         }
      }

      List<VectorLayer> vectorLayers = new ArrayList();
      Iterator i$ = components.keySet().iterator();

      while(i$.hasNext()) {
         vectorKey = (String)i$.next();
         ScalarLayer[] comps = (ScalarLayer[])components.get(vectorKey);
         if (comps[0] != null && comps[1] != null) {
            VectorLayer vec = new SimpleVectorLayer(vectorKey, comps[0], comps[1]);
            vectorLayers.add(vec);
         }
      }

      return vectorLayers;
   }

   public static boolean isVectorLayer(Layer layer) {
      return layer instanceof VectorLayer;
   }

   public static String getExceptionName(Exception e) {
      return e.getClass().getName();
   }

   public static String getTimeAxisUnits(Chronology chronology) {
      if (chronology instanceof ISOChronology) {
         return "ISO8601";
      } else if (chronology instanceof JulianChronology) {
         return "julian";
      } else if (chronology instanceof ThreeSixtyDayChronology) {
         return "360_day";
      } else if (chronology instanceof NoLeapChronology) {
         return "noleap";
      } else {
         return chronology instanceof AllLeapChronology ? "all_leap" : "unknown";
      }
   }

   public static String getTimeStringForCapabilities(List<DateTime> times) {
      if (times == null) {
         throw new NullPointerException();
      } else if (times.isEmpty()) {
         return "";
      } else if (times.size() == 1) {
         return dateTimeToISO8601((DateTime)times.get(0));
      } else {
         List<SubList> subLists = new ArrayList();

         class SubList {
            int first;
            int last;
            long spacing;

            int length() {
               return this.last - this.first + 1;
            }
         }

         SubList currentSubList = new SubList();
         currentSubList.first = 0;
         currentSubList.spacing = ((DateTime)times.get(1)).getMillis() - ((DateTime)times.get(0)).getMillis();

         for(int i = 1; i < times.size() - 1; ++i) {
            long spacing = ((DateTime)times.get(i + 1)).getMillis() - ((DateTime)times.get(i)).getMillis();
            if (spacing != currentSubList.spacing) {
               currentSubList.last = i;
               subLists.add(currentSubList);
               currentSubList = new SubList();
               currentSubList.first = i;
               currentSubList.spacing = spacing;
            }
         }

         currentSubList.last = times.size() - 1;
         subLists.add(currentSubList);
         HashSet subListsDone = new HashSet(subLists.size());

         boolean done;
         SubList subList;
         do {
            int longestSubListIndex = -1;
            SubList longestSubList = null;

            int i;
            SubList prev;
            for(i = 0; i < subLists.size(); ++i) {
               if (!subListsDone.contains(i)) {
                  prev = (SubList)subLists.get(i);
                  if (longestSubList == null || prev.length() > longestSubList.length()) {
                     longestSubListIndex = i;
                     longestSubList = prev;
                  }
               }
            }

            subListsDone.add(longestSubListIndex);
            if (longestSubListIndex > 0) {
               subList = (SubList)subLists.get(longestSubListIndex - 1);
               if (subList.last == longestSubList.first) {
                  --subList.last;
               }
            }

            if (longestSubListIndex < subLists.size() - 1) {
               subList = (SubList)subLists.get(longestSubListIndex + 1);
               if (subList.first == longestSubList.last) {
                  ++subList.first;
               }
            }

            done = true;

            for(i = 1; i < subLists.size() - 1; ++i) {
               prev = (SubList)subLists.get(i - 1);
               SubList cur = (SubList)subLists.get(i);
               SubList next = (SubList)subLists.get(i + 1);
               if (prev.last == cur.first || cur.last == next.first) {
                  done = false;
                  break;
               }
            }
         } while(!done);

         StringBuilder str = new StringBuilder();

         for(int i = 0; i < subLists.size(); ++i) {
            subList = (SubList)subLists.get(i);
            List<DateTime> timeList = times.subList(subList.first, subList.last + 1);
            if (timeList.size() > 0) {
               if (i > 0) {
                  str.append(",");
               }

               str.append(getRegularlySpacedTimeString(timeList, subList.spacing));
            }
         }

         return str.toString();
      }
   }

   static StringBuilder getRegularlySpacedTimeString(List<DateTime> times, long period) {
      if (times.isEmpty()) {
         throw new IllegalArgumentException();
      } else {
         StringBuilder str = new StringBuilder();
         str.append(dateTimeToISO8601((DateTime)times.get(0)));
         if (times.size() == 2) {
            str.append(",");
            str.append(dateTimeToISO8601((DateTime)times.get(1)));
         } else if (times.size() > 2) {
            str.append("/");
            str.append(dateTimeToISO8601((DateTime)times.get(times.size() - 1)));
            str.append("/");
            str.append(getPeriodString(period));
         }

         return str;
      }
   }

   public static String getPeriodString(long period) {
      StringBuilder str = new StringBuilder("P");
      long days = period / 86400000L;
      if (days > 0L) {
         str.append(days + "D");
         period -= days * 86400000L;
      }

      if (period > 0L) {
         str.append("T");
      }

      long hours = period / 3600000L;
      if (hours > 0L) {
         str.append(hours + "H");
         period -= hours * 3600000L;
      }

      long minutes = period / 60000L;
      if (minutes > 0L) {
         str.append(minutes + "M");
         period -= minutes * 60000L;
      }

      if (period > 0L) {
         long seconds = period / 1000L;
         long millis = period % 1000L;
         str.append(seconds);
         if (millis > 0L) {
            str.append("." + addOrRemoveZeros(millis));
         }

         str.append("S");
      }

      return str.toString();
   }

   private static String addOrRemoveZeros(long millis) {
      if (millis == 0L) {
         return "";
      } else {
         String s = Long.toString(millis);
         if (millis < 10L) {
            return "00" + s;
         } else {
            if (millis < 100L) {
               s = "0" + s;
            }

            while(s.endsWith("0")) {
               s = s.substring(0, s.length() - 1);
            }

            return s;
         }
      }
   }

   public static CoordinateReferenceSystem getCrs(String crsCode) throws InvalidCrsException {
      if (crsCode == null) {
         throw new NullPointerException("CRS code cannot be null");
      } else {
         try {
            return CRS.decode(crsCode, true);
         } catch (Exception var2) {
            throw new InvalidCrsException(crsCode + " " + var2.toString());
         }
      }
   }

   public static RegularGrid getImageGrid(GetMapDataRequest dr) throws InvalidCrsException {
      CoordinateReferenceSystem crs = getCrs(dr.getCrsCode());
      BoundingBox bbox = new BoundingBoxImpl(dr.getBbox(), crs);
      return new RegularGridImpl(bbox, dr.getWidth(), dr.getHeight());
   }

   public static ArrayList<Float> nullArrayList(int n) {
      ArrayList<Float> list = new ArrayList(n);
      Collections.fill(list, (Object)null);
      return list;
   }

   static {
      ISO_DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);
      ISO_DATE_TIME_PARSER = ISODateTimeFormat.dateTimeParser().withZone(DateTimeZone.UTC);
      ISO_TIME_FORMATTER = ISODateTimeFormat.time().withZone(DateTimeZone.UTC);
      MULTIPLE_WHITESPACE = Pattern.compile("\\s+");
      DATE_TIME_COMPARATOR = new Comparator<DateTime>() {
         public int compare(DateTime dt1, DateTime dt2) {
            return dt1.compareTo(dt2);
         }
      };
      SUPPORTED_VERSIONS.add("1.1.1");
      SUPPORTED_VERSIONS.add("1.3.0");
   }
}
