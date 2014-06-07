/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.dto;

import java.util.List;

public class PJSearchDTO {

	private String searchMode;

	private String codeSearchMode;
	private String queryField = "grant_projecttitle";
	private String queryFieldFirst = "grant_projecttitle";
	private String queryFieldSecond = "grant_projecttitle";
	private String sponsor;
	private List<String> status;
	private String queryString;
	private String queryStringFirst;
	private String queryStringSecond;
	private String firstOperator;
	private String secondOperator;

	private String codeQuery;
	private Boolean advancedSyntax;

	public String getSearchMode() {
		return searchMode;
	}

	public void setSearchMode(String searchMode) {
		this.searchMode = searchMode;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getCodeQuery() {
		return codeQuery;
	}

	public void setCodeQuery(String staffQuery) {
		this.codeQuery = staffQuery;
	}

	public Boolean getAdvancedSyntax() {
		return advancedSyntax != null ? advancedSyntax : false;
	}

	public void setAdvancedSyntax(Boolean advancedSyntax) {
		this.advancedSyntax = advancedSyntax;
	}

	public void setQueryField(String queryField) {
		this.queryField = queryField;
	}

	public String getQueryField() {
		return queryField;
	}

	public void setCodeSearchMode(String staffNoSearchMode) {
		this.codeSearchMode = staffNoSearchMode;
	}

	public String getCodeSearchMode() {
		return codeSearchMode;
	}

	public String getQueryStringFirst() {
		return queryStringFirst;
	}

	public void setQueryStringFirst(String queryStringFirst) {
		this.queryStringFirst = queryStringFirst;
	}

	public String getQueryStringSecond() {
		return queryStringSecond;
	}

	public void setQueryStringSecond(String queryStringSecond) {
		this.queryStringSecond = queryStringSecond;
	}

	public String getFirstOperator() {
		return firstOperator;
	}

	public void setFirstOperator(String firstOperator) {
		this.firstOperator = firstOperator;
	}

	public String getSecondOperator() {
		return secondOperator;
	}

	public void setSecondOperator(String secondOperator) {
		this.secondOperator = secondOperator;
	}

	public String getQueryFieldFirst() {
		return queryFieldFirst;
	}

	public void setQueryFieldFirst(String queryFieldFirst) {
		this.queryFieldFirst = queryFieldFirst;
	}

	public String getQueryFieldSecond() {
		return queryFieldSecond;
	}

	public void setQueryFieldSecond(String queryFieldSecond) {
		this.queryFieldSecond = queryFieldSecond;
	}

	public String getSponsor() {
		return sponsor;
	}

	public void setSponsor(String sponsor) {
		this.sponsor = sponsor;
	}

	public List<String> getStatus() {
		return status;
	}

	public void setStatus(List<String> status) {
		this.status = status;
	}
}
