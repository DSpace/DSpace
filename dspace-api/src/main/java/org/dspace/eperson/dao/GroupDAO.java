/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.MetadataField;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the Group object.
 * The implementation of this class is responsible for all database calls for the Group object and is autowired by
 * spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface GroupDAO extends DSpaceObjectDAO<Group>, DSpaceObjectLegacySupportDAO<Group> {

    /**
     * Look up groups based on their value for a certain metadata field (NOTE: name is not stored as metadata)
     *
     * @param session       The current request's database context.
     * @param searchValue   The value to match
     * @param metadataField The metadata field to search in
     * @return The groups that have a matching value for specified metadata field
     * @throws SQLException if database error
     */
    List<Group> findByMetadataField(Session session, String searchValue, MetadataField metadataField)
        throws SQLException;

    /**
     * Find all groups ordered by the specified metadata fields ascending
     *
     * @param session            The current request's database context.
     * @param metadataSortFields The metadata fields to sort on
     * @param pageSize           how many results return
     * @param offset             the position of the first result to return
     * @return A list of all groups, ordered by metadata fields
     * @throws SQLException if database error
     */
    List<Group> findAll(Session session, List<MetadataField> metadataSortFields, int pageSize, int offset)
        throws SQLException;

    /**
     * Find all groups ordered by name ascending
     *
     * @param session  The current request's database context.
     * @param pageSize how many results return
     * @param offset   the position of the first result to return
     * @return A list of all groups, ordered by name
     * @throws SQLException if database error
     */
    List<Group> findAll(Session session, int pageSize, int offset) throws SQLException;

    /**
     * Find all groups that the given ePerson belongs to
     *
     * @param session The current request's database context.
     * @param ePerson The EPerson to match
     * @return A list of all groups to which the given EPerson belongs
     * @throws SQLException if database error
     */
    List<Group> findByEPerson(Session session, EPerson ePerson) throws SQLException;

    /**
     * Get a list of all direct parent - child group relations in the database
     *
     * @param session      The current request's database context.
     * @param flushQueries Flush all pending queries
     * @return A list of pairs indicating parent - child
     * @throws SQLException if database error
     */
    List<Pair<UUID, UUID>> getGroup2GroupResults(Session session, boolean flushQueries) throws SQLException;

    /**
     * Return all empty groups
     *
     * @param session The current request's database context.
     * @return All empty groups
     * @throws SQLException if database error
     */
    List<Group> getEmptyGroups(Session session) throws SQLException;

    /**
     * Count the number of groups in DSpace
     *
     * @param session The current request's database context.
     * @return The number of groups
     * @throws SQLException if database error
     */
    int countRows(Session session) throws SQLException;

    /**
     * Find a group by its name (exact match)
     *
     * @param session The current request's database context.
     * @param name    The name of the group to look for
     * @return The group with the specified name
     * @throws SQLException if database error
     */
    Group findByName(Session session, String name) throws SQLException;

    /**
     * Find a group by its name (fuzzy match)
     *
     * @param session   The current request's database context.
     * @param groupName Part of the group's name to search for
     * @param offset    Offset to use for pagination (-1 to disable)
     * @param limit     The maximum number of results to return (-1 to disable)
     * @return Groups matching the query
     * @throws SQLException if database error
     */
    List<Group> findByNameLike(Session session, String groupName, int offset, int limit) throws SQLException;

    /**
     * Count the number of groups that have a name that contains the given string
     *
     * @param session   The current request's database context.
     * @param groupName Part of the group's name to search for
     * @return The number of matching groups
     * @throws SQLException if database error
     */
    int countByNameLike(Session session, String groupName) throws SQLException;

    /**
     * Find a group by its name and the membership of the given EPerson
     *
     * @param session The current request's database context.
     * @param id      The id of the group to look for
     * @param ePerson The EPerson which has to be a member
     * @return The group with the specified name
     * @throws SQLException if database error
     */
    Group findByIdAndMembership(Session session, UUID id, EPerson ePerson) throws SQLException;

}
