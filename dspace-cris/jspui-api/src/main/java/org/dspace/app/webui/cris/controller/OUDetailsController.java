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
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.jdyna.BoxOrganizationUnit;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUProperty;
import org.dspace.app.cris.model.jdyna.TabOrganizationUnit;
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
public class OUDetailsController
        extends
        SimpleDynaController<OUProperty, OUPropertiesDefinition, BoxOrganizationUnit, TabOrganizationUnit>
{
    private CrisSubscribeService subscribeService;

    public OUDetailsController(Class<OrganizationUnit> anagraficaObjectClass,
            Class<OUPropertiesDefinition> classTP,
            Class<TabOrganizationUnit> classT, Class<BoxOrganizationUnit> classH)
            throws InstantiationException, IllegalAccessException
    {
        super(anagraficaObjectClass, classTP, classT, classH);
    }

    /** log4j category */
    private static Logger log = Logger
            .getLogger(OUDetailsController.class);

    @Override
    public ModelAndView handleDetails(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        Map<String, Object> model = new HashMap<String, Object>();

        OrganizationUnit ou = extractOrganizationUnit(request);

        if (ou == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "OU page not found");
            return null;
        }

        Context context = UIUtil.obtainContext(request);
        EPerson currentUser = context.getCurrentUser();
        if ((ou.getStatus() == null || ou.getStatus().booleanValue() == false)
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
            model.put("ou_page_menu", new Boolean(true));
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
                    ou);
            model.put("subscribed", subscribed);            
        }
        
        request.setAttribute("sectionid", StatsConfig.DETAILS_SECTION);
        new DSpace().getEventService().fireEvent(
                new UsageEvent(
                        UsageEvent.Action.VIEW,
                        request,
                        context,
                        ou));
        
        mvc.getModel().putAll(model);
        mvc.getModel().put("ou", ou);
        return mvc;
    }

    @Override
    protected List<TabOrganizationUnit> findTabsWithVisibility(
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

        List<TabOrganizationUnit> tabs = applicationService.getTabsByVisibility(
                TabOrganizationUnit.class, isAdmin);

        return tabs;
    }

    @Override
    protected Integer getTabId(HttpServletRequest request)
    {
        String tabName = extractTabName(request);
        if (StringUtils.isNotEmpty(tabName))
        {
            TabOrganizationUnit tab = applicationService.getTabByShortName(
                    TabOrganizationUnit.class, tabName);
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
        JSPManager.showAuthorizeError(request, response, new AuthorizeException(ex.getMessage()));
        //response.sendRedirect("/cris/ou/details?id=" + objectId);
    }

    @Override
    protected Integer getAnagraficaId(HttpServletRequest request)
    {
        OrganizationUnit ou = null;
        try
        {
            ou = extractOrganizationUnit(request);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
        return ou.getDynamicField().getId();
    }

    private OrganizationUnit extractOrganizationUnit(HttpServletRequest request)
    {

        Integer id = extractEntityId(request);
        return ((ApplicationService) applicationService).get(OrganizationUnit.class,
                id);

    }
    
    protected Integer getRealPersistentIdentifier(String persistentIdentifier)
    {
        return ResearcherPageUtils.getRealPersistentIdentifier(persistentIdentifier, OrganizationUnit.class);
    }
    
    public void setSubscribeService(CrisSubscribeService rpSubscribeService)
    {
        this.subscribeService = rpSubscribeService;
    }
}
