/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.deduplication.model.DuplicateDecisionValue;
import org.dspace.app.rest.model.ItemRest;

/**
 * Java Bean to expose the section license during in progress submission.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 */
public class DuplicateMatch implements SectionData {

    @JsonProperty
    private String submitterDecision;

    @JsonProperty
    private String workflowDecision;

    @JsonProperty
    private String adminDecision;

    @JsonProperty
    private String submitterNote;

    @JsonProperty
    private String workflowNote;

    @JsonProperty
    private ItemRest matchObject;

    //@LinkRest(linkClass = ItemRest.class)
    @JsonIgnore
    public ItemRest getMatchObject() {
        return matchObject;
    }

    public void setMatchObject(ItemRest matchObject) {
        this.matchObject = matchObject;
    }

    public String getSubmitterDecision() {
        return submitterDecision;
    }

    public void setSubmitterDecision(DuplicateDecisionValue submitterDecision) {
        if (submitterDecision != null) {
            this.submitterDecision = submitterDecision.toString();
        }
    }

    public String getWorkflowDecision() {
        return workflowDecision;
    }

    public void setWorkflowDecision(DuplicateDecisionValue workflowDecision) {
        if (workflowDecision != null) {
            this.workflowDecision = workflowDecision.toString();
        }
    }

    public String getAdminDecision() {
        return adminDecision;
    }

    public void setAdminDecision(DuplicateDecisionValue adminDecision) {
        if (adminDecision != null) {
            this.adminDecision = adminDecision.toString();
        }
    }

    public String getSubmitterNote() {
        return submitterNote;
    }

    public void setSubmitterNote(String submitterNote) {
        this.submitterNote = submitterNote;
    }

    public String getWorkflowNote() {
        return workflowNote;
    }

    public void setWorkflowNote(String workflowNote) {
        this.workflowNote = workflowNote;
    }

    @JsonIgnore
    public boolean anyDecisionMade() {
        return StringUtils.isNotBlank(getAdminDecision()) || StringUtils.isNotBlank(getSubmitterDecision())
                || StringUtils.isNotBlank(getWorkflowDecision());
    }

}