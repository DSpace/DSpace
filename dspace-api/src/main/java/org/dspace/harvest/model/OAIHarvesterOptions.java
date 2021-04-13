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

    private final Boolean forceSynchronization;

    private final Boolean recordValidationEnabled;

    private final Boolean itemValidationEnabled;

    private final Boolean submissionEnabled;

    public OAIHarvesterOptions(Boolean forceSynchronization, Boolean recordValidationEnabled,
        Boolean itemValidationEnabled, Boolean submissionEnabled) {
        this(UUID.randomUUID(), forceSynchronization, recordValidationEnabled, itemValidationEnabled,
            submissionEnabled);
    }

    public OAIHarvesterOptions(UUID processId, Boolean forceSynchronization, Boolean recordValidationEnabled,
        Boolean itemValidationEnabled, Boolean submissionEnabled) {
        this.processId = processId;
        this.forceSynchronization = forceSynchronization;
        this.itemValidationEnabled = itemValidationEnabled;
        this.recordValidationEnabled = recordValidationEnabled;
        this.submissionEnabled = submissionEnabled;
    }

    public UUID getProcessId() {
        return processId;
    }

    public Boolean isForceSynchronization() {
        return forceSynchronization;
    }

    public Boolean isSubmissionEnabled() {
        return submissionEnabled;
    }

    public Boolean isRecordValidationEnabled() {
        return recordValidationEnabled;
    }

    public Boolean isItemValidationEnabled() {
        return itemValidationEnabled;
    }

}
