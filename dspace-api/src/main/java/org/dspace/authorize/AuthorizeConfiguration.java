/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * This class is responsible to provide access to the configuration of the
 * Authorization System
 *
 * @author bollini
 */
public class AuthorizeConfiguration {
    /**
     * A static reference to the {@link ConfigurationService} see the init method for initialization
     */
    private static ConfigurationService configurationService;

    /**
     * Default constructor
     */
    private AuthorizeConfiguration() { }

    /**
     * Complete the initialization of the class retrieving a reference to the {@link ConfigurationService}. MUST be
     * called at the start of each method
     */
    private synchronized static void init() {
        if (configurationService != null) {
            return;
        }
        configurationService = new DSpace().getConfigurationService();
    }
    /**
     * Are community admins allowed to create new, not strictly community
     * related, group?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminPerformGroupCreation() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.group", true);
    }

    /**
     * Are community admins allowed to create collections or subcommunities?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminPerformSubelementCreation() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.create-subelement", true);
    }

    /**
     * Are community admins allowed to remove collections or subcommunities?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminPerformSubelementDeletion() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.delete-subelement", true);
    }

    /**
     * Are community admins allowed to manage the community's and
     * subcommunities' policies?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminManagePolicies() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.policies", true);
    }

    /**
     * Are community admins allowed to create/edit them community's and
     * subcommunities' admin groups?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminManageAdminGroup() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.admin-group", true);
    }

    /**
     * Are community admins allowed to create/edit the community's and
     * subcommunities' admin group?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminManageCollectionPolicies() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.collection.policies", true);
    }

    /**
     * Are community admins allowed to manage the item template of them
     * collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminManageCollectionTemplateItem() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.collection.template-item",
                true);
    }

    /**
     * Are community admins allowed to manage (create/edit/remove) the
     * submitters group of them collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminManageCollectionSubmitters() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.collection.submitters",
                true);
    }

    /**
     * Are community admins allowed to manage (create/edit/remove) the workflows
     * group of them collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminManageCollectionWorkflows() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.collection.workflows", true);
    }

    /**
     * Are community admins allowed to manage (create/edit/remove) the admin
     * group of them collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminManageCollectionAdminGroup() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.collection.admin-group",
                true);
    }

    /**
     * Are community admins allowed to remove an item from them collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminPerformItemDeletion() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.item.delete", true);
    }

    /**
     * Are community admins allowed to withdrawn an item from them collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminPerformItemWithdrawn() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.item.withdraw", true);
    }

    /**
     * Are community admins allowed to reinstate an item from them
     * collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminPerformItemReinstatiate() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.item.reinstatiate", true);
    }

    /**
     * Are community admins allowed to manage the policies of an item owned by
     * one of them collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminManageItemPolicies() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.item.policies", true);
    }

    /**
     * Are community admins allowed to add a bitstream to an item owned by one
     * of them collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminPerformBitstreamCreation() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.item.create-bitstream",
                true);
    }

    /**
     * Are community admins allowed to remove a bitstream from an item owned by
     * one of them collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminPerformBitstreamDeletion() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.item.delete-bitstream",
                true);
    }

    /**
     * Are community admins allowed to perform CC License replace or addition to
     * an item owned by one of them collections?
     *
     * @return true/false
     */
    public static boolean canCommunityAdminManageCCLicense() {
        init();
        return configurationService.getBooleanProperty("core.authorization.community-admin.item-admin.cc-license",
                true);
    }

    /**
     * Are collection admins allowed to manage the collection's policies?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminManagePolicies() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.policies", true);
    }

    /**
     * Are collection admins allowed to manage (create/edit/delete) the
     * collection's item template?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminManageTemplateItem() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.template-item", true);
    }

    /**
     * Are collection admins allowed to manage (create/edit/delete) the
     * collection's submitters group?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminManageSubmitters() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.submitters", true);
    }

    /**
     * Are collection admins allowed to manage (create/edit/delete) the
     * collection's workflows group?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminManageWorkflows() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.workflows", true);
    }

    /**
     * Are collection admins allowed to manage (create/edit) the collection's
     * admins group?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminManageAdminGroup() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.admin-group", true);
    }

    /**
     * Are collection admins allowed to remove an item from the collection?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminPerformItemDeletion() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.item.delete", true);
    }

    /**
     * Are collection admins allowed to withdrawn an item from the collection?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminPerformItemWithdrawn() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.item.withdraw", true);
    }

    /**
     * Are collection admins allowed to reinstate an item from the
     * collection?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminPerformItemReinstatiate() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.item.reinstatiate", true);
    }

    /**
     * Are collection admins allowed to manage the policies of item owned by the
     * collection?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminManageItemPolicies() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.item.policies", true);
    }

    /**
     * Are collection admins allowed to add a bitstream to an item owned by the
     * collections?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminPerformBitstreamCreation() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.item.create-bitstream",
                true);
    }

    /**
     * Are collection admins allowed to remove a bitstream from an item owned by
     * the collections?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminPerformBitstreamDeletion() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.item.delete-bitstream",
                true);
    }

    /**
     * Are collection admins allowed to replace or adding a CC License to an
     * item owned by the collections?
     *
     * @return true/false
     */
    public static boolean canCollectionAdminManageCCLicense() {
        init();
        return configurationService.getBooleanProperty("core.authorization.collection-admin.item-admin.cc-license",
                true);
    }

    /**
     * Are item admins allowed to manage the item's policies?
     *
     * @return true/false
     */
    public static boolean canItemAdminManagePolicies() {
        init();
        return configurationService.getBooleanProperty("core.authorization.item-admin.policies", true);
    }

    /**
     * Are item admins allowed to add bitstreams to the item?
     *
     * @return true/false
     */
    public static boolean canItemAdminPerformBitstreamCreation() {
        init();
        return configurationService.getBooleanProperty("core.authorization.item-admin.create-bitstream", true);
    }

    /**
     * Are item admins allowed to remove bitstreams from the item?
     *
     * @return true/false
     */
    public static boolean canItemAdminPerformBitstreamDeletion() {
        init();
        return configurationService.getBooleanProperty("core.authorization.item-admin.delete-bitstream", true);
    }

    /**
     * Are item admins allowed to replace or adding CC License to the item?
     *
     * @return true/false
     */
    public static boolean canItemAdminManageCCLicense() {
        init();
        return configurationService.getBooleanProperty("core.authorization.item-admin.cc-license", true);
    }

}
