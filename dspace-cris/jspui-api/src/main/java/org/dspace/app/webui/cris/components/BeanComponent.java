/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.webui.cris.util.RelationPreferenceUtil;


public class BeanComponent implements ICrisBeanComponent
{
        
    private String componentIdentifier;
    
    private String query;
    
    private String order;
    
    private int rpp = Integer.MAX_VALUE;
    
    private int sortby = -1;
    
    private boolean useCommonFilter = true;
    
    private boolean useRelationQuery = true;
    
    private List<String> filters = new ArrayList<String>();

    private int etal = -1;
    
    private String facetQuery;
    private String facetField; 
        
    Map<String, String> subQueries = new HashMap<String,String>();
    
    private List<String> extraFields;
    
    public String getComponentIdentifier()
    {
        return componentIdentifier;
    }

    public void setComponentIdentifier(String componentIdentifier)
    {
        this.componentIdentifier = componentIdentifier;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public String getOrder()
    {
        return order;
    }

    public void setOrder(String order)
    {
        this.order = order;
    }

    public int getRpp()
    {        
        return rpp;
    }

    public void setRpp(int rpp)
    {
        this.rpp = rpp;
    }

    public int getSortby()
    {
        return sortby;
    }

    public void setSortby(int sortby)
    {
        this.sortby = sortby;
    }

    public List<String> getFilters()
    {
        return filters;
    }

    public void setFilters(List<String> filters)
    {
        this.filters = filters;
    }

    public void setEtal(int etal)
    {
        this.etal = etal;
    }

    public int getEtal()
    {
        return etal;
    }
    
    public boolean isUseCommonFilter()
    {
        return useCommonFilter;
    }

    public void setUseCommonFilter(boolean useCommonFilter)
    {
        this.useCommonFilter = useCommonFilter;
    }

    public boolean isUseRelationQuery()
    {
        return useRelationQuery;
    }

    public void setUseRelationQuery(boolean useRelationQuery)
    {
        this.useRelationQuery = useRelationQuery;
    }

    @Override
    public String getFacetQuery()
    {           
        return facetQuery;
    }

    public void setFacetQuery(String facetQuery)
    {
        this.facetQuery = facetQuery;
    }

    public String getFacetField()
    {
        return facetField;
    }

    public void setFacetField(String facetField)
    {
        this.facetField = facetField;
    }

    public Map<String, String> getSubQueries()
    {
        return subQueries;
    }

    public void setSubQueries(Map<String, String> subQueries)
    {
        this.subQueries = subQueries;
    }

    @Override
    public List<String> getExtraFields()
    {
        return extraFields;
    }

    public void setExtraFields(List<String> extraFields)
    {
        this.extraFields = extraFields;
    }

           
}
