/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.dspace.content.AccessStatus;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Implementation of the access status helper that uses an ordered list of access statuses to enforce an order
 * of precedence when there are multiple bitstreams in the original bundle that have difference policies.
 * <p>
 * The ordered list is read from the {@code access.status.order} configuration property via
 * {@link ConfigurationService}. The status with the highest precedence comes first. When the property is not
 * configured, the order defaults to {@link #DEFAULT_ORDERED_BITSTREAM_STATUSES}.
 * 
 * @author Russ Poetker (rpoetke1@jh.edu)
 */
public class OrderedAccessStatusHelper extends DefaultAccessStatusHelper {

    /**
     * Configuration key for the ordered list of access statuses.
     */
    public static final String ACCESS_STATUS_ORDER_PROPERTY = "access.status.order";

    /**
     * Default ordered list of access statuses, used when {@link #ACCESS_STATUS_ORDER_PROPERTY} is not configured.
     */
    static final String[] DEFAULT_ORDERED_BITSTREAM_STATUSES =
        new String[] { EMBARGO, RESTRICTED, UNKNOWN, OPEN_ACCESS };

    /**
     * Iterate over all the bitstreams in the item's original bundle, calculate the access status for each bitstream,
     * and select the access status with the most precedence based on the configured ordered list of statuses. The
     * access status with the highest precedence is at index 0 in the list.
     * <p>
     * If the item Original bundle is empty, "metadata.only" is returned.
     * <p>
     * If the item is null, simply returns the "unknown" value.
     *
     * @param context     the DSpace context
     * @param item        the item to check for embargoes
     * @param threshold   the embargo threshold date
     * @param type        the type of calculation
     * @return the access status
     */
    @Override
    public AccessStatus getAccessStatusFromItem(Context context, Item item, LocalDate threshold, String type) {
        if (item == null) {
            return new AccessStatus(UNKNOWN, null);
        }

        final List<Bundle> bundles = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);

        boolean noBitstreamsInBundles = bundles.stream()
            .allMatch(bundle -> bundle.getBitstreams().isEmpty());

        if (noBitstreamsInBundles) {
            return new AccessStatus(METADATA_ONLY, null);
        }

        return getAccessStatusForItemBitstreams(context, bundles, threshold, type);
    }

    private AccessStatus getAccessStatusForItemBitstreams(Context context, List<Bundle> bundles, LocalDate threshold,
                                                          String type) {
        final String[] orderedStatuses = getOrderedBitstreamStatuses();
        return bundles.stream()
            .map(Bundle::getBitstreams)
            .flatMap(List::stream)
            .map(bitstream -> getAccessStatusForBitstreamSafe(context, bitstream, threshold, type))
            .min(Comparator.comparingInt(
                accessStatus -> {
                    int index = indexOf(orderedStatuses, accessStatus.getStatus());
                    if (index == -1) {
                        throw new RuntimeException("Access status " + accessStatus.getStatus() +
                            " not found in ordered list");
                    }
                    return index;
                }
            ))
            .orElseGet(() -> new AccessStatus(UNKNOWN, null));
    }

    /**
     * Read the ordered list of access statuses from configuration, falling back to
     * {@link #DEFAULT_ORDERED_BITSTREAM_STATUSES} when the property is not set.
     *
     * @return the ordered list of access statuses; never empty
     */
    protected String[] getOrderedBitstreamStatuses() {
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String[] configured = configurationService.getArrayProperty(ACCESS_STATUS_ORDER_PROPERTY);
        if (configured == null || configured.length == 0) {
            return DEFAULT_ORDERED_BITSTREAM_STATUSES;
        }
        return configured;
    }

    private static int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }

    private AccessStatus getAccessStatusForBitstreamSafe(Context context, Bitstream bitstream, LocalDate threshold,
                                                         String type) {
        try {
            return getAccessStatusFromBitstream(context, bitstream, threshold, type);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
