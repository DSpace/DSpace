/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;


import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.network.ConstantNetwork;
import org.dspace.app.cris.network.NetworkPlugin;
import org.dspace.app.cris.network.VisualizationGraphSolrService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.app.cris.statistics.util.StatsConfig;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

public class ResearcherNetworkServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger
            .getLogger(ResearcherNetworkServlet.class);

    private DSpace dspace = new DSpace();

    private VisualizationGraphSolrService service = dspace.getServiceManager()
            .getServiceByName("visualNetworkSolrService",
                    VisualizationGraphSolrService.class);

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException
    {

        Researcher util = new Researcher();

        ApplicationService applicationService = util.getApplicationService();
        CrisSearchService searchService = (CrisSearchService) util
                .getCrisSearchService();

        
        Integer objectId = extractEntityId(request);
        if (objectId == -1)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Researcher page not found");
            return;
        }
        
        ResearcherPage researcher = applicationService.get(ResearcherPage.class, objectId, true);

        boolean found = false;

        if (researcher == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Researcher page not found");
        }
        else
        {
            found = true;
        }
        String authority = (String)request.getAttribute("authority");
        String showEXT = request.getParameter("showexternal");
        boolean showALL = false;
        if (showEXT != null)
        {
            showALL = Boolean.parseBoolean(showEXT);
        }
        else
        {
            showALL = ConfigurationManager
                    .getBooleanProperty(NetworkPlugin.CFG_MODULE, "network.connection.showexternal");
        }

        String showSameDept = request.getParameter("showsamedept");
        boolean showALLDept = true;
        if (showSameDept != null)
        {
            showALLDept = Boolean.parseBoolean(showSameDept);
        }
        else
        {
            showALLDept = ConfigurationManager
                    .getBooleanProperty(NetworkPlugin.CFG_MODULE, "network.connection.showsamedept");
        }

      
        String connection = ConfigurationManager
                .getProperty(NetworkPlugin.CFG_MODULE, "network.connection");

        String[] connections = connection.split(",");
        List<String> availableConnections = new LinkedList<String>();
        Map<String,String> colorsNodes = new HashMap<String, String>();
        Map<String,String> colorsEdges = new HashMap<String, String>();
        Map<String,Integer> maxDepths = new HashMap<String, Integer>();
        
        // check available connection data
        int i = 0;
        for (String conn : connections)
        {
            try
            {
                if (checkAvailableData(request, conn, authority))
                {
                    availableConnections.add(conn);
                    NetworkPlugin plugin = (NetworkPlugin) (PluginManager.getNamedPlugin(NetworkPlugin.CFG_MODULE,
                            NetworkPlugin.class, conn));
                    colorsNodes.put(conn, plugin.getNodeCustomColor());
                    colorsEdges.put(conn, plugin.getEdgeCustomColor());
                    maxDepths.put(conn, plugin.getCustomMaxDepth());
                    i++;
                }
            }
            catch (SearchServiceException e)
            {
                log.error(e.getMessage(), e);
            }
        }
        Integer max = 0;
        if (i > 0)
        {
            connections = new String[availableConnections.size() - 1];
            connections = availableConnections.toArray(connections);
            max = Collections.max(maxDepths.values());
        }
        request.setAttribute("configMaxDepth",
                ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, "network.connection.maxdepth"));
        request.setAttribute("relations", connections);
        request.setAttribute("colorsNodes", colorsNodes);
        request.setAttribute("colorsEdges", colorsEdges);
        request.setAttribute("customMaxDepths", maxDepths);
        request.setAttribute("maxDepth", max);
        request.setAttribute("researcher", researcher);
        request.setAttribute("authority", authority);
        request.setAttribute("showexternal", showALL);
        request.setAttribute("showsamedept", showALLDept);
        if(i > 0) {
            request.setAttribute("networkStarted", connections[0]);
        }
       
        if (found)
        {
            // Fire usage event.
            request.setAttribute("sectionid", StatsConfig.COLLABORATION_NETWORK_SECTION);
            new DSpace().getEventService().fireEvent(
                        new UsageEvent(
                                UsageEvent.Action.VIEW,
                                request,
                                context,
                                researcher));        
        }

        JSPManager.showJSP(request, response, "/graph/graph.jsp");        

    }

    private boolean checkAvailableData(HttpServletRequest request,
            String connection, String authority) throws SearchServiceException
    {

        String query = "type:" + connection + " AND focus_auth:" + authority
                + " AND entity:" + ConstantNetwork.ENTITY_RP;

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setStart(0);
        solrQuery.setRows(0);
        QueryResponse rsp = getService().search(solrQuery);
        SolrDocumentList docs = rsp.getResults();
        if (docs != null)
        {
            if (docs.getNumFound() > 0)
            {
                return true;
            }
        }
        return false;
    }

    public void setService(VisualizationGraphSolrService service)
    {
        this.service = service;
    }

    public VisualizationGraphSolrService getService()
    {
        return service;
    }
    
    protected Integer extractEntityId(HttpServletRequest request)
    {
        String path = request.getPathInfo().substring(1); // remove first /
        String[] splitted = path.split("/");
        request.setAttribute("authority", splitted[1]);
        Integer id = ResearcherPageUtils.getRealPersistentIdentifier(splitted[1], ResearcherPage.class);
        request.setAttribute("entityID", id);
        return id;
    }
}
