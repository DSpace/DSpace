/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.model;

import org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl.DeduplicationFlag;

/***
 * Define a Json object to trace the choices that can be done to resolve a
 * duplication.
 * <p>
 * Use VERIFY as type mark the deduplication to be verified. Use REJECT as type
 * mark the deduplication to be ignored.
 *
 * A DeduplicationFlag store the current status (MATCH, VERIFY or REJECT).
 *
 * @author 4Science
 */
public class DuplicateDecisionObjectRest {

    private String value;

    private DuplicateDecisionType type;

    private String note;

    /**
     * Get the duplicate decision value
     * @return  MATCH, REJECT or VERIFY
     */
    public DuplicateDecisionValue getValue() {
        return (value != null) ? DuplicateDecisionValue.fromString(value) : null;
    }

    /**
     * Set the duplicate decision value
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the decision type
     * @return  WORKSPACE, WORKFLOW or ADMIN
     */
    public DuplicateDecisionType getType() {
        return type;
    }

    /**
     * Set the decision type
     * @param type
     */
    public void setType(DuplicateDecisionType type) {
        this.type = type;
    }

    /**
     * Get the text note for this decision
     * @return  note
     */
    public String getNote() {
        return note;
    }

    /**
     * Set the text note for this decision
     * @param note
     */
    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Get the flag (for use as a Solr field) for the decision value and type
     * @return  solr flag, eg. reject_admin, verify_ws
     */
    public DeduplicationFlag getDecisionFlag() {
        DeduplicationFlag flag = DeduplicationFlag.MATCH;
        if (getValue() != null) {
            switch (getValue()) {
                case REJECT:
                    flag = getRejectDecisionFlagByType();
                    break;
                case VERIFY:
                    flag = getVerifyDecisionFlagByType();
                    break;
                default:
                    // use default
                    break;
            }
        }
        return flag;
    }

    /**
     * Get the specific reject flag (for use as a Solr field) for a decision type
     * @return  solr flag, eg. reject_ws, reject_admin
     */
    private DeduplicationFlag getRejectDecisionFlagByType() {
        DeduplicationFlag flag = null;
        switch (getType()) {
            case ADMIN:
                flag = DeduplicationFlag.REJECTADMIN;
                break;
            case WORKSPACE:
                flag = DeduplicationFlag.REJECTWS;
                break;
            case WORKFLOW:
                flag = DeduplicationFlag.REJECTWF;
                break;
            default:
                // do nothing
                break;
        }

        return flag;
    }

    /**
     * Get the specific verify flag (for use as a Solr field) for a decision type
     * @return  solr flag, eg. verify_ws, verify_wf
     */
    private DeduplicationFlag getVerifyDecisionFlagByType() {
        DeduplicationFlag flag = null;
        switch (getType()) {
            case ADMIN:
                flag = null;
                break;
            case WORKSPACE:
                flag = DeduplicationFlag.VERIFYWS;
                break;
            case WORKFLOW:
                flag = DeduplicationFlag.VERIFYWF;
                break;
            default:
                // do nothing
                break;
        }

        return flag;
    }

}