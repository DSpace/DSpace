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

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.license.CreativeCommons;

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
    protected String termsOpen = null;
	
    public DefaultEmbargoSetter()
    {
        super();
        termsOpen = ConfigurationManager.getProperty("embargo.terms.open");
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
    public DCDate parseTerms(Context context, Item item, String terms)
        throws SQLException, AuthorizeException, IOException
    {
    	if (terms != null && terms.length() > 0)
    	{
    		if (termsOpen.equals(terms))
            {
                return EmbargoManager.FOREVER;
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
    public void setEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        DCDate liftDate = EmbargoManager.getEmbargoTermsAsDate(context, item);
        for (Bundle bn : item.getBundles())
        {
            // Skip the LICENSE and METADATA bundles, they stay world-readable
            String bnn = bn.getName();
            if (!(bnn.equals(Constants.LICENSE_BUNDLE_NAME) || bnn.equals(Constants.METADATA_BUNDLE_NAME) || bnn.equals(CreativeCommons.CC_BUNDLE_NAME)))
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

            Group[] authorizedGroups = AuthorizeManager.getAuthorizedGroups(context, owningCollection, Constants.DEFAULT_ITEM_READ);

            // look for anonymous
            boolean isAnonymousInPlace=false;
            for(Group g : authorizedGroups){
                if(g.getID()==Group.ANONYMOUS_ID){
                    isAnonymousInPlace=true;
                }
            }
            if(!isAnonymousInPlace){
                // add policies for all the groups
                for(Group g : authorizedGroups){
                    ResourcePolicy rp = AuthorizeManager.createOrModifyPolicy(null, context, null, g.getID(), null, embargoDate, Constants.READ, reason, dso);
                    if(rp!=null)
                        rp.update();
                }

            }
            else{
                // add policy just for anonymous
                ResourcePolicy rp = AuthorizeManager.createOrModifyPolicy(null, context, null, 0, null, embargoDate, Constants.READ, reason, dso);
                if(rp!=null)
                    rp.update();
            }
        }

    }





    /**
     * Check that embargo is properly set on Item: no read access to bitstreams.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    public void checkEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        for (Bundle bn : item.getBundles())
        {
            // Skip the LICENSE and METADATA bundles, they stay world-readable
            String bnn = bn.getName();
            if (!(bnn.equals(Constants.LICENSE_BUNDLE_NAME) || bnn.equals(Constants.METADATA_BUNDLE_NAME) || bnn.equals(CreativeCommons.CC_BUNDLE_NAME)))
            {
                // don't report on "TEXT" or "THUMBNAIL" bundles; those
                // can have READ long as the bitstreams in them do not.
                if (!(bnn.equals("TEXT") || bnn.equals("THUMBNAIL")))
                {
                    // check for ANY read policies and report them:
                    for (ResourcePolicy rp : AuthorizeManager.getPoliciesActionFilter(context, bn, Constants.READ))
                    {
                        System.out.println("CHECK WARNING: Item "+item.getHandle()+", Bundle "+bn.getName()+" allows READ by "+
                          ((rp.getEPersonID() < 0) ? "Group "+rp.getGroup().getName() :
                                                      "EPerson "+rp.getEPerson().getFullName()));
                    }
                }

                for (Bitstream bs : bn.getBitstreams())
                {
                    for (ResourcePolicy rp : AuthorizeManager.getPoliciesActionFilter(context, bs, Constants.READ))
                    {
                        System.out.println("CHECK WARNING: Item "+item.getHandle()+", Bitstream "+bs.getName()+" (in Bundle "+bn.getName()+") allows READ by "+
                          ((rp.getEPersonID() < 0) ? "Group "+rp.getGroup().getName() :
                                                      "EPerson "+rp.getEPerson().getFullName()));
                    }
                }
            }
        }
    }
}
