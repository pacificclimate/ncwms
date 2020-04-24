package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmzFormat extends ImageFormat {
   private static final Logger logger = LoggerFactory.getLogger(KmzFormat.class);
   private static final String PICNAME = "frame";
   private static final String PICEXT = "png";
   private static final String COLOUR_SCALE_FILENAME = "legend.png";

   public void writeImage(List<BufferedImage> frames, OutputStream out, Layer layer, List<String> tValues, String zValue, BoundingBox bbox, BufferedImage legend) throws IOException {
      StringBuffer kml = new StringBuffer();

      for(int frameIndex = 0; frameIndex < frames.size(); ++frameIndex) {
         if (frameIndex == 0) {
            kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            kml.append(System.getProperty("line.separator"));
            kml.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">");
            kml.append("<Folder>");
            kml.append("<visibility>1</visibility>");
            kml.append("<name>" + layer.getDataset().getId() + ", " + layer.getId() + "</name>");
            kml.append("<description>" + layer.getDataset().getTitle() + ", " + layer.getTitle() + ": " + layer.getLayerAbstract() + "</description>");
            kml.append("<ScreenOverlay>");
            kml.append("<name>Colour scale</name>");
            kml.append("<Icon><href>legend.png</href></Icon>");
            kml.append("<overlayXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>");
            kml.append("<screenXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>");
            kml.append("<rotationXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>");
            kml.append("<size x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>");
            kml.append("</ScreenOverlay>");
         }

         kml.append("<GroundOverlay>");
         String timestamp = null;
         String z = null;
         if (tValues.get(frameIndex) != null && !((String)tValues.get(frameIndex)).equals("")) {
            DateTime dt = WmsUtils.iso8601ToDateTime((String)tValues.get(frameIndex), layer.getChronology());
            timestamp = WmsUtils.dateTimeToISO8601(dt);
            kml.append("<TimeStamp><when>" + timestamp + "</when></TimeStamp>");
         }

         if (zValue != null && !zValue.equals("") && layer.getElevationValues() != null) {
            z = "";
            if (timestamp != null) {
               z = z + "<br />";
            }

            z = z + "Elevation: " + zValue + " " + layer.getElevationUnits();
         }

         kml.append("<name>");
         if (timestamp == null && z == null) {
            kml.append("Frame " + frameIndex);
         } else {
            kml.append("<![CDATA[");
            if (timestamp != null) {
               kml.append("Time: " + timestamp);
            }

            if (z != null) {
               kml.append(z);
            }

            kml.append("]]>");
         }

         kml.append("</name>");
         kml.append("<visibility>1</visibility>");
         kml.append("<Icon><href>" + getPicFileName(frameIndex) + "</href></Icon>");
         kml.append("<LatLonBox id=\"" + frameIndex + "\">");
         kml.append("<west>" + bbox.getMinX() + "</west>");
         kml.append("<south>" + bbox.getMinY() + "</south>");
         kml.append("<east>" + bbox.getMaxX() + "</east>");
         kml.append("<north>" + bbox.getMaxY() + "</north>");
         kml.append("<rotation>0</rotation>");
         kml.append("</LatLonBox>");
         kml.append("</GroundOverlay>");
      }

      kml.append("</Folder>");
      kml.append("</kml>");
      ZipOutputStream zipOut = new ZipOutputStream(out);
      logger.debug("Writing KML file to KMZ file");
      ZipEntry kmlEntry = new ZipEntry(layer.getDataset().getId() + "_" + layer.getId() + ".kml");
      kmlEntry.setTime(System.currentTimeMillis());
      zipOut.putNextEntry(kmlEntry);
      zipOut.write(kml.toString().getBytes());
      int frameIndex = 0;
      logger.debug("Writing frames to KMZ file");
      Iterator i$ = frames.iterator();

      while(i$.hasNext()) {
         BufferedImage frame = (BufferedImage)i$.next();
         ZipEntry picEntry = new ZipEntry(getPicFileName(frameIndex));
         ++frameIndex;
         zipOut.putNextEntry(picEntry);
         ImageIO.write(frame, "png", zipOut);
      }

      logger.debug("Constructing colour scale image");
      ZipEntry scaleEntry = new ZipEntry("legend.png");
      zipOut.putNextEntry(scaleEntry);
      logger.debug("Writing colour scale image to KMZ file");
      ImageIO.write(legend, "png", zipOut);
      zipOut.close();
   }

   private static final String getPicFileName(int frameIndex) {
      return "frame" + frameIndex + "." + "png";
   }

   public String getMimeType() {
      return "application/vnd.google-earth.kmz";
   }

   public boolean supportsMultipleFrames() {
      return true;
   }

   public boolean supportsFullyTransparentPixels() {
      return true;
   }

   public boolean supportsPartiallyTransparentPixels() {
      return true;
   }

   public boolean requiresLegend() {
      return true;
   }
}
