/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.model;

import java.util.UUID;

/**
 * A VO that contains the configured option for a single run of the OAI
 * harvesting.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterOptions {

    private final UUID processId;

    private final boolean forceSynchronization;

    private final boolean validationEnabled;

    private final boolean submissionEnabled;

    public OAIHarvesterOptions(boolean forceSynchronization, boolean validationEnabled, boolean submissionEnabled) {
        this(UUID.randomUUID(), forceSynchronization, validationEnabled, submissionEnabled);
    }

    public OAIHarvesterOptions(UUID processId, boolean forceSynchronization, boolean validationEnabled,
        boolean submissionEnabled) {
        this.processId = processId;
        this.forceSynchronization = forceSynchronization;
        this.validationEnabled = validationEnabled;
        this.submissionEnabled = submissionEnabled;
    }

    public UUID getProcessId() {
        return processId;
    }

    public boolean isForceSynchronization() {
        return forceSynchronization;
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public boolean isSubmissionEnabled() {
        return submissionEnabled;
    }

}
