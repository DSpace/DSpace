/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import java.sql.SQLException;

import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface of service to manage OrcidHistory
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
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

    public OrcidHistory create(Context context, Item owner, Item entity) throws SQLException;

    /**
     * Delete an OrcidHistory
     *
     * @param context             context
     * @param OrcidHistory        orcidHistory
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void delete(Context context, OrcidHistory orcidHistory) throws SQLException, AuthorizeException;

    /**
     * Update the OrcidHistory
     *
     * @param context             context
     * @param OrcidHistory        orcidHistory
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void update(Context context, OrcidHistory orcidHistory) throws SQLException, AuthorizeException;

    public OrcidHistory sendToOrcid(Context context, OrcidQueue orcidQueue, boolean forceAddition) throws SQLException;
}
