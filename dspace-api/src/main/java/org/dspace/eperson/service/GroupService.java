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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

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
     */
    public Group create(Context context) throws SQLException, AuthorizeException;

    /**
     * set name of group
     *
     * @param name
     *            new group name
     */
    public void setName(Context context, Group group, String name) throws SQLException;

    /**
     * add an eperson member
     *
     * @param e
     *            eperson
     */
    public void addMember(Context context, Group group, EPerson e);

    /**
     * add group to this group
     *
     * @param groupParent
     */
    public void addMember(Context context, Group groupParent, Group groupChild) throws SQLException;

    /**
     * remove an eperson from a group
     *
     * @param ePerson
     *            eperson
     */
    public void removeMember(Context context, Group group, EPerson ePerson);


    /**
     * remove group from this group
     *
     * @param g
     */
    public void removeMember(Context context, Group groupParent, Group childGroup) throws SQLException;

    /**
     * check to see if an eperson is a direct member.
     * If the eperson is a member via a subgroup will be returned <code>false</code>
     *
     * @param e
     *            eperson to check membership
     */
    public boolean isDirectMember(Group group, EPerson ePerson);

    /**
     * Check to see if g is a direct group member.
     * If g is a subgroup via another group will be returned <code>false</code>
     *
     * @param g
     *            group to check
     */
    public boolean isMember(Group owningGroup, Group childGroup);

    /**
     * fast check to see if an eperson is a member called with eperson id, does
     * database lookup without instantiating all of the epeople objects and is
     * thus a static method
     *
     * @param context
     *            context
     * @param group
     *            group to check
     */
    public boolean isMember(Context context, Group group) throws SQLException;

    /**
     * Get all of the groups that an eperson is a member of.
     *
     * @param context
     * @param ePerson
     * @throws SQLException
     */
    public List<Group> allMemberGroups(Context context, EPerson ePerson) throws SQLException;

    /**
     * Get all of the epeople who are a member of the
     * specified group, or a member of a sub-group of the
     * specified group, etc.
     *
     * @param context
     *          DSpace context
     * @param group
     *          Group object
     * @return   Array of EPerson objects
     * @throws SQLException
     */
    public List<EPerson> allMembers(Context context, Group group) throws SQLException;

    /**
     * Find the group by its name - assumes name is unique
     *
     * @param context
     * @param name
     *
     * @return the named Group, or null if not found
     */
    public Group findByName(Context context, String name) throws SQLException;

    /**
     * Finds all groups in the site
     *
     * @param context
     *            DSpace context
     * @param sortField
     *            field to sort by -- Group.ID or Group.NAME
     *
     * @return array of all groups in the site
     */
    public List<Group> findAll(Context context, int sortField) throws SQLException;


    /**
     * Find the groups that match the search query across eperson_group_id or name
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return array of Group objects
     */
    public List<Group> search(Context context, String query) throws SQLException;

    /**
     * Find the groups that match the search query across eperson_group_id or name
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * @param offset
     *            Inclusive offset
     * @param limit
     *            Maximum number of matches returned
     *
     * @return array of Group objects
     */
    public List<Group> search(Context context, String query, int offset, int limit) throws SQLException;

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
     */
    public int searchResultCount(Context context, String query)	throws SQLException;

    /**
     * Return true if group has no direct or indirect members
     */
    public boolean isEmpty(Group group);

    /**
     * Initializes the group names for anymous & administrator
     * @param context the dspace context
     * @throws SQLException database exception
     * @throws AuthorizeException
     */
    public void initDefaultGroupNames(Context context) throws SQLException, AuthorizeException;
}
