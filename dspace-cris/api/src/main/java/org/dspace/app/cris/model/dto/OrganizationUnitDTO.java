/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.dto;

import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.util.ResearcherPageUtils;


/**
 * This class is the DTO used to manage a single OrganizationUnit in the
 * administrative functionality
 * 
 * 
 * @author pascarelli
 * 
 */
public class OrganizationUnitDTO {
	
	private Integer id;
	private String sourceID;
	private String uuid;
	private Boolean status;
	private String name;
		
	private OrganizationUnit organizationUnit;
	
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
	
	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
	public String getName() {
		return name;
	}
	public void setName(String title) {
		this.name = title;
	}	
    public void setOrganizationUnit(OrganizationUnit organizationUnit)
    {
        this.organizationUnit = organizationUnit;
    }
    public OrganizationUnit getOrganizationUnit()
    {
        return organizationUnit;
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
