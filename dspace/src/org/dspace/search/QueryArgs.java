package org.dspace.search;

import javax.servlet.http.HttpServletRequest;

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
    
    public String buildQuery (HttpServletRequest request)
    {
    	String newquery = "(";
    	String query1 	= request.getParameter("query1");
    	String query2 	= request.getParameter("query2");
    	String query3 	= request.getParameter("query3");
    	
    	String field1 	= request.getParameter("field1");
    	String field2 	= request.getParameter("field2");
    	String field3 	= request.getParameter("field3");

    	String conjunction1 	= request.getParameter("conjunction1");
    	String conjunction2 	= request.getParameter("conjunction2");
    	
    	if (query1.length() > 0)
    	{
    		if (!field1.equals("ANY")) {
    			newquery = newquery + field1 + ":";
    		}
    		
    		newquery = newquery + '"' + query1 + '"';
    	}
    	
    	if (query2.length() > 0)
    	{
    		newquery = newquery + " " + conjunction1 + " ";
    		if (!field2.equals("ANY")) {
    			newquery = newquery + field2 + ":";
    		}
    		
    		newquery = newquery + '"' + query2 + '"';
    	}
    	
    	newquery = newquery + ")";
    	
    	if (query3.length() > 0)
    	{
			newquery = newquery + " " + conjunction2 + " ";
    		if (!field3.equals("ANY")) {
    			newquery = newquery + field3 + ":";
    		}
    		
    		newquery = newquery + "\"" + query3 + "\"";
    	}

    	return (newquery);
    }
}
