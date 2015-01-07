/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * This class is responsible to provide access to the configuration of the
 * Authorization System
 *
 * @author bollini
 *
 */
public class AuthorizeConfiguration
{

    private static boolean can_communityAdmin_group = ConfigurationManager
            .getBooleanProperty("core.authorization.community-admin.group",
                    true);

    // subcommunities and collections
    private static boolean can_communityAdmin_createSubelement = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.create-subelement",
                    true);

    private static boolean can_communityAdmin_deleteSubelement = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.delete-subelement",
                    true);

    private static boolean can_communityAdmin_policies = ConfigurationManager
            .getBooleanProperty("core.authorization.community-admin.policies",
                    true);

    private static boolean can_communityAdmin_adminGroup = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.admin-group", true);

    private static boolean can_communityAdmin_collectionPolicies = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.collection.policies",
                    true);

    private static boolean can_communityAdmin_collectionTemplateItem = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.collection.template-item",
                    true);

    private static boolean can_communityAdmin_collectionSubmitters = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.collection.submitters",
                    true);

    private static boolean can_communityAdmin_collectionWorkflows = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.collection.workflows",
                    true);

    private static boolean can_communityAdmin_collectionAdminGroup = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.collection.admin-group",
                    true);

    private static boolean can_communityAdmin_itemDelete = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.item.delete", true);

    private static boolean can_communityAdmin_itemWithdraw = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.item.withdraw", true);

    private static boolean can_communityAdmin_itemReinstatiate = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.item.reinstatiate",
                    true);

    private static boolean can_communityAdmin_itemPolicies = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.item.policies", true);

    // # also bundle
    private static boolean can_communityAdmin_itemCreateBitstream = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.item.create-bitstream",
                    true);

    private static boolean can_communityAdmin_itemDeleteBitstream = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.item.delete-bitstream",
                    true);

    private static boolean can_communityAdmin_itemAdminccLicense = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.community-admin.item-admin.cc-license",
                    true);

    // # COLLECTION ADMIN
    private static boolean can_collectionAdmin_policies = ConfigurationManager
            .getBooleanProperty("core.authorization.collection-admin.policies",
                    true);

    private static boolean can_collectionAdmin_templateItem = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.template-item", true);

    private static boolean can_collectionAdmin_submitters = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.submitters", true);

    private static boolean can_collectionAdmin_workflows = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.workflows", true);

    private static boolean can_collectionAdmin_adminGroup = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.admin-group", true);

    private static boolean can_collectionAdmin_itemDelete = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.item.delete", true);

    private static boolean can_collectionAdmin_itemWithdraw = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.item.withdraw", true);

    private static boolean can_collectionAdmin_itemReinstatiate = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.item.reinstatiate",
                    true);

    private static boolean can_collectionAdmin_itemPolicies = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.item.policies", true);

    // # also bundle
    private static boolean can_collectionAdmin_itemCreateBitstream = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.item.create-bitstream",
                    true);

    private static boolean can_collectionAdmin_itemDeleteBitstream = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.item.delete-bitstream",
                    true);

    private static boolean can_collectionAdmin_itemAdminccLicense = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.collection-admin.item-admin.cc-license",
                    true);

    // # ITEM ADMIN
    private static boolean can_itemAdmin_policies = ConfigurationManager
            .getBooleanProperty("core.authorization.item-admin.policies", true);

    // # also bundle
    private static boolean can_itemAdmin_createBitstream = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.item-admin.create-bitstream", true);

    private static boolean can_itemAdmin_deleteBitstream = ConfigurationManager
            .getBooleanProperty(
                    "core.authorization.item-admin.delete-bitstream", true);

    private static boolean can_itemAdmin_ccLicense = ConfigurationManager
            .getBooleanProperty("core.authorization.item-admin.cc-license",
                    true);

    /**
     * Is allowed manage bitstream?
     *
     * @param context
     *            the DSpace Context Object
     * @param bitstream
     *            the bitstream
     * @throws AuthorizeException
     *             if the current context (current user) is not allowed to
     *             manage the bitstream
     * @throws SQLException
     *             if a db error occur
     */
    public static boolean authorizeManage(Context context, String action,
                                                      Bitstream bitstream) throws SQLException
    {
        Bundle bundle = bitstream.getBundles()[0];
        return authorizeManage(context, action, bundle);
    }

    /**
     * Is allowed manage bundle?
     *
     * @param context
     *            the DSpace Context Object
     * @param bundle
     *            the bundle that the policy refer to
     * @throws AuthorizeException
     *             if the current context (current user) is not allowed to
     *             manage the bundle
     * @throws SQLException
     *             if a db error occur
     */
    public static boolean authorizeManage(Context context, String action,
                                                   Bundle bundle) throws SQLException
    {
        Item item = bundle.getItems()[0];
        return authorizeManage(context, action, item);
    }

    /**
     * Is allowed manage item in the
     * current context?
     *
     * @param context
     *            the DSpace Context Object
     * @param item
     *            the item
     * @throws AuthorizeException
     *             if the current context (current user) is not allowed to
     *             manage the item
     * @throws SQLException
     *             if a db error occur
     */
    public static boolean authorizeManage(Context context, String action, Item item)
            throws SQLException
    {
        if (ConfigurationManager.getBooleanProperty("core.authorization.item-admin." + action, true))
        {
            return AuthorizeManager.authorizeActionBoolean(context, item, Constants.ADMIN);
        }
        else if (ConfigurationManager.getBooleanProperty("core.authorization.collection-admin." + action, true))
        {
            return AuthorizeManager.authorizeActionBoolean(context, item
                    .getOwningCollection(), Constants.ADMIN);
        }
        else if (ConfigurationManager.getBooleanProperty("core.authorization.community-admin." + action, true))
        {
            return AuthorizeManager
                    .authorizeActionBoolean(context, item.getOwningCollection()
                            .getCommunities()[0], Constants.ADMIN);
        }

        return AuthorizeManager.isAdmin(context);
    }

    /**
     * Is allowed manage collection in the
     * current context?
     *
     * @param context
     *            the DSpace Context Object
     * @param collection
     *            the collection
     * @throws AuthorizeException
     *             if the current context (current user) is not allowed to
     *             manage the collection
     * @throws SQLException
     *             if a db error occur
     */
    public static boolean authorizeManage(Context context, String action,
                                                       Collection collection) throws  SQLException
    {
        if (ConfigurationManager.getBooleanProperty("core.authorization.collection-admin." + action, true))
        {
            return AuthorizeManager.authorizeActionBoolean(context, collection,
                    Constants.ADMIN);
        }
        else if (ConfigurationManager.getBooleanProperty("core.authorization.community-admin." + action, true))
        {
            return AuthorizeManager.authorizeActionBoolean(context, collection
                    .getCommunities()[0], Constants.ADMIN);
        }


        return AuthorizeManager.isAdmin(context);
    }

    /**
     * Is allowed manage community in the
     * current context?
     *
     * @param context
     *            the DSpace Context Object
     * @param community
     *            the community
     * @throws SQLException
     *             if a db error occur
     */
    public static boolean authorizeManage(Context context, String action,
                                                      Community community) throws SQLException
    {
        if (ConfigurationManager.getBooleanProperty("core.authorization.community-admin." + action, true))
        {
            return AuthorizeManager.authorizeActionBoolean(context, community,
                    Constants.ADMIN);
        }

        return AuthorizeManager.isAdmin(context);
    }

    /**
     * Are community admins allowed to create new, not strictly community
     * related, group?
     *
     * @return
     */
    public static boolean canCommunityAdminPerformGroupCreation()
    {
        return can_communityAdmin_group;
    }

    /**
     * Are community admins allowed to create collections or subcommunities?
     *
     * @return
     */
    public static boolean canCommunityAdminPerformSubelementCreation()
    {
        return can_communityAdmin_createSubelement;
    }

    /**
     * Are community admins allowed to remove collections or subcommunities?
     *
     * @return
     */
    public static boolean canCommunityAdminPerformSubelementDeletion()
    {
        return can_communityAdmin_deleteSubelement;
    }

    /**
     * Are community admins allowed to manage the community's and
     * subcommunities' policies?
     *
     * @return
     */
    public static boolean canCommunityAdminManagePolicies()
    {
        return can_communityAdmin_policies;
    }

    /**
     * Are community admins allowed to create/edit them community's and
     * subcommunities' admin groups?
     *
     * @return
     */
    public static boolean canCommunityAdminManageAdminGroup()
    {
        return can_communityAdmin_adminGroup;
    }

    /**
     * Are community admins allowed to create/edit the community's and
     * subcommunities' admin group?
     *
     * @return
     */
    public static boolean canCommunityAdminManageCollectionPolicies()
    {
        return can_communityAdmin_collectionPolicies;
    }

    /**
     * Are community admins allowed to manage the item template of them
     * collections?
     *
     * @return
     */
    public static boolean canCommunityAdminManageCollectionTemplateItem()
    {
        return can_communityAdmin_collectionTemplateItem;
    }

    /**
     * Are community admins allowed to manage (create/edit/remove) the
     * submitters group of them collections?
     *
     * @return
     */
    public static boolean canCommunityAdminManageCollectionSubmitters()
    {
        return can_communityAdmin_collectionSubmitters;
    }

    /**
     * Are community admins allowed to manage (create/edit/remove) the workflows
     * group of them collections?
     *
     * @return
     */
    public static boolean canCommunityAdminManageCollectionWorkflows()
    {
        return can_communityAdmin_collectionWorkflows;
    }

    /**
     * Are community admins allowed to manage (create/edit/remove) the admin
     * group of them collections?
     *
     * @return
     */
    public static boolean canCommunityAdminManageCollectionAdminGroup()
    {
        return can_communityAdmin_collectionAdminGroup;
    }

    /**
     * Are community admins allowed to remove an item from them collections?
     *
     * @return
     */
    public static boolean canCommunityAdminPerformItemDeletion()
    {
        return can_communityAdmin_itemDelete;
    }

    /**
     * Are community admins allowed to withdrawn an item from them collections?
     *
     * @return
     */
    public static boolean canCommunityAdminPerformItemWithdrawn()
    {
        return can_communityAdmin_itemWithdraw;
    }

    /**
     * Are community admins allowed to reinstate an item from them
     * collections?
     *
     * @return
     */
    public static boolean canCommunityAdminPerformItemReinstatiate()
    {
        return can_communityAdmin_itemReinstatiate;
    }

    /**
     * Are community admins allowed to manage the policies of an item owned by
     * one of them collections?
     *
     * @return
     */
    public static boolean canCommunityAdminManageItemPolicies()
    {
        return can_communityAdmin_itemPolicies;
    }

    /**
     * Are community admins allowed to add a bitstream to an item owned by one
     * of them collections?
     *
     * @return
     */
    public static boolean canCommunityAdminPerformBitstreamCreation()
    {
        return can_communityAdmin_itemCreateBitstream;
    }

    /**
     * Are community admins allowed to remove a bitstream from an item owned by
     * one of them collections?
     *
     * @return
     */
    public static boolean canCommunityAdminPerformBitstreamDeletion()
    {
        return can_communityAdmin_itemDeleteBitstream;
    }

    /**
     * Are community admins allowed to perform CC License replace or addition to
     * an item owned by one of them collections?
     *
     * @return
     */
    public static boolean canCommunityAdminManageCCLicense()
    {
        return can_communityAdmin_itemAdminccLicense;
    }

    /**
     * Are collection admins allowed to manage the collection's policies?
     *
     * @return
     */
    public static boolean canCollectionAdminManagePolicies()
    {
        return can_collectionAdmin_policies;
    }

    /**
     * Are collection admins allowed to manage (create/edit/delete) the
     * collection's item template?
     *
     * @return
     */
    public static boolean canCollectionAdminManageTemplateItem()
    {
        return can_collectionAdmin_templateItem;
    }

    /**
     * Are collection admins allowed to manage (create/edit/delete) the
     * collection's submitters group?
     *
     * @return
     */
    public static boolean canCollectionAdminManageSubmitters()
    {
        return can_collectionAdmin_submitters;
    }

    /**
     * Are collection admins allowed to manage (create/edit/delete) the
     * collection's workflows group?
     *
     * @return
     */
    public static boolean canCollectionAdminManageWorkflows()
    {
        return can_collectionAdmin_workflows;
    }

    /**
     * Are collection admins allowed to manage (create/edit) the collection's
     * admins group?
     *
     * @return
     */
    public static boolean canCollectionAdminManageAdminGroup()
    {
        return can_collectionAdmin_adminGroup;
    }

    /**
     * Are collection admins allowed to remove an item from the collection?
     *
     * @return
     */
    public static boolean canCollectionAdminPerformItemDeletion()
    {
        return can_collectionAdmin_itemDelete;
    }

    /**
     * Are collection admins allowed to withdrawn an item from the collection?
     *
     * @return
     */
    public static boolean canCollectionAdminPerformItemWithdrawn()
    {
        return can_collectionAdmin_itemWithdraw;
    }

    /**
     * Are collection admins allowed to reinstate an item from the
     * collection?
     *
     * @return
     */
    public static boolean canCollectionAdminPerformItemReinstatiate()
    {
        return can_collectionAdmin_itemReinstatiate;
    }

    /**
     * Are collection admins allowed to manage the policies of item owned by the
     * collection?
     *
     * @return
     */
    public static boolean canCollectionAdminManageItemPolicies()
    {
        return can_collectionAdmin_itemPolicies;
    }

    /**
     * Are collection admins allowed to add a bitstream to an item owned by the
     * collections?
     *
     * @return
     */
    public static boolean canCollectionAdminPerformBitstreamCreation()
    {
        return can_collectionAdmin_itemCreateBitstream;
    }

    /**
     * Are collection admins allowed to remove a bitstream from an item owned by
     * the collections?
     *
     * @return
     */
    public static boolean canCollectionAdminPerformBitstreamDeletion()
    {
        return can_collectionAdmin_itemDeleteBitstream;
    }

    /**
     * Are collection admins allowed to replace or adding a CC License to an
     * item owned by the collections?
     *
     * @return
     */
    public static boolean canCollectionAdminManageCCLicense()
    {
        return can_collectionAdmin_itemAdminccLicense;
    }

    /**
     * Are item admins allowed to manage the item's policies?
     *
     * @return
     */
    public static boolean canItemAdminManagePolicies()
    {
        return can_itemAdmin_policies;
    }

    /**
     * Are item admins allowed to add bitstreams to the item?
     *
     * @return
     */
    public static boolean canItemAdminPerformBitstreamCreation()
    {
        return can_itemAdmin_createBitstream;
    }

    /**
     * Are item admins allowed to remove bitstreams from the item?
     *
     * @return
     */
    public static boolean canItemAdminPerformBitstreamDeletion()
    {
        return can_itemAdmin_deleteBitstream;
    }

    /**
     * Are item admins allowed to replace or adding CC License to the item?
     *
     * @return
     */
    public static boolean canItemAdminManageCCLicense()
    {
        return can_itemAdmin_ccLicense;
    }

}
