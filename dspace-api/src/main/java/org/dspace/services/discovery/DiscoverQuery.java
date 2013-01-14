/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverHitHighlightingField;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.orm.entity.DSpaceObject;
import org.dspace.orm.entity.content.DSpaceObjectType;
import org.dspace.utils.DSpace;
/**
 * This class represents a query which the discovery backend can use
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author João Melo <jmelo@lyncode.com>
 */
public class DiscoverQuery {
	private DSpaceObject dsObject;
	private boolean includeWithdrawn = false;
	
	
    /** Main attributes for the discovery query **/
    private String query;
    private List<String> filterQueries;
    private DSpaceObjectType DSpaceObjectFilter = null;
    private List<String> fieldPresentQueries;

    private int start = 0;
    private int maxResults = -1;

    /** Attributes used for sorting of results **/
    public enum SORT_ORDER {
        desc,
        asc
    }
    private String sortField;
    private SORT_ORDER sortOrder;

    /** Attributes required for the faceting of values **/
    private List<DiscoverFacetField> facetFields;
    private List<String> facetQueries;
    // private int facetLimit = -1;
    private int facetMinCount = -1;
    private int facetOffset = 0;
    private Map<String, DiscoverHitHighlightingField> hitHighlighting;

    /** Used when you want to search for a specific field value **/
    private List<String> searchFields;

    /** Misc attributes can be implementation dependent **/
    private Map<String, List<String>> properties;

    public DiscoverQuery() {
        //Initialize all our lists
        this.filterQueries = new ArrayList<String>();
        this.fieldPresentQueries = new ArrayList<String>();

        this.facetFields = new ArrayList<DiscoverFacetField>();
        this.facetQueries = new ArrayList<String>();
        this.searchFields = new ArrayList<String>();
        this.hitHighlighting = new HashMap<String, DiscoverHitHighlightingField>();
        //Use a linked hashmap since sometimes insertion order might matter
        this.properties = new LinkedHashMap<String, List<String>>();
    }


    public void setQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setSortField(String sortField, SORT_ORDER sortOrder){
        this.sortField = sortField;
        this.sortOrder = sortOrder;
    }

    public String getSortField() {
        return sortField;
    }

    public SORT_ORDER getSortOrder() {
        return sortOrder;
    }

    /**
     * Sets the DSpace object filter, must be an DSpace Object type integer
     * can be used to only return objects from a certain DSpace Object type
     * @param DSpaceObjectFilter the DSpace object filer
     */
    public void setDSpaceObjectFilter(DSpaceObjectType DSpaceObjectFilter) {
        this.DSpaceObjectFilter = DSpaceObjectFilter;
    }

    /**
     * Gets the DSpace object filter
     * can be used to only return objects from a certain DSpace Object type
     * @return the DSpace object filer
     */
    public DSpaceObjectType getDSpaceObjectFilter() {
        return DSpaceObjectFilter;
    }

    /**
     * The maximum number of results returned by this query
     * @return the number of results
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Sets the maximum number of results by this query
     * @param maxResults the number of results
     */
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    /**
     * Adds new filter queries
     * @param filterQueries the filter queries to be added
     */
    public void addFilterQueries(String ...filterQueries){
        this.filterQueries.addAll(Arrays.asList(filterQueries));
    }

    /**
     * Returns the filter queries
     * @return the filter queries in a list
     */
    public List<String> getFilterQueries() {
        return filterQueries;
    }

    /**
     * Adds a query that will ensure that a certain field is present in the index
     * @param fieldPresentQueries the queries to be added
     */
    public void addFieldPresentQueries(String ...fieldPresentQueries){
        this.fieldPresentQueries.addAll(Arrays.asList(fieldPresentQueries));
    }

    public List<String> getFieldPresentQueries() {
        return fieldPresentQueries;
    }

    /**
     * Adds a new facet query
     * @param facetQuery the new facet query to be added
     */
    public void addFacetQuery(String facetQuery){
        this.facetQueries.add(facetQuery);
    }

    /**
     * Returns the facet queries
     * @return the facet queries for this query
     */
    public List<String> getFacetQueries() {
        return facetQueries;
    }

    /**
     * Adds a new facet field
     * @param facetField the new facet field to be added
     */
    public void addFacetField(DiscoverFacetField facetField){
        facetFields.add(facetField);
    }

    /**
     * Gets the facets fields configured
     * @return the facet fields for this query
     */
    public List<DiscoverFacetField> getFacetFields() {
        return facetFields;
    }

    /**
     * Gets the minimum number of values that need to be present before a valid facet value has been found
     * @return facetMinCount the minimum number of values to be present for a valid facet
     */
    public int getFacetMinCount() {
        return facetMinCount;
    }

    /**
     * Set the minimum number of values that need to be present before a valid facet value has been found
     * @param facetMinCount the minimum number of values to be present for a valid facet
     */
    public void setFacetMinCount(int facetMinCount) {
        this.facetMinCount = facetMinCount;
    }

    /**
     * Gets the facet field offset
     * @return the facet field offset
     */
    public int getFacetOffset() {
        return facetOffset;
    }

    /**
     * Sets the facet field offset, one facet offset will be used for all the facet fields
     * @param facetOffset an integer representing the offset
     */
    public void setFacetOffset(int facetOffset) {
        this.facetOffset = facetOffset;
    }

    public void addSearchField(String field){
        this.searchFields.add(field);
    }

    public List<String> getSearchFields() {
        return searchFields;
    }

    /**
     * Returns the misc search properties
     * @return a map containing the properties
     */
    public Map<String, List<String>> getProperties() {
        return properties;
    }

