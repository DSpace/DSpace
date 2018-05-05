/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.openaireproject;

public class OpenAireProject {
	
	private String title;
	private String code;
	private String jurisdiction;
	private String funder;
	private String fundingProgram;
	
	public OpenAireProject() {
		
	}
	
	public OpenAireProject(String title,String code, String funding,String jurisdiction,String funder) {
		this.code=code;
		this.funder=funder;
		this.jurisdiction=jurisdiction;
		this.title=title;
		this.fundingProgram=funding;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getJurisdiction() {
		return jurisdiction;
	}
	public void setJurisdiction(String jurisdiction) {
		this.jurisdiction = jurisdiction;
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

}
