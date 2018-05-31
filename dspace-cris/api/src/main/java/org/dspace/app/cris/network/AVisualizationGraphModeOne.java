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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SolrServiceImpl;

public abstract class AVisualizationGraphModeOne extends AVisualizationGraph
{
    // programmatic change mode to load index (true use no pagination, false
    // paginate each 100)
    private static boolean NOT_PAGINATION = ConfigurationManager
            .getBooleanProperty(NetworkPlugin.CFG_MODULE, "network.connection.loader.heavyload.modeone",
                    true);

    
    @Override
    public List<VisualizationGraphNode> load(List<String[]> discardedNode,
            Integer importedNodes, Boolean otherError) throws Exception
    {
        // load all publications
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");

        solrQuery.setFields("search.resourceid", "search.resourcetype",
                "author_filter", "dc.title", "handle");
        solrQuery.addFilterQuery("search.resourcetype:[2 TO 4]");

        int start = 0;
        int offset = 100;
        if (NOT_PAGINATION)
        {
            solrQuery.setRows(Integer.MAX_VALUE);
        }
        else
        {
            solrQuery.setStart(start);
            solrQuery.setRows(offset);
        }
        QueryResponse rsp = getService().getSearcher().search(solrQuery);
        SolrDocumentList publications = rsp.getResults();
        System.out.println(publications.getNumFound());
        List<VisualizationGraphNode> result = null;
        boolean endwhile = false;
        while (((publications.getNumFound() + offset) > start)
                && endwhile == false)
        {
            if (start > 0 && !NOT_PAGINATION)
            {
                solrQuery.setStart(start);
                solrQuery.setRows(offset);
                rsp = getService().getSearcher().search(solrQuery);
                publications = rsp.getResults();
            }

            start = (start + 1) + offset;
            // for each publication get authority's authors facets
            Iterator<SolrDocument> iter = publications.iterator();
            int counter = 0;
            external: while (iter.hasNext())
            {

                counter++;
                log.debug("" + (start == 0 ? counter : (start + counter))
                        + " of " + publications.getNumFound());
                System.out.println(getConnectionName() + " - " + counter);

                result = new ArrayList<VisualizationGraphNode>();

                Integer pubId = null;
                try
                {
                    SolrDocument publication = iter.next();

                    pubId = (Integer) publication
                            .getFieldValue("search.resourceid");
                    Object obj = publication.getFieldValue("dc.title");
                    String handle = (String) publication
                            .getFieldValue("handle");
                    Object authList = publication.getFieldValue("author_filter");

                    List<String> values = new ArrayList<String>();
                    if (obj instanceof ArrayList)
                    {
                        for (String aaa : (List<String>) obj)
                        {
                            values.add(aaa);
                        }
                    }
                    else
                    {
                        String value = (String) obj;
                        values.add(value);
                    }

                    if (authList instanceof ArrayList)
                    {                        
                        List<String> authListArrays = (List<String>) authList;
                        
                        int i = 0;
                        Set<String> authArrays = new HashSet<String>();
                        Set<String> bAuthArrays = new HashSet<String>();
                        for(String elementInListAuth : authListArrays) {
                            authArrays.add(elementInListAuth);
                            bAuthArrays.add(elementInListAuth);
                        }
                        
                        
                        for (String aaa : authArrays)
                        {

                            String[] split = aaa.split(SolrServiceImpl.ESCAPED_FILTER_SEPARATOR);
                            
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
                            int j = i;
                            
                            for (String bbb : bAuthArrays)
                            {                                
                                String extra = handle;

                                split = bbb.split(SolrServiceImpl.ESCAPED_FILTER_SEPARATOR);

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
                                    buildRow(
                                            result,
                                            a,
                                            a_authority,
                                            a_displayValue,
                                            b,
                                            b_authority,
                                            b_displayValue,
                                            values,
                                            extra,
                                            a_dept,
                                            b_dept,
                                            ConstantNetwork.ENTITY_PLACEHOLDER_RP);
                                }
                                else
                                {
                                    buildRow(result, a, a_authority,
                                            a_displayValue, b, b_authority,
                                            b_displayValue, values, extra,
                                            a_dept, b_dept,
                                            ConstantNetwork.ENTITY_RP);
                                }
                                j++;
                            }
                            bAuthArrays.remove(aaa);
                            i++;
                        }
                    }
                    else
                    {
                        continue external;
                    }

                }
                catch (Exception e)
                {
                    log.error("Error try to build object to index with publication ID:"
                            + pubId);
                    log.error(e.getMessage(), e);
                    otherError = true;
                    continue;
                }

                importedNodes = indexNode(discardedNode, importedNodes, result);

            }
            if (NOT_PAGINATION)
            {
                endwhile = true;
            }

            log.debug("commit " + getType());
            getIndexer().commit();
        }
        return result;
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
}
