/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.util.ResearcherPageUtils;

public class ProjectAuthority extends CRISAuthority
{

    @Override
    public int getCRISTargetTypeID()
    {
        return CrisConstants.PROJECT_TYPE_ID;
    }

    @Override
    public Class<Project> getCRISTargetClass()
    {
        return Project.class;
    }
    
    @Override
    public String getPublicPath() {
    	return "project";
    }

	protected String getDisplayEntry(ACrisObject cris) {
		Project pj = (Project) cris;
		StringBuffer sb = new StringBuffer();
		sb.append(cris.getName());
		String acronym = ResearcherPageUtils.getStringValue(cris, "acronym");
		String projectNumber = ResearcherPageUtils.getStringValue(cris, "projectNumber");
		String projectAPType = ResearcherPageUtils.getStringValue(cris, "projectAPType");
		String year = ResearcherPageUtils.getStringValue(cris, "year");
		if (StringUtils.isNotBlank(projectAPType) || StringUtils.isNotBlank(projectNumber)
				|| StringUtils.isNotBlank(acronym) || StringUtils.isNotBlank(year)) {
			sb.append("<br/>");
			if (StringUtils.isNotBlank(acronym)) {
				sb.append("<b>").append(acronym).append("</b>. ");
			}
			if (StringUtils.isNotBlank(projectAPType)) {
				sb.append(projectAPType);
			}
			if (StringUtils.isNotBlank(projectAPType) && StringUtils.isNotBlank(projectNumber)) {
				sb.append(" - ");
			}
			if (StringUtils.isNotBlank(projectNumber)) {
				sb.append(projectNumber);
			}
			if (StringUtils.isNotBlank(year)) {
				sb.append(" (").append(year).append(")");
			}
		}
		return sb.toString();
	}

	@Override
	public Project getNewCrisObject() {
		return new Project();
	}    

}
