/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Implementation of {@link QAEventMessageRest} related to OPENAIRE events.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OpenaireQAEventMessageRest implements QAEventMessageRest {

    // pids
    private String type;

    private String value;

    private String pidHref;

    // abstract
    @JsonProperty(value = "abstract")
    private String abstractValue;

    // project
    private String openaireId;

    private String acronym;

    private String code;

    private String funder;

    private String fundingProgram;

    private String jurisdiction;

    private String title;

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getAbstractValue() {
        return abstractValue;
    }
    public void setAbstractValue(String abstractValue) {
        this.abstractValue = abstractValue;
    }
    public String getOpenaireId() {
        return openaireId;
    }
    public void setOpenaireId(String openaireId) {
        this.openaireId = openaireId;
    }
    public String getAcronym() {
        return acronym;
    }
    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getFunder() {
        return funder;
    }
    public void setFunder(String funder) {
        this.funder = funder;
    }
    public String getFundingProgram() {
        return fundingProgram;
    }
    public void setFundingProgram(String fundingProgram) {
        this.fundingProgram = fundingProgram;
    }
    public String getJurisdiction() {
        return jurisdiction;
    }
    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getPidHref() {
        return pidHref;
    }

    public void setPidHref(String pidHref) {
        this.pidHref = pidHref;
    }

}
