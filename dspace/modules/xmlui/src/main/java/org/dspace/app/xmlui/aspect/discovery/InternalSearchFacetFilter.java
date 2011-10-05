package org.dspace.app.xmlui.aspect.discovery;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 16-sep-2011
 * Time: 9:28:35
 */
public class InternalSearchFacetFilter extends SearchFacetFilter{

    public String getView(){
        return "nonarchived";
    }

    public String getSearchFilterUrl(){
        return "non-archived-search-filter";
    }

    public String getDiscoverUrl(){
        return "submissions";
    }
    
}
