/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.auth;

import org.dspace.services.ConfigurationService;

/**
 * This class is responsible to provide access to the configuration of the
 * Authorization System
 */
public class DSpaceAuthorizeConfiguration {
	private ConfigurationService config;
	
	public DSpaceAuthorizeConfiguration (ConfigurationService config) {
		this.config = config;
		
		can_communityAdmin_group = this.getBooleanProperty("core.authorization.community-admin.group",
                true);
		can_communityAdmin_createSubelement = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.create-subelement",
	                    true);
		
		can_communityAdmin_deleteSubelement = this
		            .getBooleanProperty(
		                    "core.authorization.community-admin.delete-subelement",
		                    true);
		can_communityAdmin_policies = this
		            .getBooleanProperty("core.authorization.community-admin.policies",
		                    true);
		
		can_communityAdmin_adminGroup = this
		            .getBooleanProperty(
		                    "core.authorization.community-admin.admin-group", true);
		can_communityAdmin_collectionPolicies = this
		            .getBooleanProperty(
		                    "core.authorization.community-admin.collection.policies",
		                    true);
		
		can_communityAdmin_collectionTemplateItem = this
		            .getBooleanProperty(
		                    "core.authorization.community-admin.collection.template-item",
		                    true);
		can_communityAdmin_collectionSubmitters = this
		            .getBooleanProperty(
		                    "core.authorization.community-admin.collection.submitters",
		                    true);
		
