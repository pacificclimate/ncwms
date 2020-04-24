package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.util.Range;

public class ColorPalette {
   private static final Logger logger = LoggerFactory.getLogger(ColorPalette.class);
   public static final int MAX_NUM_COLOURS = 254;
   private static final Map<String, ColorPalette> palettes = new HashMap();
   public static final String DEFAULT_PALETTE_NAME = "rainbow";
   public static final int LEGEND_WIDTH = 110;
   public static final int LEGEND_HEIGHT = 264;
   private static final ColorPalette DEFAULT_PALETTE = new ColorPalette("rainbow", new Color[]{new Color(0, 0, 143), new Color(0, 0, 159), new Color(0, 0, 175), new Color(0, 0, 191), new Color(0, 0, 207), new Color(0, 0, 223), new Color(0, 0, 239), new Color(0, 0, 255), new Color(0, 11, 255), new Color(0, 27, 255), new Color(0, 43, 255), new Color(0, 59, 255), new Color(0, 75, 255), new Color(0, 91, 255), new Color(0, 107, 255), new Color(0, 123, 255), new Color(0, 139, 255), new Color(0, 155, 255), new Color(0, 171, 255), new Color(0, 187, 255), new Color(0, 203, 255), new Color(0, 219, 255), new Color(0, 235, 255), new Color(0, 251, 255), new Color(7, 255, 247), new Color(23, 255, 231), new Color(39, 255, 215), new Color(55, 255, 199), new Color(71, 255, 183), new Color(87, 255, 167), new Color(103, 255, 151), new Color(119, 255, 135), new Color(135, 255, 119), new Color(151, 255, 103), new Color(167, 255, 87), new Color(183, 255, 71), new Color(199, 255, 55), new Color(215, 255, 39), new Color(231, 255, 23), new Color(247, 255, 7), new Color(255, 247, 0), new Color(255, 231, 0), new Color(255, 215, 0), new Color(255, 199, 0), new Color(255, 183, 0), new Color(255, 167, 0), new Color(255, 151, 0), new Color(255, 135, 0), new Color(255, 119, 0), new Color(255, 103, 0), new Color(255, 87, 0), new Color(255, 71, 0), new Color(255, 55, 0), new Color(255, 39, 0), new Color(255, 23, 0), new Color(255, 7, 0), new Color(246, 0, 0), new Color(228, 0, 0), new Color(211, 0, 0), new Color(193, 0, 0), new Color(175, 0, 0), new Color(158, 0, 0), new Color(140, 0, 0)});
   private final Color[] palette;
   private final String name;

   private ColorPalette(String name, Color[] palette) {
      this.name = name;
      this.palette = palette;
   }

   public int getSize() {
      return this.palette.length;
   }

   public static final Set<String> getAvailablePaletteNames() {
      return palettes.keySet();
   }

