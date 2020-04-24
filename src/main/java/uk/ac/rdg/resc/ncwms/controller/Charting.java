package uk.ac.rdg.resc.ncwms.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D.Double;
import java.awt.image.IndexColorModel;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import uk.ac.rdg.resc.edal.geometry.impl.LineString;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.AbstractXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Utils;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;

public class Charting {
   private static final Locale US_LOCALE = new Locale("us", "US");
   private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

   public static JFreeChart createTimeseriesPlot(Layer layer, LonLatPosition lonLat, Map<DateTime, Float> tsData) {
      TimeSeries ts = new TimeSeries("Data");
      Iterator i$ = tsData.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<DateTime, Float> entry = (Entry)i$.next();
         ts.add(new Millisecond(((DateTime)entry.getKey()).toDate()), (Number)entry.getValue());
      }

      TimeSeriesCollection xydataset = new TimeSeriesCollection();
      xydataset.addSeries(ts);
      String title = "Lon: " + lonLat.getLongitude() + ", Lat: " + lonLat.getLatitude();
      String yLabel = layer.getTitle() + " (" + layer.getUnits() + ")";
      JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Date / time", yLabel, xydataset, false, false, false);
      XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
      renderer.setSeriesShape(0, new Double(-1.0D, -1.0D, 2.0D, 2.0D));
      renderer.setSeriesShapesVisible(0, true);
      chart.getXYPlot().setRenderer(renderer);
      chart.getXYPlot().setNoDataMessage("There is no data for your choice");
      chart.getXYPlot().setNoDataMessageFont(new Font("sansserif", 1, 32));
      return chart;
   }

   public static JFreeChart createTimeseriesPlot(String lon, String lat, Map<DateTime, Float> tsData, String variableName, String variableUnit) {
      TimeSeries ts = new TimeSeries("Data");
      Iterator i$ = tsData.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<DateTime, Float> entry = (Entry)i$.next();
         ts.add(new Millisecond(((DateTime)entry.getKey()).toDate()), (Number)entry.getValue());
      }

      TimeSeriesCollection xydataset = new TimeSeriesCollection();
      xydataset.addSeries(ts);
      String title = "Lon: " + lon + ", Lat: " + lat;
      String yLabel = variableName + "(" + variableUnit + ")";
      JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Date / time", yLabel, xydataset, false, false, false);
      XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
      renderer.setSeriesShape(0, new Double(-1.0D, -1.0D, 2.0D, 2.0D));
      renderer.setSeriesShapesVisible(0, true);
      chart.getXYPlot().setRenderer(renderer);
      chart.getXYPlot().setNoDataMessage("There is no data for your choice");
      chart.getXYPlot().setNoDataMessageFont(new Font("sansserif", 1, 32));
      return chart;
   }

   public static JFreeChart createMultiTimeseriesPlot(List<Map<DateTime, Float>> tsData, List<String> labels, String variableUnit) {
      if (tsData.size() != labels.size()) {
         throw new IllegalArgumentException("tsData and labels must be the same length");
      } else {
         TimeSeriesCollection xydataset = new TimeSeriesCollection();
         XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
         float low = Float.MAX_VALUE;
         float high = Float.MIN_VALUE;

         for(int i = 0; i < tsData.size(); ++i) {
            Map<DateTime, Float> tsDatum = (Map)tsData.get(i);
            String label = (String)labels.get(i);
            TimeSeries ts = new TimeSeries(label);

            Entry entry;
            for(Iterator i$ = tsDatum.entrySet().iterator(); i$.hasNext(); ts.add(new Millisecond(((DateTime)entry.getKey()).toDate()), (Number)entry.getValue())) {
               entry = (Entry)i$.next();
               Float val = (Float)entry.getValue();
               if (val < low) {
                  low = val;
               }

               if (val > high) {
                  high = val;
               }
            }

            xydataset.addSeries(ts);
            renderer.setSeriesShape(i, new Double(-1.0D, -1.0D, 2.0D, 2.0D));
            renderer.setSeriesShapesVisible(i, true);
         }

         String title = "";
         JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Date / time", variableUnit, xydataset, true, false, false);
         chart.getXYPlot().setRenderer(renderer);
         chart.getXYPlot().setNoDataMessage("There is no data for your choice");
         chart.getXYPlot().setNoDataMessageFont(new Font("sansserif", 1, 32));
         chart.getXYPlot().getDomainAxis().setAutoRange(true);
         chart.getXYPlot().getRangeAxis().setRange((double)low, (double)high);
         return chart;
      }
   }

   public static JFreeChart createVerticalProfilePlot(double lon, double lat, List<Float> dataValues, List<java.lang.Double> elevationValues, String variableName, String variableUnit, DateTime dateTime) {
      if (elevationValues.size() != dataValues.size()) {
         throw new IllegalArgumentException("Z values and data values not of same length");
      } else {
         String zAxisLabel = "Depth";
         boolean invertYAxis = true;
         NumberAxis zAxis = new NumberAxis(zAxisLabel + " (meters)");
         zAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
         if (invertYAxis) {
            zAxis.setInverted(true);
         }

         Charting.ZAxisAndValues zAxisAndValues = new Charting.ZAxisAndValues(zAxis, elevationValues);
         elevationValues = zAxisAndValues.zValues;
         NumberAxis elevationAxis = zAxisAndValues.zAxis;
         elevationAxis.setAutoRangeIncludesZero(false);
         String axisLabel = variableName + "(" + variableUnit + ")";
         NumberAxis valueAxis = new NumberAxis(axisLabel);
         valueAxis.setAutoRangeIncludesZero(false);
         XYSeries series = new XYSeries("data", true);

         for(int i = 0; i < elevationValues.size(); ++i) {
            series.add((Number)elevationValues.get(i), (Number)dataValues.get(i));
         }

         XYSeriesCollection xySeriesColl = new XYSeriesCollection();
         xySeriesColl.addSeries(series);
         XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
         renderer.setSeriesShape(0, new Double(-1.0D, -1.0D, 2.0D, 2.0D));
         renderer.setSeriesPaint(0, Color.RED);
         renderer.setSeriesShapesVisible(0, true);
         XYPlot plot = new XYPlot(xySeriesColl, elevationAxis, valueAxis, renderer);
         plot.setBackgroundPaint(Color.lightGray);
         plot.setDomainGridlinesVisible(false);
         plot.setRangeGridlinePaint(Color.white);
         plot.setOrientation(PlotOrientation.HORIZONTAL);
         String lonStr = java.lang.Double.toString(Math.abs(lon)) + (lon >= 0.0D ? "E" : "W");
         String latStr = java.lang.Double.toString(Math.abs(lat)) + (lat >= 0.0D ? "N" : "S");
         String title = String.format("Profile of %s at %s, %s", variableName, lonStr, latStr);
         if (dateTime != null) {
            title = title + " at " + WmsUtils.dateTimeToISO8601(dateTime);
         }

         return new JFreeChart(title, (Font)null, plot, false);
      }
   }

   public static JFreeChart createMultiVerticalProfilePlot(List<Map<java.lang.Double, Float>> dataValues, List<String> labels, String variableUnit, DateTime dateTime) {
      if (dataValues.size() != labels.size()) {
         throw new IllegalArgumentException("must provide labels for each data series");
      } else {
         String zAxisLabel = "Depth";
         boolean invertYAxis = true;
         NumberAxis zAxis = new NumberAxis(zAxisLabel + " (meters)");
         zAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
         zAxis.setInverted(invertYAxis);
         zAxis.setAutoRangeIncludesZero(false);
         NumberAxis valueAxis = new NumberAxis(variableUnit);
         valueAxis.setAutoRangeIncludesZero(false);
         XYSeriesCollection xySeriesColl = new XYSeriesCollection();

         for(int i = 0; i < dataValues.size(); ++i) {
            Map<java.lang.Double, Float> datumValues = (Map)dataValues.get(i);
            XYSeries series = new XYSeries((Comparable)labels.get(i), true);
            Iterator i$ = datumValues.entrySet().iterator();

            while(i$.hasNext()) {
               Entry<java.lang.Double, Float> entryPair = (Entry)i$.next();
               series.add((Number)entryPair.getKey(), (Number)entryPair.getValue());
            }

            xySeriesColl.addSeries(series);
         }

         valueAxis.setAutoRange(true);
         XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
         renderer.setSeriesPaint(0, Color.RED);
         renderer.setSeriesShapesVisible(0, true);

         for(int i = 0; i < dataValues.size(); ++i) {
            renderer.setSeriesShape(i, new Double(-1.0D, -1.0D, 2.0D, 2.0D));
         }

         XYPlot plot = new XYPlot(xySeriesColl, zAxis, valueAxis, renderer);
         plot.setBackgroundPaint(Color.lightGray);
         plot.setDomainGridlinesVisible(false);
         plot.setRangeGridlinePaint(Color.white);
         plot.setOrientation(PlotOrientation.HORIZONTAL);
         String title = "";
         if (dateTime != null) {
            title = title + " at " + WmsUtils.dateTimeToISO8601(dateTime);
         }

         return new JFreeChart(title, (Font)null, plot, true);
      }
   }

   public static JFreeChart createVerticalProfilePlot(Layer layer, HorizontalPosition pos, List<java.lang.Double> elevationValues, List<Float> dataValues, DateTime dateTime) {
      if (elevationValues.size() != dataValues.size()) {
         throw new IllegalArgumentException("Z values and data values not of same length");
      } else {
         Charting.ZAxisAndValues zAxisAndValues = getZAxisAndValues(layer, elevationValues);
         elevationValues = zAxisAndValues.zValues;
         NumberAxis elevationAxis = zAxisAndValues.zAxis;
         elevationAxis.setAutoRangeIncludesZero(false);
         NumberAxis valueAxis = new NumberAxis(getAxisLabel(layer));
         valueAxis.setAutoRangeIncludesZero(false);
         valueAxis.setAutoRange(true);
         XYSeries series = new XYSeries("data", true);

         for(int i = 0; i < elevationValues.size(); ++i) {
            series.add((Number)elevationValues.get(i), (Number)dataValues.get(i));
         }

         XYSeriesCollection xySeriesColl = new XYSeriesCollection();
         xySeriesColl.addSeries(series);
         XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
         renderer.setSeriesShape(0, new Double(-1.0D, -1.0D, 2.0D, 2.0D));
         renderer.setSeriesPaint(0, Color.RED);
         renderer.setSeriesShapesVisible(0, true);
         XYPlot plot = new XYPlot(xySeriesColl, elevationAxis, valueAxis, renderer);
         plot.setBackgroundPaint(Color.lightGray);
         plot.setDomainGridlinesVisible(false);
         plot.setRangeGridlinePaint(Color.white);
         plot.setOrientation(PlotOrientation.HORIZONTAL);
         HorizontalPosition lonLatPos = Utils.transformPosition(pos, DefaultGeographicCRS.WGS84);
         double lon = lonLatPos.getX();
         String lonStr = java.lang.Double.toString(Math.abs(lon)) + (lon >= 0.0D ? "E" : "W");
         double lat = lonLatPos.getY();
         String latStr = java.lang.Double.toString(Math.abs(lat)) + (lat >= 0.0D ? "N" : "S");
         String title = String.format("Profile of %s at %s, %s", layer.getTitle(), lonStr, latStr);
         if (dateTime != null) {
            title = title + " at " + WmsUtils.dateTimeToISO8601(dateTime);
         }

         return new JFreeChart(title, (Font)null, plot, false);
      }
   }

   private static String getAxisLabel(Layer layer) {
      return WmsUtils.removeDuplicatedWhiteSpace(layer.getTitle()) + " (" + layer.getUnits() + ")";
   }

   public static JFreeChart createTransectPlot(Layer layer, LineString transectDomain, List<Float> transectData) {
      XYSeries series = new XYSeries("data", true);

      for(int i = 0; i < transectData.size(); ++i) {
         series.add((double)i, (Number)transectData.get(i));
      }

      XYSeriesCollection xySeriesColl = new XYSeriesCollection();
      xySeriesColl.addSeries(series);
      JFreeChart chart;
      XYPlot plot;
      if (layer.getElevationValues().size() > 1) {
         XYItemRenderer renderer1 = new StandardXYItemRenderer();
         NumberAxis rangeAxis1 = new NumberAxis(getAxisLabel(layer));
         plot = new XYPlot(xySeriesColl, (ValueAxis)null, rangeAxis1, renderer1);
         plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
         plot.setBackgroundPaint(Color.lightGray);
         plot.setDomainGridlinesVisible(false);
         plot.setRangeGridlinePaint(Color.white);
         plot.getRenderer().setSeriesPaint(0, Color.RED);
         plot.setOrientation(PlotOrientation.VERTICAL);
         chart = new JFreeChart(plot);
      } else {
         chart = ChartFactory.createXYLineChart("Transect for " + layer.getTitle(), "distance along transect (arbitrary units)", layer.getTitle() + " (" + layer.getUnits() + ")", xySeriesColl, PlotOrientation.VERTICAL, false, false, false);
         plot = chart.getXYPlot();
      }

      if (layer.getDataset().getCopyrightStatement() != null) {
         TextTitle textTitle = new TextTitle(layer.getDataset().getCopyrightStatement());
         textTitle.setFont(new Font("SansSerif", 0, 10));
         textTitle.setPosition(RectangleEdge.BOTTOM);
         textTitle.setHorizontalAlignment(HorizontalAlignment.RIGHT);
         chart.addSubtitle(textTitle);
      }

      NumberAxis rangeAxis = (NumberAxis)plot.getRangeAxis();
      rangeAxis.setAutoRangeIncludesZero(false);
      plot.setNoDataMessage("There is no data for what you have chosen.");
      java.lang.Double prevCtrlPointDistance = null;

      for(int i = 0; i < transectDomain.getControlPoints().size(); ++i) {
         double ctrlPointDistance = transectDomain.getFractionalControlPointDistance(i);
         if (prevCtrlPointDistance != null) {
            IntervalMarker target = new IntervalMarker((double)transectData.size() * prevCtrlPointDistance, (double)transectData.size() * ctrlPointDistance);
            target.setLabel("[" + printTwoDecimals(((HorizontalPosition)transectDomain.getControlPoints().get(i - 1)).getY()) + "," + printTwoDecimals(((HorizontalPosition)transectDomain.getControlPoints().get(i - 1)).getX()) + "]");
            target.setLabelFont(new Font("SansSerif", 2, 11));
            if (i % 2 == 0) {
               target.setPaint(new Color(222, 222, 255, 128));
               target.setLabelAnchor(RectangleAnchor.TOP_LEFT);
               target.setLabelTextAnchor(TextAnchor.TOP_LEFT);
            } else {
               target.setPaint(new Color(233, 225, 146, 128));
               target.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
               target.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
            }

            plot.addDomainMarker(target);
         }

         prevCtrlPointDistance = transectDomain.getFractionalControlPointDistance(i);
      }

      return chart;
   }

   private static String printTwoDecimals(double d) {
      DecimalFormat twoDForm = new DecimalFormat("#.##");
      DecimalFormatSymbols decSym = DecimalFormatSymbols.getInstance(US_LOCALE);
      twoDForm.setDecimalFormatSymbols(decSym);
      return twoDForm.format(d);
   }

   private static Charting.ZAxisAndValues getZAxisAndValues(Layer layer, List<java.lang.Double> elevationValues) {
      String zAxisLabel;
      boolean invertYAxis;
      if (layer.isElevationPositive()) {
         zAxisLabel = "Height";
         invertYAxis = false;
      } else if (layer.isElevationPressure()) {
         zAxisLabel = "Pressure";
         invertYAxis = true;
      } else {
         zAxisLabel = "Depth";
         List<java.lang.Double> newElValues = new ArrayList(((List)elevationValues).size());
         Iterator i$ = ((List)elevationValues).iterator();

         while(i$.hasNext()) {
            java.lang.Double zVal = (java.lang.Double)i$.next();
            newElValues.add(-zVal);
         }

         elevationValues = newElValues;
         invertYAxis = true;
      }

      NumberAxis zAxis = new NumberAxis(zAxisLabel + " (" + layer.getElevationUnits() + ")");
      zAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
      if (invertYAxis) {
         zAxis.setInverted(true);
      }

      return new Charting.ZAxisAndValues(zAxis, (List)elevationValues);
   }

   public static JFreeChart createVerticalSectionChart(Layer layer, LineString horizPath, List<java.lang.Double> elevationValues, List<List<Float>> sectionData, Range<Float> colourScaleRange, ColorPalette palette, int numColourBands, boolean logarithmic, double zValue) {
      Charting.ZAxisAndValues zAxisAndValues = getZAxisAndValues(layer, elevationValues);
      elevationValues = zAxisAndValues.zValues;
      double minElValue = 0.0D;
      double maxElValue = 1.0D;
      if (elevationValues.size() != 0 && sectionData.size() != 0) {
         minElValue = (java.lang.Double)elevationValues.get(0);
         maxElValue = (java.lang.Double)elevationValues.get(elevationValues.size() - 1);
      }

      if (minElValue > maxElValue) {
         double temp = minElValue;
         minElValue = maxElValue;
         maxElValue = temp;
      }

      int numElValues = 300;
      XYZDataset dataset = new Charting.VerticalSectionDataset(elevationValues, sectionData, minElValue, maxElValue, numElValues);
      NumberAxis xAxis = new NumberAxis("Distance along path (arbitrary units)");
      xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
      PaintScale scale = createPaintScale(palette, colourScaleRange, numColourBands, logarithmic);
      NumberAxis colorScaleBar = new NumberAxis();
      org.jfree.data.Range colorBarRange = new org.jfree.data.Range((double)(Float)colourScaleRange.getMinimum(), (double)(Float)colourScaleRange.getMaximum());
      colorScaleBar.setRange(colorBarRange);
      PaintScaleLegend paintScaleLegend = new PaintScaleLegend(scale, colorScaleBar);
      paintScaleLegend.setPosition(RectangleEdge.BOTTOM);
      XYBlockRenderer renderer = new XYBlockRenderer();
      double elevationResolution = (maxElValue - minElValue) / (double)numElValues;
      renderer.setBlockHeight(elevationResolution);
      renderer.setPaintScale(scale);
      XYPlot plot = new XYPlot(dataset, xAxis, zAxisAndValues.zAxis, renderer);
      plot.setBackgroundPaint(Color.lightGray);
      plot.setDomainGridlinesVisible(false);
      plot.setRangeGridlinePaint(Color.white);
      java.lang.Double prevCtrlPointDistance = null;
      int xAxisLength = 0;
      if (sectionData.size() > 0) {
         xAxisLength = ((List)sectionData.get(0)).size();
      }

      for(int i = 0; i < horizPath.getControlPoints().size(); ++i) {
         double ctrlPointDistance = horizPath.getFractionalControlPointDistance(i);
         if (prevCtrlPointDistance != null) {
            IntervalMarker target = new IntervalMarker((double)xAxisLength * prevCtrlPointDistance, (double)xAxisLength * ctrlPointDistance);
            target.setPaint(TRANSPARENT);
            plot.addDomainMarker(target);
            Marker verticalLevel = new ValueMarker(Math.abs(zValue));
            verticalLevel.setPaint(Color.lightGray);
            verticalLevel.setLabel("at " + zValue + "  level ");
            verticalLevel.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
            verticalLevel.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
            plot.addRangeMarker(verticalLevel);
         }

         prevCtrlPointDistance = horizPath.getFractionalControlPointDistance(i);
      }

      JFreeChart chart = new JFreeChart(layer.getTitle() + " (" + layer.getUnits() + ")", plot);
      chart.removeLegend();
      chart.addSubtitle(paintScaleLegend);
      chart.setBackgroundPaint(Color.white);
      return chart;
   }

   public static PaintScale createPaintScale(ColorPalette colorPalette, final Range<Float> colourScaleRange, final int numColourBands, final boolean logarithmic) {
      final IndexColorModel cm = colorPalette.getColorModel(numColourBands, 100, Color.white, true);
      return new PaintScale() {
         public double getLowerBound() {
            return (double)(Float)colourScaleRange.getMinimum();
         }

         public double getUpperBound() {
            return (double)(Float)colourScaleRange.getMaximum();
         }

         public Color getPaint(double value) {
            int index = this.getColourIndex(value);
            return new Color(cm.getRGB(index));
         }

         private int getColourIndex(double value) {
            if (java.lang.Double.isNaN(value)) {
               return numColourBands;
            } else if (value >= this.getLowerBound() && value <= this.getUpperBound()) {
               double min = logarithmic ? Math.log(this.getLowerBound()) : this.getLowerBound();
               double max = logarithmic ? Math.log(this.getUpperBound()) : this.getUpperBound();
               double val = logarithmic ? Math.log(value) : value;
               double frac = (val - min) / (max - min);
               int index = (int)(frac * (double)numColourBands);
               if (index == numColourBands) {
                  --index;
               }

               return index;
            } else {
               return numColourBands + 1;
            }
         }
      };
   }

   private static class VerticalSectionDataset extends AbstractXYZDataset {
      private static final long serialVersionUID = 1L;
      private final int horizPathLength;
      private final List<List<Float>> sectionData;
      private final List<java.lang.Double> elevationValues;
      private final double minElValue;
      private final double elevationResolution;
      private final int numElevations;

      public VerticalSectionDataset(List<java.lang.Double> elevationValues, List<List<Float>> sectionData, double minElValue, double maxElValue, int numElevations) {
         if (sectionData.size() > 0) {
            this.horizPathLength = ((List)sectionData.get(0)).size();
         } else {
            this.horizPathLength = 0;
         }

         this.sectionData = sectionData;
         this.elevationValues = elevationValues;
         this.minElValue = minElValue;
         this.numElevations = numElevations;
         this.elevationResolution = (maxElValue - minElValue) / (double)numElevations;
      }

      public int getSeriesCount() {
         return 1;
      }

      public String getSeriesKey(int series) {
         checkSeries(series);
         return "Vertical section";
      }

      public int getItemCount(int series) {
         checkSeries(series);
         return this.horizPathLength * this.numElevations;
      }

      public Integer getX(int series, int item) {
         checkSeries(series);
         return item % this.horizPathLength;
      }

      public java.lang.Double getY(int series, int item) {
         checkSeries(series);
         int yIndex = item / this.horizPathLength;
         return this.minElValue + (double)yIndex * this.elevationResolution;
      }

      public Float getZ(int series, int item) {
         checkSeries(series);
         int xIndex = item % this.horizPathLength;
         double elevation = this.getY(series, item);
         int nearestElevationIndex = -1;
         double minDiff = java.lang.Double.POSITIVE_INFINITY;

         for(int i = 0; i < this.elevationValues.size(); ++i) {
            double el = (java.lang.Double)this.elevationValues.get(i);
            double diff = Math.abs(el - elevation);
            if (diff < minDiff) {
               minDiff = diff;
               nearestElevationIndex = i;
            }
         }

         return (Float)((List)this.sectionData.get(nearestElevationIndex)).get(xIndex);
      }

      private static void checkSeries(int series) {
         if (series != 0) {
            throw new IllegalArgumentException("Series must be zero");
         }
      }
   }

   private static final class ZAxisAndValues {
      private final NumberAxis zAxis;
      private final List<java.lang.Double> zValues;

      public ZAxisAndValues(NumberAxis zAxis, List<java.lang.Double> zValues) {
         this.zAxis = zAxis;
         this.zValues = zValues;
      }
   }
}
