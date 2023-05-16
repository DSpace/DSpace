/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.orcid.OrcidQueue;

/**
 * Database Access Object interface class for the OrcidQueue object. The
 * implementation of this class is responsible for all database calls for the
 * OrcidQueue object and is autowired by spring. This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidQueueDAO extends GenericDAO<OrcidQueue> {

    /**
     * Get the orcid queue records by the profileItem id.
     *
     * @param  context       DSpace context object
     * @param  profileItemId the profileItem item id
     * @param  limit         limit
     * @param  offset        offset
     * @return               the orcid queue records
     * @throws SQLException  if an SQL error occurs
     */
    public List<OrcidQueue> findByProfileItemId(Context context, UUID profileItemId, Integer limit, Integer offset)
        throws SQLException;

    /**
     * Count the orcid queue records with the same profileItemId.
     *
     * @param  context       DSpace context object
     * @param  profileItemId the profileItem item id
     * @return               the count result
     * @throws SQLException  if an SQL error occurs
     */
    long countByProfileItemId(Context context, UUID profileItemId) throws SQLException;

    /**
     * Returns all the orcid queue records with the given profileItem and entity
     * items.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  entity       the entity item
     * @return              the found orcid queue records
     * @throws SQLException
     */
    public List<OrcidQueue> findByProfileItemAndEntity(Context context, Item profileItem, Item entity)
        throws SQLException;

    /**
     * Get the OrcidQueue records where the given item is the profileItem OR the
     * entity
     *
     * @param  context      DSpace context object
     * @param  item         the item to search for
     * @return              the found OrcidHistory entities
     * @throws SQLException if database error
     */
    public List<OrcidQueue> findByProfileItemOrEntity(Context context, Item item) throws SQLException;

    /**
     * Find all the OrcidQueue records with the given entity and record type.
     *
     * @param  context      DSpace context object
     * @param  entity       the entity item
     * @param  type         the record type
     * @throws SQLException if database error occurs
     */
    public List<OrcidQueue> findByEntityAndRecordType(Context context, Item entity, String type) throws SQLException;

    /**
     * Find all the OrcidQueue records with the given profileItem and record type.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  type         the record type
     * @throws SQLException if database error occurs
     */
    public List<OrcidQueue> findByProfileItemAndRecordType(Context context, Item profileItem, String type)
        throws SQLException;

    /**
     * Get all the OrcidQueue records with attempts less than the given attempts.
     *
     * @param  context      DSpace context object
     * @param  attempts     the maximum value of attempts
     * @return              the found OrcidQueue records
     * @throws SQLException if database error
     */
    public List<OrcidQueue> findByAttemptsLessThan(Context context, int attempts) throws SQLException;
}
