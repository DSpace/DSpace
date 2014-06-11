/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.Project;

public class ProjectAuthority extends CRISAuthority
{

    @Override
    protected int getCRISTargetTypeID()
    {
        return CrisConstants.PROJECT_TYPE_ID;
    }

    @Override
    protected Class<Project> getCRISTargetClass()
    {
        return Project.class;
    }

}
