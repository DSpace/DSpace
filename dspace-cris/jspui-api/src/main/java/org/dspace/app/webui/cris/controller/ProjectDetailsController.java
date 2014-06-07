/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import it.cilea.osd.jdyna.web.controller.SimpleDynaController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.jdyna.BoxProject;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectProperty;
import org.dspace.app.cris.model.jdyna.TabProject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.CrisSubscribeService;
import org.dspace.app.cris.statistics.util.StatsConfig;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;
import org.springframework.web.servlet.ModelAndView;

/**
 * This SpringMVC controller is used to build the ResearcherPage details page.
 * The DSpace items included in the details are returned by the DSpace Browse
 * System.
 * 
 * @author cilea
 * 
 */
public class ProjectDetailsController
        extends
        SimpleDynaController<ProjectProperty, ProjectPropertiesDefinition, BoxProject, TabProject>
{

    private CrisSubscribeService subscribeService;

    
    
    public ProjectDetailsController(Class<Project> anagraficaObjectClass,
            Class<ProjectPropertiesDefinition> classTP,
            Class<TabProject> classT, Class<BoxProject> classH)
            throws InstantiationException, IllegalAccessException
    {
        super(anagraficaObjectClass, classTP, classT, classH);
    }

    /** log4j category */
    private static Logger log = Logger
            .getLogger(ProjectDetailsController.class);

    @Override
    public ModelAndView handleDetails(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        Map<String, Object> model = new HashMap<String, Object>();

        Project grant = extractProject(request);

        if (grant == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Grant page not found");
            return null;
        }

        Context context = UIUtil.obtainContext(request);

        EPerson currentUser = context.getCurrentUser();
        
        if ((grant.getStatus() == null || grant.getStatus().booleanValue() == false)
                && !AuthorizeManager.isAdmin(context))
        {
            
            if (currentUser != null
                    || Authenticate.startAuthentication(context, request,
                            response))
            {
                // Log the error
                log.info(LogManager
                        .getHeader(context, "authorize_error",
                                "Only system administrator can access to disabled researcher page"));

                JSPManager
                        .showAuthorizeError(
                                request,
                                response,
                                new AuthorizeException(
                                        "Only system administrator can access to disabled researcher page"));
            }
            return null;
        }

        if (AuthorizeManager.isAdmin(context))
        {
            model.put("grant_page_menu", new Boolean(true));
        }

        ModelAndView mvc = null;

        try
        {
            mvc = super.handleDetails(request, response);
        }
        catch (RuntimeException e)
        {
            return null;
        }
        
        
        if (subscribeService != null)
        {
            boolean subscribed = subscribeService.isSubscribed(currentUser,
                    grant);
            model.put("subscribed", subscribed);
           
        }
        
        request.setAttribute("sectionid", StatsConfig.DETAILS_SECTION);
        new DSpace().getEventService().fireEvent(
                new UsageEvent(
                        UsageEvent.Action.VIEW,
                        request,
                        context,
                        grant));
        
        mvc.getModel().putAll(model);
        mvc.getModel().put("project", grant);
        return mvc;
    }

    @Override
    protected List<TabProject> findTabsWithVisibility(
            HttpServletRequest request, Map<String, Object> model,
            HttpServletResponse response) throws SQLException, Exception
    {

        // check admin authorization
        Boolean isAdmin = null; // anonymous access
        Context context = UIUtil.obtainContext(request);

        if (AuthorizeManager.isAdmin(context))
        {
            isAdmin = true; // admin
        }

        List<TabProject> tabs = applicationService.getTabsByVisibility(
                TabProject.class, isAdmin);

        return tabs;
    }

    @Override
    protected Integer getTabId(HttpServletRequest request)
    {
        String tabName = extractTabName(request);
        if (StringUtils.isNotEmpty(tabName))
        {
            TabProject tab = applicationService.getTabByShortName(
                    TabProject.class, tabName);
            if (tab != null)
                return tab.getId();
        }
        return null;
    }

    @Override
    protected String extractAnchorId(HttpServletRequest request)
    {
        String type = request.getParameter("open");

        if (type != null && !type.isEmpty())
        {
            return type;
        }

        return "";
    }

    private String extractTabName(HttpServletRequest request)
    {
        String path = request.getPathInfo().substring(1); // remove first /
        String[] splitted = path.split("/");
        if (splitted.length > 2)
        {
            return splitted[2].replaceAll("\\.html", "");
        }
        else
            return null;
    }

    @Override
    protected void sendRedirect(HttpServletRequest request,
            HttpServletResponse response, Exception ex, String objectId)
            throws IOException, ServletException
    {
        // response.sendRedirect("/cris/project/details?id=" + objectId);
        JSPManager.showAuthorizeError(request, response,
                new AuthorizeException(ex.getMessage()));
    }

    @Override
    protected Integer getAnagraficaId(HttpServletRequest request)
    {
        Project grant = null;
        try
        {
            grant = extractProject(request);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
        return grant.getDynamicField().getId();
    }

    private Project extractProject(HttpServletRequest request)
    {

        Integer id = extractEntityId(request);        
        return ((ApplicationService) applicationService).get(Project.class,id);

    }
    
    protected Integer getRealPersistentIdentifier(String persistentIdentifier)
    {
        return ResearcherPageUtils.getRealPersistentIdentifier(persistentIdentifier, Project.class);
    }
    
    public void setSubscribeService(CrisSubscribeService rpSubscribeService)
    {
        this.subscribeService = rpSubscribeService;
    }
}
