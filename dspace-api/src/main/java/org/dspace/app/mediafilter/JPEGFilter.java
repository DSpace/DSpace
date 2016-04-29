/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.*;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

/**
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 *
 * @author Jason Sherman jsherman@usao.edu
 */
public class JPEGFilter extends MediaFilter implements SelfRegisterInputFormats
{
    @Override
    public String getFilteredName(String oldFilename)
    {
        return oldFilename + ".jpg";
    }

    /**
     * @return String bundle name
     *  
     */
    @Override
    public String getBundleName()
    {
        return "THUMBNAIL";
    }

    /**
     * @return String bitstreamformat
     */
    @Override
    public String getFormatString()
    {
        return "JPEG";
    }

    /**
     * @return String description
     */
    @Override
    public String getDescription()
    {
        return "Generated Thumbnail";
    }

    /**
     * @param currentItem item
     * @param source source input stream
     * @param verbose verbose mode
     * 
     * @return InputStream the resulting input stream
     * @throws Exception if error
     */
    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
            throws Exception
    {
        // read in bitstream's image
        BufferedImage buf = ImageIO.read(source);

        return getThumb(currentItem, buf, verbose);
    }

    public InputStream getThumb(Item currentItem, BufferedImage buf, boolean verbose)
            throws Exception
    {
        // get config params
        float xmax = (float) ConfigurationManager
                .getIntProperty("thumbnail.maxwidth");
        float ymax = (float) ConfigurationManager
                .getIntProperty("thumbnail.maxheight");
        boolean blurring = (boolean) ConfigurationManager
                .getBooleanProperty("thumbnail.blurring");
        boolean hqscaling = (boolean) ConfigurationManager
                .getBooleanProperty("thumbnail.hqscaling");

        return getThumbDim(currentItem, buf, verbose, xmax, ymax, blurring, hqscaling, 0, 0, null);
    }

    public InputStream getThumbDim(Item currentItem, BufferedImage buf, boolean verbose, float xmax, float ymax, boolean blurring, boolean hqscaling, int brandHeight, int brandFontPoint, String brandFont)
            throws Exception
    {
        // now get the image dimensions
        float xsize = (float) buf.getWidth(null);
        float ysize = (float) buf.getHeight(null);

        // if verbose flag is set, print out dimensions
        // to STDOUT
        if (verbose)
        {
            System.out.println("original size: " + xsize + "," + ysize);
        }

        // scale by x first if needed
        if (xsize > xmax)
        {
            // calculate scaling factor so that xsize * scale = new size (max)
            float scale_factor = xmax / xsize;

            // if verbose flag is set, print out extracted text
            // to STDOUT
            if (verbose)
            {
                System.out.println("x scale factor: " + scale_factor);
            }

            // now reduce x size
            // and y size
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;

            // if verbose flag is set, print out extracted text
            // to STDOUT
            if (verbose)
            {
                System.out.println("size after fitting to maximum width: " + xsize + "," + ysize);
            }
        }

        // scale by y if needed
        if (ysize > ymax)
        {
            float scale_factor = ymax / ysize;

            // now reduce x size
            // and y size
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;
        }

        // if verbose flag is set, print details to STDOUT
        if (verbose)
        {
            System.out.println("size after fitting to maximum height: " + xsize + ", "
                    + ysize);
        }

        // create an image buffer for the thumbnail with the new xsize, ysize
        BufferedImage thumbnail = new BufferedImage((int) xsize, (int) ysize,
                BufferedImage.TYPE_INT_RGB);

        // Use blurring if selected in config.
        // a little blur before scaling does wonders for keeping moire in check.
        if (blurring)
        {
                // send the buffered image off to get blurred.
                buf = getBlurredInstance((BufferedImage) buf);
        }

        // Use high quality scaling method if selected in config.
        // this has a definite performance penalty.
        if (hqscaling)
        {
                // send the buffered image off to get an HQ downscale.
                buf = getScaledInstance((BufferedImage) buf, (int) xsize, (int) ysize,
                        (Object) RenderingHints.VALUE_INTERPOLATION_BICUBIC, (boolean) true);
        }

        // now render the image into the thumbnail buffer
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.drawImage(buf, 0, 0, (int) xsize, (int) ysize, null);

        if (brandHeight != 0) {
            Brand brand = new Brand((int) xsize, brandHeight, new Font(brandFont, Font.PLAIN, brandFontPoint), 5);
            BufferedImage brandImage = brand.create(ConfigurationManager.getProperty("webui.preview.brand"),
                                                    ConfigurationManager.getProperty("webui.preview.brand.abbrev"),
                                                    currentItem == null ? "" : "hdl:" + currentItem.getHandle());

            g2d.drawImage(brandImage, (int)0, (int)ysize, (int) xsize, (int) 20, null);
        }

        // now create an input stream for the thumbnail buffer and return it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageIO.write(thumbnail, "jpeg", baos);

        // now get the array
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        return bais; // hope this gets written out before its garbage collected!
    }


    @Override
    public String[] getInputMIMETypes()
    {
        return ImageIO.getReaderMIMETypes();
    }

    @Override
    public String[] getInputDescriptions()
    {
        return null;
    }

    @Override
    public String[] getInputExtensions()
    {
        // Temporarily disabled as JDK 1.6 only
        // return ImageIO.getReaderFileSuffixes();
        return null;
    }

    public BufferedImage getNormalizedInstance(BufferedImage buf)
    {
     int type = (buf.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB_PRE;
     int w, h;
     w = buf.getWidth();
     h = buf.getHeight();
     BufferedImage normal = new BufferedImage(w, h, type);
     Graphics2D g2d = normal.createGraphics();
     g2d.drawImage(buf, 0, 0, w, h, Color.WHITE, null);
     g2d.dispose();
     return normal;
    }

    /**
     * Convenience method that returns a blurred instance of the
     * provided {@code BufferedImage}.
     *
     * @param buf buffered image
     * @return updated BufferedImage
     */
    public BufferedImage getBlurredInstance(BufferedImage buf)
    {
     buf = getNormalizedInstance(buf);

     // kernel for blur op
     float[] matrix = {
        0.111f, 0.111f, 0.111f,
        0.111f, 0.111f, 0.111f,
        0.111f, 0.111f, 0.111f,
      };

     // perform the blur and return the blurred version.
     BufferedImageOp blur = new ConvolveOp( new Kernel(3, 3, matrix) );
     BufferedImage blurbuf = blur.filter(buf, null);
     return blurbuf;
    }

    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param buf the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in downscaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public BufferedImage getScaledInstance(BufferedImage buf,
                                           int targetWidth,
                                           int targetHeight,
                                           Object hint,
                                           boolean higherQuality)
    {
        int type = (buf.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scalebuf = (BufferedImage)buf;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = buf.getWidth();
            h = buf.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2d = tmp.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2d.drawImage(scalebuf, 0, 0, w, h, Color.WHITE, null);
            g2d.dispose();

            scalebuf = tmp;
        } while (w != targetWidth || h != targetHeight);

        return scalebuf;
    }
}
