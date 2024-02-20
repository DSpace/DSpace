/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreport;

import static org.dspace.content.Item.ANY;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Utility methods for applying some of the filters defined in the {@link Filter} enum.
 *
 * @author Jean-François Morin (Université Laval) (port to DSpace 7.x)
 * @author Terry Brady, Georgetown University (original code in DSpace 6.x)
 */
public class ItemFilterUtil {

    protected static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final Logger log = LogManager.getLogger(ItemFilterUtil.class);
    public static final String[] MIMES_PDF = {"application/pdf"};
    public static final String[] MIMES_JPG = {"image/jpeg"};

    /**
     * Supported bundle types.
     * N.B.: Bundle names are used in metadata as they are named here.
     * Do NOT change these names, the name() method is invoked at multiple
     * locations in this class and enum Filter.
     * If these names are to change, the name() invocations shall be changed
     * so that they refer to these unchanged names, likely through a String property.
     */
    enum BundleName {
        ORIGINAL, TEXT, LICENSE, THUMBNAIL;
    }

    private ItemFilterUtil() {}

    static String[] getDocumentMimeTypes() {
        return DSpaceServicesFactory.getInstance().getConfigurationService()
                .getArrayProperty("rest.report-mime-document");
    }

    static String[] getSupportedDocumentMimeTypes() {
        return DSpaceServicesFactory.getInstance().getConfigurationService()
                .getArrayProperty("rest.report-mime-document-supported");
    }

    static String[] getSupportedImageMimeTypes() {
        return DSpaceServicesFactory.getInstance().getConfigurationService()
                .getArrayProperty("rest.report-mime-document-image");
    }

    /**
     * Counts the original bitstreams of a given item.
     * @param item Provided item
     * @return the number of original bitstreams in the item
     */
    static int countOriginalBitstream(Item item) {
        return countBitstream(BundleName.ORIGINAL, item);
    }

    /**
     * Counts the bitstreams of a given item for a specific type.
     * @param bundleName Type of bundle to filter bitstreams
     * @param item Provided item
     * @return the number of matching bitstreams in the item
     */
    static int countBitstream(BundleName bundleName, Item item) {
        return item.getBundles().stream()
                .filter(bundle -> bundle.getName().equals(bundleName.name()))
                .mapToInt(bundle -> bundle.getBitstreams().size())
                .sum();
    }

    /**
     * Retrieves the bitstream names of an given item for a specific bundle type.
     * @param bundleName Type of bundle to filter bitstreams
     * @param item Provided item
     * @return the names of matching bitstreams in the item
     */
    static List<String> getBitstreamNames(BundleName bundleName, Item item) {
        return item.getBundles().stream()
                .filter(bundle -> bundle.getName().equals(bundleName.name()))
                .map(Bundle::getBitstreams)
                .flatMap(List::stream)
                .map(Bitstream::getName)
                .collect(Collectors.toList());
    }

    /**
     * Counts the original bitstreams of a given item matching one of a list of specific MIME types.
     * @param context DSpace context
     * @param item Provided item
     * @param mimeList List of MIME types to filter bitstreams
     * @return number of matching original bitstreams
     */
    static int countOriginalBitstreamMime(Context context, Item item, String[] mimeList) {
        return countBitstreamMime(context, BundleName.ORIGINAL, item, mimeList);
    }

    /**
     * Counts the bitstreams of a given item for a specific type matching one of a list of specific MIME types.
     * @param context DSpace context
     * @param bundleName Type of bundle to filter bitstreams
     * @param item Provided item
     * @param mimeList List of MIME types to filter bitstreams
     * @return number of matching bitstreams
     */
    static int countBitstreamMime(Context context, BundleName bundleName, Item item, String[] mimeList) {
        return item.getBundles().stream()
                .filter(bundle -> bundle.getName().equals(bundleName.name()))
                .map(Bundle::getBitstreams)
                .flatMap(List::stream)
                .mapToInt(bit -> {
                    int count = 0;
                    for (String mime : mimeList) {
                        try {
                            if (bit.getFormat(context).getMIMEType().equals(mime.trim())) {
                                count++;
                            }
                        } catch (SQLException e) {
                            log.error("Get format error for bitstream " + bit.getName());
                        }
                    }
                    return count;
                })
                .sum();
    }

    /**
     * Counts the bitstreams of a given item for a specific type matching one of a list of specific descriptions.
     * @param bundleName Type of bundle to filter bitstreams
     * @param item Provided item
     * @param descList List of descriptions to filter bitstreams
     * @return number of matching bitstreams
     */
    static int countBitstreamByDesc(BundleName bundleName, Item item, String[] descList) {
        return item.getBundles().stream()
                .filter(bundle -> bundle.getName().equals(bundleName.name()))
                .map(Bundle::getBitstreams)
                .flatMap(List::stream)
                .filter(bit -> bit.getDescription() != null)
                .mapToInt(bit -> {
                    int count = 0;
                    for (String desc : descList) {
                        String bitDesc = bit.getDescription();
                        if (bitDesc.equals(desc.trim())) {
                            count++;
                        }
                    }
                    return count;
                })
                .sum();
    }

