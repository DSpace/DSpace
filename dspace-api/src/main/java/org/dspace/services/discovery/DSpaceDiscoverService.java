/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.discovery;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.orm.dao.api.IHandleDao;
import org.dspace.orm.entity.Collection;
import org.dspace.orm.entity.Community;
import org.dspace.orm.entity.DSpaceObject;
import org.dspace.orm.entity.Handle;
import org.dspace.orm.entity.content.DSpaceObjectType;
import org.dspace.services.ConfigurationService;
import org.dspace.services.DiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceDiscoverService implements DiscoveryService {
	private static Logger log = LogManager.getLogger(DSpaceDiscoverService.class);
	
    public static final String FILTER_SEPARATOR = "\n|||\n";
    public static final String AUTHORITY_SEPARATOR = "###";
	
	@Autowired ConfigurationService configurationService;
	@Autowired IHandleDao handleDao;
	
	/**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    private SolrServer solr = null;


    protected SolrServer getSolr() throws MalformedURLException, SolrServerException
    {
        if (solr == null)
        {
           String solrService = configurationService.getProperty("discovery.search.server") ;

            log.debug("Solr URL: " + solrService);
            solr = new CommonsHttpSolrServer(solrService);
            // solr.setBaseURL(solrService);

            SolrQuery solrQuery = new SolrQuery().setQuery("search.resourcetype:2 AND search.resourceid:1");
            solr.query(solrQuery);
        }

        return solr;
    }
	
	@Override
	public DiscoverResult search(DiscoverQuery query) throws DiscoverException {
		try {
			return this.retrieveResult(query, this.getSolr().query(query.toSolrQuery()));
		} catch (Exception e) {
			throw new DiscoverException(e);
		}
	}
	
	
	// Auxiliary methods
    protected DiscoverResult retrieveResult(DiscoverQuery query, QueryResponse solrQueryResponse) throws SQLException {
        DiscoverResult result = new DiscoverResult();

        if(solrQueryResponse != null)
        {
            result.setSearchTime(solrQueryResponse.getQTime());
            result.setStart(query.getStart());
            result.setMaxResults(query.getMaxResults());
            result.setTotalSearchResults(solrQueryResponse.getResults().getNumFound());

            List<String> searchFields = query.getSearchFields();
            for (SolrDocument doc : solrQueryResponse.getResults())
            {
                DSpaceObject dso = this.findDSpaceObject(doc);

                if(dso != null)
                {
                    result.addDSpaceObject(dso);
                } else {
                    log.error("Error while retrieving DSpace object from discovery index"+ System.lineSeparator() + "Handle: " + doc.getFirstValue("handle"));
                    continue;
                }

                DiscoverResult.SearchDocument resultDoc = new DiscoverResult.SearchDocument();
                //Add information about our search fields
                for (String field : searchFields)
                {
                    List<String> valuesAsString = new ArrayList<String>();
                    for (Object o : doc.getFieldValues(field))
                    {
                        valuesAsString.add(String.valueOf(o));
                    }
                    resultDoc.addSearchField(field, valuesAsString.toArray(new String[valuesAsString.size()]));
                }
                result.addSearchDocument(dso, resultDoc);

                if(solrQueryResponse.getHighlighting() != null)
                {
                    Map<String, List<String>> highlightedFields = solrQueryResponse.getHighlighting().get(dso.getType() + "-" + dso.getID());
                    if(MapUtils.isNotEmpty(highlightedFields))
                    {
                        //We need to remove all the "_hl" appendix strings from our keys
                        Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
                        for(String key : highlightedFields.keySet())
                        {
                            resultMap.put(key.substring(0, key.lastIndexOf("_hl")), highlightedFields.get(key));
                        }

                        result.addHighlightedResult(dso, new DiscoverResult.DSpaceObjectHighlightResult(dso, resultMap));
                    }
                }
            }

            //Resolve our facet field values
            List<FacetField> facetFields = solrQueryResponse.getFacetFields();
            if(facetFields != null)
            {
                for (int i = 0; i <  facetFields.size(); i++)
                {
                    FacetField facetField = facetFields.get(i);
                    DiscoverFacetField facetFieldConfig = query.getFacetFields().get(i);
                    List<FacetField.Count> facetValues = facetField.getValues();
                    if (facetValues != null)
                    {
                        if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE) && facetFieldConfig.getSortOrder().equals(DiscoveryConfigurationParameters.SORT.VALUE))
                        {
                            //If we have a date & are sorting by value, ensure that the results are flipped for a proper result
                           Collections.reverse(facetValues);
                        }

                        for (FacetField.Count facetValue : facetValues)
                        {
                            String displayedValue = transformDisplayedValue(facetField.getName(), facetValue.getName());
                            String field = transformFacetField(facetFieldConfig, facetField.getName(), true);
                            String authorityValue = transformAuthorityValue(facetField.getName(), facetValue.getName());
                            String sortValue = transformSortValue(facetField.getName(), facetValue.getName());
                            String filterValue = displayedValue;
                            if (StringUtils.isNotBlank(authorityValue))
                            {
                                filterValue = authorityValue;
                            }
                            result.addFacetResult(
                                    field,
                                    new DiscoverResult.FacetResult(filterValue,
                                            displayedValue, authorityValue,
                                            sortValue, facetValue.getCount()));
                        }
                    }
                }
            }

            if(solrQueryResponse.getFacetQuery() != null)
            {
                //TODO: do not sort when not a date, just retrieve the facets in the order they where requested !
                //At the moment facet queries are only used for dates so we need to sort our results
                TreeMap<String, Integer> sortedFacetQueries = new TreeMap<String, Integer>(solrQueryResponse.getFacetQuery());
                for(String facetQuery : sortedFacetQueries.descendingKeySet())
                {
                    //TODO: do not assume this, people may want to use it for other ends, use a regex to make sure
                    //We have a facet query, the values looks something like: dateissued.year:[1990 TO 2000] AND -2000
                    //Prepare the string from {facet.field.name}:[startyear TO endyear] to startyear - endyear
                    String facetField = facetQuery.substring(0, facetQuery.indexOf(":"));
                    String name = facetQuery.substring(facetQuery.indexOf('[') + 1);
                    name = name.substring(0, name.lastIndexOf(']')).replaceAll("TO", "-");
                    String filter = facetQuery.substring(facetQuery.indexOf('['));
                    filter = filter.substring(0, filter.lastIndexOf(']') + 1);
                    
                    Integer count = sortedFacetQueries.get(facetQuery);

                    //No need to show empty years
                    if(0 < count)
                    {
                        result.addFacetResult(facetField, new DiscoverResult.FacetResult(filter, name, null, name, count));
                    }
                }
            }
        }

        return result;
    }
    protected String transformDisplayedValue(String field, String value) throws SQLException {
        if(field.equals("location.comm") || field.equals("location.coll"))
        {
            value = locationToName(field, value);
        }
        else if (field.endsWith("_filter") || field.endsWith("_ac")
          || field.endsWith("_acid"))
        {
            //We have a filter make sure we split !
            String separator = configurationService.getProperty("discovery.solr.facets.split.char");
            if(separator == null)
            {
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer valueBuffer = new StringBuffer();
            int start = fqParts.length / 2;
            for(int i = start; i < fqParts.length; i++)
            {
                String[] split = fqParts[i].split(AUTHORITY_SEPARATOR, 2);
                valueBuffer.append(split[0]);
            }
            value = valueBuffer.toString();
        }else if(value.matches("\\((.*?)\\)"))
        {
            //The brackets where added for better solr results, remove the first & last one
            value = value.substring(1, value.length() -1);
        }
        return value;
    }
	private DSpaceObject findDSpaceObject(SolrDocument doc) {
		Integer type = (Integer) doc.getFirstValue("search.resourcetype");
        Integer id = (Integer) doc.getFirstValue("search.resourceid");
        String handle = (String) doc.getFirstValue("handle");

        if (type != null && id != null)
        {
        	Handle h = handleDao.selectByResourceId(DSpaceObjectType.getById(type), id);
        	if (h != null) return h.toDSpaceObject();
        } 
        
        if (handle != null)
        {
            Handle h = handleDao.selectByHandle(handle);
            return h.toDSpaceObject();
        }

        return null;
	}
    @SuppressWarnings("incomplete-switch")
	private String locationToName(String field, String value) throws SQLException {
        if("location.comm".equals(field) || "location.coll".equals(field))
        {
            DSpaceObjectType type = field.equals("location.comm") ? DSpaceObjectType.COMMUNITY : DSpaceObjectType.COLLECTION;
            Handle h = handleDao.selectByResourceId(type, Integer.parseInt(value));
            DSpaceObject commColl = null;
            if (h != null) commColl = h.toDSpaceObject(); 
            if(commColl != null)
            {
            	switch (type) {
            		case COLLECTION:
            			return ((Collection) commColl).getName();
            		case COMMUNITY:
            			return ((Community) commColl).getName();
            	}
            }

        }
        return value;
    }
    private String transformAuthorityValue(String field, String value) throws SQLException {
        if (field.endsWith("_filter") || field.endsWith("_ac")
                || field.endsWith("_acid"))
        {
            //We have a filter make sure we split !
            String separator = configurationService.getProperty("discovery.solr.facets.split.char");
            if(separator == null)
            {
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer authorityBuffer = new StringBuffer();
            int start = fqParts.length / 2;
            for(int i = start; i < fqParts.length; i++)
            {
                String[] split = fqParts[i].split(AUTHORITY_SEPARATOR, 2);
                if (split.length == 2)
                {
                    authorityBuffer.append(split[1]);
                }
            }
            if (authorityBuffer.length() > 0)
            {
                return authorityBuffer.toString();
            }
        }
        return null;
    }
    private String transformSortValue(String field, String value) throws SQLException {
        if(field.equals("location.comm") || field.equals("location.coll"))
        {
            value = locationToName(field, value);
        }
        else if (field.endsWith("_filter") || field.endsWith("_ac")
                || field.endsWith("_acid"))
        {
            //We have a filter make sure we split !
            String separator = configurationService.getProperty("discovery.solr.facets.split.char");
            if(separator == null)
            {
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer valueBuffer = new StringBuffer();
            int end = fqParts.length / 2;
            for(int i = 0; i < end; i++)
            {
                valueBuffer.append(fqParts[i]);
            }
            value = valueBuffer.toString();
        }else if(value.matches("\\((.*?)\\)"))
        {
            //The brackets where added for better solr results, remove the first & last one
            value = value.substring(1, value.length() -1);
        }
        return value;
    }
    private String transformFacetField(DiscoverFacetField facetFieldConfig, String field, boolean removePostfix)
    {
        if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf("_filter"));
            }else{
                return field + "_filter";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf(".year"));
            }else{
                return field + ".year";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_AC))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf("_ac"));
            }else{
                return field + "_ac";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL))
        {
            if(removePostfix)
            {
                return StringUtils.substringBeforeLast(field, "_tax_");
            }else{
                //Only display top level filters !
                return field + "_tax_0_filter";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_AUTHORITY))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf("_acid"));
            }else{
                return field + "_acid";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_STANDARD))
        {
            return field;
        }else{
            return field;
        }
    }
}