		can_communityAdmin_collectionWorkflows = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.collection.workflows",
	                    true);
		can_communityAdmin_collectionAdminGroup = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.collection.admin-group",
	                    true);
		can_communityAdmin_itemDelete = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.item.delete", true);
		
		can_communityAdmin_itemWithdraw = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.item.withdraw", true);
		
		can_communityAdmin_itemReinstatiate = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.item.reinstatiate",
	                    true);
		can_communityAdmin_itemPolicies = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.item.policies", true);
		
		can_communityAdmin_itemCreateBitstream = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.item.create-bitstream",
	                    true);
		can_communityAdmin_itemDeleteBitstream = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.item.delete-bitstream",
	                    true);
		can_communityAdmin_itemAdminccLicense = this
	            .getBooleanProperty(
	                    "core.authorization.community-admin.item-admin.cc-license",
	                    true);
		can_collectionAdmin_policies = this
	            .getBooleanProperty("core.authorization.collection-admin.policies",
	                    true);
		can_collectionAdmin_templateItem = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.template-item", true);
		can_collectionAdmin_submitters = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.submitters", true);
		can_collectionAdmin_workflows = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.workflows", true);
		can_collectionAdmin_adminGroup = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.admin-group", true);
		can_collectionAdmin_itemDelete = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.item.delete", true);
		can_collectionAdmin_itemWithdraw = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.item.withdraw", true);
		can_collectionAdmin_itemReinstatiate = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.item.reinstatiate",
	                    true);
		can_collectionAdmin_itemPolicies = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.item.policies", true);
		can_collectionAdmin_itemCreateBitstream = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.item.create-bitstream",
	                    true);
		can_collectionAdmin_itemDeleteBitstream = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.item.delete-bitstream",
	                    true);
		can_collectionAdmin_itemAdminccLicense = this
	            .getBooleanProperty(
	                    "core.authorization.collection-admin.item-admin.cc-license",
	                    true);
		can_itemAdmin_policies = this
	            .getBooleanProperty("core.authorization.item-admin.policies", true);
		can_itemAdmin_createBitstream = this
	            .getBooleanProperty(
	                    "core.authorization.item-admin.create-bitstream", true);
		can_itemAdmin_deleteBitstream = this
	            .getBooleanProperty(
	                    "core.authorization.item-admin.delete-bitstream", true);
		can_itemAdmin_ccLicense = this
	            .getBooleanProperty("core.authorization.item-admin.cc-license",
	                    true);
	}
	
	private boolean getBooleanProperty (String param, boolean def) {
		Boolean b = this.config.getPropertyAsType(param, Boolean.valueOf(def));
		if (b == null) return false;
		else return b.booleanValue();
	}

	private static boolean can_communityAdmin_group;

    // subcommunities and collections
    private static boolean can_communityAdmin_createSubelement;

    private static boolean can_communityAdmin_deleteSubelement;

    private static boolean can_communityAdmin_policies;

    private static boolean can_communityAdmin_adminGroup;

    private static boolean can_communityAdmin_collectionPolicies;

    private static boolean can_communityAdmin_collectionTemplateItem;

    private static boolean can_communityAdmin_collectionSubmitters;

    private static boolean can_communityAdmin_collectionWorkflows;

    private static boolean can_communityAdmin_collectionAdminGroup;

    private static boolean can_communityAdmin_itemDelete;

    private static boolean can_communityAdmin_itemWithdraw;

    private static boolean can_communityAdmin_itemReinstatiate;

    private static boolean can_communityAdmin_itemPolicies;

    // # also bundle
    private static boolean can_communityAdmin_itemCreateBitstream;

    private static boolean can_communityAdmin_itemDeleteBitstream;

    private static boolean can_communityAdmin_itemAdminccLicense;

    // # COLLECTION ADMIN
    private static boolean can_collectionAdmin_policies;

    private static boolean can_collectionAdmin_templateItem;

    private static boolean can_collectionAdmin_submitters;

    private static boolean can_collectionAdmin_workflows;

    private static boolean can_collectionAdmin_adminGroup;

    private static boolean can_collectionAdmin_itemDelete;

    private static boolean can_collectionAdmin_itemWithdraw;

    private static boolean can_collectionAdmin_itemReinstatiate;
    
    private static boolean can_collectionAdmin_itemPolicies;

    // # also bundle
    private static boolean can_collectionAdmin_itemCreateBitstream;

    private static boolean can_collectionAdmin_itemDeleteBitstream;

    private static boolean can_collectionAdmin_itemAdminccLicense;

    // # ITEM ADMIN
    private static boolean can_itemAdmin_policies;

    // # also bundle
    private static boolean can_itemAdmin_createBitstream;

    private static boolean can_itemAdmin_deleteBitstream;

    private static boolean can_itemAdmin_ccLicense;

    /**
     * Are community admins allowed to create new, not strictly community
     * related, group?
     */
    public boolean canCommunityAdminPerformGroupCreation()
    {
        return can_communityAdmin_group;
    }

    /**
     * Are community admins allowed to create collections or subcommunities?
     */
    public boolean canCommunityAdminPerformSubelementCreation()
    {
        return can_communityAdmin_createSubelement;
    }

    /**
     * Are community admins allowed to remove collections or subcommunities?
     */
    public boolean canCommunityAdminPerformSubelementDeletion()
    {
        return can_communityAdmin_deleteSubelement;
    }

    /**
     * Are community admins allowed to manage the community's and
     * subcommunities' policies?
     */
    public boolean canCommunityAdminManagePolicies()
    {
        return can_communityAdmin_policies;
    }

    /**
     * Are community admins allowed to create/edit them community's and
     * subcommunities' admin groups?
     */
    public boolean canCommunityAdminManageAdminGroup()
    {
        return can_communityAdmin_adminGroup;
    }

    /**
     * Are community admins allowed to create/edit the community's and
     * subcommunities' admin group?
     */
    public boolean canCommunityAdminManageCollectionPolicies()
    {
        return can_communityAdmin_collectionPolicies;
    }

    /**
     * Are community admins allowed to manage the item template of them
     * collections?
     */
    public boolean canCommunityAdminManageCollectionTemplateItem()
    {
        return can_communityAdmin_collectionTemplateItem;
    }

    /**
     * Are community admins allowed to manage (create/edit/remove) the
     * submitters group of them collections?
     */
    public boolean canCommunityAdminManageCollectionSubmitters()
    {
        return can_communityAdmin_collectionSubmitters;
    }

    /**
     * Are community admins allowed to manage (create/edit/remove) the workflows
     * group of them collections?
     */
    public boolean canCommunityAdminManageCollectionWorkflows()
    {
        return can_communityAdmin_collectionWorkflows;
    }

    /**
     * Are community admins allowed to manage (create/edit/remove) the admin
     * group of them collections?
     */
    public boolean canCommunityAdminManageCollectionAdminGroup()
    {
        return can_communityAdmin_collectionAdminGroup;
    }

    /**
     * Are community admins allowed to remove an item from them collections?
     */
    public boolean canCommunityAdminPerformItemDeletion()
    {
        return can_communityAdmin_itemDelete;
    }

    /**
     * Are community admins allowed to withdrawn an item from them collections?
     */
    public boolean canCommunityAdminPerformItemWithdrawn()
    {
        return can_communityAdmin_itemWithdraw;
    }

    /**
     * Are community admins allowed to reinstate an item from them
     * collections?
     */
    public boolean canCommunityAdminPerformItemReinstatiate()
    {
        return can_communityAdmin_itemReinstatiate;
    }

    /**
     * Are community admins allowed to manage the policies of an item owned by
     * one of them collections?
     */
    public boolean canCommunityAdminManageItemPolicies()
    {
        return can_communityAdmin_itemPolicies;
    }

    /**
     * Are community admins allowed to add a bitstream to an item owned by one
     * of them collections?
     */
    public boolean canCommunityAdminPerformBitstreamCreation()
    {
        return can_communityAdmin_itemCreateBitstream;
    }

    /**
     * Are community admins allowed to remove a bitstream from an item owned by
     * one of them collections?
     */
    public boolean canCommunityAdminPerformBitstreamDeletion()
    {
        return can_communityAdmin_itemDeleteBitstream;
    }

    /**
     * Are community admins allowed to perform CC License replace or addition to
     * an item owned by one of them collections?
     */
    public boolean canCommunityAdminManageCCLicense()
    {
        return can_communityAdmin_itemAdminccLicense;
    }

    /**
     * Are collection admins allowed to manage the collection's policies?
     */
    public boolean canCollectionAdminManagePolicies()
    {
        return can_collectionAdmin_policies;
    }

    /**
     * Are collection admins allowed to manage (create/edit/delete) the
     * collection's item template?
     */
    public boolean canCollectionAdminManageTemplateItem()
    {
        return can_collectionAdmin_templateItem;
    }

    /**
     * Are collection admins allowed to manage (create/edit/delete) the
     * collection's submitters group?
     */
    public boolean canCollectionAdminManageSubmitters()
    {
        return can_collectionAdmin_submitters;
    }

    /**
     * Are collection admins allowed to manage (create/edit/delete) the
     * collection's workflows group?
     */
    public boolean canCollectionAdminManageWorkflows()
    {
        return can_collectionAdmin_workflows;
    }

    /**
     * Are collection admins allowed to manage (create/edit) the collection's
     * admins group?
     */
    public boolean canCollectionAdminManageAdminGroup()
    {
        return can_collectionAdmin_adminGroup;
    }

    /**
     * Are collection admins allowed to remove an item from the collection?
     */
    public boolean canCollectionAdminPerformItemDeletion()
    {
        return can_collectionAdmin_itemDelete;
    }

    /**
     * Are collection admins allowed to withdrawn an item from the collection?
     */
    public boolean canCollectionAdminPerformItemWithdrawn()
    {
        return can_collectionAdmin_itemWithdraw;
    }

    /**
     * Are collection admins allowed to reinstate an item from the
     * collection?
     */
    public boolean canCollectionAdminPerformItemReinstatiate()
    {
        return can_collectionAdmin_itemReinstatiate;
    }

    /**
     * Are collection admins allowed to manage the policies of item owned by the
     * collection?
     */
    public boolean canCollectionAdminManageItemPolicies()
    {
        return can_collectionAdmin_itemPolicies;
    }

    /**
     * Are collection admins allowed to add a bitstream to an item owned by the
     * collections?
     */
    public boolean canCollectionAdminPerformBitstreamCreation()
    {
        return can_collectionAdmin_itemCreateBitstream;
    }

    /**
     * Are collection admins allowed to remove a bitstream from an item owned by
     * the collections?
     */
    public boolean canCollectionAdminPerformBitstreamDeletion()
    {
        return can_collectionAdmin_itemDeleteBitstream;
    }

    /**
     * Are collection admins allowed to replace or adding a CC License to an
     * item owned by the collections?
     */
    public boolean canCollectionAdminManageCCLicense()
    {
        return can_collectionAdmin_itemAdminccLicense;
    }

    /**
     * Are item admins allowed to manage the item's policies?
     */
    public boolean canItemAdminManagePolicies()
    {
        return can_itemAdmin_policies;
    }

    /**
     * Are item admins allowed to add bitstreams to the item?
     */
    public boolean canItemAdminPerformBitstreamCreation()
    {
        return can_itemAdmin_createBitstream;
    }

    /**
     * Are item admins allowed to remove bitstreams from the item?
     */
    public boolean canItemAdminPerformBitstreamDeletion()
    {
        return can_itemAdmin_deleteBitstream;
    }

    /**
     * Are item admins allowed to replace or adding CC License to the item?
     */
    public boolean canItemAdminManageCCLicense()
    {
        return can_itemAdmin_ccLicense;
    }
}
