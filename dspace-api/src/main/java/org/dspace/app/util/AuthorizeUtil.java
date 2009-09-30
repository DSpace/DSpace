/*
 * AuthorizeUtil.java
 *
 * Version: $Revision: 3980 $
 *
 * Date: $Date: 2009-06-26 19:07:25 +0200 (ven, 26 giu 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
package org.dspace.app.util;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;

public class AuthorizeUtil
{

    public static void authorizeManageBitstreamPolicy(Context context,
            Bitstream bitstream) throws AuthorizeException, SQLException
    {
        Bundle bundle = bitstream.getBundles()[0];
        authorizeManageBundlePolicy(context, bundle);
    }

    public static void authorizeManageBundlePolicy(Context context,
            Bundle bundle) throws AuthorizeException, SQLException
    {
        Item item = bundle.getItems()[0];
        authorizeManageItemPolicy(context, item);
    }

    public static void authorizeManageItemPolicy(Context context, Item item)
            throws AuthorizeException, SQLException
    {
        if (AuthorizeConfiguration.canItemAdminManagePolicies())
        {
            AuthorizeManager.authorizeAction(context, item, Constants.ADMIN);
        }
        else if (AuthorizeConfiguration.canCollectionAdminManageItemPolicies())
        {
            AuthorizeManager.authorizeAction(context, item
                    .getOwningCollection(), Constants.ADMIN);
        }
        else if (AuthorizeConfiguration.canCommunityAdminManageItemPolicies())
        {
            AuthorizeManager
                    .authorizeAction(context, item.getOwningCollection()
                            .getCommunities()[0], Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin are allowed to manage item policies");
        }
    }

    public static void authorizeManageCollectionPolicy(Context context,
            Collection collection) throws AuthorizeException, SQLException
    {
        if (AuthorizeConfiguration.canCollectionAdminManagePolicies())
        {
            AuthorizeManager.authorizeAction(context, collection,
                    Constants.ADMIN);
        }
        else if (AuthorizeConfiguration
                .canCommunityAdminManageCollectionPolicies())
        {
            AuthorizeManager.authorizeAction(context, collection
                    .getCommunities()[0], Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin are allowed to manage collection policies");
        }
    }

    public static void authorizeManageCommunityPolicy(Context context,
            Community community) throws AuthorizeException, SQLException
    {
        if (AuthorizeConfiguration.canCommunityAdminManagePolicies())
        {
            AuthorizeManager.authorizeAction(context, community,
                    Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin are allowed to manage community policies");
        }
    }

    public static void requireAdminRole(Context context)
            throws AuthorizeException, SQLException
    {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin are allowed to perform this action");
        }
    }

    public static void authorizeManageCCLicense(Context context, Item item)
            throws AuthorizeException, SQLException
    {
        try
        {
            AuthorizeManager.authorizeAction(context, item, Constants.ADD);
            AuthorizeManager.authorizeAction(context, item, Constants.REMOVE);
        }
        catch (AuthorizeException authex)
        {
            if (AuthorizeConfiguration.canItemAdminManageCCLicense())
            {
                AuthorizeManager
                        .authorizeAction(context, item, Constants.ADMIN);
            }
            else if (AuthorizeConfiguration.canCollectionAdminManageCCLicense())
            {
                AuthorizeManager.authorizeAction(context, item
                        .getParentObject(), Constants.ADMIN);
            }
            else if (AuthorizeConfiguration.canCommunityAdminManageCCLicense())
            {
                AuthorizeManager.authorizeAction(context, item
                        .getParentObject().getParentObject(), Constants.ADMIN);
            }
            else
            {
                requireAdminRole(context);
            }
        }
    }

    public static void authorizeManageTemplateItem(Context context,
            Collection collection) throws AuthorizeException, SQLException
    {
        boolean isAuthorized = collection.canEditBoolean(false);

        if (!isAuthorized
                && AuthorizeConfiguration
                        .canCollectionAdminManageTemplateItem())
        {
            AuthorizeManager.authorizeAction(context, collection,
                    Constants.ADMIN);
        }
        else if (!isAuthorized
                && AuthorizeConfiguration
                        .canCommunityAdminManageCollectionTemplateItem())
        {
            Community[] communities = collection.getCommunities();
            Community parent = communities != null && communities.length > 0 ? communities[0]
                    : null;
            AuthorizeManager.authorizeAction(context, parent, Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You are not authorized to create a template item for the collection");
        }
    }

    public static void authorizeManageSubmittersGroup(Context context,
            Collection collection) throws AuthorizeException, SQLException
    {
        if (AuthorizeConfiguration.canCollectionAdminManageSubmitters())
        {
            AuthorizeManager.authorizeAction(context, collection,
                    Constants.ADMIN);
        }
        else if (AuthorizeConfiguration
                .canCommunityAdminManageCollectionSubmitters())
        {
            AuthorizeManager.authorizeAction(context, collection
                    .getCommunities()[0], Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin are allowed to manage collection submitters");
        }
    }

    public static void authorizeManageWorkflowsGroup(Context context,
            Collection collection) throws AuthorizeException, SQLException
    {
        if (AuthorizeConfiguration.canCollectionAdminManageWorkflows())
        {
            AuthorizeManager.authorizeAction(context, collection,
                    Constants.ADMIN);
        }
        else if (AuthorizeConfiguration
                .canCommunityAdminManageCollectionWorkflows())
        {
            AuthorizeManager.authorizeAction(context, collection
                    .getCommunities()[0], Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin are allowed to manage collection workflow");
        }
    }

    public static void authorizeManageAdminGroup(Context context,
            Collection collection) throws AuthorizeException, SQLException
    {
        if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup())
        {
            AuthorizeManager.authorizeAction(context, collection,
                    Constants.ADMIN);
        }
        else if (AuthorizeConfiguration
                .canCommunityAdminManageCollectionAdminGroup())
        {
            AuthorizeManager.authorizeAction(context, collection
                    .getCommunities()[0], Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin are allowed to manage collection admin");
        }
    }

    public static void authorizeRemoveAdminGroup(Context context,
            Collection collection) throws AuthorizeException, SQLException
    {
        Community[] parentCommunities = collection.getCommunities();
        if (AuthorizeConfiguration
                .canCommunityAdminManageCollectionAdminGroup()
                && parentCommunities != null && parentCommunities.length > 0)
        {
            AuthorizeManager.authorizeAction(context, collection
                    .getCommunities()[0], Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin can remove the admin group of a collection");
        }
    }

    public static void authorizeManageAdminGroup(Context context,
            Community community) throws AuthorizeException, SQLException
    {
        if (AuthorizeConfiguration.canCommunityAdminManageAdminGroup())
        {
            AuthorizeManager.authorizeAction(context, community,
                    Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin are allowed to manage community admin");
        }
    }

    public static void authorizeRemoveAdminGroup(Context context,
            Community community) throws SQLException, AuthorizeException
    {
        Community parentCommunity = community.getParentCommunity();
        if (AuthorizeConfiguration.canCommunityAdminManageAdminGroup()
                && parentCommunity != null)
        {
            AuthorizeManager.authorizeAction(context, parentCommunity,
                    Constants.ADMIN);
        }
        else if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system admin can remove the admin group of the community");
        }
    }

    public static void authorizeManagePolicy(Context c, ResourcePolicy rp)
            throws SQLException, AuthorizeException
    {
        switch (rp.getResourceType())
        {
        case Constants.BITSTREAM:
            authorizeManageBitstreamPolicy(c, Bitstream.find(c, rp
                    .getResourceID()));
            break;
        case Constants.BUNDLE:
            authorizeManageBundlePolicy(c, Bundle.find(c, rp.getResourceID()));
            break;

        case Constants.ITEM:
            authorizeManageItemPolicy(c, Item.find(c, rp.getResourceID()));
            break;
        case Constants.COLLECTION:
            authorizeManageCollectionPolicy(c, Collection.find(c, rp
                    .getResourceID()));
            break;
        case Constants.COMMUNITY:
            authorizeManageCommunityPolicy(c, Community.find(c, rp
                    .getResourceID()));
            break;

        default:
            requireAdminRole(c);
            break;
        }
    }

    public static void authorizeWithdrawItem(Context context, Item item)
            throws SQLException, AuthorizeException
    {
        boolean authorized = false;
        if (AuthorizeConfiguration.canCollectionAdminPerformItemWithdrawn())
        {
            authorized = AuthorizeManager.authorizeActionBoolean(context, item
                    .getOwningCollection(), Constants.ADMIN);
        }
        else if (AuthorizeConfiguration.canCommunityAdminPerformItemWithdrawn())
        {
            authorized = AuthorizeManager
                    .authorizeActionBoolean(context, item.getOwningCollection()
                            .getCommunities()[0], Constants.ADMIN);
        }

        if (!authorized)
        {
            authorized = AuthorizeManager.authorizeActionBoolean(context, item
                    .getOwningCollection(), Constants.REMOVE, false);
        }

        // authorized
        if (!authorized)
        {
            throw new AuthorizeException(
                    "To withdraw item must be COLLECTION_ADMIN or have REMOVE authorization on owning Collection");
        }
    }

    public static void authorizeReinstateItem(Context context, Item item)
            throws SQLException, AuthorizeException
    {
        Collection[] colls = item.getCollections();

        for (int i = 0; i < colls.length; i++)
        {
            if (!AuthorizeConfiguration
                    .canCollectionAdminPerformItemReinstatiate())
            {
                if (AuthorizeConfiguration
                        .canCommunityAdminPerformItemReinstatiate()
                        && AuthorizeManager.authorizeActionBoolean(context,
                                colls[i].getCommunities()[0], Constants.ADMIN))
                {
                    // authorized
                }
                else
                {
                    AuthorizeManager.authorizeAction(context, colls[i],
                            Constants.ADD, false);
                }
            }
            else
            {
                AuthorizeManager.authorizeAction(context, colls[i],
                        Constants.ADD);
            }
        }
    }
}
