/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.dspace.content.Item;

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
public class PDFBoxThumbnail extends MediaFilter {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(PDFBoxThumbnail.class);

    @Override
    public String getFilteredName(String oldFilename) {
        return oldFilename + ".jpg";
    }

    /**
     * @return String bundle name
     */
    @Override
    public String getBundleName() {
        return "THUMBNAIL";
    }

    /**
     * @return String bitstreamformat
     */
    @Override
    public String getFormatString() {
        return "JPEG";
    }

    /**
     * @return String description
     */
    @Override
    public String getDescription() {
        return "Generated Thumbnail";
    }

    /**
     * @param currentItem item
     * @param source      source input stream
     * @param verbose     verbose mode
     * @return InputStream the resulting input stream
     * @throws Exception if error
     */
    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
        throws Exception {
        BufferedImage buf;

        // Render the page image.
        try ( PDDocument doc = PDDocument.load(source); ) {
            PDFRenderer renderer = new PDFRenderer(doc);
            buf = renderer.renderImage(0);
        } catch (InvalidPasswordException ex) {
            log.error("PDF is encrypted. Cannot create thumbnail (item: {})", currentItem::getHandle);
            return null;
        }

        // Generate thumbnail derivative and return as IO stream.
        JPEGFilter jpegFilter = new JPEGFilter();
        return jpegFilter.getThumb(currentItem, buf, verbose);
    }
}
