/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import flexjson.JSONSerializer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedFieldWithLock;
import org.dspace.app.cris.network.NetworkPlugin;
import org.dspace.app.cris.network.dto.JsGraph;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;
import org.dspace.utils.DSpace;

public class JSONResearcherNetworkServlet extends JSONRequest
{
    /** log4j category */
    private static Logger log = Logger
            .getLogger(JSONResearcherNetworkServlet.class);

    @Override
    public void doJSONRequest(Context context, HttpServletRequest req,
            HttpServletResponse resp) throws AuthorizeException, IOException            
    {

        DSpace dspace = new DSpace();
        ApplicationService service = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);

        String researcher = req.getParameter("researcher");

        
        EPerson currUser = context.getCurrentUser();
        
        ResearcherPage rp = service.get(ResearcherPage.class, ResearcherPageUtils
                .getRealPersistentIdentifier(researcher, ResearcherPage.class), true);

        try
        {
            if (rp == null  && // cv is hide
                    !(AuthorizeManager.isAdmin(context) || // the user logged in is
                                                           // not an admin
                    (currUser != null && (rp.getEpersonID() != null && currUser
                            .getID() == rp.getEpersonID())))) // the user logged
                                                                 // in is not the
                                                                 // rp owner
                    
            {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        catch (SQLException e1)
        {
            new AuthorizeException(e1.getMessage());
        }

        String rpName = rp.getFullName();

        String connection = req.getParameter("connection");

        NetworkPlugin plugin = (NetworkPlugin) PluginManager.getNamedPlugin(NetworkPlugin.CFG_MODULE,
                NetworkPlugin.class, connection);

        String showEXT = req.getParameter("showexternal");
        boolean showALL = true;
        if (showEXT != null)
        {
            showALL = Boolean.parseBoolean(showEXT);
        }

        String showSameDept = req.getParameter("showsamedept");
        boolean showALLDept = true;
        if (showSameDept != null)
        {
            showALLDept = Boolean.parseBoolean(showSameDept);
        }

        List<JsGraph> graph = new LinkedList<JsGraph>();
        // search data from connection
        try
        {

            Integer maxDepth = plugin.getCustomMaxDepth();
            
            log.info("Request build graph for: " + rp);
            RPGraphBuilderUtils builder = new RPGraphBuilderUtils(); 
            List<RestrictedFieldWithLock> orgUnit = rp.getOrgUnit();
            builder.buildGraph(service, researcher, rpName, plugin, graph, 0, maxDepth,
                    showALL, showALLDept, (orgUnit!=null && orgUnit.size()>0)?orgUnit.get(0).getValue():"");
            
            log.info("Build graph info:");
            log.info(" ->map contains " + graph.size() + " nodes");
            log.info(" ->depth " + maxDepth);
            log.info(" ->info target:" + graph.get(0).getId());
            log.info(" ->	  #adjacences:"
                    + graph.get(0).getAdjacencies().size());
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), new ServletException(e));
        }

        JSONSerializer serializer = new JSONSerializer();
        serializer.exclude("class").exclude("adjacencies.class")
                .exclude("adjacencies.data.class").exclude("adjacencies.src")
                .exclude("adjacencies.data.relation").exclude("adjacencies.data.color").exclude("data.class")
                .exclude("data.profile").exclude("data.color").deepSerialize(graph, resp.getWriter());
        return;
    }



}
