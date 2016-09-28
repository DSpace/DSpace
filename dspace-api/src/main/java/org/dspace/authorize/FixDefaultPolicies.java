/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

//import org.dspace.browse.Browse;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;

/**
 * Command line tool to locate collections without default item and bitstream
 * read policies, and assign them some. (They must be there for submitted items
 * to inherit.)
 * 
 * @author dstuve
 * @version $Revision$
 */
public class FixDefaultPolicies
{
    /**
     * Command line interface to setPolicies - run to see arguments
     * @param argv arguments
     * @throws Exception if error
     */
    public static void main(String[] argv) throws Exception
    {
        Context c = new Context();

        // turn off authorization
        c.turnOffAuthorisationSystem();

        //////////////////////
        // carnage begins here
        //////////////////////
        ContentServiceFactory contentServiceFactory = ContentServiceFactory.getInstance();
        List<Collection> collections = contentServiceFactory.getCollectionService().findAll(c);

        for (Collection t : collections) {
            System.out.println("Collection " + t + " " + t.getName());

            // check for READ
            if (checkForPolicy(c, t, Constants.READ)) {
                System.out.println("\tFound READ policies!");
            } else {
                System.out.println("\tNo READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.READ);
            }

            if (checkForPolicy(c, t, Constants.DEFAULT_ITEM_READ)) {
                System.out.println("\tFound DEFAULT_ITEM_READ policies!");
            } else {
                System.out
                        .println("\tNo DEFAULT_ITEM_READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.DEFAULT_ITEM_READ);
            }

            if (checkForPolicy(c, t, Constants.DEFAULT_BITSTREAM_READ)) {
                System.out.println("\tFound DEFAULT_BITSTREAM_READ policies!");
            } else {
                System.out
                        .println("\tNo DEFAULT_BITSTREAM_READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.DEFAULT_BITSTREAM_READ);
            }
        }

        // now ensure communities have READ policies
        List<Community> communities = contentServiceFactory.getCommunityService().findAll(c);

        for (Community t : communities) {
            System.out.println("Community " + t + " " + t.getName());

            // check for READ
            if (checkForPolicy(c, t, Constants.READ)) {
                System.out.println("\tFound READ policies!");
            } else {
                System.out.println("\tNo READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.READ);
            }
        }

        c.complete();
        System.exit(0);
    }

    /**
     * check to see if a collection has any policies for a given action
     */
    private static boolean checkForPolicy(Context c, DSpaceObject t,
            int myaction) throws SQLException
    {
        // check to see if any policies exist for this action
        List<ResourcePolicy> policies = AuthorizeServiceFactory.getInstance().getAuthorizeService().getPoliciesActionFilter(c, t, myaction);

        return policies.size() > 0;
    }

    /**
     * add an anonymous group permission policy to the collection for this
     * action
     */
    private static void addAnonymousPolicy(Context c, DSpaceObject t,
            int myaction) throws SQLException, AuthorizeException
    {
        // group 0 is the anonymous group!
        Group anonymousGroup = EPersonServiceFactory.getInstance().getGroupService().findByName(c, Group.ANONYMOUS);

        // now create the default policies for submitted items
        ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        ResourcePolicy myPolicy = resourcePolicyService.create(c);
        myPolicy.setdSpaceObject(t);
        myPolicy.setAction(myaction);
        myPolicy.setGroup(anonymousGroup);
        resourcePolicyService.update(c, myPolicy);
    }
}
