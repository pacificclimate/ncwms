package uk.ac.rdg.resc.ncwms.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

public class ScreenshotController extends MultiActionController {
   private static final Logger log = LoggerFactory.getLogger(ScreenshotController.class);
   private static final Random RANDOM = new Random();
   private File screenshotCache;

   public void init() throws Exception {
      if (this.screenshotCache.exists()) {
         if (!this.screenshotCache.isDirectory()) {
            throw new Exception(this.screenshotCache.getPath() + " already exists but is not a directory");
         }

         log.debug("Screenshots directory already exists");
      } else {
         if (!this.screenshotCache.mkdirs()) {
            throw new Exception("Screenshots directory " + this.screenshotCache.getPath() + " could not be created");
         }

         log.debug("Screenshots directory " + this.screenshotCache.getPath() + " created");
      }

   }

   public ModelAndView createScreenshot(HttpServletRequest request, HttpServletResponse response) throws MetadataException {
      log.debug("Called createScreenshot");

      try {
         return this.createScreenshot(request);
      } catch (Exception var4) {
         log.error("Error creating screenshot", var4);
         throw new MetadataException(var4);
      }
   }

   private ModelAndView createScreenshot(HttpServletRequest request) throws Exception {
      String title = request.getParameter("title").replaceAll("&gt;", ">");
      String time = request.getParameter("time");
      String elevation = request.getParameter("elevation");
      String units = request.getParameter("units");
      String upperValue = request.getParameter("upperValue");
      String twoThirds = request.getParameter("twoThirds");
      String oneThird = request.getParameter("oneThird");
      String lowerValue = request.getParameter("lowerValue");
      boolean isLatLon = "true".equalsIgnoreCase(request.getParameter("latLon"));
      StringBuffer requestUrl = request.getRequestURL();
      String server = requestUrl.substring(0, requestUrl.indexOf("screenshots"));
      String BGparam = request.getParameter("urlBG");
      String FGparam = request.getParameter("urlFG");
      String urlStringPalette = request.getParameter("urlPalette");
      if (BGparam != null && FGparam != null && urlStringPalette != null) {
         String urlStringFG = server + FGparam;
         ScreenshotController.BoundingBox BBOX = new ScreenshotController.BoundingBox();
         String[] serverName = BGparam.split("\\?");
         StringBuffer result = buildURL(serverName[1], serverName[0], "BG", BBOX);
         serverName = urlStringFG.split("\\?");
         StringBuffer resultFG = buildURL(serverName[1], serverName[0], "FG", BBOX);
         float minX1 = 0.0F;
         float minX2 = 0.0F;
         float maxX1 = 0.0F;
         float maxX2 = 0.0F;
         int WIDTH_OF_BG_IMAGE1 = 0;
         int WIDTH_OF_BG_IMAGE2 = 0;
         int START_OF_IMAGE3 = 0;
         int START_OF_IMAGE4 = 0;
         int WIDTH_TOTAL = true;
         int HEIGHT_TOTAL = true;
         int WIDTH_OF_FINAL_IMAGE = true;
         int HEIGHT_OF_FINAL_IMAGE = true;
         String URL1 = "";
         String URL2 = "";
         float coverage = 0.0F;
         boolean isGT180 = false;
         boolean isReplicate = false;
         String bboxParam = "&BBOX=" + BBOX.minXValue + "," + BBOX.minYValue + "," + BBOX.maxXValue + "," + BBOX.maxYValue;
         String URL3;
         String bboxParam2;
         if (isLatLon && Float.compare(BBOX.minXValue, -180.0F) < 0) {
            if (Float.compare(BBOX.minXValue, -180.0F) < 0) {
               minX1 = -180.0F;
               if (Float.compare(BBOX.maxXValue, 180.0F) > 0) {
                  maxX1 = BBOX.maxXValue - 360.0F;
                  isReplicate = true;
               } else {
                  maxX1 = BBOX.maxXValue;
               }

               minX2 = BBOX.minXValue + 360.0F;
               maxX2 = 180.0F;
               float rangeofImg1 = Math.abs(maxX1 - minX1);
               float rangeofImg2 = Math.abs(maxX2 - minX2);
               float totalSpan = rangeofImg1 + rangeofImg2;
               if (isReplicate) {
                  coverage = rangeofImg1 / (totalSpan * 2.0F);
               } else {
                  coverage = rangeofImg1 / totalSpan;
               }

               WIDTH_OF_BG_IMAGE1 = Math.round(512.0F * coverage);
               if (isReplicate) {
                  WIDTH_OF_BG_IMAGE2 = 256 - WIDTH_OF_BG_IMAGE1;
                  START_OF_IMAGE3 = WIDTH_OF_BG_IMAGE1 + WIDTH_OF_BG_IMAGE2;
                  START_OF_IMAGE4 = START_OF_IMAGE3 + WIDTH_OF_BG_IMAGE2;
               } else {
                  WIDTH_OF_BG_IMAGE2 = 512 - WIDTH_OF_BG_IMAGE1;
               }
            }

            URL3 = "&BBOX=" + minX1 + "," + BBOX.minYValue + "," + maxX1 + "," + BBOX.maxYValue;
            bboxParam2 = "&BBOX=" + minX2 + "," + BBOX.minYValue + "," + maxX2 + "," + BBOX.maxYValue;
            URL1 = result.toString() + "WIDTH=" + WIDTH_OF_BG_IMAGE1 + "&HEIGHT=" + 400 + URL3;
            URL2 = result.toString() + "WIDTH=" + WIDTH_OF_BG_IMAGE2 + "&HEIGHT=" + 400 + bboxParam2;
            isGT180 = true;
         } else {
            URL1 = result.toString() + "WIDTH=" + 512 + "&HEIGHT=" + 400 + bboxParam;
         }

         URL3 = resultFG.toString() + "WIDTH=" + 512 + "&HEIGHT=" + 400 + bboxParam;
         bboxParam2 = null;
         BufferedImage bimgBG2 = null;
         BufferedImage bimgFG = null;
         BufferedImage bimgPalette = null;
         BufferedImage bimgBG1;
         if (isGT180) {
            bimgBG1 = downloadImage(URL1);
            bimgBG2 = downloadImage(URL2);
         } else {
            bimgBG1 = downloadImage(URL1);
         }

         bimgFG = downloadImage(URL3);
         bimgPalette = downloadImage(urlStringPalette);
         int type = 1;
         BufferedImage image = new BufferedImage(650, 480, type);
         Graphics2D g = image.createGraphics();
         Font font = new Font("SansSerif", 1, 12);
         g.setFont(font);
         g.setBackground(Color.white);
         g.fillRect(0, 0, 650, 480);
         g.setColor(Color.black);
         g.drawString(title, 0, 10);
         if (time != null) {
            g.drawString("Time: " + time, 0, 30);
         }

         if (elevation != null) {
            g.drawString(elevation, 0, 50);
         }

         if (isGT180) {
            g.drawImage(bimgBG1, (BufferedImageOp)null, WIDTH_OF_BG_IMAGE2, 60);
            g.drawImage(bimgBG2, (BufferedImageOp)null, 0, 60);
            if (isReplicate) {
               g.drawImage(bimgBG2, (BufferedImageOp)null, START_OF_IMAGE3, 60);
               g.drawImage(bimgBG1, (BufferedImageOp)null, START_OF_IMAGE4, 60);
            }
         } else {
            g.drawImage(bimgBG1, (BufferedImageOp)null, 0, 60);
         }

         g.drawImage(bimgFG, (BufferedImageOp)null, 0, 60);
         g.drawImage(bimgPalette, 512, 60, 45, 400, (ImageObserver)null);
         g.drawString(upperValue, 560, 63);
         g.drawString(twoThirds, 560, 192);
         if (units != null) {
            g.drawString("Units: " + units, 560, 258);
         }

         g.drawString(oneThird, 560, 325);
         g.drawString(lowerValue, 560, 460);
         g.dispose();
         String imageName = "snapshot" + RANDOM.nextLong() + System.currentTimeMillis() + ".png";
         ImageIO.write(image, "png", this.getImageFile(imageName));
         String screenshotUrl = "screenshots/getScreenshot?img=" + imageName;
         return new ModelAndView("showScreenshotUrl", "url", screenshotUrl);
      } else {
         throw new Exception("Null BG, FG or palette param");
      }
   }

