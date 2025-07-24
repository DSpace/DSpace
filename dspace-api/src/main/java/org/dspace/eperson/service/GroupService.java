/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Service interface class for the Group object.
 * The implementation of this class is responsible for all business logic calls for the Group object and is autowired
 * by Spring.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface GroupService extends DSpaceObjectService<Group>, DSpaceObjectLegacySupportService<Group> {

    public static final int NAME = 1; // sort by NAME (default)


    /**
     * Create a new group
     *
     * @param context DSpace context object
     * @return group
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Group create(Context context) throws SQLException, AuthorizeException;

    /**
     * set name of group
     *
     * @param group DSpace group
     * @param name  new group name
     * @throws SQLException if database error
     */
    public void setName(Group group, String name) throws SQLException;

    /**
     * add an eperson member
     *
     * @param context DSpace context object
     * @param group   DSpace group
     * @param e       eperson
     */
    public void addMember(Context context, Group group, EPerson e);

    /**
     * add group to this group. Be sure to call the {@link #update(Context, Group)}
     * method once that all the membership are set to trigger the rebuild of the
     * group2group cache table
     *
     * @param context     DSpace context object
     * @param groupParent parent group
     * @param groupChild  child group
     * @throws SQLException if database error
     */
    public void addMember(Context context, Group groupParent, Group groupChild) throws SQLException;

    /**
     * remove an eperson from a group
     *
     * @param context DSpace context object
     * @param group   DSpace group
     * @param ePerson eperson
     */
    public void removeMember(Context context, Group group, EPerson ePerson) throws SQLException;


    /**
     * remove group from this group. Be sure to call the {@link #update(Context, Group)}
     * method once that all the membership are set to trigger the rebuild of the
     * group2group cache table
     *
     * @param context     DSpace context object
     * @param groupParent parent group
     * @param childGroup  child group
     * @throws SQLException if database error
     */
    public void removeMember(Context context, Group groupParent, Group childGroup) throws SQLException;

    /**
     * check to see if an eperson is a direct member.
     * If the eperson is a member via a subgroup will be returned <code>false</code>
     *
     * @param group   DSpace group
     * @param ePerson eperson to check membership
     * @return true or false
     */
    public boolean isDirectMember(Group group, EPerson ePerson);

    /**
     * Check to see if childGroup is a direct group member of owningGroup.
     * If childGroup is a subgroup via another group will be returned <code>false</code>
     *
     * @param owningGroup parent group
     * @param childGroup  child group
     * @return true or false
     */
    public boolean isMember(Group owningGroup, Group childGroup);

    /**
     * Check to see if parentGroup is a direct or in-direct parent of a childGroup.
     *
     * @param context current DSpace session.
     * @param parentGroup parent group
     * @param childGroup  child group
     * @return true or false
     * @throws java.sql.SQLException
     */
    public boolean isParentOf(Context context, Group parentGroup, Group childGroup) throws SQLException;

    /**
     * fast check to see if an eperson is a member called with eperson id. Does
     * database lookup without instantiating all of the eperson objects and is
     * thus a static method
     *
     * @param context context
     * @param group   group to check
     * @return true or false
     * @throws SQLException if database error
     */
    public boolean isMember(Context context, Group group) throws SQLException;

    /**
     * fast check to see if an eperson is a member called with eperson id. Does
     * database lookup without instantiating all of the eperson objects and is
     * thus a static method. This method uses context.getCurrentUser() as
     * eperson whose membership should be checked.
     *
     * @param context   context
     * @param groupName the name of the group to check
     * @return true or false
     * @throws SQLException if database error
     */
    public boolean isMember(Context context, String groupName) throws SQLException;

    /**
     * fast check to see if an eperson is a member called with eperson id. Does
     * database lookup without instantiating all of the eperson objects and is
     * thus a static method. The eperson whose membership should be checked must
     * be defined as method attribute.
     *
     * @param context   context
     * @param epersonToCheck is this EPerson a member of the group?
     * @param groupName the name of the group to check
     * @return true or false
     * @throws SQLException if database error
     */
    public boolean isMember(Context context, EPerson epersonToCheck, String groupName) throws SQLException;

    /**
     * fast check to see if an eperson is a member called with eperson id. Does
     * database lookup without instantiating all of the eperson objects and is
     * thus a static method.
     *
     * @param context DSpace context object.
     * @param eperson EPerson whose membership should be checked.
     * @param group   The group to check against.
     * @return true or false
     * @throws SQLException if database error
     */
    public boolean isMember(Context context, EPerson eperson, Group group) throws SQLException;

    /**
     * Get all of the groups that an eperson is a member of.
     *
     * @param context DSpace context
     * @param ePerson ePerson object
     * @return list of Group objects
     * @throws SQLException if database error
     */
    public List<Group> allMemberGroups(Context context, EPerson ePerson) throws SQLException;

    Set<Group> allMemberGroupsSet(Context context, EPerson ePerson) throws SQLException;

    /**
     * Get all of the EPerson objects who are a member of the specified group, or a member of a subgroup of the
     * specified group, etc.
     * <P>
     * WARNING: This method may have bad performance for Groups with a very large number of members, as it will load
     * all member EPerson objects into memory. Only use if you need access to *every* EPerson object at once.
     *
     * @param context The relevant DSpace Context.
     * @param group   Group object
     * @return List of EPerson objects
     * @throws SQLException if error
     */
    public List<EPerson> allMembers(Context context, Group group) throws SQLException;

    /**
     * Count all of the EPerson objects who are a member of the specified group, or a member of a subgroup of the
     * specified group, etc.
     * In other words, this will return the size of "allMembers()" without having to load all EPerson objects into
     * memory.
     * @param context current DSpace context
     * @param group Group object
     * @return count of EPerson object members
     * @throws SQLException if error
     */
    int countAllMembers(Context context, Group group) throws SQLException;

    /**
     * Find the group by its name - assumes name is unique
     *
     * @param context The relevant DSpace Context.
     * @param name    Group name to search for
     * @return the named Group, or null if not found
     * @throws SQLException if error
     */
    public Group findByName(Context context, String name) throws SQLException;

    /**
     * Finds all groups in the site
     *
     * @param context            The relevant DSpace Context.
     * @param metadataSortFields metadata fields to sort by, leave empty to sort by Name
     * @param pageSize           how many results return
     * @param offset             the position of the first result to return
     * @return List of all groups in the site
     * @throws SQLException if error
     */
    public List<Group> findAll(Context context, List<MetadataField> metadataSortFields, int pageSize, int offset)
        throws SQLException;

    /**
     * @param context            The relevant DSpace Context.
     * @param metadataSortFields metadata fields to sort by, leave empty to sort by Name
     * @return List of all groups in the site
     * @throws SQLException if error
     * @deprecated Please use {@code findAll(Context context, List<MetadataField> metadataFieldsSort, int pageSize,
     * int offset)} instead
     */
    @Deprecated
    public List<Group> findAll(Context context, List<MetadataField> metadataSortFields) throws SQLException;

    /**
     * DEPRECATED: Please use {@code findAll(Context context, List<MetadataField> metadataFieldsSort)} instead
     *
     * @param context   DSpace context
     * @param sortField sort field index
     * @return List of all groups in the site
     * @throws SQLException if error
     * @deprecated
     */
    @Deprecated
    public List<Group> findAll(Context context, int sortField) throws SQLException;

    /**
     * Find the Groups that match the query across both Group name and Group ID.  This is an unpaginated search,
     * which means it will load all matching groups into memory at once. This may provide POOR PERFORMANCE when a large
     * number of groups are matched.
     *
     * @param context   DSpace context
     * @param query     The search string used to search across group name or group ID
     * @return List of matching Group objects
     * @throws SQLException if error
     */
    List<Group> search(Context context, String query) throws SQLException;

    /**
     * Find the Groups that match the query across both Group name and Group ID. This method supports pagination,
     * which provides better performance than the above non-paginated search() method.
     *
     * @param context   DSpace context
     * @param query     The search string used to search across group name or group ID
     * @param offset    Inclusive offset (the position of the first result to return)
     * @param limit     Maximum number of matches returned
     * @return List of matching Group objects
     * @throws SQLException if error
     */
    List<Group> search(Context context, String query, int offset, int limit) throws SQLException;

    /**
     * Returns the total number of Groups returned by a specific query. Search is performed based on Group name
     * and Group ID. May be used with search() above to support pagination of matching Groups.
     *
     * @param context DSpace context
     * @param query   The search string used to search across group name or group ID
     * @return the number of groups matching the query
     * @throws SQLException if error
     */
    int searchResultCount(Context context, String query) throws SQLException;

    /**
     * Find the groups that match the search query which are NOT currently members (subgroups)
     * of the given parentGroup
     *
     * @param context               DSpace context
     * @param query                 The search string used to search across group name or group ID
     * @param excludeParentGroup    Parent group to exclude results from
     * @param offset                Inclusive offset (the position of the first result to return)
     * @param limit                 Maximum number of matches returned
     * @return List of matching Group objects
     * @throws SQLException if error
     */
    List<Group> searchNonMembers(Context context, String query, Group excludeParentGroup,
                                 int offset, int limit) throws SQLException;

    /**
     * Returns the total number of groups that match the search query which are NOT currently members (subgroups)
     * of the given parentGroup. Can be used with searchNonMembers() to support pagination.
     *
     * @param context               DSpace context
     * @param query                 The search string used to search across group name or group ID
     * @param excludeParentGroup    Parent group to exclude results from
     * @return the number of Groups matching the query
     * @throws SQLException if error
     */
    int searchNonMembersCount(Context context, String query, Group excludeParentGroup) throws SQLException;

    /**
     * Return true if group has no direct or indirect members
     *
     * @param group DSpace group
     * @return true or false
     */
    public boolean isEmpty(Group group);

    /**
     * Initializes the group names for anonymous and administrator, and marks them
     * "permanent".
     *
     * @param context the DSpace context
     * @throws SQLException       database exception
     * @throws AuthorizeException authorization error
     */
    public void initDefaultGroupNames(Context context) throws SQLException, AuthorizeException;

    /**
     * Find all empty groups in DSpace
     *
     * @param context The DSpace context
     * @return All empty groups
     * @throws SQLException database exception
     */
    List<Group> getEmptyGroups(Context context) throws SQLException;

    /**
     * Count the total number of groups in DSpace
     *
     * @param context The DSpace context
     * @return The total number of groups
     * @throws SQLException database exception
     */
    int countTotal(Context context) throws SQLException;

    /**
     * Look up groups based on their value for a certain metadata field
     * (NOTE: name is not stored as metadata)
     *
     * @param context       The DSpace context
     * @param searchValue   The value to match
     * @param metadataField The metadata field to search in
     * @return The groups that have a matching value for specified metadata field
     * @throws SQLException database exception
     */
    List<Group> findByMetadataField(Context context, String searchValue, MetadataField metadataField)
        throws SQLException;

    /**
     * Find all groups which are a member of the given Parent group
     *
     * @param context The relevant DSpace Context.
     * @param parent The parent Group to search on
     * @param pageSize           how many results return
     * @param offset             the position of the first result to return
     * @return List of all groups which are members of the parent group
     * @throws SQLException database exception if error
     */
    List<Group> findByParent(Context context, Group parent, int pageSize, int offset)
        throws SQLException;

    /**
     * Return number of groups which are a member of the given Parent group.
     * Can be used with findByParent() for pagination of all groups within a given Parent group.
     *
     * @param context The relevant DSpace Context.
     * @param parent The parent Group to search on
     * @return number of groups which are members of the parent group
     * @throws SQLException database exception if error
     */
    int countByParent(Context context, Group parent)
        throws SQLException;
}
