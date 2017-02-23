/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.awt.image.*;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.dspace.content.Item;

import org.dspace.app.mediafilter.JPEGFilter;

/**
 * Create JPEG thumbnails from PDF cover page using PDFBox.
 * Based on JPEGFilter:
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 *
 * @author Ivan Masár helix84@centrum.sk
 * @author Jason Sherman jsherman@usao.edu
 */
public class PDFBoxThumbnail extends MediaFilter implements SelfRegisterInputFormats
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
        PDDocument doc = PDDocument.load(source);
        PDFRenderer renderer = new PDFRenderer(doc);
        BufferedImage buf = renderer.renderImage(0);
//        ImageIO.write(buf, "PNG", new File("custom-render.png"));
        doc.close();

        JPEGFilter jpegFilter = new JPEGFilter();
        return jpegFilter.getThumb(currentItem, buf, verbose);
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
}
