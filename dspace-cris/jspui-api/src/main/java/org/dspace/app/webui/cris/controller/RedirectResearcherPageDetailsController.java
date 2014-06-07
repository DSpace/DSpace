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
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.JSPManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * This SpringMVC controller has been added to handle RP details URL also with
 * the form:<br> 
 * [hub-url]/rp/rp/details.html?id=[rpidentifier]
 * <br>
 * doing a simple redirect to the canonical URL: [hub-url]/rp/[rpidentifier] 
 * 
 * @author cilea
 * 
 */
public class RedirectResearcherPageDetailsController extends
        ParameterizableViewController
{

    /** log4j category */
    private static Logger log = Logger
            .getLogger(RedirectResearcherPageDetailsController.class);

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {        
        String paramRPId = request.getParameter("id");
        String auth = null;
        if (paramRPId == null)
        {
            try
            {
                paramRPId = request.getParameter("crisid");
                if (paramRPId == null)
                {
                    paramRPId = request.getParameter("sourceid");
                }
                else
                {
                    paramRPId = ResearcherPageUtils.getStaffNumber(paramRPId);
                }
                auth = ResearcherPageUtils.getRPIdentifierByStaffno(paramRPId);
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
                JSPManager.showInvalidIDError(request, response, paramRPId,
                        CrisConstants.RP_TYPE_ID);
            }
        }
        else
        {
            auth = ResearcherPageUtils.getPersistentIdentifier(
                    Integer.parseInt(paramRPId), ResearcherPage.class);
        }
        if (auth == null || auth.isEmpty())
        {
            // JSPManager.showInternalError(request, response);
            JSPManager.showInvalidIDError(request, response, paramRPId,
                    CrisConstants.RP_TYPE_ID);
        }
        return new ModelAndView("redirect:/cris/rp/" + auth);
    }

 
}
