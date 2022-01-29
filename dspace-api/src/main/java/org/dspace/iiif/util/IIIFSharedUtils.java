/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.license.CreativeCommonsServiceImpl;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Shared utilities for IIIF processing.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class IIIFSharedUtils {

    // metadata used to enable the iiif features on the item
    public static final String METADATA_IIIF_ENABLED = "dspace.iiif.enabled";
    // The DSpace bundle for other content related to item.
    protected static final String OTHER_CONTENT_BUNDLE = "OtherContent";
    // The IIIF image server url from configuration
    protected static final String IMAGE_SERVER_PATH = "iiif.image.server";
    // IIIF metadata definitions
    public static final String METADATA_IIIF_SCHEMA  = "iiif";
    public static final String METADATA_IIIF_IMAGE_ELEMENT = "image";
    public static final String METADATA_IIIF_TOC_ELEMENT = "toc";
    public static final String METADATA_IIIF_LABEL_ELEMENT = "label";
    public static final String METADATA_IIIF_HEIGHT_QUALIFIER = "height";
    public static final String METADATA_IIIF_WIDTH_QUALIFIER = "width";

    protected static final ConfigurationService configurationService
        = DSpaceServicesFactory.getInstance().getConfigurationService();


    private IIIFSharedUtils() {}

    public static boolean isIIIFItem(Item item) {
        return item.getMetadata().stream().filter(m -> m.getMetadataField().toString('.')
                                                 .contentEquals(METADATA_IIIF_ENABLED))
            .anyMatch(m -> m.getValue().equalsIgnoreCase("true") ||
                m.getValue().equalsIgnoreCase("yes"));
    }

    /**
     * This method returns the bundles holding IIIF resources if any.
     * If there is no IIIF content available an empty bundle list is returned.
     * @param item the DSpace item
     *
     * @return list of DSpace bundles with IIIF content
     */
    public static List<Bundle> getIIIFBundles(Item item) {
        boolean iiif = isIIIFEnabled(item);
        List<Bundle> bundles = new ArrayList<>();
        if (iiif) {
            bundles = item.getBundles().stream().filter(IIIFSharedUtils::isIIIFBundle).collect(Collectors.toList());
        }
        return bundles;
    }

    /**
     * This method verify if the IIIF feature is enabled on the item or parent collection.
     *
     * @param item the dspace item
     * @return true if the item supports IIIF
     */
    public static boolean isIIIFEnabled(Item item) {
        return item.getOwningCollection().getMetadata().stream()
                   .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                   .anyMatch(m -> m.getValue().equalsIgnoreCase("true") ||
                       m.getValue().equalsIgnoreCase("yes"))
            || item.getMetadata().stream()
                   .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                   .anyMatch(m -> m.getValue().equalsIgnoreCase("true")  ||
                       m.getValue().equalsIgnoreCase("yes"));
    }

    /**
     * Utility method to check is a bundle can contain bitstreams to use as IIIF
     * resources
     *
     * @param b the DSpace bundle to check
     * @return true if the bundle can contain bitstreams to use as IIIF resources
     */
    public static boolean isIIIFBundle(Bundle b) {
        return !StringUtils.equalsAnyIgnoreCase(b.getName(), Constants.LICENSE_BUNDLE_NAME,
            Constants.METADATA_BUNDLE_NAME, CreativeCommonsServiceImpl.CC_BUNDLE_NAME, "THUMBNAIL",
            "BRANDED_PREVIEW", "TEXT", OTHER_CONTENT_BUNDLE)
            && b.getMetadata().stream()
                .filter(m -> m.getMetadataField().toString('.').contentEquals(METADATA_IIIF_ENABLED))
                .noneMatch(m -> m.getValue().equalsIgnoreCase("false") || m.getValue().equalsIgnoreCase("no"));
    }

    /**
     * Returns url for retrieving info.json metadata from the image server.
     * @param bitstream
     * @return
     */
    public static String getInfoJsonPath(Bitstream bitstream) {
        String iiifImageServer = configurationService.getProperty(IMAGE_SERVER_PATH);
        return iiifImageServer + bitstream.getID() + "/info.json";
    }
}
