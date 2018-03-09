/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.MetadataField;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Database Access Object interface class for the Group object.
 * The implementation of this class is responsible for all database calls for the Group object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface GroupDAO extends DSpaceObjectDAO<Group>, DSpaceObjectLegacySupportDAO<Group> {

    /**
     * Look up groups based on their value for a certain metadata field (NOTE: name is not stored as metadata)
     * @param context The DSpace context
     * @param searchValue The value to match
     * @param metadataField The metadata field to search in
     * @return The groups that have a matching value for specified metadata field
     * @throws SQLException if database error
     */
    List<Group> findByMetadataField(Context context, String searchValue, MetadataField metadataField) throws SQLException;

    /**
     * Find all groups ordered by the specified metadata fields ascending
     * @param context The DSpace context
     * @param sortMetadataFields The metadata fields to sort on
     * @return A list of all groups, ordered by metadata fields
     * @throws SQLException if database error
     */
    List<Group> findAll(Context context, List<MetadataField> sortMetadataFields) throws SQLException;

    /**
     * Find all groups ordered by name ascending
     * @param context The DSpace context
     * @return A list of all groups, ordered by name
     * @throws SQLException if database error
     */
    List<Group> findAll(Context context) throws SQLException;

    /**
     * Find all groups that the given ePerson belongs to
     * @param context The DSpace context
     * @param ePerson The EPerson to match
     * @return A list of all groups to which the given EPerson belongs
     * @throws SQLException if database error
     */
    List<Group> findByEPerson(Context context, EPerson ePerson) throws SQLException;

    /**
     * Get a list of all direct parent - child group relations in the database
     * @param context The DSpace context
     * @param flushQueries Flush all pending queries
     * @return A list of pairs indicating parent - child
     * @throws SQLException if database error
     */
    List<Pair<UUID, UUID>> getGroup2GroupResults(Context context, boolean flushQueries) throws SQLException;

    /**
     * Return all empty groups
     * @param context The DSpace context
     * @return All empty groups
     * @throws SQLException if database error
     */
    List<Group> getEmptyGroups(Context context) throws SQLException;

    /**
     * Count the number of groups in DSpace
     * @param context The DSpace context
     * @return The number of groups
     * @throws SQLException if database error
     */
    int countRows(Context context) throws SQLException;

    /**
     * Find a group by its name (exact match)
     * @param context The DSpace context
     * @param name The name of the group to look for
     * @return The group with the specified name
     * @throws SQLException if database error
     */
    Group findByName(Context context, String name) throws SQLException;

    /**
     * Find a group by its name (fuzzy match)
     * @param context The DSpace context
     * @param groupName Part of the group's name to search for
     * @param offset Offset to use for pagination (-1 to disable)
     * @param limit The maximum number of results to return (-1 to disable)
     * @return Groups matching the query
     * @throws SQLException if database error
     */
    List<Group> findByNameLike(Context context, String groupName, int offset, int limit) throws SQLException;

    /**
     * Count the number of groups that have a name that contains the given string
     * @param context The DSpace context
     * @param groupName Part of the group's name to search for
     * @return The number of matching groups
     * @throws SQLException if database error
     */
    int countByNameLike(Context context, String groupName) throws SQLException;

    /**
     * Find a group by its name and the membership of the given EPerson
     * @param context The DSpace context
     * @param id The id of the group to look for
     * @param ePerson The EPerson which has to be a member
     * @return The group with the specified name
     * @throws SQLException if database error
     */
    Group findByIdAndMembership(Context context, UUID id, EPerson ePerson) throws SQLException;

}