   public static final void loadPalettes(File paletteLocationDir) {
      File[] arr$ = paletteLocationDir.listFiles();
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         File file = arr$[i$];
         if (file.getName().endsWith(".pal")) {
            try {
               String paletteName = file.getName().substring(0, file.getName().lastIndexOf("."));
               ColorPalette palette = new ColorPalette(paletteName, readColorPalette(new FileReader(file)));
               logger.debug("Read palette with name {}", paletteName);
               palettes.put(palette.getName(), palette);
            } catch (Exception var7) {
               logger.error("Error reading from palette file {}", file.getName(), var7);
            }
         }
      }

   }

   public static ColorPalette get(String name) {
      if (name != null && !name.trim().equals("")) {
         ColorPalette ret = (ColorPalette)palettes.get(name.trim().toLowerCase());
         return ret != null ? ret : (ColorPalette)palettes.get("rainbow");
      } else {
         return (ColorPalette)palettes.get("rainbow");
      }
   }

   public String getName() {
      return this.name;
   }

   public BufferedImage createColorBar(int width, int height, int numColorBands) {
      double colorBandWidth = (double)height / (double)numColorBands;
      Color[] newPalette = this.getPalette(numColorBands);
      BufferedImage colorBar = new BufferedImage(width, height, 1);
      Graphics2D gfx = colorBar.createGraphics();

      for(int i = 0; i < height; ++i) {
         int colorIndex = (int)((double)i / colorBandWidth);
         gfx.setColor(newPalette[numColorBands - colorIndex - 1]);
         gfx.drawLine(0, i, width - 1, i);
      }

      return colorBar;
   }

   public BufferedImage createLegend(int numColorBands, String title, String units, boolean logarithmic, Range<Float> colorScaleRange) {
      float colourScaleMin = (Float)colorScaleRange.getMinimum();
      float colourScaleMax = (Float)colorScaleRange.getMaximum();
      BufferedImage colourScale = new BufferedImage(110, 264, 5);
      Graphics2D gfx = colourScale.createGraphics();
      BufferedImage colorBar = this.createColorBar(24, 254, numColorBands);
      gfx.drawImage(colorBar, (BufferedImageOp)null, 2, 5);
      gfx.setColor(Color.WHITE);
      double min = logarithmic ? Math.log((double)colourScaleMin) : (double)colourScaleMin;
      double max = logarithmic ? Math.log((double)colourScaleMax) : (double)colourScaleMax;
      double quarter = 0.25D * (max - min);
      double scaleQuarter = logarithmic ? Math.exp(min + quarter) : min + quarter;
      double scaleMid = logarithmic ? Math.exp(min + 2.0D * quarter) : min + 2.0D * quarter;
      double scaleThreeQuarter = logarithmic ? Math.exp(min + 3.0D * quarter) : min + 3.0D * quarter;
      gfx.drawString(format((double)colourScaleMax), 27, 10);
      gfx.drawString(format(scaleThreeQuarter), 27, 73);
      gfx.drawString(format(scaleMid), 27, 137);
      gfx.drawString(format(scaleQuarter), 27, 201);
      gfx.drawString(format((double)colourScaleMin), 27, 264);
      if (units != null && !units.trim().equals("")) {
         title = title + " (" + units + ")";
      }

      AffineTransform trans = new AffineTransform();
      trans.setToTranslation(90.0D, 0.0D);
      AffineTransform rot = new AffineTransform();
      rot.setToRotation(1.5707963267948966D);
      trans.concatenate(rot);
      gfx.setTransform(trans);
      gfx.drawString(title, 5, 0);
      return colourScale;
   }

   private static String format(double d) {
      if (d == 0.0D) {
         return "0";
      } else {
         return Math.abs(d) <= 1000.0D && Math.abs(d) >= 0.01D ? (new DecimalFormat("0.#####")).format(d) : (new DecimalFormat("0.###E0")).format(d);
      }
   }

   public IndexColorModel getColorModel(int numColorBands, int opacity, Color bgColor, boolean transparent) {
      Color[] newPalette = this.getPalette(numColorBands);
      int alpha;
      if (opacity >= 100) {
         alpha = 255;
      } else if (opacity <= 0) {
         alpha = 0;
      } else {
         alpha = (int)(2.55D * (double)opacity);
      }

      byte[] r = new byte[numColorBands + 2];
      byte[] g = new byte[numColorBands + 2];
      byte[] b = new byte[numColorBands + 2];
      byte[] a = new byte[numColorBands + 2];

      for(int i = 0; i < numColorBands; ++i) {
         r[i] = (byte)newPalette[i].getRed();
         g[i] = (byte)newPalette[i].getGreen();
         b[i] = (byte)newPalette[i].getBlue();
         a[i] = (byte)alpha;
      }

      r[numColorBands] = (byte)bgColor.getRed();
      g[numColorBands] = (byte)bgColor.getGreen();
      b[numColorBands] = (byte)bgColor.getBlue();
      a[numColorBands] = transparent ? 0 : (byte)alpha;
      r[numColorBands + 1] = 0;
      g[numColorBands + 1] = 0;
      b[numColorBands + 1] = 0;
      a[numColorBands + 1] = (byte)alpha;
      return new IndexColorModel(8, r.length, r, g, b, a);
   }

   private Color[] getPalette(int numColorBands) {
      if (numColorBands >= 1 && numColorBands <= 254) {
         Color[] targetPalette;
         if (numColorBands == this.palette.length) {
            targetPalette = this.palette;
         } else {
            targetPalette = new Color[numColorBands];
            targetPalette[0] = this.palette[0];
            targetPalette[targetPalette.length - 1] = this.palette[this.palette.length - 1];
            int lastIndex;
            int j;
            if (targetPalette.length < this.palette.length) {
               for(lastIndex = 1; lastIndex < targetPalette.length - 1; ++lastIndex) {
                  j = Math.round((float)(this.palette.length * lastIndex) * 1.0F / (float)(targetPalette.length - 1));
                  targetPalette[lastIndex] = this.palette[j];
               }
            } else {
               lastIndex = 0;

               for(j = 1; j < this.palette.length - 1; ++j) {
                  int nearestIndex = Math.round((float)(targetPalette.length * j) * 1.0F / (float)(this.palette.length - 1));
                  targetPalette[nearestIndex] = this.palette[j];

                  for(int j = lastIndex + 1; j < nearestIndex; ++j) {
                     float fracFromThis = (1.0F * (float)j - (float)lastIndex) / (float)(nearestIndex - lastIndex);
                     targetPalette[j] = interpolate(targetPalette[nearestIndex], targetPalette[lastIndex], fracFromThis);
                  }

                  lastIndex = nearestIndex;
               }

               for(j = lastIndex + 1; j < targetPalette.length - 1; ++j) {
                  float fracFromThis = (1.0F * (float)j - (float)lastIndex) / (float)(targetPalette.length - lastIndex);
                  targetPalette[j] = interpolate(targetPalette[targetPalette.length - 1], targetPalette[lastIndex], fracFromThis);
               }
            }
         }

         return targetPalette;
      } else {
         throw new IllegalArgumentException("numColorBands must be between 1 and 254");
      }
   }

   private static Color interpolate(Color c1, Color c2, float fracFromC1) {
      float fracFromC2 = 1.0F - fracFromC1;
      return new Color(Math.round(fracFromC1 * (float)c1.getRed() + fracFromC2 * (float)c2.getRed()), Math.round(fracFromC1 * (float)c1.getGreen() + fracFromC2 * (float)c2.getGreen()), Math.round(fracFromC1 * (float)c1.getBlue() + fracFromC2 * (float)c2.getBlue()));
   }

   private static Color[] readColorPalette(Reader paletteReader) throws Exception {
      BufferedReader reader = new BufferedReader(paletteReader);
      ArrayList colours = new ArrayList();

      String line;
      try {
         while((line = reader.readLine()) != null) {
            if (!line.startsWith("#") && !line.trim().equals("")) {
               StringTokenizer tok = new StringTokenizer(line.trim());

               try {
                  if (tok.countTokens() < 3) {
                     throw new Exception();
                  }

                  Float r = Float.valueOf(tok.nextToken());
                  Float g = Float.valueOf(tok.nextToken());
                  Float b = Float.valueOf(tok.nextToken());
                  if (r >= 0.0F && g >= 0.0F && b >= 0.0F) {
                     if (r <= 1.0F && g <= 1.0F && b <= 1.0F) {
                        colours.add(new Color(r, g, b));
                        continue;
                     }

                     if (r <= 255.0F && g <= 255.0F && b <= 255.0F) {
                        colours.add(new Color(r.intValue(), g.intValue(), b.intValue()));
                        continue;
                     }

                     throw new Exception();
                  }

                  throw new Exception();
               } catch (Exception var11) {
                  throw new Exception("File format error: each line must contain three numbers between 0 and 255 or 0.0 and 1.0 (R, G, B)");
               }
            }
         }
      } finally {
         if (reader != null) {
            reader.close();
         }

      }

      return (Color[])colours.toArray(new Color[0]);
   }

   public static void addPalette(String name, Reader reader) {
      try {
         ColorPalette palette = new ColorPalette(name, readColorPalette(reader));
         if (!palettes.containsKey(palette.getName())) {
            palettes.put(palette.getName(), palette);
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   static {
      palettes.put("rainbow", DEFAULT_PALETTE);
   }
}
