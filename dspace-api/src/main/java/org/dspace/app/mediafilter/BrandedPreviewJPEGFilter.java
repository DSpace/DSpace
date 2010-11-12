/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.dspace.core.ConfigurationManager;

/**
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 */
public class BrandedPreviewJPEGFilter extends MediaFilter
{
    public String getFilteredName(String oldFilename)
    {
        return oldFilename + ".preview.jpg";
    }

    /**
     * @return String bundle name
     *  
     */
    public String getBundleName()
    {
        return "BRANDED_PREVIEW";
    }

    /**
     * @return String bitstreamformat
     */
    public String getFormatString()
    {
        return "JPEG";
    }

    /**
     * @return String description
     */
    public String getDescription()
    {
        return "Generated Branded Preview";
    }

   
    /**
     * @param source
     *            source input stream
     * 
     * @return InputStream the resulting input stream
     */
    public InputStream getDestinationStream(InputStream source)
            throws Exception
    {
        // read in bitstream's image
        BufferedImage buf = ImageIO.read(source);

        // get config params
        float xmax = (float) ConfigurationManager
                .getIntProperty("webui.preview.maxwidth");
        float ymax = (float) ConfigurationManager
                .getIntProperty("webui.preview.maxheight");
        int brandHeight = ConfigurationManager.getIntProperty("webui.preview.brand.height");
        String brandFont = ConfigurationManager.getProperty("webui.preview.brand.font");
        int brandFontPoint = ConfigurationManager.getIntProperty("webui.preview.brand.fontpoint");
        
        // now get the image dimensions
        float xsize = (float) buf.getWidth(null);
        float ysize = (float) buf.getHeight(null);

        // if verbose flag is set, print out dimensions
        // to STDOUT
        if (MediaFilterManager.isVerbose)
        {
            System.out.println("original size: " + xsize + "," + ysize);
        }

        // scale by x first if needed
        if (xsize > xmax)
        {
            // calculate scaling factor so that xsize * scale = new size (max)
            float scaleFactor = xmax / xsize;

            // if verbose flag is set, print out extracted text
            // to STDOUT
            if (MediaFilterManager.isVerbose)
            {
                System.out.println("x scale factor: " + scaleFactor);
            }

            // now reduce x size
            // and y size
            xsize = xsize * scaleFactor;
            ysize = ysize * scaleFactor;

            // if verbose flag is set, print out extracted text
            // to STDOUT
            if (MediaFilterManager.isVerbose)
            {
                System.out.println("new size: " + xsize + "," + ysize);
            }
        }

        // scale by y if needed
        if (ysize > ymax)
        {
            float scaleFactor = ymax / ysize;

            // now reduce x size
            // and y size
            xsize = xsize * scaleFactor;
            ysize = ysize * scaleFactor;
        }

        // if verbose flag is set, print details to STDOUT
        if (MediaFilterManager.isVerbose)
        {
            System.out.println("created thumbnail size: " + xsize + ", "
                    + ysize);
        }

        // create an image buffer for the preview with the new xsize, ysize
        // we add
        BufferedImage branded = new BufferedImage((int) xsize, (int) ysize + brandHeight,
                BufferedImage.TYPE_INT_RGB);

        // now render the image into the preview buffer
        Graphics2D g2d = branded.createGraphics();
        g2d.drawImage(buf, 0, 0, (int) xsize, (int) ysize, null);
        
        Brand brand = new Brand((int) xsize, brandHeight, new Font(brandFont, Font.PLAIN, brandFontPoint), 5);
		BufferedImage brandImage = brand.create(ConfigurationManager.getProperty("webui.preview.brand"),
												ConfigurationManager.getProperty("webui.preview.brand.abbrev"),
												MediaFilterManager.getCurrentItem() == null ? "" : "hdl:" + MediaFilterManager.getCurrentItem().getHandle());
		
		g2d.drawImage(brandImage, (int)0, (int)ysize, (int) xsize, (int) 20, null);

        // now create an input stream for the thumbnail buffer and return it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageIO.write(branded, "jpeg", baos);

        // now get the array
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        return bais; // hope this gets written out before its garbage collected!
	}
}
