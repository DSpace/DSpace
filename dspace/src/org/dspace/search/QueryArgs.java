/*
 * QueryArgs.java
 *
 * $Id$
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

    public String buildQuery(HttpServletRequest request)
    {
        String newquery = "(";
        String query1 = request.getParameter("query1");
        String query2 = request.getParameter("query2");
        String query3 = request.getParameter("query3");

        String field1 = request.getParameter("field1");
        String field2 = request.getParameter("field2");
        String field3 = request.getParameter("field3");

        String conjunction1 = request.getParameter("conjunction1");
        String conjunction2 = request.getParameter("conjunction2");

        if (query1.length() > 0)
        {
            newquery = newquery + buildQueryPart(query1, field1);
        }

        if (query2.length() > 0)
        {
            newquery = newquery + " " + conjunction1 + " ";
            newquery = newquery + buildQueryPart(query2, field2);
        }

        newquery = newquery + ")";

        if (query3.length() > 0)
        {
            newquery = newquery + " " + conjunction2 + " ";
            newquery = newquery + buildQueryPart(query3, field3);
        }

        return (newquery);
    }

    private String buildQueryPart(String myquery, String myfield)
    {
        Perl5Util util = new Perl5Util();
        String newquery = "(";
        String pattern = "";

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

    public HashMap buildQueryHash(HttpServletRequest request)
    {
        HashMap queryHash = new HashMap();
        queryHash.put("query1", (request.getParameter("query1") == null) ? ""
                : request.getParameter("query1"));
        queryHash.put("query2", (request.getParameter("query2") == null) ? ""
                : request.getParameter("query2"));
        queryHash.put("query3", (request.getParameter("query3") == null) ? ""
                : request.getParameter("query3"));

        queryHash.put("field1",
                (request.getParameter("field1") == null) ? "ANY" : request
                        .getParameter("field1"));
        queryHash.put("field2",
                (request.getParameter("field2") == null) ? "ANY" : request
                        .getParameter("field2"));
        queryHash.put("field3",
                (request.getParameter("field3") == null) ? "ANY" : request
                        .getParameter("field3"));

        queryHash.put("conjunction1",
                (request.getParameter("conjunction1") == null) ? "AND"
                        : request.getParameter("conjunction1"));
        queryHash.put("conjunction2",
                (request.getParameter("conjunction2") == null) ? "AND"
                        : request.getParameter("conjunction1"));

        return (queryHash);
    }

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

        return (querystring);
    }
}