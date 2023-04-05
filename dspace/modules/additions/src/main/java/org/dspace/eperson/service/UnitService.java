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
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;

/**
 * Service interface class for the Unit object.
 * The implementation of this class is responsible for all business logic calls
 * for the Unit object and is autowired by spring
 *
 * @author mohideen at umd.edu
 */
public interface UnitService extends DSpaceObjectService<Unit>, DSpaceObjectLegacySupportService<Unit> {
    /**
     * Create a new unit, with a new ID.
     *
     * @param context DSpace context object
     *
     * @return the newly created unit
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public Unit create(Context context) throws SQLException, AuthorizeException;

    /**
     * set name of unit
     *
     * @param unit DSpace unit
     * @param name  new unit name
     * @throws SQLException if database error
     */
    public void setName(Unit unit, String name) throws SQLException;

    /**
     * Find the unit by its name - assumes name is unique
     *
     * @param context the DSpace context
     * @param name the name of the unit to find
     *
     * @return the Unit with the given name, or null if the name is not found.
     * @throws SQLException if error
     */
    public Unit findByName(Context context, String name) throws SQLException;

    /**
     * Finds all units in the site, sorted by name
     *
     * @param context DSpace context
     * @param pageSize           how many results return
     * @param offset             the position of the first result to return
     * @return list of all units in the site
     * @throws SQLException if error
     */
    public List<Unit> findAll(Context context, int pageSize, int offset) throws SQLException;

    /**
     * Finds all units that are mapped to the group
     *
     * @param context DSpace context
     * @param group group the Group to find
     *
     * @return list of all units mapped to the given group
     * @throws SQLException if error
     */
    public List<Unit> findAllByGroup(Context context, Group group) throws SQLException;

     /**
     * Find the units that match the search query across unit_id or name
     *
     * @param context DSpace context
     * @param query The search string
     *
     * @return array of Unit objects
     * @throws SQLException if error
     */
    public List<Unit> search(Context context, String query) throws SQLException;

    /**
     * Find the units that match the search query across unit_id or name
     *
     * @param context DSpace context
     * @param query The search string
     * @param offset Inclusive offset
     * @param limit Maximum number of matches returned
     *
     * @return list of Unit objects
     * @throws SQLException if error
     */
    public List<Unit> search(Context context, String query, int offset, int limit) throws SQLException;

    /**
     * Returns the total number of units returned by a specific query,
     * without the overhead of creating the Unit objects to store the
     * results.
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return the number of units matching the query
     * @throws SQLException if error
     */
    public int searchResultCount(Context context, String query) throws SQLException;


    /**
     * Return an list of groups of this unit and its subcommunities
     *
     * @param context context
     * @param unit unit
     * @return an array of groups
     * @throws SQLException if database error
     */
    public List<Group> getAllGroups(Context context, Unit unit)
            throws SQLException;


    /**
     * Add an existing group to the unit
     *
     * @param context context
     * @param unit unit
     * @param group
     *            group to add
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void addGroup(Context context, Unit unit, Group group)
            throws SQLException, AuthorizeException;

    /**
     * Remove a group. If it only belongs to one parent unit,
     * then it is permanently deleted. If it has more than one parent unit,
     * it is simply unmapped from the current unit.
     *
     * @param context context
     * @param group group to remove
     * @param unit unit
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void removeGroup(Context context, Unit unit, Group group)
            throws SQLException, AuthorizeException;

    /**
     * Returns true or false based on whether a given group is a member
     * to the unit.
     *
     * @param unit unit
     * @param group group
     *
     * @return true if group is a member of the unit, false otherwise
     */
    public boolean isMember(Unit unit, Group group);

    /**
     * return true if context's user can edit unit, false otherwise
     *
     * @param context context
     * @return true if the current user can edit the unit, false otherwise
     * @throws SQLException if database error
     */
    public boolean canEditBoolean(Context context) throws SQLException;

    /**
     * Throws an AuthorizeException if the given context is not allowed to
     * edit the unit.
     *
     * @param context the DSpace context
     * @throws AuthorizeException if editing the unit is not allowed
     * @throws SQLException if database error
     */
    public void canEdit(Context context) throws AuthorizeException, SQLException;

    /**
     * Return the total number of units
     *
     * @param context the DSpace context
     * @return the total number of units
     * @throws SQLException if database error
     */
    int countTotal(Context context) throws SQLException;
}
