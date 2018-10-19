/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;


import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.integration.BindItemToRP;
import org.dspace.app.cris.integration.NameResearcherPage;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.Item;
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
        if(id_s!=null && !id_s.isEmpty()) {
            id = Integer.parseInt(id_s);
            researcher = applicationService.get(ResearcherPage.class, id);
        }
        List<ResearcherPage> r = new LinkedList<ResearcherPage>();
        r.add(researcher);
        
        String operation = arg0.getParameter("operation");
        if(StringUtils.isNotBlank(operation) && OPERATION_LIST.equals(operation)) {
            Map<NameResearcherPage, Item[]> result = BindItemToRP.list(r, relationPreferenceService);
            arg0.setAttribute("requesterMapPublication", researcher.getCrisID());
            arg0.setAttribute("mapPublicationList", result);
            return new ModelAndView("forward:/tools/claim");
        }
        else {
            BindItemToRP.work(r, relationPreferenceService);
        }
        return new ModelAndView(getViewName()+ ResearcherPageUtils.getPersistentIdentifier(researcher));
    }


    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }


    public void setRelationPreferenceService(RelationPreferenceService relationPreferenceService)
    {
        this.relationPreferenceService = relationPreferenceService;
    }


    public RelationPreferenceService getRelationPreferenceService()
    {
        return relationPreferenceService;
    }
}
