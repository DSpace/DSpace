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

    private final boolean recordValidationEnabled;

    private final boolean itemValidationEnabled;

    private final boolean submissionEnabled;

    public OAIHarvesterOptions(boolean forceSynchronization, boolean recordValidationEnabled,
        boolean itemValidationEnabled, boolean submissionEnabled) {
        this(UUID.randomUUID(), forceSynchronization, recordValidationEnabled, itemValidationEnabled,
            submissionEnabled);
    }

    public OAIHarvesterOptions(UUID processId, boolean forceSynchronization, boolean recordValidationEnabled,
        boolean itemValidationEnabled, boolean submissionEnabled) {
        this.processId = processId;
        this.forceSynchronization = forceSynchronization;
        this.itemValidationEnabled = itemValidationEnabled;
        this.recordValidationEnabled = recordValidationEnabled;
        this.submissionEnabled = submissionEnabled;
    }

    public UUID getProcessId() {
        return processId;
    }

    public boolean isForceSynchronization() {
        return forceSynchronization;
    }
    public boolean isSubmissionEnabled() {
        return submissionEnabled;
    }

    public boolean isRecordValidationEnabled() {
        return recordValidationEnabled;
    }

    public boolean isItemValidationEnabled() {
        return itemValidationEnabled;
    }

}
