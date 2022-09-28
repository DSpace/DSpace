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
import org.dspace.orcid.OrcidHistory;

/**
 * Database Access Object interface class for the OrcidHistory object. The
 * implementation of this class is responsible for all database calls for the
 * OrcidHistory object and is autowired by spring. This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidHistoryDAO extends GenericDAO<OrcidHistory> {

    /**
     * Find all the ORCID history records by the given profileItem and entity uuids.
     *
     * @param  context       the DSpace context
     * @param  profileItemId the profileItem item uuid
     * @param  entityId      the entity item uuid
     * @return               the records list
     * @throws SQLException  if an SQL error occurs
     */
    List<OrcidHistory> findByProfileItemAndEntity(Context context, UUID profileItemId, UUID entityId)
        throws SQLException;

    /**
     * Get the OrcidHistory records where the given item is the profileItem or the
     * entity
     *
     * @param  context      DSpace context object
     * @param  item         the item to search for
     * @return              the found OrcidHistory entities
     * @throws SQLException if database error
     */
    public List<OrcidHistory> findByProfileItemOrEntity(Context context, Item item) throws SQLException;

    /**
     * Find the OrcidHistory records related to the given entity item.
     *
     * @param  context      DSpace context object
     * @param  entity       the entity item
     * @return              the found put codes
     * @throws SQLException if database error
     */
    List<OrcidHistory> findByEntity(Context context, Item entity) throws SQLException;

    /**
     * Find all the successfully Orcid history records with the given record type
     * related to the given entity. An history record is considered successful if
     * the status is between 200 and 300.
     *
     * @param  context      DSpace context object
     * @param  entity       the entity item
     * @param  recordType   the record type
     * @return              the found orcid history records
     * @throws SQLException if database error
     */
    List<OrcidHistory> findSuccessfullyRecordsByEntityAndType(Context context, Item entity,
        String recordType) throws SQLException;
}
