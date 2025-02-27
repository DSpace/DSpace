/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * Default plugin implementation of the access status helper.
 * The getAccessStatusFromItem method provides a simple logic to
 * calculate the access status of an item based on the policies of
 * the primary or the first bitstream in the original bundle.
 * Users can override this method for enhanced functionality.
 *
 * The getEmbargoFromItem method provides a simple logic to retrieve
 * an embargo date based on information of bitstreams from an item
 * based on the policies of the primary or the first bitstream in the
 * original bundle. Users can override this method for enhanced
 * functionality.
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
    protected GroupService groupService =
            EPersonServiceFactory.getInstance().getGroupService();

    public DefaultAccessStatusHelper() {
        super();
    }

    /**
     * Look at the item's policies to determine an access status value.
     * It is also considering a date threshold for embargoes and restrictions.
     *
     * If the item is null, simply returns the "unknown" value.
     *
     * @param context     the DSpace context
     * @param item        the item to check for embargoes
     * @param threshold   the embargo threshold date
     * @return an access status value
     */
    @Override
    public String getAccessStatusFromItem(Context context, Item item, Date threshold)
            throws SQLException {
        if (item == null) {
            return UNKNOWN;
        }
        Bitstream bitstream = getPrimaryOrFirstBitstreamInOriginalBundle(item);
        return calculateAccessStatusForDso(context, bitstream, threshold);
    }

    /**
     * Look at the DSpace object's policies to determine an access status value.
     *
     * If the object is null, returns the "metadata.only" value.
     * If any policy attached to the object is valid for the anonymous group,
     * returns the "open.access" value.
     * Otherwise, if the policy start date is after or equal to the embargo
     * threshold date, returns the "restricted" value.
     * Every other cases return the "embargo" value.
     *
     * @param context     the DSpace context
     * @param dso         the DSpace object
     * @param threshold   the embargo threshold date
     * @return an access status value
     */
    private String calculateAccessStatusForDso(Context context, DSpaceObject dso, Date threshold)
            throws SQLException {
        if (dso == null) {
            return METADATA_ONLY;
        }
        // Only calculate the status for the anonymous group read policies
        List<ResourcePolicy> policies = getAnonymousReadPolicies(context, dso);
        Date availabilityDate = findAvailabilityDate(policies, threshold);
        // Get the access status based on the availability date
        return getAccessStatusFromAvailabilityDate(availabilityDate, threshold);
    }

    /**
     * Look at the DSpace object availability date to determine an access status value.
     *
     * If the object is null, returns the "metadata.only" value.
     * If there's no availability date, returns the "open.access" value.
     * If the availability date is after or equal to the embargo
     * threshold date, returns the "restricted" value.
     * Every other cases return the "embargo" value.
     *
     * @param availabilityDate  the DSpace object availability date
     * @param threshold         the embargo threshold date
     * @return an access status value
     */
    @Override
    public String getAccessStatusFromAvailabilityDate(Date availabilityDate, Date threshold) {
        // If there is no availability date, it's an open access.
        if (availabilityDate == null) {
            return OPEN_ACCESS;
        }
        // If the policy start date have a value and if this value
        // is equal or superior to the configured forever date, the
        // access status is also restricted.
        if (!availabilityDate.before(threshold)) {
            return RESTRICTED;
        }
        return EMBARGO;
    }

    /**
     * Look at the anonymous policies of the primary (or first)
     * bitstream of the item to retrieve its embargo.
     *
     * If the item is null, simply returns an empty map with no embargo information.
     *
     * @param context       the DSpace context
     * @param item          the item
     * @param threshold     the embargo threshold date
     * @return an embargo date
     */
    @Override
    public String getEmbargoFromItem(Context context, Item item, Date threshold)
            throws SQLException {
        if (item == null) {
            return null;
        }
        Bitstream bitstream = getPrimaryOrFirstBitstreamInOriginalBundle(item);
        if (bitstream == null) {
            return null;
        }
        Date availabilityDate = retrieveAvailabilityDateForAnonymousUsers(context, bitstream, threshold);
        // If the date is null, it's an open access
        // If the date is equal of after the threshold, it's a restriction
        if (availabilityDate == null || !availabilityDate.before(threshold)) {
            return null;
        }
        return availabilityDate.toString();
    }

    /**
     * Look at the current user bitstream policies to retrieve its availability date.
     *
     * @param context       the DSpace context
     * @param bitstream     the bitstream
     * @param threshold     the embargo threshold date
     * @return an availability date
     */
    @Override
    public Date getAvailabilityDateFromBitstream(Context context, Bitstream bitstream, Date threshold)
            throws SQLException {
        return retrieveAvailabilityDateForCurrentUser(context, bitstream, threshold);
    }

    /**
     * Look in the item's original bundle. First, try to get the primary bitstream.
     * If the bitstream is null, simply returns the first one.
     *
     * @param item      the DSpace item
     * @return the bitstream
     */
    private Bitstream getPrimaryOrFirstBitstreamInOriginalBundle(Item item) {
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
        return bitstream;
    }

    /**
     * Look at the current user bitstream policies to retrieve its availability date.
     *
     * If the bitstream is null, simply returns no date.
     * 
     * @param context       the DSpace context
     * @param bitstream     the bitstream
     * @param threshold     the embargo threshold date
     * @return an availability date
     */
    private Date retrieveAvailabilityDateForCurrentUser(Context context, Bitstream bitstream, Date threshold)
        throws SQLException {
        if (bitstream == null) {
            return null;
        }
        // First, look if the user can read the bitstream
        boolean canRead = authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ);
        // If the current user can read the bitstream, it can't be an embargo
        if (canRead) {
            return null;
        }
        // Only consider read policies
        List<ResourcePolicy> policies = resourcePolicyService.find(context, bitstream, Constants.READ);
        // Only calculate the embargo date for the current user
        EPerson currentUser = context.getCurrentUser();
        List<ResourcePolicy> readPolicies = new ArrayList<ResourcePolicy>();
        for (ResourcePolicy policy : policies) {
            EPerson eperson = policy.getEPerson();
            if (eperson != null && currentUser != null && eperson.getID() == currentUser.getID()) {
                readPolicies.add(policy);
                continue;
            }
            Group group = policy.getGroup();
            if (group != null && groupService.isMember(context, currentUser, group)) {
                readPolicies.add(policy);
            }
        }
        return findAvailabilityDate(readPolicies, threshold);
    }

    /**
     * Look at the anonymous policies of the bitstream retrieve its availability date
     *
     * @param context       the DSpace context
     * @param bitstream     the bitstream
     * @param threshold     the embargo threshold date
     * @return an availability date
     */
    private Date retrieveAvailabilityDateForAnonymousUsers(Context context, Bitstream bitstream, Date threshold)
        throws SQLException {
        // Only calculate the status for the anonymous group read policies
        List<ResourcePolicy> policies = getAnonymousReadPolicies(context, bitstream);
        return findAvailabilityDate(policies, threshold);
    }

    /**
     * Retrieves the anonymous read policies for a DSpace object
     *
     * @param context   the DSpace context
     * @param dso       the DSpace object
     * @return a list of policies
     */
    private List<ResourcePolicy> getAnonymousReadPolicies(Context context, DSpaceObject dso) throws SQLException {
        // Only consider read policies. Use the find without a group
        // as it's not returning all expected values
        List<ResourcePolicy> readPolicies = resourcePolicyService.find(context, dso, Constants.READ);
        // Filter the policies with the anonymous group
        List<ResourcePolicy> filteredPolicies = readPolicies.stream()
            .filter(p -> StringUtils.equals(p.getGroup().getName(), Group.ANONYMOUS))
            .collect(Collectors.toList());
        return filteredPolicies;
    }

    /**
     * Look at the read policies to retrieve the access status availability date.
     *
     * @param readPolicies  the read policies
     * @param threshold     the embargo threshold date
     * @return an availability date
     */
    private Date findAvailabilityDate(List<ResourcePolicy> readPolicies, Date threshold) {
        // If there's no policies, return the threshold date (restriction)
        if (readPolicies.size() == 0) {
            return threshold;
        }
        Date availabilityDate = null;
        Date currentDate = Date.from(Instant.now());
        boolean takeMostRecentDate = true;
        // Looks at all read policies
        for (ResourcePolicy policy : readPolicies) {
            boolean isValid = resourcePolicyService.isDateValid(policy);
            // If any policy is valid, the bitstream is accessible
            if (isValid) {
                return null;
            }
            // There may be an active embargo
            Date startDate = policy.getStartDate();
            // Ignore policy with no start date or which is expired
            if (startDate == null || startDate.before(currentDate)) {
                continue;
            }
            // Policy with a start date over the threshold (restriction)
            // overrides the embargos
            if (!startDate.before(threshold)) {
                takeMostRecentDate = false;
            }
            // Take the most recent embargo date if there is no restriction, otherwise
            // take the highest date (account for rare cases where more than one resource
            // policy exists)
            if (availabilityDate == null) {
                availabilityDate = startDate;
            } else if (takeMostRecentDate) {
                availabilityDate = startDate.before(availabilityDate) ? startDate : availabilityDate;
            } else {
                availabilityDate = startDate.after(availabilityDate) ? startDate : availabilityDate;
            }
        }
        return availabilityDate;
    }
}
