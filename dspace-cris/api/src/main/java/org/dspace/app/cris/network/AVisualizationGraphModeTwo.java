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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.core.ConfigurationManager;

public abstract class AVisualizationGraphModeTwo extends AVisualizationGraph
{
    public static final String JOIN_FROM_SEARCH_PARENTFK_TO_SEARCH_UNIQUEID = "{!join from=search.parentfk to=search.uniqueid}";
    public static final String FA_VALUE = "objectpeople_filter";
    
    @Override
    public List<VisualizationGraphNode> load(List<String[]> discardedNode,
            Integer importedNodes, Boolean otherError) throws Exception
    {
        // load all publications
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(getQuery());
        solrQuery.addFacetField(getFacetFieldQuery());
        // solrQuery.addFilterQuery("authors_fauthority:rp00001");
        solrQuery.setFacetLimit(Integer.MAX_VALUE);
        solrQuery.setFacetMinCount(1);
        solrQuery.setRows(0);
        QueryResponse rsp = getService().getSearcher().search(solrQuery);
        FacetField facets = rsp.getFacetField(getFacetFieldQuery());
        System.out.println(facets.getValueCount());
        // for each interests get authority's authors
        List<VisualizationGraphNode> result = null;
        int counter = 0;
        external: for (Count facetElement : facets.getValues())
        {
            counter++;

            log.debug(getConnectionName() + " - " + counter + " of "
                    + facets.getValueCount());
            System.out.println(getConnectionName() + " - " + counter + " of "
                    + facets.getValueCount());
            result = new LinkedList<VisualizationGraphNode>();
            try
            {

                String facetValue = facetElement.getName();

                solrQuery = new SolrQuery();
                String query = (useJoin()?getJoin():"") + getFacetFieldQuery() + ":\"" + ClientUtils.escapeQueryChars(facetValue) + "\"";
                solrQuery.setQuery(query);
                solrQuery.addFacetField(getFacet(facetValue));
                solrQuery.setFacetMinCount(1);
                solrQuery.setFacetLimit(getFacetLimit());
                solrQuery.setRows(0);

                rsp = getService().getSearcher().search(solrQuery);
                FacetField relations = rsp.getFacetField(getFacet(facetValue));
                int i = 0;
                internal: for (Count relation : relations.getValues())
                {

                    log.debug("" + counter + " works on " + i+1 + " of "
                            + relations.getValueCount());
                    System.out.println(getConnectionName() + " - " + counter
                            + " of " + facets.getValueCount() + " works on "
                            + i+1 + " of " + relations.getValueCount());
                    String aaa = relation.getName();
                    String[] split = aaa.split("\\|\\|\\|");

                    String a = aaa;
                    String a_authority = null;
                    String a_dept = null;
                    String a_displayValue = "";

                    if (split.length > 1)
                    {
                                                
                        String[] splitAuthority = split[1].split(splitterAuthority);
                        
                        a_displayValue = splitAuthority[0];
                        if (splitAuthority.length > 1)
                        {
                            a_authority = splitAuthority[1];
                            // a_dept = ResearcherPageUtils
                            // .getDepartment(a_authority);
                            a_dept = getDepartmentFromSOLR(a_authority);
                        }
                        
                    }

                    internalprivate: for (int j = i; j < relations.getValues()
                            .size(); j++)
                    {
                        List<String> values = new LinkedList<String>();
                        try { 
                            values.addAll(transform(facetValue));
                        }
                        catch(Exception ex) {
                            continue external;
                        }

                        String bbb = relations.getValues().get(j).getName();
                        split = bbb.split("\\|\\|\\|");

                        String b = bbb;
                        String b_authority = null;
                        String b_dept = null;
                        String b_displayValue = "";

                        if (split.length > 1)
                        {                            
                            String[] splitAuthority = split[1].split(splitterAuthority);
                            
                            b_displayValue = splitAuthority[0];
                            if (splitAuthority.length > 1)
                            {
                                b_authority = splitAuthority[1];
                                // a_dept = ResearcherPageUtils
                                // .getDepartment(a_authority);
                                b_dept = getDepartmentFromSOLR(b_authority);
                            }
                        }

                  
                        if (j == i)
                        {
                            buildRow(result, a, a_authority,
                                    a_displayValue, b, b_authority,
                                    b_displayValue, values,
                                    buildExtra(facetValue), a_dept, b_dept,
                                    ConstantNetwork.ENTITY_PLACEHOLDER_RP);
                        }
                        else
                        {
                            if (!a.equals(b))
                            {
                                buildRow(result, a, a_authority,
                                        a_displayValue, b, b_authority,
                                        b_displayValue, values,
                                        buildExtra(facetValue), a_dept, b_dept,
                                        ConstantNetwork.ENTITY_RP);
                            }
                        }
                    }

                    i++;

                }

            }
            catch (Exception e)
            {
                log.error("Error try to build object to index with ID:" + "");
                log.error(e.getMessage(), e);
                otherError = true;
                continue;
            }
            importedNodes = indexNode(discardedNode, importedNodes, result);
        }

        log.debug("commit " + getType());
        getIndexer().commit();
        return result;
    }


    protected List<String> transform(String values)
    {
        List result = new ArrayList<String>();
        result.add(values);
        return result;
    }

    protected String getJoin()
    {
        return JOIN_FROM_SEARCH_PARENTFK_TO_SEARCH_UNIQUEID;
    }


    protected boolean useJoin()
    {        
        return false;
    }



    private Integer indexNode(List<String[]> discardedNode,
            Integer importedNodes, List<VisualizationGraphNode> result)
    {
        try
        {
            getIndexer().index(result); // index node
            importedNodes = result.size();
        }
        catch (SolrServerException e)
        {
            log.error(e.getMessage(), e);
            discardedNode.add(new String[] { getConnectionName() });
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
            discardedNode.add(new String[] { getConnectionName() });
        }
        catch (NoSuchAlgorithmException e)
        {
            log.error(e.getMessage(), e);
            discardedNode.add(new String[] { getConnectionName() });
        }
        return importedNodes;
    }

    protected abstract String getFacetFieldQuery();

    public abstract String getFacet(String value);

    protected String getQuery()
    {
        return "*:*";
    }
    
    
    public int getFacetLimit()
    {
        int result = ConfigurationManager
                .getIntProperty(NetworkPlugin.CFG_MODULE, "network.connection.loader.limitnode."
                        + getType());
        if (result == 0)
        {
            result = ConfigurationManager
                    .getIntProperty(NetworkPlugin.CFG_MODULE, "network.connection.loader.limitnode.default");
        }
        return result;
    }
}
