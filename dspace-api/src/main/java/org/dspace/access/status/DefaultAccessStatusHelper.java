/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * Default plugin implementation of the access status helper.
 * The getAccessStatusFromItem method provides a simple logic to
 * calculate the access status of an item based on the policies of
 * the primary or the first bitstream in the original bundle.
 * Users can override this method for enhanced functionality.
 */
public class DefaultAccessStatusHelper implements AccessStatusHelper {
    public static final String EMBARGO = "embargo";
    public static final String METADATA_ONLY = "metadata.only";
    public static final String OPEN_ACCESS = "open.access";
    public static final String RESTRICTED = "restricted";
    public static final String UNKNOWN = "unknown";

    protected ItemService itemService =
            ContentServiceFactory.getInstance().getItemService();
    protected ResourcePolicyService resourcePolicyService =
            AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    protected AuthorizeService authorizeService =
            AuthorizeServiceFactory.getInstance().getAuthorizeService();

    public DefaultAccessStatusHelper() {
        super();
    }

    /**
     * Look at the item's policies to determine an access status value.
     * It is also considering a date threshold for embargos and restrictions.
     *
     * If the item is null, simply returns the "unknown" value.
     *
     * @param context     the DSpace context
     * @param item        the item to embargo
     * @param threshold   the embargo threshold date
     * @return an access status value
     */
    @Override
    public String getAccessStatusFromItem(Context context, Item item, Date threshold)
            throws SQLException {
        if (item == null) {
            return UNKNOWN;
        }
        // Consider only the original bundles.
        List<Bundle> bundles = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);
        // Check for primary bitstreams first.
        Bitstream bitstream = bundles.stream()
            .map(bundle -> bundle.getPrimaryBitstream())
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (bitstream == null) {
            // If there is no primary bitstream,
            // take the first bitstream in the bundles.
            bitstream = bundles.stream()
                .map(bundle -> bundle.getBitstreams())
                .flatMap(List::stream)
                .findFirst()
                .orElse(null);
        }
        return caculateAccessStatusForDso(context, bitstream, threshold);
    }

    /**
     * Look at the DSpace object's policies to determine an access status value.
     *
     * If the object is null, returns the "metadata.only" value.
     * If any policy attached to the object is valid for the anonymous group,
     * returns the "open.access" value.
     * Otherwise, if the policy start date is before the embargo threshold date,
     * returns the "embargo" value.
     * Every other cases return the "restricted" value.
     *
     * @param context     the DSpace context
     * @param dso         the DSpace object
     * @param threshold   the embargo threshold date
     * @return an access status value
     */
    private String caculateAccessStatusForDso(Context context, DSpaceObject dso, Date threshold)
            throws SQLException {
        if (dso == null) {
            return METADATA_ONLY;
        }
        // Only consider read policies.
        List<ResourcePolicy> policies = authorizeService
            .getPoliciesActionFilter(context, dso, Constants.READ);
        int openAccessCount = 0;
        int embargoCount = 0;
        int restrictedCount = 0;
        int unknownCount = 0;
        // Looks at all read policies.
        for (ResourcePolicy policy : policies) {
            boolean isValid = resourcePolicyService.isDateValid(policy);
            Group group = policy.getGroup();
            // The group must not be null here. However,
            // if it is, consider this as an unexpected case.
            if (group == null) {
                unknownCount++;
            } else if (StringUtils.equals(group.getName(), Group.ANONYMOUS)) {
                // Only calculate the status for the anonymous group.
                if (isValid) {
                    // If the policy is valid, the anonymous group have access
                    // to the bitstream.
                    openAccessCount++;
                } else {
                    Date startDate = policy.getStartDate();
                    if (startDate != null && !startDate.before(threshold)) {
                        // If the policy start date have a value and if this value
                        // is equal or superior to the configured forever date, the
                        // access status is also restricted.
                        restrictedCount++;
                    } else {
                        // If the current date is not between the policy start date
                        // and end date, the access status is embargo.
                        embargoCount++;
                    }
                }
            }
        }
        if (openAccessCount > 0) {
            return OPEN_ACCESS;
        }
        if (embargoCount > 0 && restrictedCount == 0) {
            return EMBARGO;
        }
        if (unknownCount > 0) {
            return UNKNOWN;
        }
        return RESTRICTED;
    }
}
