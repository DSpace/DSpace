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
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.event.Event;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * Service implementation for the Community object.
 * This class is responsible for all business logic calls for the Community object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class CommunityServiceImpl extends DSpaceObjectServiceImpl<Community> implements CommunityService {

    /** log4j category */
    private static Logger log = Logger.getLogger(CommunityServiceImpl.class);

    @Autowired(required = true)
    protected CommunityDAO communityDAO;


    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected SiteService siteService;

    protected CommunityServiceImpl()
    {
        super();

    }

    @Override
    public Community create(Community parent, Context context) throws SQLException, AuthorizeException {
        return create(parent, context, null);
    }

    @Override
    public Community create(Community parent, Context context, String handle) throws SQLException, AuthorizeException {
        if (!(authorizeService.isAdmin(context) ||
              (parent != null && authorizeService.authorizeActionBoolean(context, parent, Constants.ADD))))
        {
            throw new AuthorizeException(
                    "Only administrators can create communities");
        }

        Community newCommunity = communityDAO.create(context, new Community());

        try
        {
            if(handle == null)
            {
                handleService.createHandle(context, newCommunity);
            } else {
                handleService.createHandle(context, newCommunity, handle);
            }
        }
        catch(IllegalStateException ie)
        {
            //If an IllegalStateException is thrown, then an existing object is already using this handle
            throw ie;
        }

        if(parent != null)
        {
            parent.addSubCommunity(newCommunity);
            newCommunity.addParentCommunity(parent);
        }


        // create the default authorization policy for communities
        // of 'anonymous' READ
        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        authorizeService.createResourcePolicy(context, newCommunity, anonymousGroup, null, Constants.READ, null);

        communityDAO.save(context, newCommunity);

        context.addEvent(new Event(Event.CREATE, Constants.COMMUNITY, newCommunity.getID(), newCommunity.getHandle(), getIdentifiers(context, newCommunity)));

                // if creating a top-level Community, simulate an ADD event at the Site.
        if (parent == null)
        {
            context.addEvent(new Event(Event.ADD, Constants.SITE, siteService.findSite(context).getID(),
                    Constants.COMMUNITY, newCommunity.getID(), newCommunity.getHandle(),
                    getIdentifiers(context, newCommunity)));
        }

        log.info(LogManager.getHeader(context, "create_community",
                "community_id=" + newCommunity.getID())
                + ",handle=" + newCommunity.getHandle());

        return newCommunity;
    }

    @Override
    public Community find(Context context, UUID id) throws SQLException {
        return communityDAO.findByID(context, Community.class, id);
    }

    @Override
    public List<Community> findAll(Context context) throws SQLException {
        MetadataField sortField = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null);
        if(sortField==null)
        {
            throw new IllegalArgumentException("Required metadata field '" + MetadataSchema.DC_SCHEMA + ".title' doesn't exist!");
        }

        return communityDAO.findAll(context, sortField);
    }

    @Override
    public List<Community> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        MetadataField nameField = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null);
        if(nameField==null)
        {
            throw new IllegalArgumentException("Required metadata field '" + MetadataSchema.DC_SCHEMA + ".title' doesn't exist!");
        }

        return communityDAO.findAll(context, nameField, limit, offset);
    }

    @Override
    public List<Community> findAllTop(Context context) throws SQLException
    {
        // get all communities that are not children
        MetadataField sortField = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null);
        if(sortField==null)
        {
            throw new IllegalArgumentException("Required metadata field '" + MetadataSchema.DC_SCHEMA + ".title' doesn't exist!");
        }

        return communityDAO.findAllNoParent(context, sortField);
    }

    @Override
    public String getMetadata(Community community, String field) {
        String[] MDValue = getMDValueByLegacyField(field);
        String value = getMetadataFirstValue(community, MDValue[0], MDValue[1], MDValue[2], Item.ANY);
        return value == null ? "" : value;
    }

    @Override
    public void setMetadata(Context context, Community community, String field, String value) throws MissingResourceException, SQLException {
        if ((field.trim()).equals("name")
                && (value == null || value.trim().equals("")))
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
            clearMetadata(context, community, MDValue[0], MDValue[1], MDValue[2], Item.ANY);
        }
        else
        {
            setMetadataSingleValue(context, community, MDValue[0], MDValue[1], MDValue[2], null, value);
        }
        community.addDetails(field);
    }

    @Override
    public Bitstream setLogo(Context context, Community community, InputStream is) throws AuthorizeException, IOException, SQLException {
        // Check authorisation
        // authorized to remove the logo when DELETE rights
        // authorized when canEdit
        if (!((is == null) && authorizeService.authorizeActionBoolean(
                context, community, Constants.DELETE)))
        {
            canEdit(context, community);
        }

        // First, delete any existing logo
        Bitstream oldLogo = community.getLogo();
        if (oldLogo != null)
        {
            log.info(LogManager.getHeader(context, "remove_logo",
                    "community_id=" + community.getID()));
            community.setLogo(null);
            bitstreamService.delete(context, oldLogo);
        }

        if (is != null)
        {
            Bitstream newLogo = bitstreamService.create(context, is);
            community.setLogo(newLogo);

            // now create policy for logo bitstream
            // to match our READ policy
            List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, community, Constants.READ);
            authorizeService.addPolicies(context, policies, newLogo);

            log.info(LogManager.getHeader(context, "set_logo",
                    "community_id=" + community.getID() + "logo_bitstream_id="
                            + newLogo.getID()));
        }

        return community.getLogo();
    }

    @Override
    public void update(Context context, Community community) throws SQLException, AuthorizeException {
                // Check authorisation
        canEdit(context, community);

        log.info(LogManager.getHeader(context, "update_community",
                "community_id=" + community.getID()));

        super.update(context, community);

        communityDAO.save(context, community);
        if (community.isModified())
        {
            context.addEvent(new Event(Event.MODIFY, Constants.COMMUNITY, community.getID(), null, getIdentifiers(context, community)));
            community.setModified();
        }
        if (community.isMetadataModified())
        {
            context.addEvent(new Event(Event.MODIFY_METADATA, Constants.COMMUNITY, community.getID(), community.getDetails(), getIdentifiers(context, community)));
            community.clearModified();
        }
        community.clearDetails();
    }

    @Override
    public Group createAdministrators(Context context, Community community) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin to create more Admins
        AuthorizeUtil.authorizeManageAdminGroup(context, community);

        Group admins = community.getAdministrators();
        if (admins == null)
        {
            //turn off authorization so that Community Admins can create Sub-Community Admins
            context.turnOffAuthorisationSystem();
            admins = groupService.create(context);
            context.restoreAuthSystemState();

            groupService.setName(admins, "COMMUNITY_" + community.getID() + "_ADMIN");
            groupService.update(context, admins);
        }

        authorizeService.addPolicy(context, community, Constants.ADMIN, admins);

        // register this as the admin group
        community.setAdmins(admins);
        return admins;
    }

    @Override
    public void removeAdministrators(Context context, Community community) throws SQLException, AuthorizeException {
        // Check authorisation - Must be an Admin of the parent community (or system admin) to delete Admin group
        AuthorizeUtil.authorizeRemoveAdminGroup(context, community);

        // just return if there is no administrative group.
        if (community.getAdministrators() == null)
        {
            return;
        }

        // Remove the link to the community table.
        community.setAdmins(null);
    }

    @Override
    public List<Community> getAllParents(Context context, Community community) throws SQLException {
        List<Community> parentList = new ArrayList<Community>();
        Community parent = (Community) getParentObject(context, community);
        while (parent != null)
        {
            parentList.add(parent);
            parent = (Community) getParentObject(context, parent);
        }
        return parentList;
    }

    @Override
    public List<Community> getAllParents(Context context, Collection collection) throws SQLException {
        List<Community> result = new ArrayList<>();
        List<Community> communities = collection.getCommunities();
        result.addAll(communities);
        for (Community community : communities) {
            result.addAll(getAllParents(context, community));
        }
        return result;
    }

    @Override
    public List<Collection> getAllCollections(Context context, Community community) throws SQLException {
        List<Collection> collectionList = new ArrayList<Collection>();
        List<Community> subCommunities = community.getSubcommunities();
        for (Community subCommunity : subCommunities)
        {
            addCollectionList(subCommunity, collectionList);
        }

        List<Collection> collections = community.getCollections();
        for (Collection collection : collections)
        {
            collectionList.add(collection);
        }
        return collectionList;
    }


    /**
     * Internal method to process subcommunities recursively
     * @param community community
     * @param collectionList list of collections
     * @throws SQLException if database error
     */
    protected void addCollectionList(Community community, List<Collection> collectionList) throws SQLException
    {
        for (Community subcommunity : community.getSubcommunities())
        {
            addCollectionList(subcommunity, collectionList);
        }

        for (Collection collection : community.getCollections())
        {
            collectionList.add(collection);
        }
    }

    @Override
    public void addCollection(Context context, Community community, Collection collection) throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, community, Constants.ADD);

        log.info(LogManager.getHeader(context, "add_collection",
                "community_id=" + community.getID() + ",collection_id=" + collection.getID()));

        if(!community.getCollections().contains(collection))
        {
            community.addCollection(collection);
            collection.addCommunity(community);
        }
        context.addEvent(new Event(Event.ADD, Constants.COMMUNITY, community.getID(), Constants.COLLECTION, collection.getID(), community.getHandle(), getIdentifiers(context, community)));
    }

    @Override
    public Community createSubcommunity(Context context, Community parentCommunity) throws SQLException, AuthorizeException {
        return createSubcommunity(context, parentCommunity, null);
    }

    @Override
    public Community createSubcommunity(Context context, Community parentCommunity, String handle) throws SQLException, AuthorizeException {
                // Check authorisation
        authorizeService.authorizeAction(context, parentCommunity, Constants.ADD);

        Community c = create(parentCommunity, context, handle);
        addSubcommunity(context, parentCommunity, c);

        return c;
    }

    @Override
    public void addSubcommunity(Context context, Community parentCommunity, Community childCommunity) throws SQLException, AuthorizeException {
        // Check authorisation
        authorizeService.authorizeAction(context, parentCommunity, Constants.ADD);

        log.info(LogManager.getHeader(context, "add_subcommunity",
                "parent_comm_id=" + parentCommunity.getID() + ",child_comm_id=" + childCommunity.getID()));

        if(!parentCommunity.getSubcommunities().contains(childCommunity))
        {
            parentCommunity.addSubCommunity(childCommunity);
            childCommunity.addParentCommunity(parentCommunity);
        }
        context.addEvent(new Event(Event.ADD, Constants.COMMUNITY, parentCommunity.getID(), Constants.COMMUNITY, childCommunity.getID(),parentCommunity.getHandle(), getIdentifiers(context, parentCommunity)));
    }

    @Override
    public void removeCollection(Context context, Community community, Collection collection) throws SQLException, AuthorizeException, IOException {
        // Check authorisation
        authorizeService.authorizeAction(context, community, Constants.REMOVE);

        ArrayList<String> removedIdentifiers = collectionService.getIdentifiers(context, collection);
        String removedHandle = collection.getHandle();
        UUID removedId = collection.getID();

        if(collection.getCommunities().size() == 1)
        {
            collectionService.delete(context, collection);
        }else{
            community.removeCollection(collection);
            collection.removeCommunity(community);
        }

        log.info(LogManager.getHeader(context, "remove_collection",
                "community_id=" + community.getID() + ",collection_id=" + collection.getID()));

        // Remove any mappings
        context.addEvent(new Event(Event.REMOVE, Constants.COMMUNITY, community.getID(),
                Constants.COLLECTION, removedId, removedHandle, removedIdentifiers));
    }

    @Override
    public void removeSubcommunity(Context context, Community parentCommunity, Community childCommunity) throws SQLException, AuthorizeException, IOException {
                // Check authorisation
        authorizeService.authorizeAction(context, parentCommunity, Constants.REMOVE);

        ArrayList<String> removedIdentifiers = getIdentifiers(context, childCommunity);
        String removedHandle = childCommunity.getHandle();
        UUID removedId = childCommunity.getID();
        
        rawDelete(context, childCommunity);
        
        log.info(LogManager.getHeader(context, "remove_subcommunity",
                "parent_comm_id=" + parentCommunity.getID() + ",child_comm_id=" + childCommunity.getID()));

        context.addEvent(new Event(Event.REMOVE, Constants.COMMUNITY, parentCommunity.getID(), Constants.COMMUNITY, removedId, removedHandle, removedIdentifiers));
    }

    @Override
    public void delete(Context context, Community community) throws SQLException, AuthorizeException, IOException {
        // Check authorisation
        // FIXME: If this was a subcommunity, it is first removed from it's
        // parent.
        // This means the parentCommunity == null
        // But since this is also the case for top-level communities, we would
        // give everyone rights to remove the top-level communities.
        // The same problem occurs in removing the logo
        if (!authorizeService.authorizeActionBoolean(context, getParentObject(context, community), Constants.REMOVE))
        {
            authorizeService.authorizeAction(context, community, Constants.DELETE);
        }
        ArrayList<String> removedIdentifiers = getIdentifiers(context, community);
        String removedHandle = community.getHandle();
        UUID removedId = community.getID();


        // If not a top-level community, have parent remove me; this
        // will call rawDelete() before removing the linkage
        Community parent = (Community) getParentObject(context, community);

        if (parent != null)
        {
            // remove the subcommunities first
            Iterator<Community> subcommunities = community.getSubcommunities().iterator();
            while (subcommunities.hasNext()) {
                Community subCommunity = subcommunities.next();
                community.removeSubCommunity(subCommunity);
                delete(context, subCommunity);
            }
            // now let the parent remove the community
            removeSubcommunity(context, parent, community);

            return;
        }

        rawDelete(context, community);
        context.addEvent(new Event(Event.REMOVE, Constants.SITE, siteService.findSite(context).getID(), Constants.COMMUNITY, removedId, removedHandle, removedIdentifiers));

    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.COMMUNITY;
    }


    /**
     * Internal method to remove the community and all its children from the
     * database, and perform any pre/post-cleanup
     *
     * @param context context
     * @param community community
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     */
    protected void rawDelete(Context context, Community community) throws SQLException, AuthorizeException, IOException
    {
        log.info(LogManager.getHeader(context, "delete_community",
                "community_id=" + community.getID()));

        context.addEvent(new Event(Event.DELETE, Constants.COMMUNITY, community.getID(), community.getHandle(), getIdentifiers(context, community)));

        // Remove collections
        Iterator<Collection> collections = community.getCollections().iterator();

        while (collections.hasNext())
        {
            Collection collection = collections.next();
            community.removeCollection(collection);
            removeCollection(context, community, collection);
        }
        // delete subcommunities
        Iterator<Community> subCommunities = community.getSubcommunities().iterator();

        while (subCommunities.hasNext())
        {
            Community subComm = subCommunities.next();
            community.removeSubCommunity(subComm);
            delete(context, subComm);
        }

        // Remove the logo
        setLogo(context, community, null);

        // Remove any Handle
        handleService.unbindHandle(context, community);
        
        // Remove the parent-child relationship for the community we want ot delete
        Community parent = (Community) getParentObject(context, community);
        if (parent != null) {
            community.removeParentCommunity(parent);
            parent.removeSubCommunity(community);
        }

        Group g = community.getAdministrators();
        
        // Delete community row
        communityDAO.delete(context, community);

        // Remove administrators group - must happen after deleting community

        if (g != null)
        {
            groupService.delete(context, g);
        }
    }

    @Override
    public boolean canEditBoolean(Context context, Community community) throws SQLException {
        try
        {
            canEdit(context, community);

            return true;
        }
        catch (AuthorizeException e)
        {
            return false;
        }
    }

    @Override
    public void canEdit(Context context, Community community) throws AuthorizeException, SQLException {
        List<Community> parents = getAllParents(context, community);

        for (Community parent : parents) {
            if (authorizeService.authorizeActionBoolean(context, parent,
                    Constants.WRITE)) {
                return;
            }

            if (authorizeService.authorizeActionBoolean(context, parent,
                    Constants.ADD)) {
                return;
            }
        }

        authorizeService.authorizeAction(context, community, Constants.WRITE);
    }

    @Override
    public Community findByAdminGroup(Context context, Group group) throws SQLException {
        return communityDAO.findByAdminGroup(context, group);
    }

    @Override
    public List<Community> findAuthorized(Context context, List<Integer> actions) throws SQLException {
        return communityDAO.findAuthorized(context, context.getCurrentUser(), actions);
    }

    @Override
    public List<Community> findAuthorizedGroupMapped(Context context, List<Integer> actions) throws SQLException {
        return communityDAO.findAuthorizedByGroup(context, context.getCurrentUser(), actions);
    }

    @Override
	public DSpaceObject getAdminObject(Context context, Community community, int action) throws SQLException
    {
        DSpaceObject adminObject = null;
        switch (action)
        {
        case Constants.REMOVE:
            if (AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion())
            {
                adminObject = community;
            }
            break;

        case Constants.DELETE:
            if (AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion())
            {
                adminObject = getParentObject(context, community);
            }
            break;
        case Constants.ADD:
            if (AuthorizeConfiguration.canCommunityAdminPerformSubelementCreation())
            {
                adminObject = community;
            }
            break;
        default:
            adminObject = community;
            break;
        }
        return adminObject;
    }


    @Override
    public DSpaceObject getParentObject(Context context, Community community) throws SQLException
    {
        List<Community> parentCommunities = community.getParentCommunities();
        if (CollectionUtils.isNotEmpty(parentCommunities))
        {
            return parentCommunities.iterator().next();
        }
        else
        {
            return null;
        }
    }

    @Override
    public void updateLastModified(Context context, Community community) {
        //Also fire a modified event since the community HAS been modified
        context.addEvent(new Event(Event.MODIFY, Constants.COMMUNITY,
                community.getID(), null, getIdentifiers(context, community)));

    }

    @Override
    public Community findByIdOrLegacyId(Context context, String id) throws SQLException {
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
    public Community findByLegacyId(Context context, int id) throws SQLException {
        return communityDAO.findByLegacyId(context, id, Community.class);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return communityDAO.countRows(context);
    }
}
