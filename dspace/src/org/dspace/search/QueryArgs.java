package org.dspace.search;

public class QueryArgs
{
    // the query string
    private String query;
    
    // start and count defines a search 'cursor' or page
    // query will return 'count' hits beginning at offset 'start'
    private int start    = 0;    // default values
    private int pageSize = 10;


    /**
     * set the query string
     */
    public void setQuery( String newQuery ) { query = newQuery; }

    
    /**
     * retrieve the query string
     */
    public String getQuery() { return query; }
    
    
    /**
     * set the offset of the desired search results,
     *  beginning with 0 ; used to page results
     *  (the default value is 0)
     */
    public void setStart( int newStart ) { start = newStart; }
    
    
    /**
     * read the search's starting offset
     */
    public int getStart() { return start; }
    
    
    /**
     * set the count of hits to return;
     *  used to implement paged searching
     *  see the initializer for the default value
     */
    public void setPageSize( int newSize ) { pageSize = newSize; }
    
    
    /**
     * get the count of hits to return
     */
    public int getPageSize() { return pageSize; }
}
