/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.authenticate.PostLoggedInAction;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class ResearcherLoggedInAction implements PostLoggedInAction
{

    private ApplicationService applicationService;
    
    private String typeSourceRef;
    
	/** the logger */
	private static Logger log = Logger.getLogger(ResearcherLoggedInAction.class);
    
    @Override
    public void loggedIn(Context context, HttpServletRequest request,
            EPerson eperson)
    {
        try
        {
            ResearcherPage rp = applicationService.getResearcherPageByEPersonId(eperson.getID());
            String key = typeSourceRef.equals("netid")?eperson.getNetid():eperson.getMetadata(typeSourceRef);
            if(rp==null && StringUtils.isNotBlank(key)) {
				rp = applicationService.getEntityBySourceId(typeSourceRef,
				        key, ResearcherPage.class);
                if (rp != null) {
					if(rp.getEpersonID()!=null) {
	                    if (rp.getEpersonID() != eperson.getID())
	                    {
	                        rp.setEpersonID(eperson.getID());
	                    }
	                }
	                else {
	                    rp.setEpersonID(eperson.getID());
	                }
					applicationService.saveOrUpdate(ResearcherPage.class, rp, false);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public void setTypeSourceRef(String netidSourceRef) {
		this.typeSourceRef = netidSourceRef;
	}
}
