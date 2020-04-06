package org.dspace.xmlworkflow.cristin;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Class to manage item access policies (and bundles and bitstreams in those items)</p>
 *
 * <p>The Default policies that Duo items have are as follows:</p>
 *
 * <ul>
 * <li>ORIGINAL - Publicly readable</li>
 * <li>LICENSE - Publicly readable</li>
 * <li>METADATA - Administrator only</li>
 * <li>SECONDARY - Publicly readable</li>
 * <li>SECONDARY_CLOSED - Administrator only</li>
 * <li>SWORD - Administrator only</li>
 * </ul>
 */
public class CristinPolicyManager {
    /**
     * Apply the default access policies to an item (see class documentation for details)
     *
     * @param context
     * @param item
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void setDefaultPolicies(Context context, Item item)
            throws SQLException, AuthorizeException {
        this.setReadPolicies(context, item, CristinConstants.ORIGINAL_BUNDLE, CristinConstants.ANON_GROUP, true);
        // no longer working with licences
        // this.setReadPolicies(context, item, DuoConstants.LICENSE_BUNDLE, DuoConstants.ANON_GROUP, true);
        this.setReadPolicies(context, item, CristinConstants.METADATA_BUNDLE, CristinConstants.ADMIN_GROUP, false);
        this.setReadPolicies(context, item, CristinConstants.SECONDARY_BUNDLE, CristinConstants.ANON_GROUP, true);
        this.setReadPolicies(context, item, CristinConstants.SECONDARY_RESTRICTED_BUNDLE, CristinConstants.ADMIN_GROUP, false);
    }

    private void setReadPolicies(Context context, Item item, String bundleName, String groupName, boolean respectDefault)
            throws SQLException, AuthorizeException {
        List<Bundle> bundles = item.getBundles(bundleName);
        for (Bundle b : bundles) {
            boolean cascade = this.doPolicy(context, b, groupName, Constants.BUNDLE, respectDefault);

            if (cascade) {
                List<Bitstream> bss = b.getBitstreams();
                for (Bitstream bs : bss) {
                    this.doPolicy(context, bs, groupName, Constants.BITSTREAM, respectDefault);
                }
            }
        }
    }

    private boolean doPolicy(Context context, DSpaceObject dso, String groupName, int resourceType, boolean respectDefault)
            throws SQLException, AuthorizeException {
        List<ResourcePolicy> read = this.getReadPolicies(context, dso);
        if (read.size() > 0 && respectDefault) {
            // if there is already read permissions and we are requested to respect
            // the default, then carry on to the next bundle
            return false;
        }
        if (!respectDefault) {
            AuthorizeServiceFactory.getInstance().getResourcePolicyService().removePolicies(context, dso, Constants.READ);
        }
        this.setReadPolicy(context, dso, groupName);
        return true;
    }

    private void setReadPolicy(Context context, DSpaceObject dso, String groupName)
            throws SQLException, AuthorizeException {
        // set a hyper restrictive resource policy for testing purposes
        ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        ResourcePolicy rp = resourcePolicyService.create(context);
        rp.setAction(Constants.READ);
        rp.setGroup(groupService.findByName(context, groupName));
        rp.setdSpaceObject(dso);
        resourcePolicyService.update(context, rp);
    }

    private List<ResourcePolicy> getReadPolicies(Context context, DSpaceObject dso) throws SQLException {
        List<ResourcePolicy> read = new ArrayList<ResourcePolicy>();
        List<ResourcePolicy> all = AuthorizeServiceFactory.getInstance().getAuthorizeService().getPolicies(context, dso);
        for (ResourcePolicy rp : all) {
            if (rp.getAction() == Constants.READ) {
                read.add(rp);
            }
        }
        return read;
    }

}
