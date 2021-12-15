/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.license.CreativeCommonsServiceImpl;

public class Utils {

    // metadata used to enable the iiif features on the item
    public static final String METADATA_IIIF_ENABLED = "dspace.iiif.enabled";
    private static final String IIIF_WIDTH_METADATA = "iiif.image.width";
    // The DSpace bundle for other content related to item.
    protected static final String OTHER_CONTENT_BUNDLE = "OtherContent";

    private Utils() {}

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
            bundles = item.getBundles().stream().filter(Utils::isIIIFBundle).collect(Collectors.toList());
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
}
