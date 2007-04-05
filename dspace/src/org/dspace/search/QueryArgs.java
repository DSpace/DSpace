/*
 * QueryArgs.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Constants;

import org.apache.oro.text.perl.Perl5Util;

/**
 * Contains the arguments for a query. Fill it out and pass to the query engine
 */
public class QueryArgs
{
    // the query string
    private String query;

    // start and count defines a search 'cursor' or page
    // query will return 'count' hits beginning at offset 'start'
    private int start = 0; // default values

    private int pageSize = 10;

    /**
     * set the query string
     * 
     * @param newQuery
     */
    public void setQuery(String newQuery)
    {
        query = newQuery;
    }

    /**
     * retrieve the query string
     * 
     * @return the current query string
     */
    public String getQuery()
    {
        return query;
    }

    /**
     * set the offset of the desired search results, beginning with 0 ; used to
     * page results (the default value is 0)
     * 
     * @param newStart
     *            index of first desired result
     */
    public void setStart(int newStart)
    {
        start = newStart;
    }

    /**
     * read the search's starting offset
     * 
     * @return current index of first desired result
     */
    public int getStart()
    {
        return start;
    }

    /**
     * set the count of hits to return; used to implement paged searching see
     * the initializer for the default value
     * 
     * @param newSize
     *            number of hits per page
     */
    public void setPageSize(int newSize)
    {
        pageSize = newSize;
    }

    /**
     * get the count of hits to return
     * 
     * @return number of results per page
     */
    public int getPageSize()
    {
        return pageSize;
    }

    /**
     * Builds an advanced-query description string.
     *
     * The string is built using the passed in values
     * query{1,2,3}, field{1,2,3} and conjunction{1,2} taken from
     * the parameter request.
     * 
     * @param request the request object to take the values from
     *
     * @return the query description string built
     */
    public String buildQuery(HttpServletRequest request)
    {
        String newquery = "(";
        String numFieldStr = request.getParameter("num_search_field");
        // for backward compatibility
        if (numFieldStr == null) numFieldStr ="3";
        int numField = Integer.parseInt(numFieldStr);
        ArrayList query = new ArrayList();
        ArrayList field = new ArrayList();
        ArrayList conjunction = new ArrayList();
        
        for (int i = 1; i <= numField; i++)
        {
        	String tmp_query = request.getParameter("query"+i);
        	String tmp_field = request.getParameter("field"+i);
        	// TODO: Ensure a valid field from config
            // Disarm fields with regexp control characters
            if (tmp_field != null)
            {
                tmp_field = tmp_field.replace('/', ' ');
                tmp_field = tmp_field.replace('<', ' ');
                tmp_field = tmp_field.replace('\\', ' ');
                tmp_field = tmp_field.replace(':', ' ');
            }

            if (tmp_query != null && !tmp_query.equals(""))
        	{
        		query.add(tmp_query.trim());
        		if (tmp_field == null)        		        			
        			field.add("ANY");
        		else  			
        			field.add(tmp_field.trim());
        		if (i != numField)
            	{
            		conjunction.add(request.getParameter("conjunction"+i) != null?
            				request.getParameter("conjunction"+i):"AND");
            	}
        	}
        }
        Iterator iquery = query.iterator();
        Iterator ifield = field.iterator();
        Iterator iconj = conjunction.iterator();
        
        String conj_curr = "";
        while (iquery.hasNext())
        {	newquery = newquery + conj_curr;
        	String query_curr = (String) iquery.next();
        	String field_curr = (String) ifield.next();
        	newquery = newquery + buildQueryPart(query_curr,field_curr);
        	if (iconj.hasNext())
        	{
        		conj_curr = " " + (String)iconj.next() + " ";        	    
        	}
        }
        
        newquery = newquery + ")";
        return (newquery);
    }

    /**
     * Builds a query-part using the field and value passed in
     * with ' --&gt; " (single to double quote) translation.
     *
     * @param myquery the value the query will look for
     * @param myfield the field myquery will be looked for in
     *
     * @return the query created
     */
    private String buildQueryPart(String myquery, String myfield)
    {
        Perl5Util util = new Perl5Util();
        String newquery = "(";

        if (!myfield.equals("ANY"))
        {
            newquery = newquery + myfield + ":";
            myquery = util.substitute("s/\'(.*)\'/\"$1\"/g", myquery);

            if (!util.match("/\".*\"/", myquery))
            {
                myquery = util.substitute("s/ / " + myfield + ":/g", myquery);
            }
        }

        newquery = newquery + myquery + ")";

        return (newquery);
    }

    /**
     * Constructs a HashMap with the keys field{1,2,3}, query{1,2,3} and
     * conjunction{1,2} taking the values from the passed-in argument
     * defaulting to "".
     *
     * @param request the request-describing object to take the values from
     *
     * @return the created HashMap
     */
    public HashMap buildQueryHash(HttpServletRequest request)
    {
        HashMap queryHash = new HashMap();
        String numFieldStr = request.getParameter("num_search_field");
        // for backward compatibility
        if (numFieldStr == null) numFieldStr = "3"; 
        int numField = Integer.parseInt(numFieldStr);
        for (int i = 1; i < numField; i++)
        {
        	queryHash.put("query"+i, (request.getParameter("query"+i) == null) ? ""
                    : request.getParameter("query"+i));
        	queryHash.put("field"+i,
                    (request.getParameter("field"+i) == null) ? "ANY" : request
                            .getParameter("field"+i));
            queryHash.put("conjunction"+i,
                    (request.getParameter("conjunction"+i) == null) ? "AND"
                            : request.getParameter("conjunction"+i));            
        }
        
        queryHash.put("query"+numField, (request.getParameter("query"+numField) == null) ? ""
                : request.getParameter("query"+numField));
        
        queryHash.put("field"+numField,
                (request.getParameter("field"+numField) == null) ? "ANY" 
                : request.getParameter("field"+numField));
        
        return (queryHash);
    }

    /**
     * Builds an HTTP query string for some parameters with the value
     * taken from the request context passed in.
     *
     * The returned string includes key/value pairs in the HTTP query string
     * format (key1=value1&amp;key2=value2...) for the keys query{1,2,3},
     * field{1,2,3} and conjunction{1,2} with values taken from request
     * and defaulting to "".
     * <P>
     * Note, that the values are url-encoded using the UTF-8 encoding scheme
     * as the corresponding W3C recommendation states.
     * <P>
     * Also note that neither leading ? (question mark)
     * nor leading &amp; (ampersand mark) is included.
     * Take this into account when appending to a real URL.
     * 
     * @param request the request object to take the values from
     *
     * @return the query string that can be used without further
     *          transformationin URLs
     * 
     */
    public String buildHTTPQuery(HttpServletRequest request)
            throws UnsupportedEncodingException
    {
        String querystring = "";
        HashMap queryHash = buildQueryHash(request);

        Iterator i = queryHash.keySet().iterator();

        while (i.hasNext())
        {
            String key = (String) i.next();
            String value = (String) queryHash.get(key);

            querystring = querystring + "&" + key + "="
                    + URLEncoder.encode(value, Constants.DEFAULT_ENCODING);
        }
        if (request.getParameter("num_search_field") != null)
        {
        	querystring = querystring + "&num_search_field="+request.getParameter("num_search_field");	
        }        

        // return the result with the leading "&" removed
        return (querystring.substring(1));
    }
}
