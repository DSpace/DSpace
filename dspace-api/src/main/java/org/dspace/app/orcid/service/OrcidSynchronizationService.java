/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import java.sql.SQLException;

import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.profile.OrcidProfileDisconnectionMode;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service that handle the the syncronization between a DSpace profile and the
 * relative ORCID profile, if any.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface OrcidSynchronizationService {

    /**
     * Configure the given profile with the data present in the given ORCID token.
     * This action is required to synchronize profile and related entities with
     * ORCID.
     *
     * @param context the relevant DSpace Context.
     * @param profile the profile to configure
     * @param token   the ORCID token
     * @throws SQLException if a SQL error occurs during the profile update
     */
    public void linkProfile(Context context, Item profile, OrcidTokenResponseDTO token) throws SQLException;

    /**
     * Disconnect the given profile from ORCID.
     *
     * @param  context      the relevant DSpace Context.
     * @param  profile      the profile to disconnect
     * @throws SQLException if a SQL error occurs during the profile update
     */
    public void unlinkProfile(Context context, Item profile) throws SQLException;

    /**
     * Returns the configuration ORCID profile's disconnection mode. If that mode is
     * not configured or the configuration is wrong, the value DISABLED is returned.
     *
     * @return the disconnection mode
     */
    OrcidProfileDisconnectionMode getDisconnectionMode();
}
