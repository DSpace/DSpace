/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import it.cilea.osd.jdyna.components.IBeanSubComponent;
import it.cilea.osd.jdyna.components.IComponent;
import it.cilea.osd.jdyna.web.controller.SimpleDynaController;

import java.io.IOException;
import java.util.HashMap;
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
import org.dspace.app.cris.model.jdyna.RPAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.TabResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.CrisSubscribeService;
import org.dspace.app.cris.statistics.util.StatsConfig;
import org.dspace.app.cris.util.ResearcherPageUtils;
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
import org.dspace.statistics.SolrLogger;
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

        boolean isAdmin = AuthorizeManager.isAdmin(context);
      
        if (isAdmin
                || (currUser != null && (researcher.getEpersonID() != null && currUser
                        .getID() == researcher.getEpersonID())))
        {
            model.put("researcher_page_menu", new Boolean(true));
            model.put("authority_key",
                    ResearcherPageUtils.getPersistentIdentifier(researcher));

            if (isAdmin)
            {
                AuthorityDAO dao = AuthorityDAOFactory.getInstance(context);
                long pendingItems = dao
                        .countIssuedItemsByAuthorityValueInAuthority(
                                RPAuthority.RP_AUTHORITY_NAME,
                                ResearcherPageUtils
                                        .getPersistentIdentifier(researcher));
                model.put("pendingItems", new Long(pendingItems));
            }
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

        
        if (subscribeService != null)
        {
            boolean subscribed = subscribeService.isSubscribed(currUser,
                    researcher);
            model.put("subscribed", subscribed);
            EPerson eperson = EPerson.findByNetid(context, researcher.getSourceID());
            if (eperson != null) {
            	model.put("subscriptions", subscribeService.getSubscriptions(eperson));
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
        mvc.getModel().put("researcher", researcher);
        mvc.getModel().put("exportscitations",
                ConfigurationManager.getProperty("exportcitation.options"));
        mvc.getModel()
                .put("showStatsOnlyAdmin",
                        ConfigurationManager
                                .getBooleanProperty(SolrLogger.CFG_STAT_MODULE,"authorization.admin"));
        
        
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
        ResearcherPage researcher = null;
        try
        {
            researcher = ((ApplicationService) applicationService).get(
                    ResearcherPage.class, researcherId);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
        // check admin authorization
        Boolean isAdmin = null; // anonymous access
        Context context = UIUtil.obtainContext(request);
        EPerson currUser = context.getCurrentUser();
        if (AuthorizeManager.isAdmin(context))
        {
            isAdmin = true; // admin
        }
        else if ((currUser != null && researcher.getId() == currUser.getID()))
        {
            isAdmin = false; // owner
        }
        List<TabResearcherPage> tabs = applicationService.getTabsByVisibility(
                TabResearcherPage.class, isAdmin);
        return tabs;

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

}
