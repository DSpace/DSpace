/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.service.*;
import org.dspace.core.*;
import org.dspace.core.service.LicenseService;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.event.Event;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import org.dspace.authorize.service.ResourcePolicyService;

/**
 * Service implementation for the Collection object.
 * This class is responsible for all business logic calls for the Collection object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CollectionServiceImpl extends DSpaceObjectServiceImpl<Collection> implements CollectionService {

    /** log4j category */
    private static final Logger log = Logger.getLogger(CollectionServiceImpl.class);

    @Autowired(required = true)
    protected CollectionDAO collectionDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected CommunityService communityService;
    @Autowired(required = true)
    protected GroupService groupService;

    @Autowired(required = true)
    protected LicenseService licenseService;
    @Autowired(required = true)
    protected SubscribeService subscribeService;
    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;
    @Autowired(required=true)
    protected HarvestedCollectionService harvestedCollectionService;


    protected CollectionServiceImpl()
    {
        super();
    }

    @Override
    public Collection create(Context context, Community community) throws SQLException, AuthorizeException {
        return create(context, community, null);
    }

    @Override
    public Collection create(Context context, Community community, String handle) throws SQLException, AuthorizeException {
        if(community == null)
        {
            throw new IllegalArgumentException("Community cannot be null when creating a new collection.");
        }

        Collection newCollection = collectionDAO.create(context, new Collection());
        //Add our newly created collection to our community, authorization checks occur in THIS method
        communityService.addCollection(context, community, newCollection);

                //Update our community so we have a collection identifier
        if(handle == null)
        {
            handleService.createHandle(context, newCollection);
        }else{
            handleService.createHandle(context, newCollection, handle);
        }

                // create the default authorization policy for collections
        // of 'anonymous' READ
        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);


        authorizeService.createResourcePolicy(context, newCollection, anonymousGroup, null, Constants.READ, null);
        // now create the default policies for submitted items
        authorizeService.createResourcePolicy(context, newCollection, anonymousGroup, null, Constants.DEFAULT_ITEM_READ, null);
        authorizeService.createResourcePolicy(context, newCollection, anonymousGroup, null, Constants.DEFAULT_BITSTREAM_READ, null);



        context.addEvent(new Event(Event.CREATE, Constants.COLLECTION,
                newCollection.getID(), newCollection.getHandle(), getIdentifiers(context, newCollection)));

        log.info(LogManager.getHeader(context, "create_collection",
                "collection_id=" + newCollection.getID())
                + ",handle=" + newCollection.getHandle());

        collectionDAO.save(context, newCollection);
        return newCollection;
    }

    @Override
    public List<Collection> findAll(Context context) throws SQLException {
        MetadataField nameField = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null);
        if(nameField==null)
        {
            throw new IllegalArgumentException("Required metadata field '" + MetadataSchema.DC_SCHEMA + ".title' doesn't exist!");
        }

        return collectionDAO.findAll(context, nameField);
    }

    @Override
    public List<Collection> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        MetadataField nameField = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null);
        if(nameField==null)
        {
            throw new IllegalArgumentException("Required metadata field '" + MetadataSchema.DC_SCHEMA + ".title' doesn't exist!");
        }

        return collectionDAO.findAll(context, nameField, limit, offset);
    }

    @Override
    public List<Collection> findAuthorizedOptimized(Context context, int actionID) throws SQLException {
        if(! ConfigurationManager.getBooleanProperty("org.dspace.content.Collection.findAuthorizedPerformanceOptimize", false)) {
            // Fallback to legacy query if config says so. The rationale could be that a site found a bug.
            return findAuthorized(context, null, actionID);
        }

        List<Collection> myResults = new ArrayList<>();

        if(authorizeService.isAdmin(context))
        {
            return findAll(context);
        }

        //Check eperson->policy
        List<Collection> directToCollection = findDirectMapped(context, actionID);
        for (int i = 0; i< directToCollection.size(); i++)
        {
            if(!myResults.contains(directToCollection.get(i)))
            {
                myResults.add(directToCollection.get(i));
            }
        }

        //Check eperson->groups->policy
        List<Collection> groupToCollection = findGroupMapped(context, actionID);

        for (Collection aGroupToCollection : groupToCollection) {
            if (!myResults.contains(aGroupToCollection)) {
                myResults.add(aGroupToCollection);
            }
        }

        //Check eperson->groups->groups->policy->collection
        //i.e. Malcolm Litchfield is a member of OSU_Press_Embargo,
        // which is a member of: COLLECTION_24_ADMIN, COLLECTION_24_SUBMIT
        List<Collection> group2GroupToCollection = findGroup2GroupMapped(context, actionID);

        for (Collection aGroup2GroupToCollection : group2GroupToCollection) {
            if (!myResults.contains(aGroup2GroupToCollection)) {
                myResults.add(aGroup2GroupToCollection);
            }
        }

        //TODO Check eperson->groups->groups->policy->community


        //TODO Check eperson->groups->policy->community
        // i.e. Typical Community Admin -- name.# > COMMUNITY_10_ADMIN > Ohio State University Press

        //Check eperson->comm-admin
        List<Collection> group2commCollections = findGroup2CommunityMapped(context);
        for (Collection group2commCollection : group2commCollections) {
            if (!myResults.contains(group2commCollection)) {
                myResults.add(group2commCollection);
            }
        }


        // Return the collections, sorted alphabetically
        Collections.sort(myResults, new CollectionNameComparator());

        return myResults;
    }

    @Override
    public List<Collection> findDirectMapped(Context context, int actionID) throws SQLException {
        return collectionDAO.findAuthorized(context, context.getCurrentUser(), Arrays.asList(Constants.ADD,Constants.ADMIN));
    }

    @Override
    public List<Collection> findGroup2CommunityMapped(Context context) throws SQLException {
        List<Community> communities = communityService.findAuthorizedGroupMapped(context, Arrays.asList(Constants.ADD, Constants.ADMIN));
        List<Collection> collections = new ArrayList<>();
        for (Community community : communities) {
            collections.addAll(community.getCollections());
        }
        return collections;
    }

    @Override
    public List<Collection> findGroup2GroupMapped(Context context, int actionID) throws SQLException {
        return collectionDAO.findAuthorizedByGroup(context, context.getCurrentUser(), Collections.singletonList(actionID));
    }

    @Override
    public List<Collection> findGroupMapped(Context context, int actionID) throws SQLException {
        List<Community> communities = communityService.findAuthorized(context, Arrays.asList(Constants.ADD, Constants.ADMIN));
        List<Collection> collections = new ArrayList<>();
        for (Community community : communities) {
            collections.addAll(community.getCollections());
        }
        return collections;
    }

    @Override
    public Collection find(Context context, UUID id) throws SQLException {
        return collectionDAO.findByID(context, Collection.class, id);
    }

    @Override
    public void setMetadata(Context context, Collection collection, String field, String value) throws MissingResourceException, SQLException {
        if ((field.trim()).equals("name") && (value == null || value.trim().equals("")))
        {
            try
            {
                value = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e)
            {
                value = "Untitled";
            }
        }

        String[] MDValue = getMDValueByLegacyField(field);

        /*
         * Set metadata field to null if null
         * and trim strings to eliminate excess
         * whitespace.
         */
		if(value == null)
        {
            clearMetadata(context, collection, MDValue[0], MDValue[1], MDValue[2], Item.ANY);
            collection.setMetadataModified();
        }
        else
        {
            setMetadataSingleValue(context, collection, MDValue[0], MDValue[1], MDValue[2], null, value);
        }

        collection.addDetails(field);
    }

    @Override
    public Bitstream setLogo(Context context, Collection collection, InputStream is) throws AuthorizeException, IOException, SQLException {
        // Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && authorizeService.authorizeActionBoolean(
                context, collection, Constants.DELETE)))
        {
            canEdit(context, collection, true);
        }

        // First, delete any existing logo
        if (collection.getLogo() != null)
        {
            bitstreamService.delete(context, collection.getLogo());
        }

        if (is == null)
        {
            collection.setLogo(null);
            log.info(LogManager.getHeader(context, "remove_logo",
                    "collection_id=" + collection.getID()));
        }
        else
        {
            Bitstream newLogo = bitstreamService.create(context, is);
            collection.setLogo(newLogo);

            // now create policy for logo bitstream
            // to match our READ policy
            List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, collection, Constants.READ);
            authorizeService.addPolicies(context, policies, newLogo);

            log.info(LogManager.getHeader(context, "set_logo",
                    "collection_id=" + collection.getID() + "logo_bitstream_id="
                            + newLogo.getID()));
        }

        collection.setModified();
        return collection.getLogo();
    }

    @Override
    public Group createWorkflowGroup(Context context, Collection collection, int step) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to create Workflow Group
        AuthorizeUtil.authorizeManageWorkflowsGroup(context, collection);

        if (getWorkflowGroup(collection, step) == null)
        {
            //turn off authorization so that Collection Admins can create Collection Workflow Groups
            context.turnOffAuthorisationSystem();
            Group g = groupService.create(context);
            context.restoreAuthSystemState();

            groupService.setName(g,
                    "COLLECTION_" + collection.getID() + "_WORKFLOW_STEP_" + step);
            groupService.update(context, g);
            setWorkflowGroup(context, collection, step, g);

        }

        return getWorkflowGroup(collection, step);
    }

    @Override
    public void setWorkflowGroup(Context context, Collection collection, int step, Group group)
            throws SQLException, AuthorizeException
    {
        // we need to store the old group to be able to revoke permissions if granted before
        Group oldGroup = null;
        int action;
        
        switch (step)
        {
            case 1:
                oldGroup = collection.getWorkflowStep1();
                action = Constants.WORKFLOW_STEP_1;
                collection.setWorkflowStep1(group);
                break;
            case 2:
                oldGroup = collection.getWorkflowStep2();
                action = Constants.WORKFLOW_STEP_2;
                collection.setWorkflowStep2(group);
                break;
            case 3:
                oldGroup = collection.getWorkflowStep3();
                action = Constants.WORKFLOW_STEP_3;
                collection.setWorkflowStep3(group);
                break;
            default:
                throw new IllegalArgumentException("Illegal step count: " + step);
        }
        
        // deal with permissions.
        try
        {
            context.turnOffAuthorisationSystem();
            // remove the policies for the old group
            if (oldGroup != null)
            {
                Iterator<ResourcePolicy> oldPolicies =
                        resourcePolicyService.find(context, collection, oldGroup, action).iterator();
                while (oldPolicies.hasNext())
                {
                    resourcePolicyService.delete(context, oldPolicies.next());
                }
                oldPolicies = resourcePolicyService.find(context, collection, oldGroup, Constants.ADD).iterator();
                while (oldPolicies.hasNext())
                {
                    ResourcePolicy rp = oldPolicies.next();
                    if (rp.getRpType() == ResourcePolicy.TYPE_WORKFLOW)
                    {
                        resourcePolicyService.delete(context, rp);
                    }
                }
            }
            
            // group can be null to delete workflow step.
            // we need to grant permissions if group is not null
            if (group != null)
            {
                authorizeService.addPolicy(context, collection, action, group, ResourcePolicy.TYPE_WORKFLOW);
                authorizeService.addPolicy(context, collection, Constants.ADD, group, ResourcePolicy.TYPE_WORKFLOW);
            }
        } finally {
            context.restoreAuthSystemState();
        }
        collection.setModified();
    }

    @Override
    public Group getWorkflowGroup(Collection collection, int step) {
        switch (step)
        {
            case 1:
                return collection.getWorkflowStep1();
            case 2:
                return collection.getWorkflowStep2();
            case 3:
                return collection.getWorkflowStep3();
            default:
                throw new IllegalStateException("Illegal step count: " + step);
        }
    }

    /**
     * Get the value of a metadata field
     *
     * @param collection
     * @param field
     *            the name of the metadata field to get
     *
     * @return the value of the metadata field
     *
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     */
    @Override
    @Deprecated
    public String getMetadata(Collection collection, String field)
    {
        String[] MDValue = getMDValueByLegacyField(field);
        String value = getMetadataFirstValue(collection, MDValue[0], MDValue[1], MDValue[2], Item.ANY);
        return value == null ? "" : value;
    }

    @Override
    public Group createSubmitters(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to create Submitters Group
        AuthorizeUtil.authorizeManageSubmittersGroup(context, collection);

        Group submitters = collection.getSubmitters();
        if (submitters == null)
        {
            //turn off authorization so that Collection Admins can create Collection Submitters
            context.turnOffAuthorisationSystem();
            submitters = groupService.create(context);
            context.restoreAuthSystemState();

            groupService.setName(submitters,
                    "COLLECTION_" + collection.getID() + "_SUBMIT");
            groupService.update(context, submitters);
        }

        // register this as the submitter group
        collection.setSubmitters(submitters);

        authorizeService.addPolicy(context, collection, Constants.ADD, submitters);

        return submitters;
    }

    @Override
    public void removeSubmitters(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to delete Submitters Group
        AuthorizeUtil.authorizeManageSubmittersGroup(context, collection);

        // just return if there is no administrative group.
        if (collection.getSubmitters() == null)
        {
            return;
        }

        // Remove the link to the collection table.
        collection.setSubmitters(null);
    }

    @Override
    public Group createAdministrators(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to create more Admins
        AuthorizeUtil.authorizeManageAdminGroup(context, collection);

        Group admins = collection.getAdministrators();
        if (admins == null)
        {
            //turn off authorization so that Community Admins can create Collection Admins
            context.turnOffAuthorisationSystem();
            admins = groupService.create(context);
            context.restoreAuthSystemState();

            groupService.setName(admins, "COLLECTION_" + collection.getID() + "_ADMIN");
            groupService.update(context, admins);
        }

        authorizeService.addPolicy(context, collection,
                Constants.ADMIN, admins);

        // register this as the admin group
        collection.setAdmins(admins);
        return admins;
    }

    @Override
    public void removeAdministrators(Context context, Collection collection) throws SQLException, AuthorizeException {
                // Check authorisation - Must be an Admin of the parent community to delete Admin Group
        AuthorizeUtil.authorizeRemoveAdminGroup(context, collection);

        Group admins = collection.getAdministrators();
        // just return if there is no administrative group.
        if (admins == null)
        {
            return;
        }

        // Remove the link to the collection table.
        collection.setAdmins(null);
    }

    @Override
    public String getLicense(Collection collection) {
        String license = getMetadata(collection, "license");

        if (license == null || license.trim().equals(""))
        {
            // Fallback to site-wide default
            license = licenseService.getDefaultSubmissionLicense();
        }

        return license;
    }

    @Override
    public boolean hasCustomLicense(Collection collection) {
        String license = collection.getLicenseCollection();
        return StringUtils.isNotBlank(license);
    }

    @Override
    public void createTemplateItem(Context context, Collection collection) throws SQLException, AuthorizeException {
                // Check authorisation
        AuthorizeUtil.authorizeManageTemplateItem(context, collection);

        if (collection.getTemplateItem() == null)
        {
            Item template = itemService.createTemplateItem(context, collection);
            collection.setTemplateItem(template);

            log.info(LogManager.getHeader(context, "create_template_item",
                    "collection_id=" + collection.getID() + ",template_item_id="
                            + template.getID()));
        }
    }

    @Override
    public void removeTemplateItem(Context context, Collection collection) throws SQLException, AuthorizeException, IOException {
                // Check authorisation
        AuthorizeUtil.authorizeManageTemplateItem(context, collection);

        Item template = collection.getTemplateItem();

        if (template != null)
        {
            log.info(LogManager.getHeader(context, "remove_template_item",
                    "collection_id=" + collection.getID() + ",template_item_id="
                            + template.getID()));
            // temporarily turn off auth system, we have already checked the permission on the top of the method
            // check it again will fail because we have already broken the relation between the collection and the item
            context.turnOffAuthorisationSystem();
            collection.setTemplateItem(null);
            itemService.delete(context, template);
            context.restoreAuthSystemState();
        }

        context.addEvent(new Event(Event.MODIFY, Constants.COLLECTION,
                collection.getID(), "remove_template_item", getIdentifiers(context, collection)));
    }

    @Override
    public void addItem(Context context, Collection collection, Item item) throws SQLException, AuthorizeException {
                // Check authorisation
        authorizeService.authorizeAction(context, collection, Constants.ADD);

        log.info(LogManager.getHeader(context, "add_item", "collection_id="
                + collection.getID() + ",item_id=" + item.getID()));

        // Create mapping
        // We do NOT add the item to the collection template since we would have to load in all our items
        // Instead we add the collection to an item which works in the same way.
        if(!item.getCollections().contains(collection))
        {
            item.addCollection(collection);
        }

        context.addEvent(new Event(Event.ADD, Constants.COLLECTION, collection.getID(),
                Constants.ITEM, item.getID(), item.getHandle(),
                getIdentifiers(context, collection)));
    }

    @Override
    public void removeItem(Context context, Collection collection, Item item) throws SQLException, AuthorizeException, IOException {
        // Check authorisation
        authorizeService.authorizeAction(context, collection, Constants.REMOVE);

        //Check if we orphaned our poor item
        if (item.getCollections().size() == 1)
        {
            // Orphan; delete it
            itemService.delete(context, item);
        } else {
            //Remove the item from the collection if we have multiple collections
            item.removeCollection(collection);

        }

        context.addEvent(new Event(Event.REMOVE, Constants.COLLECTION,
                collection.getID(), Constants.ITEM, item.getID(), item.getHandle(),
                getIdentifiers(context, collection)));
    }

    @Override
    public void update(Context context, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation
        canEdit(context, collection, true);

        log.info(LogManager.getHeader(context, "update_collection",
                "collection_id=" + collection.getID()));

        super.update(context, collection);
        collectionDAO.save(context, collection);

        if (collection.isModified())
        {
            context.addEvent(new Event(Event.MODIFY, Constants.COLLECTION,
                    collection.getID(), null, getIdentifiers(context, collection)));
            collection.clearModified();
        }
        if (collection.isMetadataModified())
        {
            collection.clearDetails();
        }
    }

    @Override
    public boolean canEditBoolean(Context context, Collection collection) throws SQLException {
        return canEditBoolean(context, collection, true);
    }

    @Override
    public boolean canEditBoolean(Context context, Collection collection, boolean useInheritance) throws SQLException {
        try
        {
            canEdit(context, collection, useInheritance);

            return true;
        }
        catch (AuthorizeException e)
        {
            return false;
        }
    }

    @Override
    public void canEdit(Context context, Collection collection) throws SQLException, AuthorizeException {
        canEdit(context, collection, true);
    }

    @Override
    public void canEdit(Context context, Collection collection, boolean useInheritance) throws SQLException, AuthorizeException {
        List<Community> parents = communityService.getAllParents(context, collection);
        for (Community parent : parents) {
            if (authorizeService.authorizeActionBoolean(context, parent,
                    Constants.WRITE, useInheritance)) {
                return;
            }

            if (authorizeService.authorizeActionBoolean(context, parent,
                    Constants.ADD, useInheritance)) {
                return;
            }
        }
        authorizeService.authorizeAction(context, collection, Constants.WRITE, useInheritance);
    }

    @Override
    public void delete(Context context, Collection collection) throws SQLException, AuthorizeException, IOException {
        log.info(LogManager.getHeader(context, "delete_collection",
                "collection_id=" + collection.getID()));

        // remove harvested collections.
        HarvestedCollection hc = harvestedCollectionService.find(context,collection);
        if(hc!=null)
        {
            harvestedCollectionService.delete(context, hc);
        }

        context.addEvent(new Event(Event.DELETE, Constants.COLLECTION,
                collection.getID(), collection.getHandle(), getIdentifiers(context, collection)));

        // remove subscriptions - hmm, should this be in Subscription.java?
        subscribeService.deleteByCollection(context, collection);

        // Remove Template Item
        removeTemplateItem(context, collection);

        // Remove items
        // Remove items
        Iterator<Item> items = itemService.findAllByCollection(context, collection);
        while (items.hasNext())
        {
            Item item = items.next();
//            items.remove();
            if (itemService.isOwningCollection(item, collection))
            {
                // the collection to be deleted is the owning collection, thus remove
                // the item from all collections it belongs to
                itemService.delete(context, item);
            }
            // the item was only mapped to this collection, so just remove it
            else
            {
                removeItem(context, collection, item);
            }
        }


        // Delete bitstream logo
        setLogo(context, collection, null);

        Iterator<WorkspaceItem> workspaceItems = workspaceItemService.findByCollection(context, collection).iterator();
        while (workspaceItems.hasNext()) {
            WorkspaceItem workspaceItem = workspaceItems.next();
            workspaceItems.remove();
            workspaceItemService.deleteAll(context, workspaceItem);
        }


        WorkflowServiceFactory.getInstance().getWorkflowService().deleteCollection(context, collection);
        WorkflowServiceFactory.getInstance().getWorkflowItemService().deleteByCollection(context, collection);

        //  get rid of the content count cache if it exists
        // Remove any Handle
        handleService.unbindHandle(context, collection);

        // Remove any workflow groups - must happen after deleting collection
        Group g = collection.getWorkflowStep1();
        if (g != null)
        {
            collection.setWorkflowStep1(null);
            groupService.delete(context, g);
        }

        g = collection.getWorkflowStep2();

        if (g != null)
        {
            collection.setWorkflowStep2(null);
            groupService.delete(context, g);
        }

        g = collection.getWorkflowStep3();

        if (g != null)
        {
            collection.setWorkflowStep3(null);
            groupService.delete(context, g);
        }

        // Remove default administrators group
        g = collection.getAdministrators();

        if (g != null)
        {
            collection.setAdmins(null);
            groupService.delete(context, g);
        }

        // Remove default submitters group
        g = collection.getSubmitters();

        if (g != null)
        {
            collection.setSubmitters(null);
            groupService.delete(context, g);
        }

        Iterator<Community> owningCommunities = collection.getCommunities().iterator();
        while (owningCommunities.hasNext())
        {
            Community owningCommunity = owningCommunities.next();
            collection.removeCommunity(owningCommunity);
            owningCommunity.removeCollection(collection);
        }

        collectionDAO.delete(context, collection);
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.COLLECTION;
    }

    @Override
    public List<Collection> findAuthorized(Context context, Community community, int actionID) throws SQLException {
        List<Collection> myResults = new ArrayList<>();

        List<Collection> myCollections;

        if (community != null)
        {
            myCollections = community.getCollections();
        }
        else
        {
            myCollections = findAll(context);
        }

        // now build a list of collections you have authorization for
        for (Collection myCollection : myCollections) {
            if (authorizeService.authorizeActionBoolean(context,
                    myCollection, actionID)) {
                myResults.add(myCollection);
            }
        }
        return myResults;
    }

    @Override
    public Collection findByGroup(Context context, Group group) throws SQLException {
        return collectionDAO.findByGroup(context, group);
    }

    @Override
    public List<Collection> findCollectionsWithSubscribers(Context context) throws SQLException {
        return collectionDAO.findCollectionsWithSubscribers(context);
    }

    @Override
    public DSpaceObject getAdminObject(Context context, Collection collection, int action) throws SQLException {
        DSpaceObject adminObject = null;
        Community community = null;
        List<Community> communities = collection.getCommunities();
        if (CollectionUtils.isNotEmpty(communities))
        {
            community = communities.get(0);
        }

        switch (action)
        {
            case Constants.REMOVE:
                if (AuthorizeConfiguration.canCollectionAdminPerformItemDeletion())
                {
                    adminObject = collection;
                }
                else if (AuthorizeConfiguration.canCommunityAdminPerformItemDeletion())
                {
                    adminObject = community;
                }
                break;

            case Constants.DELETE:
                if (AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion())
                {
                    adminObject = community;
                }
                break;
            default:
                adminObject = collection;
                break;
        }
        return adminObject;
    }

    @Override
    public DSpaceObject getParentObject(Context context, Collection collection) throws SQLException {
        List<Community> communities = collection.getCommunities();
        if(CollectionUtils.isNotEmpty(communities)){
            return communities.get(0);
        }else{
            return null;
        }
    }

    @Override
    public void updateLastModified(Context context, Collection collection) throws SQLException, AuthorizeException {
        //Also fire a modified event since the collection HAS been modified
        context.addEvent(new Event(Event.MODIFY, Constants.COLLECTION,
                collection.getID(), null, getIdentifiers(context, collection)));
    }

    @Override
    public Collection findByIdOrLegacyId(Context context, String id) throws SQLException {
        if(StringUtils.isNumeric(id))
        {
            return findByLegacyId(context, Integer.parseInt(id));
        }
        else
        {
            return find(context, UUID.fromString(id));
        }
    }

    @Override
    public Collection findByLegacyId(Context context, int id) throws SQLException {
        return collectionDAO.findByLegacyId(context, id, Collection.class);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return collectionDAO.countRows(context);
    }

    @Override
    public List<Map.Entry<Collection, Long>> getCollectionsWithBitstreamSizesTotal(Context context) throws SQLException {
        return collectionDAO.getCollectionsWithBitstreamSizesTotal(context);
    }
}
