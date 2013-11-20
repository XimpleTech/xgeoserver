package org.geoserver.map.png;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

public class ImageAssert {
    
    static boolean clibAvailable;
    
    public static void assertImagesEqual(BufferedImage original, BufferedImage image) {
        assertEquals(original.getWidth(), image.getWidth());
        assertEquals(original.getHeight(), image.getHeight());
        // we cannot match in output the image types generated by the CLIB reader, they are not
        // really matching the PNG data model
        if(!isClibAvailable()) {
            assertEquals(original.getSampleModel(), image.getSampleModel());
            assertEquals(original.getColorModel(), image.getColorModel());
        }
    
        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                int rgbOriginal = original.getRGB(x, y);
                int rgbActual = image.getRGB(x, y);
                if(rgbOriginal != rgbActual) {
                    fail("Comparison failed at x:" + x + ", y: " + y + ", expected " + colorToString(rgbOriginal) + ", got " 
                        + colorToString(rgbActual));
                }
            }
        }
    }

    private static String colorToString(int rgb) {
        Color c = new Color(rgb);
        return "RGBA[" + c.getRed() + ", " + c.getGreen() + ", " + c.getBlue() + ", " + c.getAlpha() + "]"; 
    }

    static boolean isClibAvailable() {
        String firstWriterName = ImageIO.getImageWritersByFormatName("PNG").next().getClass().getSimpleName();
        return "CLibPNGImageWriter".equals(firstWriterName);
    }

    public static void showImage(String title, long timeOut, final BufferedImage image)
            throws InterruptedException {
        final String headless = System.getProperty("java.awt.headless", "false");
        if (!headless.equalsIgnoreCase("true")) {
            try {
                Frame frame = new Frame(title);
                frame.addWindowListener(new WindowAdapter() {

                    public void windowClosing(WindowEvent e) {
                        e.getWindow().dispose();
                    }
                });

                Panel p = new Panel() {

                    /** <code>serialVersionUID</code> field */
                    private static final long serialVersionUID = 1L;

                    {
                        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                    }

                    public void paint(Graphics g) {
                        g.drawImage(image, 0, 0, this);
                    }

                };

                frame.add(p);
                frame.pack();
                frame.setVisible(true);

                Thread.sleep(timeOut);
                frame.dispose();
            } catch (HeadlessException exception) {
                // The test is running on a machine without X11 display. Ignore.
            }
        }
    }
    
}