   private static StringBuffer buildURL(String url, String serverName, String type, ScreenshotController.BoundingBox bb) {
      String[] params = url.split("&");
      StringBuffer result = new StringBuffer();
      result.append(serverName);
      result.append("?");
      String separator = "&";

      for(int i = 0; i < params.length; ++i) {
         if (params[i].startsWith("BBOX")) {
            String tempParam = params[i];
            String bbValues = tempParam.substring(5);
            String[] bbox = bbValues.split(",");
            if (type.equals("BG")) {
               bb.minXValue = (float)Double.parseDouble(bbox[0]);
               bb.maxXValue = (float)Double.parseDouble(bbox[2]);
               bb.minYValue = (float)Double.parseDouble(bbox[1]);
               bb.maxYValue = (float)Double.parseDouble(bbox[3]);
            }

            for(int indx = 0; indx < bbox.length; ++indx) {
            }
         } else if (!params[i].startsWith("WIDTH") && !params[i].startsWith("HEIGHT")) {
            result.append(params[i]);
            result.append(separator);
         }
      }

      return result;
   }

   private static BufferedImage downloadImage(String path) throws IOException {
      return ImageIO.read(new URL(path));
   }

   public void getScreenshot(HttpServletRequest request, HttpServletResponse response) throws Exception {
      log.debug("Called getScreenshot with params {}", request.getParameterMap());
      String imageName = request.getParameter("img");
      if (imageName == null) {
         throw new Exception("Must give a screenshot image name");
      } else {
         File screenshotFile = this.getImageFile(imageName);
         InputStream in = null;
         ServletOutputStream out = null;

         try {
            in = new FileInputStream(screenshotFile);
            byte[] imageBytes = new byte[1024];
            response.setContentType("image/png");
            out = response.getOutputStream();

            int n;
            do {
               n = in.read(imageBytes);
               if (n >= 0) {
                  out.write(imageBytes);
               }
            } while(n >= 0);
         } catch (FileNotFoundException var12) {
            throw new Exception(imageName + " not found");
         } finally {
            if (in != null) {
               in.close();
            }

            if (out != null) {
               out.close();
            }

         }

      }
   }

   private File getImageFile(String imageName) {
      return new File(this.screenshotCache, imageName);
   }

   public void setScreenshotCache(File screenshotCache) {
      this.screenshotCache = screenshotCache;
   }

   private static final class BoundingBox {
      float minXValue;
      float maxXValue;
      float minYValue;
      float maxYValue;

      private BoundingBox() {
      }

      // $FF: synthetic method
      BoundingBox(Object x0) {
         this();
      }
   }
}
