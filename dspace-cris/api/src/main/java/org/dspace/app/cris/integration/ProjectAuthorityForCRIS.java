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

public class ProjectAuthorityForCRIS extends CRISAuthorityForCRIS<Project>
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

	@Override
	public Project getNewCrisObject() {
		return new Project();
	}    

}
