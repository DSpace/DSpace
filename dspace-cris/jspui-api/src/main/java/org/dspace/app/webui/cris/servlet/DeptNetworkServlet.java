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
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.network.ConstantNetwork;
import org.dspace.app.cris.network.NetworkPlugin;
import org.dspace.app.cris.network.VisualizationGraphSolrService;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class DeptNetworkServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DeptNetworkServlet.class);

    private DSpace dspace = new DSpace();

    private VisualizationGraphSolrService service = dspace.getServiceManager()
            .getServiceByName("visualNetworkSolrService",
                    VisualizationGraphSolrService.class);

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException
    {

        String dept = request.getParameter("dept");

        boolean found = false;

        if (dept == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "A department name could be passed as parameter");
        }
        else
        {
            found = true;
        }
        
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

                                
        String connection = ConfigurationManager
                .getProperty(NetworkPlugin.CFG_MODULE, "network.connection");
        Map<String,String> colorsNodes = new HashMap<String, String>();
        Map<String,String> colorsEdges = new HashMap<String, String>();
        Map<String,Integer> maxDepths = new HashMap<String, Integer>();
        String[] connections = connection.split(",");
        List<String> availableConnections = new LinkedList<String>();
        // check available connection data
        int i = 0;
        for (String conn : connections)
        {
            try
            {
                if (checkAvailableData(request, conn, dept))
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
        request.setAttribute("dept", dept);        
        request.setAttribute("colorsNodes", colorsNodes);
        request.setAttribute("colorsEdges", colorsEdges);
        request.setAttribute("customMaxDepths", maxDepths);        
        request.setAttribute("maxDepth", max);
        request.setAttribute("showexternal", showALL);
        request.setAttribute("showsamedept", false);
        if (i > 0) {
            request.setAttribute("networkStarted", connections[0]);    
        }
        Map<String, Integer> relations = getRelationsInformation(dept,
                "", connections);
        request.setAttribute("relationsdept", relations);
        
        JSPManager.showJSP(request, response, "/graph/dgraph.jsp");
       
    }

    private Map<String, Integer> getRelationsInformation(String from, String to, String... types)
    {

        Map<String, Integer> result = new HashMap<String, Integer>();

        try
        {
            String query = "a_dept:\"" + from +"\" OR b_dept:\"" + from +"\"";
            QueryResponse rsp = shootQuery(query, true, "focus_dept");            
            FacetField relations = rsp.getFacetField("focus_dept");            
            int otherDPconnected = relations.getValueCount();

            
            query = "a_dept:\"" + from +"\" AND b_dept:\"" + from +"\"";
            rsp = shootQuery(query, true, "focus_auth");            
            relations = rsp.getFacetField("focus_auth");
            int internalRPs = relations.getValueCount();

            query = "a_dept:\"" + from +"\" OR b_dept:\"" + from +"\"";
            rsp = shootQuery(query, true, "focus_auth");            
            relations = rsp.getFacetField("focus_auth");            
            int fromOtherDPconnected = relations.getValueCount();
            
            query = "focus_dept:\"" + from +"|||null\" OR focus_dept:\"null|||" + from +"\"";
            rsp = shootQuery(query, true, "focus_dept"); 
            relations = rsp.getFacetField("focus_dept");  
            int externalRPs = (int)rsp.getResults().getNumFound();
            int externalCardinality = relations.getValueCount();            
            
            //need to remove n row from facet because the first match the department and one or two are matching with external
            otherDPconnected = otherDPconnected - 1 - externalCardinality;
            
            result.put("external", externalRPs);
            result.put("internal", internalRPs);
            result.put("fromOtherDPConnected", fromOtherDPconnected);
            result.put("dpConnected", otherDPconnected);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }

        return result;
    }

    private QueryResponse shootQuery(String query, boolean facet,
            String facetField) throws SearchServiceException
    {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setStart(0);
        solrQuery.setRows(0);

        if (facet)
        {
            solrQuery.setFacet(true);
            solrQuery.setFacetMinCount(1);
            solrQuery.addFacetField(facetField);
        }
        QueryResponse rsp = service.search(solrQuery);        
        return rsp;
    }
 


    private boolean checkAvailableData(HttpServletRequest request,
            String connection, String value) throws SearchServiceException
    {

        String query = "type:" + connection + " AND focus_val:\"" + value +"\"" + " AND entity:"+ ConstantNetwork.ENTITY_DEPT;
        QueryResponse rsp = shootQuery(query, false, null);
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
}
