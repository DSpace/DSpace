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
import javax.imageio.ImageIO;

import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 *
 * @author Jason Sherman jsherman@usao.edu
 */
public class BrandedPreviewJPEGFilter extends MediaFilter {
    @Override
    public String getFilteredName(String oldFilename) {
        return oldFilename + ".preview.jpg";
    }

    /**
     * @return String bundle name
     */
    @Override
    public String getBundleName() {
        return "BRANDED_PREVIEW";
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
        return "Generated Branded Preview";
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
        // read in bitstream's image
        BufferedImage buf = ImageIO.read(source);

        // get config params
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        float xmax = (float) configurationService
            .getIntProperty("webui.preview.maxwidth");
        float ymax = (float) configurationService
            .getIntProperty("webui.preview.maxheight");
        boolean blurring = (boolean) configurationService
            .getBooleanProperty("webui.preview.blurring");
        boolean hqscaling = (boolean) configurationService
            .getBooleanProperty("webui.preview.hqscaling");
        int brandHeight = configurationService.getIntProperty("webui.preview.brand.height");
        String brandFont = configurationService.getProperty("webui.preview.brand.font");
        int brandFontPoint = configurationService.getIntProperty("webui.preview.brand.fontpoint");

        JPEGFilter jpegFilter = new JPEGFilter();
        return jpegFilter
            .getThumbDim(currentItem, buf, verbose, xmax, ymax, blurring, hqscaling, brandHeight, brandFontPoint,
                         brandFont);
    }
}
