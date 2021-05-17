/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import org.dspace.app.orcid.webhook.OrcidWebhookMode;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service that handle the ORCID webhook registration.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidWebhookService {

    /**
     * Returns the configured ORCID webhook registration mode. If the configuration
     * value has not been set or has been set incorrectly, this returns DISABLED.
     * @return
     */
    public OrcidWebhookMode getOrcidWebhookMode();

    /**
     * Check if the given profile is already registered for ORCID webhook.
     *
     * @param  profile the profile item to check
     * @return         true if the given profile is already registered, false
     *                 otherwise
     */
    public boolean isProfileRegistered(Item profile);

    /**
     * Register an ORCID webhook callback related to the given profile.
     *
     * @param context the DSpace context
     * @param profile the profile to register
     */
    public void register(Context context, Item profile);

    /**
     * Unregister the ORCID webhook callback related to the given profile.
     *
     * @param context the DSpace context
     * @param profile the profile to unregister
     */
    public void unregister(Context context, Item profile);

    /**
     * Unregister the ORCID webhook callback related to the given ORCID iD.
     *
     * @param context the DSpace context
     * @param orcid   the orcid id to unregister
     */
    public void unregister(Context context, String orcid);
}
