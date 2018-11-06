/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.jdyna.BoxOrganizationUnit;
import org.dspace.app.cris.model.jdyna.EditTabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUProperty;
import org.dspace.app.cris.model.jdyna.TabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.TabProject;
import org.dspace.app.cris.model.jdyna.VisibilityTabConstant;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.CrisSubscribeService;
import org.dspace.app.cris.statistics.util.StatsConfig;
import org.dspace.app.cris.util.ICrisHomeProcessor;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.cris.metrics.ItemMetricsDTO;
import org.dspace.app.webui.cris.util.CrisAuthorizeManager;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;
import org.springframework.web.servlet.ModelAndView;

import it.cilea.osd.jdyna.web.controller.SimpleDynaController;

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
    
    private List<ICrisHomeProcessor<OrganizationUnit>> processors;

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
        
        EPerson currUser = context.getCurrentUser();
        if(currUser != null) {
            model.put("isLoggedIn", new Boolean(true));    
        }
        else {
            model.put("isLoggedIn", new Boolean(false));
        }
        
        boolean isAdmin = CrisAuthorizeManager.isAdmin(context,ou);
        boolean canEdit = isAdmin || CrisAuthorizeManager.canEdit(context, applicationService, EditTabOrganizationUnit.class, ou);
        if ((ou.getStatus() == null || ou.getStatus().booleanValue() == false)
                && !isAdmin)
        {
            
            if (currUser != null
                    || Authenticate.startAuthentication(context, request,
                            response))
            {
                // Log the error
                log.info(LogManager
                        .getHeader(context, "authorize_error",
                                "Only administrator can access to disabled researcher page"));

                JSPManager
                        .showAuthorizeError(
                                request,
                                response,
                                new AuthorizeException(
                                        "Only administrator can access to disabled researcher page"));
            }
            return null;
        }

        if (isAdmin)
        {
            model.put("ou_page_menu", new Boolean(true));
        }

		if (canEdit) 
		{
			model.put("canEdit", new Boolean(true));
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
            boolean subscribed = subscribeService.isSubscribed(currUser,
                    ou);
            model.put("subscribed", subscribed);            
        }
        
        List<ICrisHomeProcessor<OrganizationUnit>> resultProcessors = new ArrayList<ICrisHomeProcessor<OrganizationUnit>>();
        Map<String, Object> extraTotal = new HashMap<String, Object>();
        Map<String, ItemMetricsDTO> metricsTotal = new HashMap<String, ItemMetricsDTO>();
        List<String> metricsTypeTotal = new ArrayList<String>();
        for (ICrisHomeProcessor processor : processors)
        {
            if (OrganizationUnit.class.isAssignableFrom(processor.getClazz()))
            {
                processor.process(context, request, response, ou);
                Map<String, Object> extra = (Map<String, Object>)request.getAttribute("extra");
                if(extra!=null && !extra.isEmpty()) {
                    Object metricsObject = extra.get("metrics");
                    if(metricsObject!=null) {
                        Map<String, ItemMetricsDTO> metrics = (Map<String, ItemMetricsDTO>)metricsObject;
                        List<String> metricTypes = (List<String>)extra.get("metricTypes");
                        if(metrics!=null && !metrics.isEmpty()) {
                            metricsTotal.putAll(metrics);
                        }
                        if(metricTypes!=null && !metricTypes.isEmpty()) {
                            metricsTypeTotal.addAll(metricTypes);
                        }
                    }
                }
            }
        }
        extraTotal.put("metricTypes", metricsTypeTotal);
        extraTotal.put("metrics", metricsTotal);
        request.setAttribute("extra", extraTotal);        
        request.setAttribute("sectionid", StatsConfig.DETAILS_SECTION);
        new DSpace().getEventService().fireEvent(
                new UsageEvent(
                        UsageEvent.Action.VIEW,
                        request,
                        context,
                        ou));
        
        mvc.getModel().putAll(model);
        mvc.getModel().put("isAdmin", isAdmin);
        mvc.getModel().put("ou", ou);
        return mvc;
    }

    @Override
    protected List<TabOrganizationUnit> findTabsWithVisibility(
            HttpServletRequest request, Map<String, Object> model,
            HttpServletResponse response) throws SQLException, Exception
    {
        
        Integer entityId = extractEntityId(request);
        
        if(entityId==null) {
            return null;
        }
        Context context = UIUtil.obtainContext(request);

        List<TabOrganizationUnit> tabs = applicationService.getList(TabOrganizationUnit.class);
        List<TabOrganizationUnit> authorizedTabs = new LinkedList<TabOrganizationUnit>();
        
        for(TabOrganizationUnit tab : tabs) {
            if(CrisAuthorizeManager.authorize(context, applicationService, OrganizationUnit.class, OUPropertiesDefinition.class, entityId, tab)) {
                authorizedTabs.add(tab);
            }
        }
        return authorizedTabs;
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

	public void setProcessors(List<ICrisHomeProcessor<OrganizationUnit>> processors) {
		this.processors = processors;
	}
	
    @Override
    protected boolean authorize(HttpServletRequest request, BoxOrganizationUnit box) throws SQLException
    {
        return CrisAuthorizeManager.authorize(UIUtil.obtainContext(request), getApplicationService(), OrganizationUnit.class, OUPropertiesDefinition.class, extractEntityId(request), box);
    }
}
