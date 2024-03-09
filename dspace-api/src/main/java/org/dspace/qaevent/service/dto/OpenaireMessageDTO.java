/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Implementation of {@link QAMessageDTO} that model message coming from OPENAIRE.
 * @see <a href="https://graph.openaire.eu/docs/category/entities" target="_blank"> see </a>
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenaireMessageDTO implements QAMessageDTO {

    @JsonProperty("pids[0].value")
    private String value;

    @JsonProperty("pids[0].type")
    private String type;

    @JsonProperty("instances[0].hostedby")
    private String instanceHostedBy;

    @JsonProperty("instances[0].instancetype")
    private String instanceInstanceType;

    @JsonProperty("instances[0].license")
    private String instanceLicense;

    @JsonProperty("instances[0].url")
    private String instanceUrl;

    @JsonProperty("abstracts[0]")
    private String abstracts;

    @JsonProperty("projects[0].acronym")
    private String acronym;

    @JsonProperty("projects[0].code")
    private String code;

    @JsonProperty("projects[0].funder")
    private String funder;

    @JsonProperty("projects[0].fundingProgram")
    private String fundingProgram;

    @JsonProperty("projects[0].jurisdiction")
    private String jurisdiction;

    @JsonProperty("projects[0].openaireId")
    private String openaireId;

    @JsonProperty("projects[0].title")
    private String title;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInstanceHostedBy() {
        return instanceHostedBy;
    }

    public void setInstanceHostedBy(String instanceHostedBy) {
        this.instanceHostedBy = instanceHostedBy;
    }

    public String getInstanceInstanceType() {
        return instanceInstanceType;
    }

    public void setInstanceInstanceType(String instanceInstanceType) {
        this.instanceInstanceType = instanceInstanceType;
    }

    public String getInstanceLicense() {
        return instanceLicense;
    }

    public void setInstanceLicense(String instanceLicense) {
        this.instanceLicense = instanceLicense;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public String getAbstracts() {
        return abstracts;
    }

    public void setAbstracts(String abstracts) {
        this.abstracts = abstracts;
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

    public String getOpenaireId() {
        return openaireId;
    }

    public void setOpenaireId(String openaireId) {
        this.openaireId = openaireId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
