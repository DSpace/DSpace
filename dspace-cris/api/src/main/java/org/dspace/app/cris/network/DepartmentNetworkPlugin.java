/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.utils.DSpace;

public class DepartmentNetworkPlugin
{

    /** log4j logger */
    protected static Logger log = Logger.getLogger(AVisualizationGraph.class);

    private DSpace dspace = new DSpace();

    private VisualizationGraphSolrService service = dspace.getServiceManager()
            .getServiceByName("visualNetworkSolrService",
                    VisualizationGraphSolrService.class);

    private VisualizationGraphIndexer indexer = dspace.getServiceManager()
            .getServiceByName("visualNetworkIndexer",
                    VisualizationGraphIndexer.class);

    private static String FACET = "focus_dept";

    private static String FACET_AUTHOR = "focus";

    public List<VisualizationGraphNode> load(List<String[]> discardedNode,
            Integer importedNodes, Boolean otherError, List<String> connections)
            throws Exception
    {

        for (String connection : connections)
        {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery("type:" + connection);
            solrQuery.addFacetField(FACET);
            solrQuery.setFacetLimit(Integer.MAX_VALUE);
            solrQuery.setFacetMinCount(1);
            solrQuery.setRows(0);
            QueryResponse rsp = service.search(solrQuery);
            FacetField facets = rsp.getFacetField(FACET);

            // for each interests get authority's authors
            List<VisualizationGraphNode> result = null;
            int counter = 0;
            external: for (Count facetElement : facets.getValues())
            {

                counter++;
                log.debug("" + counter + " of " + facets.getValueCount());
                result = new LinkedList<VisualizationGraphNode>();
                try
                {
                    String facetValue = facetElement.getName();
                    String[] splittedFacetValue = facetValue.split("\\|\\|\\|");
                    if (!splittedFacetValue[0].equals("null") && splittedFacetValue[0].equals(splittedFacetValue[1]))
                    {
                        SolrQuery solrQuery2 = new SolrQuery();
                        solrQuery2.setQuery("type:" + connection + " AND "
                                + FACET + ":\"" + ClientUtils.escapeQueryChars(facetValue) + "\" AND a_auth:[* TO *] AND b_auth:[* TO *]");

                        solrQuery2.addFacetField(FACET_AUTHOR);
                        solrQuery2.setFacetMinCount(1);
                        solrQuery2.setFacetLimit(Integer.MAX_VALUE);
                        solrQuery2.setRows(0);

                        QueryResponse rsp2 = service.search(
                                solrQuery2);
                        FacetField relations = rsp2.getFacetField(FACET_AUTHOR);
                        int i = 0;
                        internal: for (Count relation : relations.getValues())
                        {
                            log.debug("" + counter + " works on " + i + " of "
                                    + relations.getValueCount());
                            List<String> values = new LinkedList<String>();
                            values.add(splittedFacetValue[0]);

                            String aaa = relation.getName();
                            String[] split = aaa.split("\\|\\|\\|");

                            String a = aaa;
                            String a_authority = null;
                            String a_displayValue = "";

                            if (split.length > 1)
                            {
                                a_displayValue = split[1];
                            }

                            if (split.length > 2)
                            {
                                a_authority = split[2];
                            }

                            buildRow(result, splittedFacetValue[0], null, splittedFacetValue[1], a,
                                    a_authority, a_displayValue, values,
                                    buildExtra(splittedFacetValue[0]), connection);

                            i++;

                        }
                        
                        importedNodes = importNode(discardedNode, importedNodes, result);
                        log.debug("commit DEPARTMENT " + facetValue);
                        indexer.getSolr().commit();
                    }                  
                    
                }
                catch (Exception e)
                {
                    log.error("Error try to build object to index with ID:"
                            + "");
                    log.error(e.getMessage(), e);
                    otherError = true;
                    continue;
                }            
                
            }
                        
        }

        return null;
    }

    private Integer importNode(List<String[]> discardedNode,
            Integer importedNodes, List<VisualizationGraphNode> result)
    {
        try
        {
            indexer.index(result); // index node
            importedNodes = result.size();
        }
        catch (SolrServerException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (NoSuchAlgorithmException e)
        {
            log.error(e.getMessage(), e);
        }
        return importedNodes;
    }

    private String buildExtra(String facetValue)
    {
        // TODO Auto-generated method stub
        return null;
    }

    protected void buildRow(List<VisualizationGraphNode> result, String a,
            String a_authority, String a_value, String b, String b_authority,
            String b_value, List<String> values, String extra, String connection)
            throws NoSuchAlgorithmException
    {

        VisualizationGraphNode node = new VisualizationGraphNode();
        node.setA(a);
        node.setA_auth(a_authority);
        node.setFavalue(a_value);
        node.setType(connection);
        node.setB(b);
        node.setB_auth(b_authority);
        node.setFbvalue(b_value);
        node.getValue().addAll(values);
        node.getExtra().add(extra);
        node.setA_dept(a);
        node.setB_dept(a);
        node.setEntity(ConstantNetwork.ENTITY_DEPT);
        result.add(node);

    }

}
