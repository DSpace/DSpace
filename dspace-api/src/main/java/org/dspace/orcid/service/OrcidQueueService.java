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
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.profile.OrcidEntitySyncPreference;

/**
 * Service that handles ORCID queue records.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidQueueService {

    /**
     * Create an OrcidQueue record with the given profileItem and entity. The type
     * of operation is calculated based on whether or not the given entity was
     * already pushed to the ORCID registry.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  entity       the entity item
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    public OrcidQueue create(Context context, Item profileItem, Item entity) throws SQLException;

    /**
     * Create an OrcidQueue record with the given profileItem and entity to push new
     * data to ORCID.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  entity       the entity item
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    public OrcidQueue createEntityInsertionRecord(Context context, Item profileItem, Item entity) throws SQLException;

    /**
     * Create an OrcidQueue record with the given profileItem to update a record on
     * ORCID with the given putCode.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  entity       the entity item
     * @param  putCode      the putCode related to the given entity item
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    public OrcidQueue createEntityUpdateRecord(Context context, Item profileItem, Item entity, String putCode)
        throws SQLException;

    /**
     * Create an OrcidQueue record with the given profileItem to delete a record on
     * ORCID related to the given entity type with the given putCode.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  description  the orcid queue record description
     * @param  type         the type of the entity item
     * @param  putCode      the putCode related to the given entity item
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    OrcidQueue createEntityDeletionRecord(Context context, Item profileItem, String description, String type,
        String putCode)
        throws SQLException;

    /**
     * Create an OrcidQueue record with the profile to add data to ORCID.
     *
     * @param  context      DSpace context object
     * @param  profile      the profile item
     * @param  description  the record description
     * @param  recordType   the record type
     * @param  metadata     the metadata signature
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    OrcidQueue createProfileInsertionRecord(Context context, Item profile, String description, String recordType,
        String metadata) throws SQLException;

    /**
     * Create an OrcidQueue record with the profile to remove data from ORCID.
     *
     * @param  context      DSpace context object
     * @param  profile      the profile item
     * @param  description  the record description
     * @param  recordType   the record type
     * @param  putCode      the putCode
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    OrcidQueue createProfileDeletionRecord(Context context, Item profile, String description, String recordType,
        String metadata, String putCode) throws SQLException;

    /**
     * Find all the ORCID queue records.
     *
     * @param  context      DSpace context object
     * @return              the ORCID queue records
     * @throws SQLException if an SQL error occurs
     */
    public List<OrcidQueue> findAll(Context context) throws SQLException;

    /**
     * Get the orcid queue records by the profileItem id.
     *
     * @param  context       DSpace context object
     * @param  profileItemId the profileItem item id
     * @return               the orcid queue records
     * @throws SQLException  if an SQL error occurs
     */
    public List<OrcidQueue> findByProfileItemId(Context context, UUID profileItemId) throws SQLException;

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
     * Get the orcid queue records by the profileItem and entity.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  entity       the entity item
     * @return              the found OrcidQueue records
     * @throws SQLException if an SQL error occurs
     */
    public List<OrcidQueue> findByProfileItemAndEntity(Context context, Item profileItem, Item entity)
        throws SQLException;

    /**
     * Get the OrcidQueue records where the given item is the profileItem OR the
     * entity
     *
     * @param  context      DSpace context object
     * @param  item         the item to search for
     * @return              the found OrcidQueue records
     * @throws SQLException if database error
     */
    public List<OrcidQueue> findByProfileItemOrEntity(Context context, Item item) throws SQLException;

    /**
     * Get all the OrcidQueue records with attempts less than the given attempts.
     *
     * @param  context      DSpace context object
     * @param  attempts     the maximum value of attempts
     * @return              the found OrcidQueue records
     * @throws SQLException if database error
     */
    public List<OrcidQueue> findByAttemptsLessThan(Context context, int attempts) throws SQLException;

    /**
     * Returns the number of records on the OrcidQueue associated with the given
     * profileItemId.
     *
     * @param  context       DSpace context object
     * @param  profileItemId the profileItem item id
     * @return               the record's count
     * @throws SQLException  if an SQL error occurs
     */
    long countByProfileItemId(Context context, UUID profileItemId) throws SQLException;

    /**
     * Delete the OrcidQueue record with the given id.
     *
     * @param context DSpace context object
     * @param id      the id of the record to be deleted
     * @throws SQLException if an SQL error occurs
     */
    public void deleteById(Context context, Integer id) throws SQLException;

    /**
     * Delete an OrcidQueue
     *
     * @param  context            DSpace context object
     * @param  orcidQueue         the orcidQueue record to delete
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void delete(Context context, OrcidQueue orcidQueue) throws SQLException;

    /**
     * Delete all the OrcidQueue records with the given entity and record type.
     *
     * @param  context      DSpace context object
     * @param  entity       the entity item
     * @param  recordType   the record type
     * @throws SQLException if database error occurs
     */
    public void deleteByEntityAndRecordType(Context context, Item entity, String recordType) throws SQLException;

    /**
     * Delete all the OrcidQueue records with the given profileItem and record type.
     *
     * @param  context      DSpace context object
     * @param  profileItem  the profileItem item
     * @param  recordType   the record type
     * @throws SQLException if database error occurs
     */
    public void deleteByProfileItemAndRecordType(Context context, Item profileItem, String recordType)
        throws SQLException;

    /**
     * Get an OrcidQueue from the database.
     *
     * @param  context      DSpace context object
     * @param  id           ID of the OrcidQueue
     * @return              the OrcidQueue format, or null if the ID is invalid.
     * @throws SQLException if database error
     */
    public OrcidQueue find(Context context, int id) throws SQLException;

    /**
     * Update the OrcidQueue
     *
     * @param  context      context
     * @param  orcidQueue   the OrcidQueue to update
     * @throws SQLException if database error
     */
    public void update(Context context, OrcidQueue orcidQueue) throws SQLException;

    /**
     * Recalculates the ORCID queue records linked to the given profileItem as
     * regards the entities of the given type. The recalculation is done based on
     * the preference indicated.
     *
     * @param  context      context
     * @param  profileItem  the profileItem
     * @param  entityType   the entity type related to the records to recalculate
     * @param  preference   the preference value on which to base the recalculation
     * @throws SQLException if database error
     */
    public void recalculateOrcidQueue(Context context, Item profileItem, OrcidEntityType entityType,
        OrcidEntitySyncPreference preference) throws SQLException;
}
