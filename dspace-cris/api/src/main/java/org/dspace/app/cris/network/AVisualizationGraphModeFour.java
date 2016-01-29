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

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.core.ConfigurationManager;

public abstract class AVisualizationGraphModeFour extends AVisualizationGraph
{
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
                List<String> relations = getValues(facetValue);

                int i = 0;
                internal: for (String relation : relations)
                {

                    log.debug("" + counter + " works on " + i + " of "
                            + relations.size());
                    System.out.println(getConnectionName() + " - " + counter
                            + " of " + facets.getValueCount() + " works on "
                            + i + " of " + relations.size());
                    String aaa = relation;
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

                    internalprivate: for (int j = i; j < relations.size(); j++)
                    {
                        List<String> values = new LinkedList<String>();
                        values.add(facetValue);

                        String bbb = relations.get(j);
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
                            buildRow(result, a, a_authority, a_displayValue, b,
                                    b_authority, b_displayValue, values, buildExtra(facetValue),
                                    a_dept, b_dept,
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

    protected abstract List<String> getValues(String value);

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

    public String getFacet(String value)
    {
        return null; // DO NOT SUPPORT
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
    
    protected String getQuery()
    {
        return "*:*";
    }
}
