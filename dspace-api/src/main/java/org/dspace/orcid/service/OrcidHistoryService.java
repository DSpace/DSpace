/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.orcid.OrcidHistory;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.exception.OrcidValidationException;

/**
 * Interface of service to manage OrcidHistory.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface OrcidHistoryService {

    /**
     * Get an OrcidHistory from the database.
     *
     * @param context  DSpace context object
     * @param id       ID of the OrcidHistory
     * @return         the OrcidHistory format, or null if the ID is invalid.
     * @throws         SQLException if database error
     */
    public OrcidHistory find(Context context, int id) throws SQLException;

    /**
     * Find all the ORCID history records.
     *
     * @param  context      DSpace context object
     * @return              the ORCID history records
     * @throws SQLException if an SQL error occurs
     */
    public List<OrcidHistory> findAll(Context context) throws SQLException;

    /**
     * Get the OrcidHistory records where the given item is the profile item OR the
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
    public List<OrcidHistory> findByEntity(Context context, Item entity) throws SQLException;

    /**
     * Create a new OrcidHistory records related to the given profileItem and entity
     * items.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  entity       the entity item
     * @return              the created orcid history record
     * @throws SQLException if database error
     */
    public OrcidHistory create(Context context, Item profileItem, Item entity) throws SQLException;

    /**
     * Delete an OrcidHistory
     *
     * @param  context      context
     * @param  orcidHistory the OrcidHistory entity to delete
     * @throws SQLException if database error
     */
    public void delete(Context context, OrcidHistory orcidHistory) throws SQLException;

    /**
     * Update the OrcidHistory
     *
     * @param  context      context
     * @param  orcidHistory the OrcidHistory entity to update
     * @throws SQLException if database error
     */
    public void update(Context context, OrcidHistory orcidHistory) throws SQLException;

    /**
     * Find the last put code related to the given profileItem and entity item.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  entity       the entity item
     * @return              the found put code, if any
     * @throws SQLException if database error
     */
    public Optional<String> findLastPutCode(Context context, Item profileItem, Item entity) throws SQLException;

    /**
     * Find all the last put code related to the entity item each associated with
     * the profileItem to which it refers.
     *
     * @param  context      DSpace context object
     * @param  entity       the entity item
     * @return              a map that relates the profileItems with the identified
     *                      putCode
     * @throws SQLException if database error
     */
    public Map<Item, String> findLastPutCodes(Context context, Item entity) throws SQLException;

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
    List<OrcidHistory> findSuccessfullyRecordsByEntityAndType(Context context, Item entity, String recordType)
        throws SQLException;

    /**
     * Synchronize the entity related to the given orcidQueue record with ORCID.
     *
     * @param  context                  DSpace context object
     * @param  orcidQueue               the orcid queue record that has the
     *                                  references of the data to be synchronized
     * @param  forceAddition            to force the insert on the ORCID registry
     * @return                          the created orcid history record with the
     *                                  synchronization result
     * @throws SQLException             if database error
     * @throws OrcidValidationException if the data to synchronize with ORCID is not
     *                                  valid
     */
    public OrcidHistory synchronizeWithOrcid(Context context, OrcidQueue orcidQueue, boolean forceAddition)
        throws SQLException, OrcidValidationException;

}
