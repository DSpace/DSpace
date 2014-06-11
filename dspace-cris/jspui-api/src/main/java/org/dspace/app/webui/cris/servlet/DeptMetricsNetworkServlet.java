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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.network.ConstantNetwork;
import org.dspace.app.cris.network.NetworkPlugin;
import org.dspace.app.cris.network.VisualizationGraphSolrService;
import org.dspace.app.cris.network.dto.DTOMetric;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class DeptMetricsNetworkServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger
            .getLogger(DeptMetricsNetworkServlet.class);

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

        // load all publications
        SolrQuery solrQuery = new SolrQuery();

        List<DTOMetric> results = new ArrayList<DTOMetric>();

        String connection = ConfigurationManager
                .getProperty(NetworkPlugin.CFG_MODULE, "network.connection");

        String[] connections = connection.split(",");
        List<String> fieldsToRetrieve = new ArrayList<String>();
        fieldsToRetrieve.add("search.resourceid");
        fieldsToRetrieve.add("rp_fullname");
        for (String conn : connections)
        {
            fieldsToRetrieve
                    .add(ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_1_RETRIEVE
                            + conn);
            fieldsToRetrieve
                    .add(ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_2_RETRIEVE
                            + conn);
            fieldsToRetrieve
                    .add(ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_3_RETRIEVE
                            + conn);
            fieldsToRetrieve
                    .add(ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_4_RETRIEVE
                            + conn);
        }
        String[] fields = fieldsToRetrieve.toArray(new String[] {});
        QueryResponse rsp;
        try
        {
            solrQuery = new SolrQuery();
            solrQuery.setQuery("search.resourcetype:"
                    + CrisConstants.RP_TYPE_ID + " AND rp_dept:\"" + dept
                    + "\"");
            solrQuery.setFields(fields);
            solrQuery.setRows(Integer.MAX_VALUE);
            rsp = getService().getSearcher().search(solrQuery);

            SolrDocumentList rows = rsp.getResults();

            external: for (String conn : connections)
            {
                Iterator<SolrDocument> iter = rows.iterator();
                internal: while (iter.hasNext())
                {
                    DTOMetric metric = new DTOMetric();

                    SolrDocument publication = iter.next();

                    Integer rp_id = (Integer) publication
                            .getFirstValue("search.resourceid");
                    String rp_fullname = (String) publication
                            .getFirstValue("rp_fullname");
                    String averageStrength = (String) publication
                            .getFirstValue(ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_3_RETRIEVE
                                    + conn);
                    String maxStrength = (String) publication
                            .getFirstValue(ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_2_RETRIEVE
                                    + conn);
                    String numbersConnections = (String) publication
                            .getFirstValue(ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_1_RETRIEVE
                                    + conn);
                    String quadraticVariance = (String) publication
                            .getFirstValue(ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_4_RETRIEVE
                                    + conn);

                    metric.setAuthority(ResearcherPageUtils
                            .getPersistentIdentifier(rp_id, ResearcherPage.class));
                    metric.setFullName(rp_fullname);
                    metric.setType(conn);
                    metric.setAverageStrength(averageStrength);
                    metric.setMaxStrength(maxStrength);
                    metric.setNumbersConnections(numbersConnections);
                    metric.setQuadraticVariance(quadraticVariance);

                    if ((averageStrength == null || averageStrength.isEmpty())
                            && (maxStrength == null || maxStrength.isEmpty())
                            && (numbersConnections == null || numbersConnections
                                    .isEmpty())
                            && (quadraticVariance == null || quadraticVariance
                                    .isEmpty()))
                    {
                        //do not show anythings
                    }
                    else {
                        results.add(metric);
                    }

                }
            }
        }
        catch (SearchServiceException e)
        {
            log.error(e.getMessage(), new ServletException(e));
        }

        request.setAttribute("dept", dept);
        request.setAttribute("metrics", results);

        JSPManager.showJSP(request, response, "/graph/dmetrics.jsp");

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
