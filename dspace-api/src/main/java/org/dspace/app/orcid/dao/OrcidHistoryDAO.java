/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.orcid.OrcidHistory;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

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
     * Find all the ORCID history records by the given owner and entity uuids.
     *
     * @param context  the DSpace context
     * @param ownerId  the owner item uuid
     * @param entityId the entity item uuid
     * @return the records list
     * @throws SQLException if an SQL error occurs
     */
    List<OrcidHistory> findByOwnerAndEntity(Context context, UUID ownerId, UUID entityId) throws SQLException;
}
