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
 * BrandedPreviewJPEGFilter generates JPEG previews with branding. Extends
 * JPEGFilter to reuse thumbnail generation methods.
 */
public class BrandedPreviewJPEGFilter extends JPEGFilter {

    @Override
    public String getFilteredName(String oldFilename) {
        return oldFilename + ".preview.jpg";
    }

    @Override
    public String getBundleName() {
        return "BRANDED_PREVIEW";
    }

    @Override
    public String getDescription() {
        return "Generated Branded Preview";
    }

    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
            throws Exception {

        // Read the source image
        BufferedImage buf = ImageIO.read(source);

        // Get configuration parameters
        ConfigurationService configurationService = DSpaceServicesFactory
                .getInstance()
                .getConfigurationService();

        float xmax = (float) configurationService.getIntProperty("webui.preview.maxwidth");
        float ymax = (float) configurationService.getIntProperty("webui.preview.maxheight");
        boolean blurring = configurationService.getBooleanProperty("webui.preview.blurring");
        boolean hqscaling = configurationService.getBooleanProperty("webui.preview.hqscaling");
        int brandHeight = configurationService.getIntProperty("webui.preview.brand.height");
        String brandFont = configurationService.getProperty("webui.preview.brand.font");
        int brandFontPoint = configurationService.getIntProperty("webui.preview.brand.fontpoint");

        // Use JPEGFilter's method directly
        return getThumbDim(currentItem, buf, verbose, xmax, ymax, blurring, hqscaling,
                brandHeight, brandFontPoint, brandFont);
    }
}