    /**
     * Adds a new search property to the misc search properties
     * @param property the name of the property
     * @param value the value of the property
     */
    public void addProperty(String property, String value){
        List<String> toAddList = properties.get(property);
        if(toAddList == null)
        {
            toAddList = new ArrayList<String>();
        }

        toAddList.add(value);

        properties.put(property, toAddList);
    }

    public DiscoverHitHighlightingField getHitHighlightingField(String field)
    {
        return hitHighlighting.get(field);
    }

    public List<DiscoverHitHighlightingField> getHitHighlightingFields()
    {
        return new ArrayList<DiscoverHitHighlightingField>(hitHighlighting.values());
    }

    public void addHitHighlightingField(DiscoverHitHighlightingField hitHighlighting)
    {
        this.hitHighlighting.put(hitHighlighting.getField(), hitHighlighting);
    }


	public DSpaceObject getDSpaceObject() {
		return dsObject;
	}


	public void setDSpaceObject(DSpaceObject dsObject) {
		this.dsObject = dsObject;
	}


	public boolean isIncludeWithdrawn() {
		return includeWithdrawn;
	}


	public void setIncludeWithdrawn(boolean includeWithdrawn) {
		this.includeWithdrawn = includeWithdrawn;
	}
	
	protected SolrQuery toSolrQuery()
    {
        SolrQuery solrQuery = new SolrQuery();

        String query = "*:*";
        if(this.getQuery() != null)
        {
        	query = this.getQuery();
		}

        solrQuery.setQuery(query);
            
        if (!includeWithdrawn)
        {
        	solrQuery.addFilterQuery("NOT(withdrawn:true)");
		}
        
        for (String filterQuery : this.getFilterQueries())
        	solrQuery.addFilterQuery(filterQuery);
        
        if (this.getDSpaceObjectFilter() != null)
        	solrQuery.addFilterQuery("search.resourcetype:" + this.getDSpaceObjectFilter().getId());
        
        for (String filterQuery : this.getFieldPresentQueries())
        	solrQuery.addFilterQuery(filterQuery+":[* TO *]");

        if(this.getStart() != -1)
        {
            solrQuery.setStart(this.getStart());
        }

        if(this.getMaxResults() != -1)
        {
            solrQuery.setRows(this.getMaxResults());
        }

        if(this.getSortField() != null)
        {
            SolrQuery.ORDER order = SolrQuery.ORDER.asc;
            if(this.getSortOrder().equals(DiscoverQuery.SORT_ORDER.desc))
                order = SolrQuery.ORDER.desc;

            solrQuery.addSortField(this.getSortField(), order);
        }

        for(String property : this.getProperties().keySet())
        {
            List<String> values = this.getProperties().get(property);
            solrQuery.add(property, values.toArray(new String[values.size()]));
        }

        List<DiscoverFacetField> facetFields = this.getFacetFields();
        if(0 < facetFields.size())
        {
            //Only add facet information if there are any facets
            for (DiscoverFacetField facetFieldConfig : facetFields)
            {
                String field = transformFacetField(facetFieldConfig, facetFieldConfig.getField(), false);
                solrQuery.addFacetField(field);

                // Setting the facet limit in this fashion ensures that each facet can have its own max
                solrQuery.add("f." + field + "." + FacetParams.FACET_LIMIT, String.valueOf(facetFieldConfig.getLimit()));
                String facetSort;
                if(DiscoveryConfigurationParameters.SORT.COUNT.equals(facetFieldConfig.getSortOrder()))
                {
                    facetSort = FacetParams.FACET_SORT_COUNT;
                }else{
                    facetSort = FacetParams.FACET_SORT_INDEX;
                }
                solrQuery.add("f." + field + "." + FacetParams.FACET_SORT, facetSort);
                if (facetFieldConfig.getOffset() != -1)
                {
                    solrQuery.setParam("f." + field + "."
                            + FacetParams.FACET_OFFSET,
                            String.valueOf(facetFieldConfig.getOffset()));
                }
                if(facetFieldConfig.getPrefix() != null)
                {
                    solrQuery.setFacetPrefix(field, facetFieldConfig.getPrefix());
                }
            }

            List<String> facetQueries = this.getFacetQueries();
            for (String facetQuery : facetQueries)
            {
                solrQuery.addFacetQuery(facetQuery);
            }

            if(this.getFacetMinCount() != -1)
            {
                solrQuery.setFacetMinCount(this.getFacetMinCount());
            }

            solrQuery.setParam(FacetParams.FACET_OFFSET, String.valueOf(this.getFacetOffset()));
        }

        if(!this.getHitHighlightingFields().isEmpty())
        {
            solrQuery.setHighlight(true);
            solrQuery.add(HighlightParams.USE_PHRASE_HIGHLIGHTER, Boolean.TRUE.toString());
            for (DiscoverHitHighlightingField highlightingField : this.getHitHighlightingFields())
            {
                solrQuery.addHighlightField(highlightingField.getField() + "_hl");
                solrQuery.add("f." + highlightingField.getField() + "_hl." + HighlightParams.FRAGSIZE, String.valueOf(highlightingField.getMaxChars()));
                solrQuery.add("f." + highlightingField.getField() + "_hl." + HighlightParams.SNIPPETS, String.valueOf(highlightingField.getMaxSnippets()));
            }

        }

        //Add any configured search plugins !
        List<DiscoverServicePlugin> solrServiceSearchPlugins = new DSpace().getServiceManager().getServicesByType(DiscoverServicePlugin.class);
        for (DiscoverServicePlugin searchPlugin : solrServiceSearchPlugins)
        {
            searchPlugin.additionalSearchParameters(this, solrQuery);
        }
        return solrQuery;
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
