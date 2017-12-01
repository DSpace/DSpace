/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.environment.Request;
import org.apache.oro.text.perl.Perl5Util;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.core.Constants;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

/**
 * Central place where advanced search queries can be built since these are built on several places
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * 
 * @deprecated Since DSpace 4 the system use an abstraction layer named
 *             Discovery to provide access to different search provider. The
 *             legacy system build upon Apache Lucene is likely to be removed in
 *             a future version. If you are interested in use Lucene as backend
 *             for the DSpace search system please consider to build a Lucene
 *             implementation of the Discovery interfaces
 */
@Deprecated
public class AdvancedSearchUtils {

    private static final int FIELD_MAX_COUNT = 12;

    /**
     * Given a list of search fields build a lucene search query string.
     *
     * @param fields The search fields
     * @return A string
     */
    public static String buildQuery(java.util.List<SearchField> fields)
    {
    	Perl5Util util = new Perl5Util();

    	StringBuilder query = new StringBuilder();
        query.append("(");

    	// Loop through the fields building the search query as we go.
    	for (SearchField field : fields)
    	{
    		// if the field is empty, then skip it and try a later one.
    		if (field.getQuery() == null)
            {
                continue;
            }

    		// Add the conjunction for everything but the first field.
    		if (fields.indexOf(field) > 0)
            {
                query.append(" ").append(field.getConjunction()).append(" ");
            }

    		// Two cases, one if a specific search field is specified or if
    		// ANY is given then just a general search is performed.
            if ("ANY".equals(field.getField()))
            {
            	// No field specified,
            	query.append("(").append(field.getQuery()).append(")");
            }
            else
            {
            	// Specific search field specified, add the field specific field.

            	// Replace single quotes with double quotes (only if they match)
            	String subQuery = util.substitute("s/\'(.*)\'/\"$1\"/g", field.getQuery());

            	// If the field is not quoted ...
            	if (!util.match("/\".*\"/", subQuery))
                {
            		// ... then separate each word and re-specify the search field.
                    subQuery = util.substitute("s/[ ]+/ " + field.getField() + ":/g", subQuery);
                }

            	// Put the subQuery into the general query
            	query.append("(").append(field.getField()).append(":").append(subQuery).append(")");
            }
    	}


    	if (query.length() == 1)
        {
    		return "";
        }

    	return query.append(")").toString();
    }


    /**
     * Get a list of search fields from the request object
     * and parse them into a linear array of fileds. The field's
     * index is preserved, so if it comes in as index 17 it will
     * be outputted as field 17.
     *
     * @param request The http request object
     * @return Array of search fields
     * @throws org.dspace.app.xmlui.utils.UIException
     */
    public static java.util.List<SearchField> getSearchFields(Request request) throws UIException
	{
    	// Get how many fields to search
	    int numSearchField;
	    try {
	    	String numSearchFieldStr = request.getParameter("num_search_field");
	    	numSearchField = Integer.valueOf(numSearchFieldStr);
	    }
	    catch (NumberFormatException nfe)
	    {
	    	numSearchField = FIELD_MAX_COUNT;
	    }

    	// Iterate over all the possible fields and add each one to the list of fields.
		ArrayList<SearchField> fields = new ArrayList<SearchField>();
		for (int i = 1; i <= numSearchField; i++)
		{
			String field = request.getParameter("field"+i);
			String query = decodeFromURL(request.getParameter("query"+i));
			String conjunction = request.getParameter("conjunction"+i);

			if (field != null)
			{
				field = field.trim();
				if (field.length() == 0)
                {
                    field = null;
                }
			}


			if (query != null)
			{
				query = query.trim();
				if (query.length() == 0)
                {
                    query = null;
                }
			}

			if (conjunction != null)
			{
				conjunction = conjunction.trim();
				if (conjunction.length() == 0)
                {
                    conjunction = null;
                }
			}

			if (field == null)
            {
                field = "ANY";
            }
			if (conjunction == null)
            {
                conjunction = "AND";
            }

			if (query != null)
            {
                fields.add(new SearchField(i, field, query, conjunction));
            }
		}

		return fields;
	}

    /**
     * A private record keeping class to relate the various
     * components of a search field together.
     */
    public static class SearchField {

    	/** What index the search field is, typically there are just three - but the theme may expand this number */
    	private int index;

    	/** The field to search, ANY if none specified */
    	private String field;

    	/** The query string to search for */
    	private String query;

    	/** the conjunction: either "AND" or "OR" */
    	private String conjuction;

    	public SearchField(int index, String field, String query, String conjunction)
    	{
    		this.index = index;
    		this.field = field;
    		this.query = query;
    		this.conjuction = conjunction;
    	}

    	public int    getIndex() { return this.index;}
    	public String getField() { return this.field;}
    	public String getQuery() { return this.query;}
    	public String getConjunction() { return this.conjuction;}
    }

    /**
     * Decode the given string from URL transmission.
     *
     * @param encodedString
     *            The encoded string.
     * @return The unencoded string
     */
    private static String decodeFromURL(String encodedString) throws UIException
    {
        if (encodedString == null)
        {
            return null;
        }

        try
        {
            // Percent(%) is a special character, and must first be escaped as %25
            if (encodedString.contains("%"))
            {
                encodedString = encodedString.replace("%", "%25");
            }

            return URLDecoder.decode(encodedString, Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }

    }
}
