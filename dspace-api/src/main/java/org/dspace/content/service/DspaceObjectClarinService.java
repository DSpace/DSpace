/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;

import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Additional service interface class of DspaceObjectService for the DspaceObject in Clarin-DSpace.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public interface DspaceObjectClarinService<T extends DSpaceObject> {

    /* Created for LINDAT/CLARIAH-CZ (UFAL) */
    /**
     * Retrieve all handle from the registry
     *
     * @param context       DSpace context object
     * @return              array of handles
     * @throws SQLException if database error
     */
    public Community getPrincipalCommunity(Context context, DSpaceObject dso) throws SQLException;
}
