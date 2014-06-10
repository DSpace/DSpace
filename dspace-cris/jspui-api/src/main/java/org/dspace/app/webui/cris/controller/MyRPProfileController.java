/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Context;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Redirect the user to his "home page" on a role basis
 * 
 * @author cilea
 * 
 */
public class MyRPProfileController extends ParameterizableViewController
{
    /**
     * the applicationService for query the RP db, injected by Spring IoC
     */
    private ApplicationService applicationService;

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        Context context = UIUtil.obtainContext(request);
        String crisID = context.getCrisID();
        if (crisID == null)
        {
            throw new ServletException(
                    "Wrong data or configuration: access to the my rp servlet without a valid user: there is no user logged in");
        }

        ResearcherPage rp = applicationService.getEntityByCrisId(crisID,
                ResearcherPage.class);
        if (rp != null && rp.getStatus() != null
                && rp.getStatus().booleanValue())
        {
            response.sendRedirect(request.getContextPath() + "/cris/rp/"
                    + ResearcherPageUtils.getPersistentIdentifier(rp));
        }
        else
        {
            // the researcher page is not active so redirect the user to the
            // home page
            response.sendRedirect(request.getContextPath() + "/");
        }
        // nothing to save so abort the context to release resources
        context.abort();
        return null;
    }
}
