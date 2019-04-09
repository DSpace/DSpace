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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.RPAuthority;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.BoxResearcherPage;
import org.dspace.app.cris.model.jdyna.EditTabResearcherPage;
import org.dspace.app.cris.model.jdyna.RPAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.TabResearcherPage;
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
import org.dspace.content.authority.AuthorityDAO;
import org.dspace.content.authority.AuthorityDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.statistics.SolrLogger;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;
import org.springframework.web.servlet.ModelAndView;

import it.cilea.osd.jdyna.components.IBeanSubComponent;
import it.cilea.osd.jdyna.components.IComponent;
import it.cilea.osd.jdyna.web.controller.SimpleDynaController;

/**
 * This SpringMVC controller is used to build the ResearcherPage details page.
 * The DSpace items included in the details are returned by the DSpace Browse
 * System.
 * 
 * @author cilea
 * 
 */
public class ResearcherPageDetailsController
        extends
        SimpleDynaController<RPProperty, RPPropertiesDefinition, BoxResearcherPage, TabResearcherPage>
{

    public ResearcherPageDetailsController(
            Class<RPAdditionalFieldStorage> anagraficaObjectClass,
            Class<RPPropertiesDefinition> classTP,
            Class<TabResearcherPage> classT, Class<BoxResearcherPage> classH)
            throws InstantiationException, IllegalAccessException
    {
        super(anagraficaObjectClass, classTP, classT, classH);
    }

    /** log4j category */
    private static Logger log = Logger
            .getLogger(ResearcherPageDetailsController.class);

    private CrisSubscribeService subscribeService;
    
    private List<ICrisHomeProcessor<ResearcherPage>> processors;
    
    public void setSubscribeService(CrisSubscribeService rpSubscribeService)
    {
        this.subscribeService = rpSubscribeService;
    }

    @Override
    public ModelAndView handleDetails(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        log.debug("Start handleRequest");
        Map<String, Object> model = new HashMap<String, Object>();

        Integer objectId = extractEntityId(request);
        if (objectId == -1)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Researcher page not found");
            return null;
        }

        ResearcherPage researcher = null;
        try
        {

            researcher = ((ApplicationService) applicationService).get(
                    ResearcherPage.class, objectId);

        }
        catch (NumberFormatException e)
        {
        }

        if (researcher == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Researcher page not found");
            return null;
        }

        Context context = UIUtil.obtainContext(request);
        EPerson currUser = context.getCurrentUser();
        
        model.put("selfClaimRP", new Boolean(false));
        model.put("publicationSelfClaimRP", new Boolean(false));
        if(currUser != null) {
            model.put("isLoggedIn", new Boolean(true));
            ResearcherPage rp = ((ApplicationService) applicationService).getResearcherPageByEPersonId(currUser.getID());
        	model.put("userID", currUser.getID());
            if(rp!=null){
            	model.put("userHasRP", new Boolean(true));

            }else
            {
            	model.put("userHasRP", new Boolean(false));
                String nameGroupSelfClaim = ConfigurationManager.getProperty("cris",
                        "rp.claim.group.name");
                if (StringUtils.isNotBlank(nameGroupSelfClaim))
                {
                    Group selfClaimGroup = Group.findByName(context,
                            nameGroupSelfClaim);
                    if (selfClaimGroup != null)
                    {
                        if (Group.isMember(context, selfClaimGroup.getID()))
                        {
                            model.put("selfClaimRP", new Boolean(true));
                        }
                    }
                }
            }
            
            
            String nameGroupPublicationSelfClaim = ConfigurationManager.getProperty("cris",
                    "publication.claim.group.name");
            if (StringUtils.isNotBlank(nameGroupPublicationSelfClaim))
            {
                Group selfClaimGroup = Group.findByName(context,
                		nameGroupPublicationSelfClaim);
                if (selfClaimGroup != null)
                {
                    if (Group.isMember(context, selfClaimGroup.getID()))
                    {
                        model.put("publicationSelfClaimRP", new Boolean(true));
                    }
                }
            }
        }
        else {
            model.put("isLoggedIn", new Boolean(false));
        }
        
        boolean isAdmin = CrisAuthorizeManager.isAdmin(context,researcher);
		boolean canEdit = isAdmin
				|| CrisAuthorizeManager.canEdit(context, applicationService, EditTabResearcherPage.class, researcher);
        
        if (isAdmin
                || (currUser != null && (researcher.getEpersonID() != null && currUser
                        .getID() == researcher.getEpersonID())))
        {
            model.put("researcher_page_menu", new Boolean(true));
            model.put("authority_key",
                    ResearcherPageUtils.getPersistentIdentifier(researcher));

			AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
			long pendingItems = dao.countIssuedItemsByAuthorityValueInAuthority(RPAuthority.RP_AUTHORITY_NAME,
					ResearcherPageUtils.getPersistentIdentifier(researcher));
			model.put("pendingItems", new Long(pendingItems));
        }
        
        else if ((researcher.getStatus() == null || researcher.getStatus()
                .booleanValue() == false))
        {
            if (context.getCurrentUser() != null
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

        
        if (subscribeService != null)
        {
            boolean subscribed = subscribeService.isSubscribed(currUser,
                    researcher);
            model.put("subscribed", subscribed);
            if (researcher.getEpersonID() != null)
            {
                EPerson eperson = EPerson.find(context,
                        researcher.getEpersonID());
                if (eperson != null)
                {
                    model.put("subscriptions",
                            subscribeService.getSubscriptions(eperson));
                }
            }
        }

        ModelAndView mvc = null;

        try
        {
            mvc = super.handleDetails(request, response);
        }
        catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);
            return null;
        }

        mvc.getModel().putAll(model);
        
        Map<String, Object> extraTotal = new HashMap<String, Object>();
        Map<String, ItemMetricsDTO> metricsTotal = new HashMap<String, ItemMetricsDTO>();
        HashSet<String> metricsTypeTotal = new LinkedHashSet<String>();
        for (ICrisHomeProcessor processor : processors)
        {
            if (ResearcherPage.class.isAssignableFrom(processor.getClazz()))
            {
                processor.process(context, request, response, researcher);
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
        
        List<String> metricsTypes = new ArrayList<String>( metricsTypeTotal);
        extraTotal.put("metricTypes",metricsTypes );
        extraTotal.put("metrics", metricsTotal);
        request.setAttribute("extra", extraTotal);  
        request.setAttribute("components", super.getComponents());
        request.setAttribute("entity", researcher);        
        mvc.getModel().put("researcher", researcher);
        mvc.getModel().put("exportscitations",
                ConfigurationManager.getProperty("exportcitation.options"));
        mvc.getModel()
                .put("showStatsOnlyAdmin",
                        ConfigurationManager
                                .getBooleanProperty(SolrLogger.CFG_STAT_MODULE,"authorization.admin"));
        mvc.getModel().put("isAdmin", isAdmin);
        if (canEdit)
        {
        	mvc.getModel().put("canEdit", new Boolean(true));
        }
        
        // Fire usage event.
        request.setAttribute("sectionid", StatsConfig.DETAILS_SECTION);
        new DSpace().getEventService().fireEvent(
                    new UsageEvent(
                            UsageEvent.Action.VIEW,
                            request,
                            context,
                            researcher));
        
        
        log.debug("end servlet handleRequest");

        return mvc;
    }

    @Override
    protected List<TabResearcherPage> findTabsWithVisibility(
            HttpServletRequest request, Map<String, Object> model,
            HttpServletResponse response) throws Exception
    {
        Integer researcherId = extractEntityId(request);
        
        if(researcherId==null) {
            return null;
        }
        Context context = UIUtil.obtainContext(request);

        List<TabResearcherPage> tabs = applicationService.getList(TabResearcherPage.class);
        List<TabResearcherPage> authorizedTabs = new LinkedList<TabResearcherPage>();
        
        for(TabResearcherPage tab : tabs) {
            if(CrisAuthorizeManager.authorize(context, applicationService, ResearcherPage.class, RPPropertiesDefinition.class, researcherId, tab)) {
                authorizedTabs.add(tab);
            }
        }
        return authorizedTabs;
    }

    @Override
    protected Integer getAnagraficaId(HttpServletRequest request)
    {
        Integer researcherId = extractEntityId(request);
        ResearcherPage researcher = null;
        try
        {
            researcher = ((ApplicationService) applicationService).get(
                    ResearcherPage.class, researcherId);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
        return researcher.getDynamicField().getId();
    }

    @Override
    protected Integer getTabId(HttpServletRequest request)
    {
        String tabName = extractTabName(request);
        if (StringUtils.isNotEmpty(tabName))
        {
            TabResearcherPage tab = applicationService.getTabByShortName(
                    TabResearcherPage.class, tabName);
            if (tab != null)
                return tab.getId();
        }
        return null;
    }

    private Integer extractResearcherId(HttpServletRequest request)
    {
        return extractEntityId(request);
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
    protected String extractAnchorId(HttpServletRequest request)
    {
        String type = request.getParameter("open");
        if (type != null && !type.isEmpty())
        {

            if (getComponents() != null && !getComponents().isEmpty())
            {
                for (String key : getComponents().keySet())
                {
                    IComponent component = getComponents().get(key);
                    Map<String, IBeanSubComponent> comp = component.getTypes();

                    if (comp.containsKey(type))
                    {
                        return key;
                    }
                }
            }

            return type;
        }

        return "";
    }

    @Override
    protected void sendRedirect(HttpServletRequest request,
            HttpServletResponse response, Exception ex, String objectId)
            throws IOException, ServletException
    {
        JSPManager.showAuthorizeError(request, response,
                new AuthorizeException(ex.getMessage()));
        // response.sendRedirect("/cris/rp/" + objectId);
    }

    
    protected Integer getRealPersistentIdentifier(String persistentIdentifier)
    {
        return ResearcherPageUtils.getRealPersistentIdentifier(persistentIdentifier, ResearcherPage.class);
    }

    public void setProcessors(List<ICrisHomeProcessor<ResearcherPage>> processors)
    {
        this.processors = processors;
    }

    @Override
    protected boolean authorize(HttpServletRequest request, BoxResearcherPage box) throws SQLException
    {
        return CrisAuthorizeManager.authorize(UIUtil.obtainContext(request), getApplicationService(), ResearcherPage.class, RPPropertiesDefinition.class, extractEntityId(request), box);        
    }

}
