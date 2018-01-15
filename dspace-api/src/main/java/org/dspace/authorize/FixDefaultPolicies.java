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

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

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
     */
    public static void main(String[] argv) throws Exception
    {
        Context c = new Context();

        // turn off authorization
        c.turnOffAuthorisationSystem();

        //////////////////////
        // carnage begins here
        //////////////////////
        Collection[] collections = Collection.findAll(c);

        for (int i = 0; i < collections.length; i++)
        {
            Collection t = collections[i];

            System.out.println("Collection " + t + " " + t.getMetadata("name"));

            // check for READ
            if (checkForPolicy(c, t, Constants.READ))
            {
                System.out.println("\tFound READ policies!");
            }
            else
            {
                System.out.println("\tNo READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.READ);
            }

            if (checkForPolicy(c, t, Constants.DEFAULT_ITEM_READ))
            {
                System.out.println("\tFound DEFAULT_ITEM_READ policies!");
            }
            else
            {
                System.out
                        .println("\tNo DEFAULT_ITEM_READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.DEFAULT_ITEM_READ);
            }

            if (checkForPolicy(c, t, Constants.DEFAULT_BITSTREAM_READ))
            {
                System.out.println("\tFound DEFAULT_BITSTREAM_READ policies!");
            }
            else
            {
                System.out
                        .println("\tNo DEFAULT_BITSTREAM_READ policy found, adding anonymous.");
                addAnonymousPolicy(c, t, Constants.DEFAULT_BITSTREAM_READ);
            }
        }

        // now ensure communities have READ policies
        Community[] communities = Community.findAll(c);

        for (int i = 0; i < communities.length; i++)
        {
            Community t = communities[i];

            System.out.println("Community " + t + " " + t.getMetadata("name"));

            // check for READ
            if (checkForPolicy(c, t, Constants.READ))
            {
                System.out.println("\tFound READ policies!");
            }
            else
            {
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
        List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(c, t, myaction);

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
        Group anonymousGroup = Group.find(c, 0);

        // now create the default policies for submitted items
        ResourcePolicy myPolicy = ResourcePolicy.create(c);
        myPolicy.setResource(t);
        myPolicy.setAction(myaction);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();
    }
}
