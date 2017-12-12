/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * Was Hack/Tool to set policies for items, bundles, and bitstreams. Now has
 * helpful method, setPolicies();
 *
 * @author dstuve
 * @version $Revision$
 */
public class PolicySet
{
    /**
     * Command line interface to setPolicies - run to see arguments
     */
    public static void main(String[] argv) throws Exception
    {
        if (argv.length < 6)
        {
            System.out
                    .println("Args: containerType containerID contentType actionID groupID command [filter]");
            System.out.println("container=COLLECTION command = ADD|REPLACE");

            return;
        }

        int containertype = Integer.parseInt(argv[0]);
        int containerID = Integer.parseInt(argv[1]);
        int contenttype = Integer.parseInt(argv[2]);
        int actionID = Integer.parseInt(argv[3]);
        int groupID = Integer.parseInt(argv[4]);

        boolean isReplace = false;
        String command = argv[5];
        String filter = null;
        if ( argv.length == 7 )
        {
            filter = argv[6];
        }

        if (command.equals("REPLACE"))
        {
            isReplace = true;
        }

        Context c = new Context();

        // turn off authorization
        c.turnOffAuthorisationSystem();

        //////////////////////
        // carnage begins here
        //////////////////////
        setPoliciesFilter(c, containertype, containerID, contenttype, actionID,
                groupID, isReplace, false, filter);

        c.complete();
        System.exit(0);
    }

    /**
     * Useful policy wildcard tool. Can set entire collections' contents'
     * policies
     *
     * @param c
     *            current context
     * @param containerType
     *            type, Constants.ITEM or Constants.COLLECTION
     * @param containerID
     *            ID of container (DB primary key)
     * @param contentType
     *            type (BUNDLE, ITEM, or BITSTREAM)
     * @param actionID
     *            action ID
     * @param groupID
     *            group ID (database key)
     * @param isReplace
     *            if <code>true</code>, existing policies are removed first,
     *            otherwise add to existing policies
     * @param clearOnly
     *            if <code>true</code>, just delete policies for matching
     *            objects
     * @throws SQLException
     *             if database problem
     * @throws AuthorizeException
     *             if current user is not authorized to change these policies
     */
    public static void setPolicies(Context c, int containerType,
                                   int containerID, int contentType, int actionID, int groupID,
                                   boolean isReplace, boolean clearOnly) throws SQLException,
            AuthorizeException
    {
        setPoliciesFilter(c, containerType,containerID, contentType, actionID, groupID, isReplace, clearOnly, null, null, null, null, null);
    }


    /**
     *
     * @param c
     * @param containerType
     * @param containerID
     * @param contentType
     * @param actionID
     * @param groupID
     * @param isReplace
     * @param clearOnly
     * @param name
     * @param description
     * @param startDate
     * @param endDate
     * @throws SQLException
     * @throws AuthorizeException
     */
    public static void setPolicies(Context c, int containerType,
                                   int containerID, int contentType, int actionID, int groupID,
                                   boolean isReplace, boolean clearOnly,
                                   String name, String description, Date startDate, Date endDate) throws SQLException, AuthorizeException
    {
        setPoliciesFilter(c, containerType, containerID, contentType,
                actionID, groupID, isReplace, clearOnly, null, name, description, startDate, endDate);
    }

    /**
     * Useful policy wildcard tool. Can set entire collections' contents'
     * policies
     *
     * @param c
     *            current context
     * @param containerType
     *            type, Constants.ITEM or Constants.COLLECTION
     * @param containerID
     *            ID of container (DB primary key)
     * @param contentType
     *            type (BUNDLE, ITEM, or BITSTREAM)
     * @param actionID
     *            action ID
     * @param groupID
     *            group ID (database key)
     * @param isReplace
     *            if <code>true</code>, existing policies are removed first,
     *            otherwise add to existing policies
     * @param clearOnly
     *            if <code>true</code>, just delete policies for matching
     *            objects
     * @param filter
     *            if non-null, only process bitstreams whose names contain filter
     * @throws SQLException
     *             if database problem
     * @throws AuthorizeException
     *             if current user is not authorized to change these policies
     */
    public static void setPoliciesFilter(Context c, int containerType,
                                         int containerID, int contentType, int actionID, int groupID,
                                         boolean isReplace, boolean clearOnly, String filter) throws SQLException,AuthorizeException
    {
        setPoliciesFilter(c, containerType,containerID, contentType, actionID, groupID, isReplace, clearOnly, filter, null, null, null, null);
    }

