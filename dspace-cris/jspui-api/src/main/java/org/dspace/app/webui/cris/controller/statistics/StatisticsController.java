/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.statistics;

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
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.cris.integration.statistics.StatComponentsService;
import org.dspace.app.cris.model.StatSubscription;
import org.dspace.app.cris.statistics.bean.ResultBean;
import org.dspace.app.cris.statistics.bean.TreeKeyMap;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.app.webui.cris.components.statistics.StatsComponent;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.springframework.web.servlet.ModelAndView;

public class StatisticsController extends AStatisticsController
{
   
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(StatisticsController.class);
    
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {

        String id = getId(request);
        String type = request.getParameter("type");
        ModelAndView modelAndView = new ModelAndView(success);
        try
        {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put(_ID_LABEL, id);
            data.put(_JSP_KEY, jspKey);
            HttpSolrServer solrServer = new HttpSolrServer(getSolrConfig().getStatisticsCore());

            Map<String, IStatsComponent> components = statsComponentsService.getComponents();
            TwoKeyMap label = new TwoKeyMap();
            TreeKeyMap dataBeans = new TreeKeyMap();
            IStatsComponent statcomponent = null;
            
            if(components.containsKey(type)) {
                statcomponent = components.get(type);
            }
            else {
                type = StatComponentsService._SELECTED_OBJECT;
                statcomponent = (IStatsComponent)statsComponentsService.getSelectedObjectComponent(); 
            }
            
            dataBeans.putAll(statcomponent.query(id, solrServer));                
            label.putAll(statcomponent.getLabels(UIUtil.obtainContext(request),type));
            
            ResultBean result = new ResultBean(dataBeans, statsComponentsService.getCommonsParams());
            data.put(_RESULT_BEAN, result);
            data.put("label",label);
            data.put("title", getTitle(request));
            data.put("object", getObject(request));            
            modelAndView.addObject("data", data);
            addSubscriptionStatus(modelAndView, request);

        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            modelAndView = new ModelAndView(error);
        }
        return modelAndView;
    }

 

  
    @Override
    public String getId(HttpServletRequest request)
    {
        String id = request.getParameter("handle");
        if (StringUtils.isNotEmpty(id))
        {           
            return Integer.toString(_getObject(request, id).getID());
        }
        return null;
    }
    

    @Override
    public DSpaceObject getObject(HttpServletRequest request)
    {
        String id = request.getParameter("handle");
        if (StringUtils.isNotEmpty(id))
        {
            return _getObject(request, id);
        }
        return null;
    }
    
    private DSpaceObject _getObject(HttpServletRequest request, String id)
    {
        DSpaceObject dso;
        try
        {
            dso = HandleManager.resolveToObject(
                    UIUtil.obtainContext(request), id);
        }
        catch (SQLException e)
        {
            return null;
        }
        return dso;
    }

    @Override
    public String getTitle(HttpServletRequest request)
    {
        String id = request.getParameter("handle");
        if (StringUtils.isNotEmpty(id))
        {          
            return _getObject(request, id).getName();
        }
        return null;

    }
    

    
  

}
