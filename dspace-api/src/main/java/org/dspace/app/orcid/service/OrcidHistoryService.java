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
import java.util.Map;
import java.util.Optional;

import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.content.Item;
import org.dspace.core.Context;

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
     * Get the OrcidHistory records where the given item is the owner OR the entity
     *
     * @param  context      DSpace context object
     * @param  item         the item to search for
     * @return              the found OrcidHistory entities
     * @throws SQLException if database error
     */
    public List<OrcidHistory> findByOwnerOrEntity(Context context, Item item) throws SQLException;

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
     * Create a new OrcidHistory records related to the given owner and entity
     * items.
     *
     * @param  context      DSpace context object
     * @param  owner        the owner item
     * @param  entity       the entity item
     * @return              the created orcid history record
     * @throws SQLException if database error
     */
    public OrcidHistory create(Context context, Item owner, Item entity) throws SQLException;

    /**
     * Delete an OrcidHistory
     *
     * @param context             context
     * @param OrcidHistory        orcidHistory
     * @throws SQLException       if database error
     */
    public void delete(Context context, OrcidHistory orcidHistory) throws SQLException;

    /**
     * Update the OrcidHistory
     *
     * @param context             context
     * @param OrcidHistory        orcidHistory
     * @throws SQLException       if database error
     */
    public void update(Context context, OrcidHistory orcidHistory) throws SQLException;

    /**
     * Find the last put code related to the given owner and entity item.
     *
     * @param  context      DSpace context object
     * @param  owner        the owner item
     * @param  entity       the entity item
     * @return              the found put code, if any
     * @throws SQLException if database error
     */
    public Optional<String> findLastPutCode(Context context, Item owner, Item entity) throws SQLException;

    /**
     * Find all the last put code related to the entity item each associated with
     * the owner to which it refers.
     *
     * @param  context      DSpace context object
     * @param  entity       the entity item
     * @return              a map that relates the owners with the identified
     *                      putCode
     * @throws SQLException if database error
     */
    public Map<Item, String> findLastPutCodes(Context context, Item entity) throws SQLException;

    public OrcidHistory sendToOrcid(Context context, OrcidQueue orcidQueue, boolean forceAddition) throws SQLException;

    List<OrcidHistory> findSuccessfullyRecordsByEntityAndType(Context context, Item entity, String recordType)
        throws SQLException;

}
