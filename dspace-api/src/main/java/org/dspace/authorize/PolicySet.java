/*
 * PolicySet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.authorize;

import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.authorize.dao.ResourcePolicyDAOFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;

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
        c.setIgnoreAuthorization(true);

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
     *             if database problem
     * @throws AuthorizeException
     *             if current user is not authorized to change these policies
     */
    public static void setPolicies(Context c, int containerType,
            int containerID, int contentType, int actionID, int groupID,
            boolean isReplace, boolean clearOnly) throws AuthorizeException
    {
        setPoliciesFilter(c, containerType, containerID, contentType,
                           actionID, groupID, isReplace, clearOnly, null);                         
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
     * @throws AuthorizeException
     *             if current user is not authorized to change these policies
     */
    public static void setPoliciesFilter(Context c, int containerType,
            int containerID, int contentType, int actionID, int groupID,
            boolean isReplace, boolean clearOnly, String filter)
        throws AuthorizeException
    {
        if (containerType == Constants.COLLECTION)
        {
            GroupDAO groupDAO = GroupDAOFactory.getInstance(c);
            ItemDAO itemDAO = ItemDAOFactory.getInstance(c);
            ResourcePolicyDAO rpDAO = ResourcePolicyDAOFactory.getInstance(c);

            Collection collection =
                CollectionDAOFactory.getInstance(c).retrieve(containerID);
            Group group = groupDAO.retrieve(groupID);

            for (Item item : itemDAO.getItemsByCollection(collection))
            {
                // build list of all items in a collection
                if (contentType == Constants.ITEM)
                {
                    // is this a replace? delete policies first
                    if (isReplace || clearOnly)
                    {
                        AuthorizeManager.removeAllPolicies(c, item);
                    }

                    if (!clearOnly)
                    {
                        // now add the policy
                        ResourcePolicy rp = rpDAO.create();

                        rp.setResource(item);
                        rp.setAction(actionID);
                        rp.setGroup(group);

                        rpDAO.update(rp);
                    }
                }
                else if (contentType == Constants.BUNDLE)
                {
                    // build list of all items in a collection
                    // build list of all bundles in those items
                    Bundle[] bundles = item.getBundles();

                    for (int j = 0; j < bundles.length; j++)
                    {
                        Bundle t = bundles[j]; // t for target

                        // is this a replace? delete policies first
                        if (isReplace || clearOnly)
                        {
                            AuthorizeManager.removeAllPolicies(c, t);
                        }

                        if (!clearOnly)
                        {
                            // now add the policy
                            ResourcePolicy rp = rpDAO.create();

                            rp.setResource(t);
                            rp.setAction(actionID);
                            rp.setGroup(group);

                            rpDAO.update(rp);
                        }
                    }
                }
                else if (contentType == Constants.BITSTREAM)
                {
                    // build list of all bitstreams in a collection
                    // iterate over items, bundles, get bitstreams
                    System.out.println("Item " + item.getID());

                    Bundle[] bundles = item.getBundles();

                    for (int j = 0; j < bundles.length; j++)
                    {
                        System.out.println("Bundle " + bundles[j].getID());

                        Bitstream[] bitstreams = bundles[j].getBitstreams();

                        for (int k = 0; k < bitstreams.length; k++)
                        {
                            Bitstream t = bitstreams[k]; // t for target

                            if ( filter == null ||
                                 t.getName().indexOf( filter ) != -1 )
                            {
                                // is this a replace? delete policies first
                                if (isReplace || clearOnly)
                                {
                                        AuthorizeManager.removeAllPolicies(c, t);
                                }

                                if (!clearOnly)
                                {
                                        // now add the policy
                                        ResourcePolicy rp = rpDAO.create();

                                        rp.setResource(t);
                                        rp.setAction(actionID);
                                        rp.setGroup(group);

                                        rpDAO.update(rp);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
