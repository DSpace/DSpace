package org.dspace.app.rest.model;

import org.dspace.app.nbevent.service.dto.MessageDto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NBEventMessage {
	private String type;
	private String value;
	@JsonProperty("abstract")
	private String abstractField;
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

	public String getAbstractField() {
		return abstractField;
	}

	public void setAbstractField(String abstractField) {
		this.abstractField = abstractField;
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

	public NBEventMessage() {

	}

	public NBEventMessage(MessageDto dto) {
		this.abstractField = dto.getAbstracts();
		this.code = dto.getCode();
		this.funder = dto.getFunder();
		this.fundingProgram = dto.getFundingProgram();
		this.jurisdiction = dto.getJurisdiction();
		this.title = dto.getTitle();
		this.type = dto.getType();
		this.value = dto.getValue();
	}

}
