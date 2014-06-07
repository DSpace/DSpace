/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.dto;

public class RPSearchDTO {
	   private String searchMode;
	    private String rpSearchMode;
	    private String staffNoSearchMode;
	    private String queryField = "names";
	    private String queryFieldFirst = "names";
	    private String queryFieldSecond = "names";
	    private String faculty;
	    private String queryString;
	    private String queryStringFirst;
	    private String queryStringSecond;
	    private String firstOperator;
	    private String secondOperator;
	    private String rpQuery;
		private String staffQuery;
	    private Boolean advancedSyntax;
	    
	    
	    public String getSearchMode()
	    {
	        return searchMode;
	    }
	    public void setSearchMode(String searchMode)
	    {
	        this.searchMode = searchMode;
	    }
	    public String getQueryString()
	    {   	
	        return queryString;
	    }
	    public void setQueryString(String queryString)
	    {
	        this.queryString = queryString;
	    }
	    public String getRpQuery()
	    {
	        return rpQuery;
	    }
	    public void setRpQuery(String rpQuery)
	    {
	        this.rpQuery = rpQuery;
	    }
	    public String getStaffQuery()
	    {
	        return staffQuery;
	    }
	    public void setStaffQuery(String staffQuery)
	    {
	        this.staffQuery = staffQuery;
	    }
	    public Boolean getAdvancedSyntax()
	    {
	        return advancedSyntax != null?advancedSyntax:false;
	    }
	    public void setAdvancedSyntax(Boolean advancedSyntax)
	    {
	        this.advancedSyntax = advancedSyntax;
	    }
	    public void setQueryField(String queryField)
	    {
	        this.queryField = queryField;
	    }
	    public String getQueryField()
	    {
	        return queryField;
	    }
	    public void setRpSearchMode(String rpSearchMode)
	    {
	        this.rpSearchMode = rpSearchMode;
	    }
	    public String getRpSearchMode()
	    {
	        return rpSearchMode;
	    }
	    public void setStaffNoSearchMode(String staffNoSearchMode)
	    {
	        this.staffNoSearchMode = staffNoSearchMode;
	    }
	    public String getStaffNoSearchMode()
	    {
	        return staffNoSearchMode;
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
		public String getFaculty() {
			return faculty;
		}
		public void setFaculty(String faculty) {
			this.faculty = faculty;
		}
	}
