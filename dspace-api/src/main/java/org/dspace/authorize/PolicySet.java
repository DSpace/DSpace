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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * Was Hack/Tool to set policies for items, bundles, and bitstreams. Now has
 * helpful method, setPolicies();
 *
 * @author dstuve
 * @version $Revision$
 */
public class PolicySet
{
    private static final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    private static final ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private static final GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    /**
     * Command line interface to setPolicies - run to see arguments
     *
     * @param argv the command line arguments given
     * @throws Exception if error
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
        UUID containerID = UUID.fromString(argv[1]);
        int contenttype = Integer.parseInt(argv[2]);
        int actionID = Integer.parseInt(argv[3]);
        UUID groupID = UUID.fromString(argv[4]);

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
     * @throws SQLException if database error
     *             if database problem
     * @throws AuthorizeException if authorization error
     *             if current user is not authorized to change these policies
     */
    public static void setPolicies(Context c, int containerType,
                                   UUID containerID, int contentType, int actionID, UUID groupID,
                                   boolean isReplace, boolean clearOnly) throws SQLException,
            AuthorizeException
    {
        setPoliciesFilter(c, containerType,containerID, contentType, actionID, groupID, isReplace, clearOnly, null, null, null, null, null);
    }


    /**
     *
     * @param c
     *            current context
     * @param containerType
     *            type, Constants.ITEM or Constants.COLLECTION
     * @param containerID
     *            ID of container (DB primary key)
     * @param contentType
     *            ID of container (DB primary key)
     * @param actionID
     *            action ID (e.g. Constants.READ)
     * @param groupID
     *            group ID (database key)
     * @param isReplace
     *            if <code>true</code>, existing policies are removed first,
     *            otherwise add to existing policies
     * @param clearOnly
     *            if non-null, only process bitstreams whose names contain filter
     * @param name
     *            policy name
     * @param description
     *            policy descrption
     * @param startDate
     *            policy start date
     * @param endDate
     *            policy end date
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public static void setPolicies(Context c, int containerType,
                                   UUID containerID, int contentType, int actionID, UUID groupID,
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
     *            action ID (e.g. Constants.READ)
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
     * @throws SQLException if database error
     *             if database problem
     * @throws AuthorizeException if authorization error
     *             if current user is not authorized to change these policies
     */
    public static void setPoliciesFilter(Context c, int containerType,
                                         UUID containerID, int contentType, int actionID, UUID groupID,
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
     *            policy name
     * @param description
     *            policy description
     * @param startDate
     *            policy start date
     * @param endDate
     *            policy end date
     * @throws SQLException if database error
     *             if database problem
     * @throws AuthorizeException if authorization error
     *             if current user is not authorized to change these policies
     */
    public static void setPoliciesFilter(Context c, int containerType,
                                         UUID containerID, int contentType, int actionID, UUID groupID,
                                         boolean isReplace, boolean clearOnly, String filter,
                                         String name, String description, Date startDate, Date endDate) throws SQLException, AuthorizeException
    {
        if (containerType == Constants.COLLECTION)
        {
            Collection collection = collectionService.find(c, containerID);
            Group group = groupService.find(c, groupID);

            Iterator<Item> i = itemService.findAllByCollection(c, collection);
            if (contentType == Constants.ITEM)
            {
                // build list of all items in a collection
                while (i.hasNext())
                {
                    Item myitem = i.next();

                    // is this a replace? delete policies first
                    if (isReplace || clearOnly)
                    {
                        authorizeService.removeAllPolicies(c, myitem);
                    }

                    if (!clearOnly)
                    {

                        // before create a new policy check if an identical policy is already in place
                        if (!authorizeService.isAnIdenticalPolicyAlreadyInPlace(c, myitem, group, actionID, -1)) {
                            // now add the policy
                            ResourcePolicy rp = resourcePolicyService.create(c);

                            rp.setdSpaceObject(myitem);
                            rp.setAction(actionID);
                            rp.setGroup(group);

                            rp.setRpName(name);
                            rp.setRpDescription(description);
                            rp.setStartDate(startDate);
                            rp.setEndDate(endDate);

                            resourcePolicyService.update(c, rp);
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

                    List<Bundle> bundles = myitem.getBundles();

                    for (Bundle bundle : bundles) {
                        // is this a replace? delete policies first
                        if (isReplace || clearOnly) {
                            authorizeService.removeAllPolicies(c, bundle);
                        }

                        if (!clearOnly) {
                            // before create a new policy check if an identical policy is already in place
                            if (!authorizeService.isAnIdenticalPolicyAlreadyInPlace(c, bundle, group, actionID, -1)) {
                                // now add the policy
                                ResourcePolicy rp = resourcePolicyService.create(c);

                                rp.setdSpaceObject(bundle);
                                rp.setAction(actionID);
                                rp.setGroup(group);

                                rp.setRpName(name);
                                rp.setRpDescription(description);
                                rp.setStartDate(startDate);
                                rp.setEndDate(endDate);

                                resourcePolicyService.update(c, rp);
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

                    List<Bundle> bundles = myitem.getBundles();

                    for (Bundle bundle : bundles) {
                        System.out.println("Bundle " + bundle.getID());

                        List<Bitstream> bitstreams = bundle.getBitstreams();

                        for (Bitstream bitstream : bitstreams) {
                            if (filter == null ||
                                    bitstream.getName().indexOf(filter) != -1) {
                                // is this a replace? delete policies first
                                if (isReplace || clearOnly) {
                                    authorizeService.removeAllPolicies(c, bitstream);
                                }

                                if (!clearOnly) {
                                    // before create a new policy check if an identical policy is already in place
                                    if (!authorizeService.isAnIdenticalPolicyAlreadyInPlace(c, bitstream, group, actionID, -1)) {
                                        // now add the policy
                                        ResourcePolicy rp = resourcePolicyService.create(c);

                                        rp.setdSpaceObject(bitstream);
                                        rp.setAction(actionID);
                                        rp.setGroup(group);

                                        rp.setRpName(name);
                                        rp.setRpDescription(description);
                                        rp.setStartDate(startDate);
                                        rp.setEndDate(endDate);

                                        resourcePolicyService.update(c, rp);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
