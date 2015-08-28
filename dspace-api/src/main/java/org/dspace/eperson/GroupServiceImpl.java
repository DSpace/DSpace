/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.dao.Group2GroupCacheDAO;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.event.Event;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.*;

/**
 * Service implementation for the Group object.
 * This class is responsible for all business logic calls for the Group object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class GroupServiceImpl extends DSpaceObjectServiceImpl<Group> implements GroupService
{
    private static final Logger log = Logger.getLogger(GroupServiceImpl.class);

    @Autowired(required = true)
    protected GroupDAO groupDAO;

//    @Autowired(required = true)
//    protected Group2GroupDAO group2GroupDAO;

    @Autowired(required = true)
    protected Group2GroupCacheDAO group2GroupCacheDAO;

    @Autowired(required = true)
    protected CollectionService collectionService;

    @Autowired(required = true)
    protected EPersonService ePersonService;

    @Autowired(required = true)
    protected CommunityService communityService;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;


    @Override
    public Group create(Context context) throws SQLException, AuthorizeException {
        // FIXME - authorization?
        if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson Group");
        }

        // Create a table row
        Group g = groupDAO.create(context, new Group());

        log.info(LogManager.getHeader(context, "create_group", "group_id="
                + g.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.GROUP, g.getID(), null, getIdentifiers(context, g)));
        update(context, g);

        return g;
    }

    @Override
    public void setName(Context context, Group group, String name) throws SQLException {
        setMetadataSingleValue(context, group, MetadataSchema.DC_SCHEMA, "title", null, null, name);
    }

    @Override
    public void addMember(Context context, Group group, EPerson e) {
        if (isDirectMember(group, e))
        {
            return;
        }
        group.addMember(e);
        e.getGroups().add(group);
        context.addEvent(new Event(Event.ADD, Constants.GROUP, group.getID(), Constants.EPERSON, e.getID(), e.getEmail(), getIdentifiers(context, group)));
    }

    @Override
    public void addMember(Context context, Group groupParent, Group groupChild) throws SQLException {
                // don't add if it's already a member
        // and don't add itself
        if (groupParent.contains(groupChild) || groupParent.getID()==groupChild.getID())
        {
            return;
        }

        groupParent.addMember(groupChild);
        groupChild.addParentGroup(groupParent);

        context.addEvent(new Event(Event.ADD, Constants.GROUP, groupParent.getID(), Constants.GROUP, groupChild.getID(), groupChild.getName(), getIdentifiers(context, groupParent)));
    }

    @Override
    public void removeMember(Context context, Group group, EPerson ePerson) {
        if (group.remove(ePerson))
        {
            context.addEvent(new Event(Event.REMOVE, Constants.GROUP, group.getID(), Constants.EPERSON, ePerson.getID(), ePerson.getEmail(), getIdentifiers(context, group)));
        }
    }

    @Override
    public void removeMember(Context context, Group groupParent, Group childGroup) throws SQLException {
        if (groupParent.remove(childGroup))
        {
            childGroup.removeParentGroup(groupParent);
            context.addEvent(new Event(Event.REMOVE, Constants.GROUP, groupParent.getID(), Constants.GROUP, childGroup.getID(), childGroup.getName(), getIdentifiers(context, groupParent)));
        }
    }

    @Override
    public boolean isDirectMember(Group group, EPerson ePerson) {
        // special, group 0 is anonymous
        return StringUtils.equals(group.getName(), Group.ANONYMOUS) || group.contains(ePerson);
    }

    @Override
    public boolean isMember(Group owningGroup, Group childGroup) {
        return owningGroup.contains(childGroup);
    }

    @Override
    public boolean isMember(Context context, Group group) throws SQLException {
        // special, everyone is member of group 0 (anonymous)
        if (group.getName().equals(Group.ANONYMOUS))
        {
            return true;
        }

        EPerson currentuser = context.getCurrentUser();

        return epersonInGroup(context, group, currentuser);
    }

    @Override
    public List<Group> allMemberGroups(Context context, EPerson ePerson) throws SQLException {
        Set<Group> groups = new HashSet<Group>();

        if (ePerson != null)
        {
            // two queries - first to get groups eperson is a member of
            // second query gets parent groups for groups eperson is a member of
            groups.addAll(groupDAO.findByEPerson(context, ePerson));
        }
        // Also need to get all "Special Groups" user is a member of!
        // Otherwise, you're ignoring the user's membership to these groups!
        // However, we only do this is we are looking up the special groups
        // of the current user, as we cannot look up the special groups
        // of a user who is not logged in.
        if ((context.getCurrentUser() == null) || (context.getCurrentUser().equals(ePerson)))
        {
            List<Group> specialGroups = context.getSpecialGroups();
            for(Group special : specialGroups)
            {
                groups.add(special);
            }
        }

        // all the users are members of the anonymous group
        groups.add(findByName(context, Group.ANONYMOUS));


        List<Group2GroupCache> groupCache = group2GroupCacheDAO.findByChildren(context, groups);
        // now we have all owning groups, also grab all parents of owning groups
        // yes, I know this could have been done as one big query and a union,
        // but doing the Oracle port taught me to keep to simple SQL!
        for (Group2GroupCache group2GroupCache : groupCache) {
            groups.add(group2GroupCache.getParent());
        }

        return new ArrayList<>(groups);
    }

    @Override
    public List<EPerson> allMembers(Context c, Group g) throws SQLException
    {
        // two queries - first to get all groups which are a member of this group
        // second query gets all members of each group in the first query

        // Get all groups which are a member of this group
        List<Group2GroupCache> group2GroupCaches = group2GroupCacheDAO.findByParent(c, g);
        Set<Group> groups = new HashSet<Group>();
        for (Group2GroupCache group2GroupCache : group2GroupCaches) {
            groups.add(group2GroupCache.getChild());
        }


        Set<EPerson> childGroupChildren = new HashSet<>(ePersonService.findByGroups(c, groups));
        //Don't forget to add our direct children
        childGroupChildren.addAll(g.getMembers());

        return new ArrayList<>(childGroupChildren);
    }

    @Override
    public Group find(Context context, UUID id) throws SQLException {
        if(id == null) {
            return null;
        } else {
            return groupDAO.findByID(context, Group.class, id);
        }
    }

    @Override
    public Group findByName(Context context, String name) throws SQLException {
        if(name == null)
        {
            return null;
        }

        return groupDAO.findByMetadataField(context, name, metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null));
    }

    @Override
    public List<Group> findAll(Context context, int sortField) throws SQLException
    {
        List<MetadataField> metadataFieldsSort = new ArrayList<>();
        switch (sortField)
        {
            case NAME:
                metadataFieldsSort.add(metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null));

                break;

            default:
                metadataFieldsSort.add(metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null));
        }

        return groupDAO.findAll(context, metadataFieldsSort, null);
    }

    @Override
    public List<Group> search(Context context, String query) throws SQLException {
        return search(context, query, -1, -1);
    }

    @Override
    public List<Group> search(Context context, String query, int offset, int limit) throws SQLException
    {
        try {
            List<Group> groups = new ArrayList<>();
            Group group = find(context, UUID.fromString(query));
            if(group != null)
            {
                groups.add(group);
            }
            return groups;
        } catch(IllegalArgumentException e) {
            MetadataField nameField = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null);
            if(StringUtils.isBlank(query)) query = null;
            return groupDAO.search(context, query, Arrays.asList(nameField), offset, limit);
        }
    }

    @Override
    public int searchResultCount(Context context, String query) throws SQLException {
        try {
            return find(context, UUID.fromString(query)) != null ? 1 : 0;
        } catch(IllegalArgumentException e) {
            MetadataField nameField = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "title", null);
            if (StringUtils.isBlank(query)) query = null;
            return groupDAO.searchResultCount(context, query, Arrays.asList(nameField));
        }
    }

    @Override
    public void delete(Context context, Group group) throws SQLException {
        context.addEvent(new Event(Event.DELETE, Constants.GROUP, group.getID(), group.getName(), getIdentifiers(context, group)));

        //Remove the supervised group from any workspace items linked to us.
        group.getSupervisedItems().clear();

        // Remove any ResourcePolicies that reference this group
        authorizeService.removeGroupPolicies(context, group);

        group.getMemberGroups().clear();
        group.getParentGroups().clear();

        //Remove all eperson references from this group
        Iterator<EPerson> ePeople = group.getMembers().iterator();
        while (ePeople.hasNext()) {
            EPerson ePerson = ePeople.next();
            ePeople.remove();
            ePerson.getGroups().remove(group);
        }

        deleteMetadata(context, group);

        // empty out group2groupcache table (if we do it after we delete our object we get an issue with references)
        group2GroupCacheDAO.deleteAll(context);
        // Remove ourself
        groupDAO.delete(context, group);
        rethinkGroupCache(context, false);

        log.info(LogManager.getHeader(context, "delete_group", "group_id="
                + group.getID()));
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.GROUP;
    }

    /**
     * Return true if group has no direct or indirect members
     */
    @Override
    public boolean isEmpty(Group group)
    {
        // the only fast check available is on epeople...
        boolean hasMembers = (group.getMembers().size() != 0);

        if (hasMembers)
        {
            return false;
        }
        else
        {
            // well, groups is never null...
            for (Group subGroup : group.getMemberGroups()){
                hasMembers = !isEmpty(subGroup);
                if (hasMembers){
                    return false;
                }
            }
            return !hasMembers;
        }
    }

    @Override
    public void initDefaultGroupNames(Context context) throws SQLException, AuthorizeException {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        // Check for Anonymous group. If not found, create it
        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
        if(anonymousGroup==null)
        {
            anonymousGroup = groupService.create(context);
            anonymousGroup.setName(context, Group.ANONYMOUS);
            groupService.update(context, anonymousGroup);
        }


        // Check for Administrator group. If not found, create it
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        if(adminGroup == null)
        {
            adminGroup = groupService.create(context);
            adminGroup.setName(context, Group.ADMIN);
            groupService.update(context, adminGroup);
        }
    }

    /**
     * Update the group - writing out group object and EPerson list if necessary
     */
    @Override
    public void update(Context context, Group group) throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation
        groupDAO.save(context, group);

        if (group.isMetadataModified())
        {
            context.addEvent(new Event(Event.MODIFY_METADATA, Constants.GROUP, group.getID(), group.getDetails(), getIdentifiers(context, group)));
            group.clearDetails();
        }

        if(group.isGroupsChanged())
        {
            rethinkGroupCache(context, true);
            group.clearGroupsChanged();
        }

        log.info(LogManager.getHeader(context, "update_group", "group_id="
                + group.getID()));
    }




    protected boolean epersonInGroup(Context c, Group group, EPerson e)
            throws SQLException
    {
        List<Group> groups = allMemberGroups(c, e);

        return groups.contains(group);
    }


    /**
     * Regenerate the group cache AKA the group2groupcache table in the database -
     * meant to be called when a group is added or removed from another group
     *
     */
    protected void rethinkGroupCache(Context context, boolean flushQueries) throws SQLException {

        Map<UUID, Set<UUID>> parents = new HashMap<>();

        List<Object[]> group2groupResults = groupDAO.getGroup2GroupResults(context, flushQueries);
        for (Object[] group2groupResult : group2groupResults) {
            //We cannot use UUID.nameUUIDFromBytes(), because it only generated "type 3 UUIDs"
            UUID parent = (UUID) group2groupResult[0];
            UUID child = (UUID) group2groupResult[1];

            // if parent doesn't have an entry, create one
            if (!parents.containsKey(parent)) {
                Set<UUID> children = new HashSet<>();

                // add child id to the list
                children.add(child);
                parents.put(parent, children);
            } else {
                // parent has an entry, now add the child to the parent's record
                // of children
                Set<UUID> children = parents.get(parent);
                children.add(child);
            }
        }

        // now parents is a hash of all of the IDs of groups that are parents
        // and each hash entry is a hash of all of the IDs of children of those
        // parent groups
        // so now to establish all parent,child relationships we can iterate
        // through the parents hash
        for (Map.Entry<UUID, Set<UUID>> parent : parents.entrySet()) {
            Set<UUID> myChildren = getChildren(parents, parent.getKey());
            parent.getValue().addAll(myChildren);
        }

        // empty out group2groupcache table
        group2GroupCacheDAO.deleteAll(context);

        // write out new one
        for (Map.Entry<UUID, Set<UUID>> parent : parents.entrySet()) {
            UUID key = parent.getKey();

            for (UUID child : parent.getValue()) {

                Group parentGroup = find(context, key);
                Group childGroup = find(context, child);


                if(parentGroup != null && childGroup != null && group2GroupCacheDAO.find(context, parentGroup, childGroup) == null)
                {
                    Group2GroupCache group2GroupCache = group2GroupCacheDAO.create(context, new Group2GroupCache());
                    group2GroupCache.setParent(parentGroup);
                    group2GroupCache.setChild(childGroup);
                    group2GroupCacheDAO.save(context, group2GroupCache);
                }
            }
        }
    }

    @Override
    public DSpaceObject getParentObject(Context context, Group group) throws SQLException
    {
        if(group == null)
        {
            return null;
        }
        // could a collection/community administrator manage related groups?
        // check before the configuration options could give a performance gain
        // if all group management are disallowed
        if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup()
                || AuthorizeConfiguration.canCollectionAdminManageSubmitters()
                || AuthorizeConfiguration.canCollectionAdminManageWorkflows()
                || AuthorizeConfiguration.canCommunityAdminManageAdminGroup()
                || AuthorizeConfiguration
                .canCommunityAdminManageCollectionAdminGroup()
                || AuthorizeConfiguration
                .canCommunityAdminManageCollectionSubmitters()
                || AuthorizeConfiguration
                .canCommunityAdminManageCollectionWorkflows())
        {
            // is this a collection related group?
            org.dspace.content.Collection collection = collectionService.findByGroup(context, group);

            if (collection != null)
            {
                if ((group.equals(collection.getWorkflowStep1()) ||
                        group.equals(collection.getWorkflowStep2()) ||
                        group.equals(collection.getWorkflowStep3())))
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageWorkflows())
                    {
                        return collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionWorkflows())
                    {
                        return collectionService.getParentObject(context, collection);
                    }
                }
                if (group.equals(collection.getSubmitters()))
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageSubmitters())
                    {
                        return collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionSubmitters())
                    {
                        return collectionService.getParentObject(context, collection);
                    }
                }
                if (group.equals(collection.getAdministrators()))
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup())
                    {
                        return collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionAdminGroup())
                    {
                        return collectionService.getParentObject(context, collection);
                    }
                }
            }
            // is the group related to a community and community administrator allowed
            // to manage it?
            else if (AuthorizeConfiguration.canCommunityAdminManageAdminGroup())
            {
                return communityService.findByAdminGroup(context, group);
            }
        }
        return null;
    }

    @Override
    public void updateLastModified(Context context, Group dso) {
        //Not needed.
    }

    /**
     * Used recursively to generate a map of ALL of the children of the given
     * parent
     *
     * @param parents
     *            Map of parent,child relationships
     * @param parent
     *            the parent you're interested in
     * @return Map whose keys are all of the children of a parent
     */
    protected Set<UUID> getChildren(Map<UUID,Set<UUID>> parents, UUID parent)
    {
        Set<UUID> myChildren = new HashSet<>();

        // degenerate case, this parent has no children
        if (!parents.containsKey(parent))
        {
            return myChildren;
        }

        // got this far, so we must have children
        Set<UUID> children =  parents.get(parent);

        // now iterate over all of the children

        for (UUID child : children) {
            // add this child's ID to our return set
            myChildren.add(child);

            // and now its children
            myChildren.addAll(getChildren(parents, child));
        }

        return myChildren;
    }

    @Override
    public Group findByIdOrLegacyId(Context context, String id) throws SQLException {
        if(org.apache.commons.lang.StringUtils.isNumeric(id))
        {
            return findByLegacyId(context, Integer.parseInt(id));
        }
        else
        {
            return find(context, UUID.fromString(id));
        }
    }

    @Override
    public Group findByLegacyId(Context context, int id) throws SQLException {
        return groupDAO.findByLegacyId(context, id, Group.class);
    }
}
