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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.network.ConstantNetwork;
import org.dspace.app.cris.network.VisualizationGraphSolrService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class RelationsResearcherNetworkServlet extends DSpaceServlet
{

    /** log4j category */
    private static Logger log = Logger
            .getLogger(RelationsResearcherNetworkServlet.class);

    private DSpace dspace = new DSpace();

    private VisualizationGraphSolrService service = dspace.getServiceManager()
            .getServiceByName("visualNetworkSolrService",
                    VisualizationGraphSolrService.class);

    public static Pattern patternRP = Pattern.compile("rp[0-9]{5}$");

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException
    {
        Researcher util = new Researcher();

        ApplicationService applicationService = util.getApplicationService();
        
        String idString = request.getPathInfo();
        String[] pathInfo = idString.split("/", 2);
        String authority = pathInfo[1];

        String target = request.getParameter("with");
        String relation = request.getParameter("relation");

        if (relation != null && relation.contains("_"))
        {
            relation = relation.split("_")[1];
        }

        Pattern patt = Pattern.compile("rp[0-9]{5}$");
        Matcher matcher = patt.matcher(authority);

        ResearcherPage researcherFocus = new ResearcherPage();
        if (matcher.find())
        {
            researcherFocus = applicationService.get(ResearcherPage.class, ResearcherPageUtils
                    .getRealPersistentIdentifier(authority, ResearcherPage.class), true);
            
        }
        else
        {
            researcherFocus.setFullName(authority);
        }

        ResearcherPage researcherTarget = new ResearcherPage();
        String authorityTarget = target;
        if (target != null)
        {
            matcher = patt.matcher(target);
            if (matcher.find())
            {

                researcherTarget = applicationService.get(ResearcherPage.class, ResearcherPageUtils
                        .getRealPersistentIdentifier(target, ResearcherPage.class), true);

            }
            else
            {
                researcherTarget.setFullName(target);
            }
        }
        request.setAttribute("researchertarget", researcherTarget);
        request.setAttribute("authoritytarget", authorityTarget);
        request.setAttribute("authority", authority);
        MultiValueMap relations = null;

        try
        {
            relations = getRelationsInformation(relation, authority,
                    authorityTarget);
        }
        catch (SearchServiceException e)
        {
            log.error(e.getMessage(), e);
        }

        request.setAttribute("relations", relations);
        request.setAttribute("type", relation);

        JSPManager.showJSP(request, response, "/graph/relationsfragment.jsp");

    }

    public MultiValueMap getRelationsInformation(String type,
            String from, String to) throws SearchServiceException
    {
        MultiValueMap result = new MultiValueMap();
        SolrQuery solrQuery = new SolrQuery();

        Matcher matcher = patternRP.matcher(from);
        String field1 = "";
        String field2 = "";

        if (matcher.find())
        {
            field1 = "a_auth";
        }
        else
        {
            field1 = "a_val";
        }

        matcher = patternRP.matcher(to);

        if (matcher.find())
        {
            field2 = "b_auth";
        }
        else
        {
            field2 = "b_val";
        }

        solrQuery.setQuery("("+field1 + ":\"" + from + "\" AND "+ field2 + ":\""+ to +"\"" + ") OR ("+field2 + ":\"" + from + "\" AND "+ field1 + ":\""+ to +"\"" + ")");

        solrQuery.addFilterQuery("type:" + type);
        solrQuery.addFilterQuery("entity:" + ConstantNetwork.ENTITY_RP);
        solrQuery.setRows(Integer.MAX_VALUE);
        QueryResponse rsp = service.search(solrQuery);

        for (SolrDocument doc : rsp.getResults())
        {
            String resultField = "";
            if (doc.getFieldValue("value") instanceof String)
            {
                resultField = (String) doc.getFieldValue("value");
            }
            else
            {
                for (String ss : (List<String>) doc.getFieldValue("value"))
                {
                    resultField += ss;
                }
            }

            String resultFieldExtra = "";

            if (doc.getFieldValue("extra")!=null)
            {
                if (doc.getFieldValue("extra") instanceof String)
                {
                    resultFieldExtra = (String) doc.getFieldValue("extra");
                }
                else
                {

                    for (String ss : (List<String>) doc.getFieldValue("extra"))
                    {
                        resultFieldExtra += ss;
                    }
                }
            }
            result.put(resultField, resultFieldExtra);
        }
        return result;
    }
}
