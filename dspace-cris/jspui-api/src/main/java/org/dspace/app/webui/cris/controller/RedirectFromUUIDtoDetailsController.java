/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * This SpringMVC controller has been added to handle RP details URL also with
 * the form:<br> 
 * [hub-url]/rp/rp/details.html?persistentIdentifier=[rpidentifier]
 * <br>
 * doing a simple redirect to the canonical URL: [hub-url]/rp/[rpidentifier] 
 * 
 * @author cilea
 * 
 */
public class RedirectFromUUIDtoDetailsController extends
        ParameterizableViewController
{

    /** log4j category */
    private static Logger log = Logger
            .getLogger(RedirectFromUUIDtoDetailsController.class);

    private ApplicationService applicationService;
    
    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {        
        String uuid = extractUUID(request);
        ACrisObject crisObject = getApplicationService().getEntityByUUID(uuid);
        return new ModelAndView("redirect:/cris/"+ crisObject.getPublicPath() + "/" + ResearcherPageUtils.getPersistentIdentifier(crisObject));     
    }

    private String extractUUID(HttpServletRequest request)
    {
        String path = request.getPathInfo().substring(1); // remove first /
        String[] splitted = path.split("/");        
        return splitted[splitted.length-1];
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
 
}