    /**
     * Useful policy wildcard tool. Can set entire collections' contents'
     * policies
     *
     * @param c
     *            current context
     * @param containerType
     *            type, Constants.ITEM or Constants.COLLECTION
     * @param containerID
     *            ID of container (DB primary key)
     * @param contentType
     *            type (BUNDLE, ITEM, or BITSTREAM)
     * @param actionID
     *            action ID
     * @param groupID
     *            group ID (database key)
     * @param isReplace
     *            if <code>true</code>, existing policies are removed first,
     *            otherwise add to existing policies
     * @param clearOnly
     *            if <code>true</code>, just delete policies for matching
     *            objects
     * @param filter
     *            if non-null, only process bitstreams whose names contain filter
     * @param name
     * @param description
     * @param startDate
     * @param endDate
     * @throws SQLException
     *             if database problem
     * @throws AuthorizeException
     *             if current user is not authorized to change these policies
     */
    public static void setPoliciesFilter(Context c, int containerType,
                                         int containerID, int contentType, int actionID, int groupID,
                                         boolean isReplace, boolean clearOnly, String filter,
                                         String name, String description, Date startDate, Date endDate) throws SQLException, AuthorizeException
    {
        if (containerType == Constants.COLLECTION)
        {
            Collection collection = Collection.find(c, containerID);
            Group group = Group.find(c, groupID);

            ItemIterator i = collection.getItems();
            try
            {
                if (contentType == Constants.ITEM)
                {
                    // build list of all items in a collection
                    while (i.hasNext())
                    {
                        Item myitem = i.next();

                        // is this a replace? delete policies first
                        if (isReplace || clearOnly)
                        {
                            AuthorizeManager.removeAllPolicies(c, myitem);
                        }

                        if (!clearOnly)
                        {

                            // before create a new policy check if an identical policy is already in place
                            if(!AuthorizeManager.isAnIdenticalPolicyAlreadyInPlace(c, myitem, groupID, actionID, -1)){
                                // now add the policy
                                ResourcePolicy rp = ResourcePolicy.create(c);

                                rp.setResource(myitem);
                                rp.setAction(actionID);
                                rp.setGroup(group);

                                rp.setRpName(name);
                                rp.setRpDescription(description);
                                rp.setStartDate(startDate);
                                rp.setEndDate(endDate);

                                rp.update();
                            }
                        }
                    }
                }
                else if (contentType == Constants.BUNDLE)
                {
                    // build list of all items in a collection
                    // build list of all bundles in those items
                    while (i.hasNext())
                    {
                        Item myitem = i.next();

                        Bundle[] bundles = myitem.getBundles();

                        for (int j = 0; j < bundles.length; j++)
                        {
                            Bundle bundle = bundles[j]; // t for target

                            // is this a replace? delete policies first
                            if (isReplace || clearOnly)
                            {
                                AuthorizeManager.removeAllPolicies(c, bundle);
                            }

                            if (!clearOnly)
                            {
                                // before create a new policy check if an identical policy is already in place
                                if(!AuthorizeManager.isAnIdenticalPolicyAlreadyInPlace(c, bundle, groupID, actionID, -1)){
                                    // now add the policy
                                    ResourcePolicy rp = ResourcePolicy.create(c);

                                    rp.setResource(bundle);
                                    rp.setAction(actionID);
                                    rp.setGroup(group);

                                    rp.setRpName(name);
                                    rp.setRpDescription(description);
                                    rp.setStartDate(startDate);
                                    rp.setEndDate(endDate);

                                    rp.update();
                                }
                            }
                        }
                    }
                }
                else if (contentType == Constants.BITSTREAM)
                {
                    // build list of all bitstreams in a collection
                    // iterate over items, bundles, get bitstreams
                    while (i.hasNext())
                    {
                        Item myitem = i.next();
                        System.out.println("Item " + myitem.getID());

                        Bundle[] bundles = myitem.getBundles();

                        for (int j = 0; j < bundles.length; j++)
                        {
                            System.out.println("Bundle " + bundles[j].getID());

                            Bitstream[] bitstreams = bundles[j].getBitstreams();

                            for (int k = 0; k < bitstreams.length; k++)
                            {
                                Bitstream bitstream = bitstreams[k]; // t for target

                                if ( filter == null ||
                                        bitstream.getName().indexOf( filter ) != -1 )
                                {
                                    // is this a replace? delete policies first
                                    if (isReplace || clearOnly)
                                    {
                                        AuthorizeManager.removeAllPolicies(c, bitstream);
                                    }

                                    if (!clearOnly)
                                    {
                                        // before create a new policy check if an identical policy is already in place
                                        if(!AuthorizeManager.isAnIdenticalPolicyAlreadyInPlace(c, bitstream,  groupID, actionID, -1)){
                                            // now add the policy
                                            ResourcePolicy rp = ResourcePolicy.create(c);

                                            rp.setResource(bitstream);
                                            rp.setAction(actionID);
                                            rp.setGroup(group);

                                            rp.setRpName(name);
                                            rp.setRpDescription(description);
                                            rp.setStartDate(startDate);
                                            rp.setEndDate(endDate);

                                            rp.update();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            finally
            {
                if (i != null)
                {
                    i.close();
                }
            }
        }
    }
}
