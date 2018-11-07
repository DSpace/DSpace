/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import it.cilea.osd.jdyna.web.Utils;
import it.cilea.osd.jdyna.web.controller.SimpleDynaController;

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
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.BoxOrganizationUnit;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicProperty;
import org.dspace.app.cris.model.jdyna.EditTabDynamicObject;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
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

/**
 * This SpringMVC controller is used to build the ResearcherPage details page.
 * The DSpace items included in the details are returned by the DSpace Browse
 * System.
 * 
 * @author cilea
 * 
 */
public class DynamicObjectDetailsController
        extends
        SimpleDynaController<DynamicProperty, DynamicPropertiesDefinition, BoxDynamicObject, TabDynamicObject>
{
    private CrisSubscribeService subscribeService;
    
    private List<ICrisHomeProcessor<ResearchObject>> processors;

    public DynamicObjectDetailsController(Class<ResearchObject> anagraficaObjectClass,
            Class<DynamicPropertiesDefinition> classTP,
            Class<TabDynamicObject> classT, Class<BoxDynamicObject> classH)
            throws InstantiationException, IllegalAccessException
    {
        super(anagraficaObjectClass, classTP, classT, classH);
    }

    /** log4j category */
    private static Logger log = Logger
            .getLogger(DynamicObjectDetailsController.class);

    @Override
    public ModelAndView handleDetails(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        Map<String, Object> model = new HashMap<String, Object>();
        
        setSpecificPartPath(Utils.getSpecificPath(request, null));
        ResearchObject dyn = extractDynamicObject(request);

        if (dyn == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    getSpecificPartPath() + " page not found");
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
        
        boolean isAdmin = CrisAuthorizeManager.isAdmin(context,dyn);
        boolean canEdit = isAdmin || CrisAuthorizeManager.canEdit(context, applicationService, EditTabDynamicObject.class, dyn);
        if ((dyn.getStatus() == null || dyn.getStatus().booleanValue() == false)
                && !isAdmin)
        {
            
            if (currUser != null
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

        if (isAdmin)
        {
            model.put("do_page_menu", new Boolean(true));
        }

		if (canEdit) {
			model.put("canEdit", new Boolean(true));
		}
        ModelAndView mvc = null;

        try
        {
            mvc = super.handleDetails(request, response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        
        
        if (subscribeService != null)
        {
            boolean subscribed = subscribeService.isSubscribed(currUser,
                    dyn);
            model.put("subscribed", subscribed);            
        }
        
        List<ICrisHomeProcessor<ResearchObject>> resultProcessors = new ArrayList<ICrisHomeProcessor<ResearchObject>>();
        Map<String, Object> extraTotal = new HashMap<String, Object>();
        Map<String, ItemMetricsDTO> metricsTotal = new HashMap<String, ItemMetricsDTO>();
        List<String> metricsTypeTotal = new ArrayList<String>();
        for (ICrisHomeProcessor processor : processors)
        {
            if (ResearchObject.class.isAssignableFrom(processor.getClazz()))
            {
                processor.process(context, request, response, dyn);
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
                        dyn));
        
        mvc.getModel().putAll(model);
        mvc.getModel().put("isAdmin", isAdmin);
        mvc.getModel().put("entity", dyn);
        return mvc;
    }

    @Override
    protected List<TabDynamicObject> findTabsWithVisibility(
            HttpServletRequest request, Map<String, Object> model,
            HttpServletResponse response) throws SQLException, Exception
    {
       
        Integer entityId = extractEntityId(request);
        
        if(entityId==null) {
            return null;
        }
        Context context = UIUtil.obtainContext(request);

        List<TabDynamicObject> tabs = applicationService.findTabByType(tabClass, extractDynamicObject(request).getTypo());
        List<TabDynamicObject> authorizedTabs = new LinkedList<TabDynamicObject>();
        
        for(TabDynamicObject tab : tabs) {
            if(CrisAuthorizeManager.authorize(context, applicationService, ResearchObject.class, DynamicPropertiesDefinition.class, entityId, tab)) {
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
            TabDynamicObject tab = applicationService.getTabByShortName(
                    TabDynamicObject.class, tabName);
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
        //response.sendRedirect("/cris/dyn/details?id=" + objectId);
    }

    @Override
    protected Integer getAnagraficaId(HttpServletRequest request)
    {
        ResearchObject dyn = null;
        try
        {
            dyn = extractDynamicObject(request);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
        return dyn.getDynamicField().getId();
    }

    private ResearchObject extractDynamicObject(HttpServletRequest request)
    {

        Integer id = extractEntityId(request);
        return ((ApplicationService) applicationService).get(ResearchObject.class,
                id);

    }
    
    protected Integer getRealPersistentIdentifier(String persistentIdentifier)
    {
        return ResearcherPageUtils.getRealPersistentIdentifier(persistentIdentifier, ResearchObject.class);
    }
    
    public void setSubscribeService(CrisSubscribeService rpSubscribeService)
    {
        this.subscribeService = rpSubscribeService;
    }
    
    public void setProcessors(List<ICrisHomeProcessor<ResearchObject>> processors) {
		this.processors = processors;
	}
    
    @Override
    protected boolean authorize(HttpServletRequest request, BoxDynamicObject box) throws SQLException
    {
        return CrisAuthorizeManager.authorize(UIUtil.obtainContext(request), getApplicationService(), ResearchObject.class, DynamicPropertiesDefinition.class, extractEntityId(request), box);
    }
}