/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.embargo;

import java.sql.SQLException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.embargo.factory.EmbargoServiceFactory;
import org.dspace.embargo.service.EmbargoService;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.license.CreativeCommonsServiceImpl;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Default plugin implementation of the embargo setting function.
 * The parseTerms() provides only very rudimentary terms logic - entry
 * of a configurable string (in terms field) for 'unlimited' embargo, otherwise
 * a standard ISO 8601 (yyyy-mm-dd) date is assumed. Users are encouraged 
 * to override this method for enhanced functionality.
 *
 * @author Larry Stone
 * @author Richard Rodgers
 */
public class DefaultEmbargoSetter implements EmbargoSetter
{
    protected AuthorizeService authorizeService;
    protected ResourcePolicyService resourcePolicyService;

    public DefaultEmbargoSetter()
    {
        super();
    }

    /**
     * Parse the terms into a definite date. Terms are expected to consist of
     * either: a token (value configured in 'embargo.terms.open' property) to indicate
     * indefinite embargo, or a literal lift date formatted in ISO 8601 format (yyyy-mm-dd)
     * 
     * @param context the DSpace context
     * @param item the item to embargo
     * @param terms the embargo terms
     * @return parsed date in DCDate format
     */
    @Override
    public DCDate parseTerms(Context context, Item item, String terms)
        throws SQLException, AuthorizeException
    {
        String termsOpen = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("embargo.terms.open");

    	if (terms != null && terms.length() > 0)
    	{
    		if (termsOpen.equals(terms))
            {
                return EmbargoService.FOREVER;
            }
            else
            {
                return new DCDate(terms);
            }
    	}
        return null;
    }

    /**
     * Enforce embargo by turning off all read access to bitstreams in
     * this Item.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    @Override
    public void setEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException
    {
        DCDate liftDate = EmbargoServiceFactory.getInstance().getEmbargoService().getEmbargoTermsAsDate(context, item);
        for (Bundle bn : item.getBundles())
        {
            // Skip the LICENSE and METADATA bundles, they stay world-readable
            String bnn = bn.getName();
            if (!(bnn.equals(Constants.LICENSE_BUNDLE_NAME) || bnn.equals(Constants.METADATA_BUNDLE_NAME) || bnn.equals(CreativeCommonsServiceImpl.CC_BUNDLE_NAME)))
            {
                //AuthorizeManager.removePoliciesActionFilter(context, bn, Constants.READ);
                generatePolicies(context, liftDate.toDate(), null, bn, item.getOwningCollection());
                for (Bitstream bs : bn.getBitstreams())
                {
                    //AuthorizeManager.removePoliciesActionFilter(context, bs, Constants.READ);
                    generatePolicies(context, liftDate.toDate(), null, bs, item.getOwningCollection());
                }
            }
        }
    }

    protected void generatePolicies(Context context, Date embargoDate,
                                        String reason, DSpaceObject dso, Collection owningCollection) throws SQLException, AuthorizeException {

        // add only embargo policy
        if(embargoDate!=null){

            List<Group> authorizedGroups = getAuthorizeService().getAuthorizedGroups(context, owningCollection, Constants.DEFAULT_ITEM_READ);

            // look for anonymous
            boolean isAnonymousInPlace=false;
            for(Group g : authorizedGroups){
                if(StringUtils.equals(g.getName(), Group.ANONYMOUS)){
                    isAnonymousInPlace=true;
                }
            }
            if(!isAnonymousInPlace){
                // add policies for all the groups
                for(Group g : authorizedGroups){
                    ResourcePolicy rp = getAuthorizeService().createOrModifyPolicy(null, context, null, g, null, embargoDate, Constants.READ, reason, dso);
                    if(rp!=null)
                        getResourcePolicyService().update(context, rp);
                }

            }
            else{
                // add policy just for anonymous
                ResourcePolicy rp = getAuthorizeService().createOrModifyPolicy(null, context, null, EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS), null, embargoDate, Constants.READ, reason, dso);
                if(rp!=null)
                    getResourcePolicyService().update(context, rp);
            }
        }

    }





    /**
     * Check that embargo is properly set on Item: no read access to bitstreams.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    @Override
    public void checkEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        for (Bundle bn : item.getBundles())
        {
            // Skip the LICENSE and METADATA bundles, they stay world-readable
            String bnn = bn.getName();
            if (!(bnn.equals(Constants.LICENSE_BUNDLE_NAME) || bnn.equals(Constants.METADATA_BUNDLE_NAME) || bnn.equals(CreativeCommonsServiceImpl.CC_BUNDLE_NAME)))
            {
                // don't report on "TEXT" or "THUMBNAIL" bundles; those
                // can have READ long as the bitstreams in them do not.
                if (!(bnn.equals("TEXT") || bnn.equals("THUMBNAIL")))
                {
                    // check for ANY read policies and report them:
                    for (ResourcePolicy rp : getAuthorizeService().getPoliciesActionFilter(context, bn, Constants.READ))
                    {
                        System.out.println("CHECK WARNING: Item "+item.getHandle()+", Bundle "+bn.getName()+" allows READ by "+
                          ((rp.getEPerson() != null) ? "Group "+rp.getGroup().getName() :
                                                      "EPerson "+rp.getEPerson().getFullName()));
                    }
                }

                for (Bitstream bs : bn.getBitstreams())
                {
                    for (ResourcePolicy rp : getAuthorizeService().getPoliciesActionFilter(context, bs, Constants.READ))
                    {
                        System.out.println("CHECK WARNING: Item "+item.getHandle()+", Bitstream "+bs.getName()+" (in Bundle "+bn.getName()+") allows READ by "+
                          ((rp.getEPerson() != null) ? "Group "+rp.getGroup().getName() :
                                                      "EPerson "+rp.getEPerson().getFullName()));
                    }
                }
            }
        }
    }

    private AuthorizeService getAuthorizeService() {
        if(authorizeService == null)
        {
            authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        }
        return authorizeService;
    }

    private ResourcePolicyService getResourcePolicyService() {
        if(resourcePolicyService == null)
        {
            resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        }
        return resourcePolicyService;
    }
}
