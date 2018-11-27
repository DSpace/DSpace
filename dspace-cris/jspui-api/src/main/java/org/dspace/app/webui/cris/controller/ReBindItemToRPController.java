/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.BindItemToRP;
import org.dspace.app.cris.integration.NameResearcherPage;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryViewAndHighlightConfiguration;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * This SpringMVC controller allows the admin to execute the reBindItemToRP
 * script via WebUI on a RP basis
 * 
 * @see BindItemToRP#work(List, ApplicationService)
 * 
 * @author cilea
 * 
 */
public class ReBindItemToRPController extends ParameterizableViewController
{

    private static final String OPERATION_LIST = "list";

    /** the logger */
    private static Logger log = Logger
            .getLogger(ReBindItemToRPController.class);

    /**
     * the applicationService for query the RP db, injected by Spring IoC
     */
    private ApplicationService applicationService;

    private RelationPreferenceService relationPreferenceService;

    @Override
    public ModelAndView handleRequest(HttpServletRequest arg0,
            HttpServletResponse arg1) throws Exception
    {
        String id_s = arg0.getParameter("id");
        Integer id = null;
        ResearcherPage researcher = null;
        if (id_s != null && !id_s.isEmpty())
        {
            id = Integer.parseInt(id_s);
            researcher = applicationService.get(ResearcherPage.class, id);
        }
        List<ResearcherPage> r = new LinkedList<ResearcherPage>();
        r.add(researcher);

        Context context = null;
        try
        {
            String operation = arg0.getParameter("operation");
            if (StringUtils.isNotBlank(operation)
                    && OPERATION_LIST.equals(operation))
            {

                Map<NameResearcherPage, Item[]> result = BindItemToRP.listExcludeAuthority(r,
                        relationPreferenceService);

                List<Item> resultMatch = new ArrayList<Item>();

                context = new Context();
                context.setIgnoreAuthorization(true);

                for (NameResearcherPage tempName : result.keySet())
                {
                    for (Item hitItem : result.get(tempName))
                    {
                        if (hitItem != null)
                        {
                            if (tempName.getRejectItems() != null && tempName
                                    .getRejectItems().contains(hitItem.getID()))
                            {
                                log.warn(
                                        "Item has been reject for this authority - itemID "
                                                + hitItem.getID());
                            }
                            else
                            {
                                resultMatch.add(hitItem);
                            }
                        }
                    }

                }

                arg0.setAttribute("requesterMapPublication",
                        researcher.getCrisID());
                arg0.setAttribute("publicationList", resultMatch);
                return new ModelAndView("forward:/tools/claim");
            }
            else
            {
                BindItemToRP.work(r, relationPreferenceService);
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        return new ModelAndView(getViewName()
                + ResearcherPageUtils.getPersistentIdentifier(researcher));
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public void setRelationPreferenceService(
            RelationPreferenceService relationPreferenceService)
    {
        this.relationPreferenceService = relationPreferenceService;
    }

    public RelationPreferenceService getRelationPreferenceService()
    {
        return relationPreferenceService;
    }
}
