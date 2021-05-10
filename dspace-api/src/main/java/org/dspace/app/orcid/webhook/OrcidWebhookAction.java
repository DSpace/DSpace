/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.webhook;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface for classes that act on a profile with a specified orcid when a
 * webhook is received from ORCID for an update associated with that profile.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidWebhookAction {

    /**
     * Perform an action on the given profile.
     *
     * @param context the DSpace context
     * @param profile the profile
     * @param orcid   the profile's orcid id
     */
    void perform(Context context, Item profile, String orcid);
}
