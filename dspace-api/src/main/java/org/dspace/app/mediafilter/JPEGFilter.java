/*
 * JPEGFilter.java
 *
 * Version: $Revision: 3733 $
 *
 * Date: $Date: 2009-04-23 23:52:11 -0400 (Thu, 23 Apr 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.mediafilter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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
public class JPEGFilter extends MediaFilter
{
    public String getFilteredName(String oldFilename)
    {
        return oldFilename + ".jpg";
    }

    /**
     * @return String bundle name
     *  
     */
    public String getBundleName()
    {
        return "THUMBNAIL";
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
        return "Generated Thumbnail";
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
                .getIntProperty("thumbnail.maxwidth");
        float ymax = (float) ConfigurationManager
                .getIntProperty("thumbnail.maxheight");

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
            float scale_factor = xmax / xsize;

            // if verbose flag is set, print out extracted text
            // to STDOUT
            if (MediaFilterManager.isVerbose)
            {
                System.out.println("x scale factor: " + scale_factor);
            }

            // now reduce x size
            // and y size
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;

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
            float scale_factor = ymax / ysize;

            // now reduce x size
            // and y size
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;
        }

        // if verbose flag is set, print details to STDOUT
        if (MediaFilterManager.isVerbose)
        {
            System.out.println("created thumbnail size: " + xsize + ", "
                    + ysize);
        }

        // create an image buffer for the thumbnail with the new xsize, ysize
        BufferedImage thumbnail = new BufferedImage((int) xsize, (int) ysize,
                BufferedImage.TYPE_INT_RGB);

        // now render the image into the thumbnail buffer
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.drawImage(buf, 0, 0, (int) xsize, (int) ysize, null);

        // now create an input stream for the thumbnail buffer and return it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageIO.write(thumbnail, "jpeg", baos);

        // now get the array
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        return bais; // hope this gets written out before its garbage collected!
    }
}
