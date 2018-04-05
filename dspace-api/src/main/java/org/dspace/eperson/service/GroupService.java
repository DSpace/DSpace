/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Service interface class for the Group object.
 * The implementation of this class is responsible for all business logic calls for the Group object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface GroupService extends DSpaceObjectService<Group>, DSpaceObjectLegacySupportService<Group> {

    public static final int NAME = 1; // sort by NAME (default)


    /**
     * Create a new group
     *
     * @param context
     *            DSpace context object
     * @return group
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Group create(Context context) throws SQLException, AuthorizeException;

    /**
     * set name of group
     *
     * @param group DSpace group
     * @param name
     *            new group name
     * @throws SQLException if database error
     */
    public void setName(Group group, String name) throws SQLException;

    /**
     * add an eperson member
     *
     * @param context
     *            DSpace context object
     * @param group DSpace group
     * @param e
     *            eperson
     */
    public void addMember(Context context, Group group, EPerson e);

    /**
     * add group to this group
     *
     * @param context
     *            DSpace context object
     * @param groupParent parent group
     * @param groupChild child group
     * @throws SQLException if database error
     */
    public void addMember(Context context, Group groupParent, Group groupChild) throws SQLException;

    /**
     * remove an eperson from a group
     *
     * @param context
     *            DSpace context object
     * @param group DSpace group
     * @param ePerson
     *            eperson
     */
    public void removeMember(Context context, Group group, EPerson ePerson);


    /**
     * remove group from this group
     *
     * @param context
     *            DSpace context object
     * @param groupParent parent group
     * @param childGroup child group
     * @throws SQLException if database error
     */
    public void removeMember(Context context, Group groupParent, Group childGroup) throws SQLException;

    /**
     * check to see if an eperson is a direct member.
     * If the eperson is a member via a subgroup will be returned <code>false</code>
     *
     * @param group DSpace group
     * @param ePerson
     *            eperson to check membership
     * @return true or false
     */
    public boolean isDirectMember(Group group, EPerson ePerson);

    /**
     * Check to see if childGroup is a direct group member of owningGroup.
     * If childGroup is a subgroup via another group will be returned <code>false</code>
     *
     * @param owningGroup parent group
     * @param childGroup child group
     * @return true or false
     */
    public boolean isMember(Group owningGroup, Group childGroup);

    /**
     * Check to see if parentGroup is a direct or in-direct parent of a childGroup.
     *
     * @param parentGroup parent group
     * @param childGroup child group
     * @return true or false
     */
    public boolean isParentOf(Context context, Group parentGroup, Group childGroup) throws SQLException;

    /**
     * fast check to see if an eperson is a member called with eperson id, does
     * database lookup without instantiating all of the epeople objects and is
     * thus a static method
     *
     * @param context
     *            context
     * @param group
     *            group to check
     * @return true or false
     * @throws SQLException if database error
     */
    public boolean isMember(Context context, Group group) throws SQLException;

    /**
     * fast check to see if an eperson is a member called with eperson id, does
     * database lookup without instantiating all of the epeople objects and is
     * thus a static method. This method uses context.getCurrentUser() as
     * eperson whos membership should be checked.
     *
     * @param context
     *            context
     * @param groupName
     *            the name of the group to check
     * @return true or false
     * @throws SQLException if database error
     */
    public boolean isMember(Context context, String groupName) throws SQLException;

    /**
     * fast check to see if an eperson is a member called with eperson id, does
     * database lookup without instantiating all of the epeople objects and is
     * thus a static method. The eperson whos membership should be checked must
     * be defined as method attribute.
     *
     * @param context
     *            context
     * @param groupName
     *            the name of the group to check
     * @return true or false
     * @throws SQLException if database error
     */
    public boolean isMember(Context context, EPerson epersonToCheck, String groupName) throws SQLException;

    /**
     * fast check to see if an eperson is a member called with eperson id, does
     * database lookup without instantiating all of the epeople objects and is
     * thus a static method
     *
     * @param context DSpace context object.
     * @param eperson EPerson whos membership should be checked.
     * @param group The group to check against.
     * @return true or false
     * @throws SQLException if database error
     */
    public boolean isMember(Context context, EPerson eperson, Group group) throws SQLException;

    /**
     * Get all of the groups that an eperson is a member of.
     *
     * @param context DSpace contenxt
     * @param ePerson ePerson object
     * @return list of Group objects
     * @throws SQLException if database error
     */
    public List<Group> allMemberGroups(Context context, EPerson ePerson) throws SQLException;

    Set<Group> allMemberGroupsSet(Context context, EPerson ePerson) throws SQLException;

    /**
     * Get all of the epeople who are a member of the
     * specified group, or a member of a sub-group of the
     * specified group, etc.
     *
     * @param context
     *          DSpace context
     * @param group
     *          Group object
     * @return List of EPerson objects
     * @throws SQLException if error
     */
    public List<EPerson> allMembers(Context context, Group group) throws SQLException;

    /**
     * Find the group by its name - assumes name is unique
     *
     * @param context
     * @param name
     *
     * @return the named Group, or null if not found
     * @throws SQLException if error
     */
    public Group findByName(Context context, String name) throws SQLException;

    /**
     * Finds all groups in the site
     *
     * @param context
     *            DSpace context
     * @param metadataSortFields
     *            metadata fields to sort by, leave empty to sort by Name
     *
     * @return List of all groups in the site
     * @throws SQLException if error
     */
    public List<Group> findAll(Context context, List<MetadataField> metadataSortFields) throws SQLException;


    /**
     * DEPRECATED: Please use {@code findAll(Context context, List<MetadataField> metadataFieldsSort)} instead
     * @param context DSpace context
     * @param sortField sort field index
     * @return List of all groups in the site
     * @throws SQLException if error
     * @deprecated
     */
    @Deprecated
    public List<Group> findAll(Context context, int sortField) throws SQLException;

    /**
     * Find the groups that match the search query across eperson_group_id or name
     *
     * @param context
     *            DSpace context
     * @param groupIdentifier
     *            The group name or group ID
     *
     * @return array of Group objects
     * @throws SQLException if error
     */
    public List<Group> search(Context context, String groupIdentifier) throws SQLException;

    /**
     * Find the groups that match the search query across eperson_group_id or name
     *
     * @param context
     *            DSpace context
     * @param groupIdentifier
     *            The group name or group ID
     * @param offset
     *            Inclusive offset
     * @param limit
     *            Maximum number of matches returned
     *
     * @return array of Group objects
     * @throws SQLException if error
     */
    public List<Group> search(Context context, String groupIdentifier, int offset, int limit) throws SQLException;

    /**
     * Returns the total number of groups returned by a specific query, without the overhead
     * of creating the Group objects to store the results.
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return the number of groups matching the query
     * @throws SQLException if error
     */
    public int searchResultCount(Context context, String query)	throws SQLException;

    /**
     * Return true if group has no direct or indirect members
     * @param group DSpace group
     * @return true or false
     */
    public boolean isEmpty(Group group);

    /**
     * Initializes the group names for anonymous & administrator, and marks them
     * "permanent".
     *
     * @param context the DSpace context
     * @throws SQLException database exception
     * @throws AuthorizeException authorization error
     */
    public void initDefaultGroupNames(Context context) throws SQLException, AuthorizeException;

    /**
     * Find all empty groups in DSpace
     * @param context The DSpace context
     * @return All empty groups
     * @throws SQLException database exception
     */
    List<Group> getEmptyGroups(Context context) throws SQLException;

    /**
     * Count the total number of groups in DSpace
     * @param context The DSpace context
     * @return The total number of groups
     * @throws SQLException database exception
     */
    int countTotal(Context context) throws SQLException;

    /**
     * Look up groups based on their value for a certain metadata field (NOTE: name is not stored as metadata)
     * @param context The DSpace context
     * @param searchValue The value to match
     * @param metadataField The metadata field to search in
     * @return The groups that have a matching value for specified metadata field
     * @throws SQLException database exception
     */
    List<Group> findByMetadataField(Context context, String searchValue, MetadataField metadataField) throws SQLException;
}
