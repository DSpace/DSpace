/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.webhook;

/**
 * Enum that model all the allowed property values for ORCID webhook
 * registration configuration.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidWebhookMode {

    /**
     * The webhook registration is disabled.
     */
    DISABLED,

    /**
     * The webhook registration is done only for profile linked to ORCID.
     */
    ONLY_LINKED,

    /**
     * The webhook registration is done for all the profiles with an ORCID id.
     */
    ALL;
}
