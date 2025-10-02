package org.dspace.app.mediafilter;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.InputStream;

import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

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
        BufferedImage buf = javax.imageio.ImageIO.read(source);

        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        float xmax = (float) configurationService.getIntProperty("webui.preview.maxwidth");
        float ymax = (float) configurationService.getIntProperty("webui.preview.maxheight");
        boolean blurring = configurationService.getBooleanProperty("webui.preview.blurring");
        boolean hqscaling = configurationService.getBooleanProperty("webui.preview.hqscaling");
        int brandHeight = configurationService.getIntProperty("webui.preview.brand.height");
        String brandFont = configurationService.getProperty("webui.preview.brand.font");
        int brandFontPoint = configurationService.getIntProperty("webui.preview.brand.fontpoint");

        return getThumbDim(currentItem, buf, verbose, xmax, ymax, blurring, hqscaling,
                brandHeight, brandFontPoint, brandFont);
    }

    // Temporary main method for local testing
    public static void main(String[] args) throws Exception {
        BrandedPreviewJPEGFilter filter = new BrandedPreviewJPEGFilter();
        InputStream in = new FileInputStream("test.jpg"); // make sure test.jpg exists
        Item item = null; // dummy item

        InputStream out = filter.getDestinationStream(item, in, true);
        System.out.println("Filter ran successfully: " + (out != null));
    }
}
