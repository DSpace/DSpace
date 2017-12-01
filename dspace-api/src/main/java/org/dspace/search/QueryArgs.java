/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.sort.SortOption;
import org.apache.commons.lang.StringUtils;

/**
 * Contains the arguments for a query. Fill it out and pass to the query engine
 * 
 * @deprecated Since DSpace 4 the system use an abstraction layer named
 *             Discovery to provide access to different search provider. The
 *             legacy system build upon Apache Lucene is likely to be removed in
 *             a future version. If you are interested in use Lucene as backend
 *             for the DSpace search system please consider to build a Lucene
 *             implementation of the Discovery interfaces
 */
@Deprecated
public class QueryArgs
{
    // the query string
    private String query;

    // start and count defines a search 'cursor' or page
    // query will return 'count' hits beginning at offset 'start'
    private int start = 0; // default values

    private int pageSize = 10;

    private SortOption sortOption = null;

    private String sortOrder = SortOption.DESCENDING;

    /** number of metadata elements to display before truncating using "et al" */
    private int etAl = ConfigurationManager.getIntProperty("webui.itemlist.author-limit");

    /**
     * @return  the number of metadata fields at which to truncate with "et al"
     */
    public int getEtAl()
    {
        return etAl;
    }

    /**
     * set the number of metadata fields at which to truncate with "et al"
     *
     * @param etAl
     */
    public void setEtAl(int etAl)
    {
        this.etAl = etAl;
    }

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

    public SortOption getSortOption()
    {
        return sortOption;
    }

    public void setSortOption(SortOption sortOption)
    {
        this.sortOption = sortOption;
    }

    public String getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder)
    {
        this.sortOrder = sortOrder;
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
        if (numFieldStr == null)
        {
            numFieldStr = "3";
        }
        int numField = Integer.parseInt(numFieldStr);
        List<String> query = new ArrayList<String>();
        List<String> field = new ArrayList<String>();
        List<String> conjunction = new ArrayList<String>();
        
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
                {
                    field.add("ANY");
                }
        		else
                {
                    field.add(tmp_field.trim());
                }
        		if (i != numField)
            	{
            		conjunction.add(request.getParameter("conjunction"+i) != null?
            				request.getParameter("conjunction"+i):"AND");
            	}
        	}
        }
        Iterator<String> iquery = query.iterator();
        Iterator<String> ifield = field.iterator();
        Iterator<String> iconj = conjunction.iterator();
        
        String conj_curr = "";
        while (iquery.hasNext())
        {	newquery = newquery + conj_curr;
        	String query_curr = iquery.next();
        	String field_curr = ifield.next();
        	newquery = newquery + buildQueryPart(query_curr,field_curr);
        	if (iconj.hasNext())
        	{
        		conj_curr = " " + iconj.next() + " ";
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
        StringBuilder newQuery = new StringBuilder();
        newQuery.append("(");

        boolean newTerm = true;
        boolean inPhrase = false;
        char phraseChar = '\"';

        StringTokenizer qtok = new StringTokenizer(myquery, " \t\n\r\f\"\'", true);

        while (qtok.hasMoreTokens())
        {
            String token = qtok.nextToken();
            if (StringUtils.isWhitespace(token))
            {
                if (!inPhrase)
                {
                    newTerm = true;
                }

                newQuery.append(token);
            }
            else
            {
                // Matched the end of the phrase
                if (inPhrase && token.charAt(0) == phraseChar)
                {
                    newQuery.append("\"");
                    inPhrase = false;
                }
                else
                {
                    // If we aren't dealing with a new term, and have a single quote
                    // don't touch it. (for example, the apostrophe in it's).
                    if (!newTerm && token.charAt(0) == '\'')
                    {
                        newQuery.append(token);
                    }
                    else
                    {
                        // Treat - my"phrased query" - as - my "phrased query"
                        if (!newTerm && token.charAt(0) == '\"')
                        {
                            newQuery.append(" ");
                            newTerm = true;
                        }

                        // This is a new term in the query (ie. preceeded by nothing or whitespace)
                        // so apply a field restriction if specified
                        if (newTerm && !myfield.equals("ANY"))
                        {
                            newQuery.append(myfield).append(":");
                        }

                        // Open a new phrase, and closing at the corresponding character
                        // ie. 'my phrase' or "my phrase"
                        if (token.charAt(0) == '\"' || token.charAt(0) == '\'')
                        {
                            newQuery.append("\"");
                            inPhrase = true;
                            newTerm = false;
                            phraseChar = token.charAt(0);
                        }
                        else
                        {
                            newQuery.append(token);
                            newTerm = false;
                        }
                    }
                }
            }
        }

        newQuery.append(")");
        return newQuery.toString();
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
    public Map<String, String> buildQueryMap(HttpServletRequest request)
    {
        Map<String, String> queryMap = new HashMap<String, String>();
        String numFieldStr = request.getParameter("num_search_field");
        // for backward compatibility
        if (numFieldStr == null)
        {
            numFieldStr = "3";
        }
        int numField = Integer.parseInt(numFieldStr);
        for (int i = 1; i < numField; i++)
        {
            String queryStr = "query" + i;
            String fieldStr = "field" + i;
            String conjunctionStr = "conjunction" + i;

        	queryMap.put(queryStr, StringUtils.defaultString(request.getParameter(queryStr), ""));
        	queryMap.put(fieldStr, StringUtils.defaultString(request.getParameter(fieldStr), "ANY"));
            queryMap.put(conjunctionStr, StringUtils.defaultString(request.getParameter(conjunctionStr), "AND"));
        }
        
        String queryStr = "query" + numField;
        String fieldStr = "field" + numField;
        queryMap.put(queryStr, StringUtils.defaultString(request.getParameter(queryStr), ""));
        queryMap.put(fieldStr, StringUtils.defaultString(request.getParameter(fieldStr), "ANY"));
        
        return (queryMap);
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
        StringBuilder queryString = new StringBuilder();
        Map<String, String> queryMap = buildQueryMap(request);

        for (Map.Entry<String, String> query : queryMap.entrySet())
        {
            queryString.append("&")
                       .append(query.getKey())
                       .append("=")
                       .append(URLEncoder.encode(query.getValue(), Constants.DEFAULT_ENCODING));
        }

        if (request.getParameter("num_search_field") != null)
        {
            queryString.append("&num_search_field=").append(request.getParameter("num_search_field"));
        }

        // return the result with the leading "&" removed
        return queryString.substring(1);
    }
}
