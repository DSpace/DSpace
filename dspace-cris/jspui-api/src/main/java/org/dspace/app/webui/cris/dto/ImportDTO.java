/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.dto;

import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.springframework.web.multipart.MultipartFile;

public class ImportDTO {
	
	private MultipartFile file;
	
	private boolean template;
	
	private String format;
	
	private String type;
	
	public void setFile(MultipartFile file) {
		this.file = file;
	}

	public MultipartFile getFile() {
		return file;
	}

	public String getTargetEntity() {
	    switch (type)
        {
        case "researcher":
            return ResearcherPage.class.getName();
        case "project":
            return Project.class.getName();
        case "orgunit":
            return OrganizationUnit.class.getName();
        default:
            return ResearchObject.class.getName();
        }
	}

	public boolean isTemplate() {
		return template;
	}

	public void setTemplate(boolean template) {
		this.template = template;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
}
