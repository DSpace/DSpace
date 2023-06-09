/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.profile;

/**
 * Enum that model the allowed values to configure the ORCID synchronization
 * mode.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OrcidSynchronizationMode {

    /**
     * Mode in which the user can manually decide when to synchronize data with
     * ORCID.
     */
    MANUAL,

    /**
     * Mode in which synchronizations with ORCID occur through an automatic process.
     */
    BATCH;
}