    /**
     * Counts the bitstreams of a given item smaller than a given size for a specific type
     * matching one of a list of specific MIME types.
     * @param context DSpace context
     * @param bundleName Type of bundle to filter bitstreams
     * @param item Provided item
     * @param mimeList List of MIME types to filter bitstreams
     * @param prop Configurable property providing the size to filter bitstreams
     * @return number of matching bitstreams
     */
    static int countBitstreamSmallerThanMinSize(
            Context context, BundleName bundleName, Item item, String[] mimeList, String prop) {
        long size = DSpaceServicesFactory.getInstance().getConfigurationService().getLongProperty(prop);
        return item.getBundles().stream()
                .filter(bundle -> bundle.getName().equals(bundleName.name()))
                .map(Bundle::getBitstreams)
                .flatMap(List::stream)
                .mapToInt(bit -> {
                    int count = 0;
                    for (String mime : mimeList) {
                        try {
                            if (bit.getFormat(context).getMIMEType().equals(mime.trim())) {
                                if (bit.getSizeBytes() < size) {
                                    count++;
                                }
                            }
                        } catch (SQLException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                    return count;
                })
                .sum();
    }

    /**
     * Counts the bitstreams of a given item larger than a given size for a specific type
     * matching one of a list of specific MIME types.
     * @param context DSpace context
     * @param bundleName Type of bundle to filter bitstreams
     * @param item Provided item
     * @param mimeList List of MIME types to filter bitstreams
     * @param prop Configurable property providing the size to filter bitstreams
     * @return number of matching bitstreams
     */
    static int countBitstreamLargerThanMaxSize(
            Context context, BundleName bundleName, Item item, String[] mimeList, String prop) {
        long size = DSpaceServicesFactory.getInstance().getConfigurationService().getLongProperty(prop);
        return item.getBundles().stream()
                .filter(bundle -> bundle.getName().equals(bundleName.name()))
                .map(Bundle::getBitstreams)
                .flatMap(List::stream)
                .mapToInt(bit -> {
                    int count = 0;
                    for (String mime : mimeList) {
                        try {
                            if (bit.getFormat(context).getMIMEType().equals(mime.trim())) {
                                if (bit.getSizeBytes() > size) {
                                    count++;
                                }
                            }
                        } catch (SQLException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                    return count;
                })
                .sum();
    }

    /**
     * Counts the original bitstreams of a given item whose MIME type starts with a specific prefix.
     * @param context DSpace context
     * @param item Provided item
     * @param prefix Prefix to filter bitstreams
     * @return number of matching original bitstreams
     */
    static int countOriginalBitstreamMimeStartsWith(Context context, Item item, String prefix) {
        return countBitstreamMimeStartsWith(context, BundleName.ORIGINAL, item, prefix);
    }

    /**
     * Counts the bitstreams of a given item for a specific type whose MIME type starts with a specific prefix.
     * @param context DSpace context
     * @param bundleName Type of bundle to filter bitstreams
     * @param item Provided item
     * @param prefix Prefix to filter bitstreams
     * @return number of matching bitstreams
     */
    static int countBitstreamMimeStartsWith(Context context, BundleName bundleName, Item item, String prefix) {
        return item.getBundles().stream()
                .filter(bundle -> bundle.getName().equals(bundleName.name()))
                .map(Bundle::getBitstreams)
                .flatMap(List::stream)
                .mapToInt(bit -> {
                    int count = 0;
                    try {
                        if (bit.getFormat(context).getMIMEType().startsWith(prefix)) {
                            count++;
                        }
                    } catch (SQLException e) {
                        log.error(e.getMessage(), e);
                    }
                    return count;
                })
                .sum();
    }

    /**
     * Returns true if a given item has a bundle not matching a specific list of bundles.
     * @param item Provided item
     * @param bundleList List of bundle names to filter bundles
     * @return true if the item has a (non-)matching bundle
     */
    static boolean hasUnsupportedBundle(Item item, String[] bundleList) {
        if (bundleList == null) {
            return false;
        }
        Set<String> bundles = Arrays.stream(bundleList)
                .collect(Collectors.toSet());
        return item.getBundles().stream()
                .anyMatch(bundle -> !bundles.contains(bundle.getName()));
    }

    static boolean hasOriginalBitstreamMime(Context context, Item item, String[] mimeList) {
        return hasBitstreamMime(context, BundleName.ORIGINAL, item, mimeList);
    }

    static boolean hasBitstreamMime(Context context, BundleName bundleName, Item item, String[] mimeList) {
        return countBitstreamMime(context, bundleName, item, mimeList) > 0;
    }

    /**
     * Returns true if a given item has at least one field of a specific list whose value
     * matches a provided regular expression.
     * @param item Provided item
     * @param fieldList List of fields to check
     * @param regex Regular expression to check field values against
     * @return true if there is at least one matching field, false otherwise
     */
    static boolean hasMetadataMatch(Item item, String fieldList, Pattern regex) {
        if ("*".equals(fieldList)) {
            return itemService.getMetadata(item, ANY, ANY, ANY, ANY).stream()
                    .anyMatch(md -> regex.matcher(md.getValue()).matches());
        }

        return Arrays.stream(fieldList.split(","))
                .map(field -> itemService.getMetadataByMetadataString(item, field.trim()))
                .flatMap(List::stream)
                .anyMatch(md -> regex.matcher(md.getValue()).matches());
    }

    /**
     * Returns true if a given item has at all fields of a specific list whose values
     * match a provided regular expression.
     * @param item Provided item
     * @param fieldList List of fields to check
     * @param regex Regular expression to check field values against
     * @return true if all specified fields match, false otherwise
     */
    static boolean hasOnlyMetadataMatch(Item item, String fieldList, Pattern regex) {
        if ("*".equals(fieldList)) {
            return itemService.getMetadata(item, ANY, ANY, ANY, ANY).stream()
                    .allMatch(md -> regex.matcher(md.getValue()).matches());
        }

        return Arrays.stream(fieldList.split(","))
                .map(field -> itemService.getMetadataByMetadataString(item, field.trim()))
                .flatMap(List::stream)
                .allMatch(md -> regex.matcher(md.getValue()).matches());
    }

    static boolean recentlyModified(Item item, int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        return cal.getTime().before(item.getLastModified());
    }

}
