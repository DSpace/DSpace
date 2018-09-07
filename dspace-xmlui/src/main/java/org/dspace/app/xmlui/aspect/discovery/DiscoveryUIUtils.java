/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.solr.client.solrj.util.ClientUtils;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DiscoveryUIUtils {

    private static SearchService searchService = null;

    static {
        searchService = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(SearchService.class.getName(),SearchService.class);
    }



    /**
     * Returns a list of the filter queries for use in rendering pages, creating page more urls, ....
     * @param request user's request.
     * @return an array containing the filter queries
     */
    public static Map<String, String[]> getParameterFilterQueries(Request request) {
        Map<String, String[]> fqs = new HashMap<String, String[]>();

        List<String> filterTypes = getRepeatableParameters(request, "filtertype");
        List<String> filterOperators = getRepeatableParameters(request, "filter_relational_operator");
        List<String> filterValues = getRepeatableParameters(request, "filter");

        for (int i = 0; i < filterTypes.size(); i++) {
            String filterType = filterTypes.get(i);
            String filterValue = filterValues.get(i);
            String filterOperator = filterOperators.get(i);

            fqs.put("filtertype_" + i, new String[]{filterType});
            fqs.put("filter_relational_operator_" + i, new String[]{filterOperator});
            fqs.put("filter_" + i, new String[]{filterValue});
        }
        return fqs;
    }

    /**
     * Returns all the filter queries for use by discovery
     * @param request user's request.
     * @param context session context.
     * @return an array containing the filter queries
     */
    public static String[] getFilterQueries(Request request, Context context) {
        try {
            List<String> allFilterQueries = new ArrayList<String>();
            List<String> filterTypes = getRepeatableParameters(request, "filtertype");
            List<String> filterOperators = getRepeatableParameters(request, "filter_relational_operator");
            List<String> filterValues = getRepeatableParameters(request, "filter");

            for (int i = 0; i < filterTypes.size(); i++) {
                String filterType = filterTypes.get(i);
                String filterOperator = filterOperators.get(i);
                String filterValue = filterValues.get(i);

                if(StringUtils.isNotBlank(filterValue)){
                    allFilterQueries.add(searchService.toFilterQuery(context, (filterType.equals("*") ? "" : filterType), filterOperator, filterValue).getFilterQuery());
                }
            }

            return allFilterQueries.toArray(new String[allFilterQueries.size()]);
        }
        catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            return new String[0];
        }
    }

    public static List<String> getRepeatableParameters(Request request, String prefix){
        TreeMap<String, String> result = new TreeMap<String, String>();

        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameter = (String) parameterNames.nextElement();
            if(parameter.startsWith(prefix)){
                result.put(parameter, request.getParameter(parameter));
            }
        }
        return new ArrayList<String>(result.values());
    }

    /**
     * Escape some solr special characters from the user's query.
     * 
     * 1 - when a query ends with one of solr's special characters (^, \,!, +, -,:, ||, && (,),{,},[,]) 
     *     (a space in between or not) (e.g. "keyword3 :") the user gets 
     *     an erroneous notification or the search doesn't produce results. 
     *     Those characters at the end of the query should be escaped.
     * 
     * 2 - escape every colon, followed by a space (e.g. "title: subtitle")
     *     in a user's query. This is intended to let end users to pass 
     *     in a title containing colon-space without requiring them to escape the colon.
     * 
     * @param query user-entered query string
     * @return query escaping some of solr's special characters at the end and 
     *         with a colon in colon-space sequence escaped if they occur.
     */
    public static String escapeQueryChars(String query)
    {
        query = query.trim();
        
        // [+\\-&|!()\\s{}\\[\\]\\^\"\\\\:]: Śome of the solr's special characters that need to be escaped for regex as well as for string.
        //                                   Regex representation of \ is \\. Therefore the string representation of \\ is \\\\).
        //                                   \\s is in case withespaces is in between the characters.
        // + : Match or more of the preceding token
        // (?=\s+$|$): Matches all solr's special characters at the end of a string independently of any whitespace characters 
        //            - ?= is a positive lookahead. Matches a group after the main expression without including it in the result
        //            - \s: Matches any whitespace character (spaces, tabs, line breaks )
        //            - $: Matches the end of a string
        String regx = "[+\\-&|!()\\s{}\\[\\]\\^\"\\\\:]+(?=\\s+$|$)";  
        Pattern pattern = Pattern.compile(regx);
        Matcher matcher = pattern.matcher(query);
       
        if(matcher.find())
        {
            String matcherGroup = matcher.group();
            String escapedMatcherGroup = ClientUtils.escapeQueryChars(matcherGroup);
            
            // Do not escape brackets if they are properly opened and closed.
            if(matcherGroup.equals(")") ||
                    matcherGroup.equals("]") || 
                    matcherGroup.equals("}") ||
                    matcherGroup.equals("\""))
            {
                String closingBracket = matcher.group();
                String openingBracket = new String();
                
                switch(closingBracket)
                {
                    case "}":
                        openingBracket = "{";
                        break;
                    case ")":
                        openingBracket = "(";
                        break;
                    case "]":
                        openingBracket = "[";
                        break;
                    case "\"":
                        openingBracket = "\"";
                        break;
                }
                
                String bracketsRegex = "\\".concat(openingBracket)
                                           .concat("(.*?)\\")
                                           .concat(closingBracket);
               
                if(!Pattern.compile(bracketsRegex).matcher(query).find()) 
                {
                    query = StringUtils.replace(query, matcherGroup, escapedMatcherGroup);
                }
            }
            else
            {
                query = StringUtils.replace(query, matcherGroup, escapedMatcherGroup);
            }
        }
        
        query = StringUtils.replace(query, ": ", "\\:");
        return query;
    }
}
