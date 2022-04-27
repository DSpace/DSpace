/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.orcid.OrcidQueue;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service that handles ORCID queue records.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidQueueService {

    /**
     * Create an OrcidQueue record with the given owner and entity. The type of
     * operation is calculated based on whether or not the given entity was already
     * pushed to the ORCID registry.
     *
     * @param  context      DSpace context object
     * @param  owner        the owner item
     * @param  entity       the entity item
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    public OrcidQueue create(Context context, Item owner, Item entity) throws SQLException;

    /**
     * Create an OrcidQueue record with the given owner and entity to push new data
     * to ORCID.
     *
     * @param  context      DSpace context object
     * @param  owner        the owner item
     * @param  entity       the entity item
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    public OrcidQueue createEntityInsertionRecord(Context context, Item owner, Item entity) throws SQLException;

    /**
     * Create an OrcidQueue record with the given owner to update a record on ORCID
     * with the given putCode.
     *
     * @param  context      DSpace context object
     * @param  owner        the owner item
     * @param  entity       the entity item
     * @param  putCode      the putCode related to the given entity item
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    public OrcidQueue createEntityUpdateRecord(Context context, Item owner, Item entity, String putCode)
        throws SQLException;

    /**
     * Create an OrcidQueue record with the given owner to delete a record on ORCID
     * related to the given entity type with the given putCode.
     *
     * @param  context      DSpace context object
     * @param  owner        the owner item
     * @param  description  the orcid queue record description
     * @param  type         the type of the entity item
     * @param  putCode      the putCode related to the given entity item
     * @return              the stored record
     * @throws SQLException if an SQL error occurs
     */
    OrcidQueue createEntityDeletionRecord(Context context, Item owner, String description, String type, String putCode)
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
     * Get the orcid queue records by the owner id.
     *
     * @param  context      DSpace context object
     * @param  ownerId      the owner item id
     * @return              the orcid queue records
     * @throws SQLException if an SQL error occurs
     */
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId) throws SQLException;

    /**
     * Get the orcid queue records by the owner id.
     *
     * @param  context      DSpace context object
     * @param  ownerId      the owner item id
     * @param  limit        limit
     * @param  offset       offset
     * @return              the orcid queue records
     * @throws SQLException if an SQL error occurs
     */
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId, Integer limit, Integer offset)
        throws SQLException;

    /**
     * Get the orcid queue records by the owner and entity.
     *
     * @param  context      DSpace context object
     * @param  ownerId      the owner item
     * @param  entityId     the entity item
     * @param  limit        limit
     * @param  offset       offset
     * @return              the found OrcidQueue records
     * @throws SQLException if an SQL error occurs
     */
    public List<OrcidQueue> findByOwnerAndEntity(Context context, Item owner, Item entity) throws SQLException;

    /**
     * Get the OrcidQueue records where the given item is the owner OR the entity
     *
     * @param  context      DSpace context object
     * @param  item         the item to search for
     * @return              the found OrcidQueue records
     * @throws SQLException if database error
     */
    public List<OrcidQueue> findByOwnerOrEntity(Context context, Item item) throws SQLException;

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
     * ownerId.
     *
     * @param context DSpace context object
     * @param ownerId the owner item id
     * @return the record's count
     * @throws SQLException if an SQL error occurs
     */
    long countByOwnerId(Context context, UUID ownerId) throws SQLException;

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
     * @param  OrcidQueue         orcidQueue
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
     * Delete all the OrcidQueue records with the given owner and record type.
     *
     * @param  context      DSpace context object
     * @param  owner        the owner item
     * @param  recordType   the record type
     * @throws SQLException if database error occurs
     */
    public void deleteByOwnerAndRecordType(Context context, Item owner, String recordType) throws SQLException;

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
     * @param context             context
     * @param OrcidQueue          orcidQueue
     * @throws SQLException       if database error
     */
    public void update(Context context, OrcidQueue orcidQueue) throws SQLException;
}
