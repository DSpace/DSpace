/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.dto;

import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.util.ResearcherPageUtils;


/**
 * This class is the DTO used to manage a single Project in the
 * administrative functionality
 * 
 * 
 * @author pascarelli
 * 
 */
public class ProjectDTO {
	
	private Integer id;
	private String uuid;
	private String sourceID;
	private Boolean status;
	private String title;
	private String year;
	private String investigators;
	private Project grant;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getSourceID() {
		return sourceID;
	}
	public void setSourceID(String code) {
		this.sourceID = code;
	}
	    
  
	public Project getGrant() {
		return grant;
	}
	public void setGrant(Project grant) {
		this.grant = grant;
	}
	
	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public String getInvestigators() {
		return investigators;
	}
	public void setInvestigators(String investigators) {
		this.investigators = investigators;
	}
    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
    public String getUuid()
    {
        return uuid;
    }


}
