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
import org.dspace.utils.DSpace;

import java.util.*;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DiscoveryUIUtils {

    private static SearchService searchService = null;
    public static final String filtertypeParam = "filtertype";
    public static final String relationalOperatorParam = "filter_relational_operator";
    public static final String filterParam = "filter";
    static {
        DSpace dspace = new DSpace();
        searchService = dspace.getServiceManager().getServiceByName(SearchService.class.getName(),SearchService.class);
    }



    /**
     * Returns a list of the filter queries for use in rendering pages, creating page more urls, ....
     * @return an array containing the filter queries
     */
    public static Map<String, String[]> getParameterFilterQueries(Request request) {
        Map<String, String[]> fqs = new HashMap<String, String[]>();

        List<String> filterTypes = getRepeatableParameters(request, filtertypeParam);
        List<String> filterOperators = getRepeatableParameters(request, relationalOperatorParam);
        List<String> filterValues = getRepeatableParameters(request, filterParam);

        for (int i = 0; i < filterTypes.size(); i++) {
            String filterType = filterTypes.get(i);
            String filterValue = filterValues.get(i);
            String filterOperator = filterOperators.get(i);

            fqs.put(filtertypeParam+"_" + i, new String[]{filterType});
            fqs.put(relationalOperatorParam+"_" + i, new String[]{filterOperator});
            fqs.put(filterParam+"_" + i, new String[]{filterValue});
        }
        return fqs;
    }

    /**
     * Returns all the filter queries for use by discovery
     * @return an array containing the filter queries
     */
    public static String[] getFilterQueries(Request request, Context context) {
        try {
            List<String> allFilterQueries = new ArrayList<String>();
            List<String> filterTypes = getRepeatableParameters(request, filtertypeParam);
            List<String> filterOperators = getRepeatableParameters(request, relationalOperatorParam);
            List<String> filterValues = getRepeatableParameters(request, filterParam);

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
        return getRepeatableParameters(request,prefix,null);
            }


    /**
     * Returns the repeatable parameters based on a given prefix, excluding (if any) the provided prefixes to ignore
     * @param prefix The prefix to match against
     * @param prefixesToIgnore The prefixes to ignore from the end results
     * @return an array containing the repeatable parameters
     */
    public static java.util.List<String> getRepeatableParameters(Request request, String prefix, String[] prefixesToIgnore){
        TreeMap<String, String> result = new TreeMap<>();

        java.util.List<String> ignoredList = new ArrayList<>();
        if(prefixesToIgnore != null){
            ignoredList = Arrays.asList(prefixesToIgnore);
        }
        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameter = (String) parameterNames.nextElement();
            if(parameter.startsWith(prefix) && !ignoredList.contains(StringUtils.substringBeforeLast(parameter,"_"))){
                result.put(parameter, request.getParameter(parameter));
            }
        }
        return new ArrayList<>(result.values());
    }

    /**
     * Escape colon-space sequence in a user-entered query, based on the
     * underlying search service. This is intended to let end users paste in a
     * title containing colon-space without requiring them to escape the colon.
     *
     * @param query user-entered query string
     * @return query with colon in colon-space sequence escaped
     */
    public static String escapeQueryChars(String query)
    {
        return StringUtils.replace(query, ": ", "\\: ");
    }
}
