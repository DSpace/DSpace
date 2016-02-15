/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ExternalService;

public class CrisService implements ExternalService
{

    private ApplicationService applicationService;
    
    @Override
    public DSpaceObject getObject(String externalId)
    {
        DSpaceObject dso = applicationService.getEntityByCrisId(externalId, ResearcherPage.class);
        if (dso == null) {
            dso = applicationService.getEntityByCrisId(externalId, OrganizationUnit.class);
            if (dso == null) {
                dso = applicationService.getEntityByCrisId(externalId, Project.class);
                if (dso == null) {
                    dso = applicationService.getEntityByCrisId(externalId, ResearchObject.class);
                }
            }
        }
        return dso;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

}
