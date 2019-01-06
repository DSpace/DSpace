/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

public class DiscoverUtility
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DiscoverUtility.class);
    
    public static final int TYPE_FACETS = 1;
    public static final int TYPE_TAGCLOUD = 2;
    
    /**
     * Get the scope of the search using the parameter found in the request.
     * 
     * @param context
     * @param request
     * @throws IllegalStateException
     * @throws SQLException
     */
    public static DSpaceObject getSearchScope(Context context,
            HttpServletRequest request) throws IllegalStateException,
            SQLException
    {
        // Get the location parameter, if any
        String location = request.getParameter("location");
        if (location == null)
        {
            if (UIUtil.getCollectionLocation(request) != null)
            {
                return UIUtil.getCollectionLocation(request);
            }
            if (UIUtil.getCommunityLocation(request) != null)
            {
                return UIUtil.getCommunityLocation(request);
            }
            return null;
        }
        HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
        DSpaceObject scope = handleService.resolveToObject(context, location);
        return scope;
    }

    /**
     * Build a DiscoverQuery object using the parameter in the request
     * 
     * @param request
     * @return the query.
     * @throws SearchServiceException
     */
    public static DiscoverQuery getDiscoverQuery(Context context,
            HttpServletRequest request, DSpaceObject scope, boolean enableFacet)
    {
        DiscoverQuery queryArgs = new DiscoverQuery();
        DiscoveryConfiguration discoveryConfiguration = SearchUtils
                .getDiscoveryConfiguration(scope);

        List<String> userFilters = setupBasicQuery(context,
                discoveryConfiguration, request, queryArgs);

        setPagination(request, queryArgs, discoveryConfiguration);

        if (enableFacet
                && !"submit_export_metadata".equals(UIUtil.getSubmitButton(
                        request, "submit")))
        {
            setFacet(context, request, scope, queryArgs,
                    discoveryConfiguration, userFilters, discoveryConfiguration
                    .getSidebarFacets(), TYPE_FACETS);
        }

        return queryArgs;
    }

    /**
     * Build a DiscoverQuery object using the tag cloud parameter in the request
     * 
     * @param request
     * @return the query.
     * @throws SearchServiceException
     */
    public static DiscoverQuery getTagCloudDiscoverQuery(Context context,
            HttpServletRequest request, DSpaceObject scope, boolean enableFacet)
    {
        DiscoverQuery queryArgs = new DiscoverQuery();
        DiscoveryConfiguration discoveryConfiguration = SearchUtils
                .getDiscoveryConfiguration(scope);

        List<String> userFilters = setupBasicQuery(context,
                discoveryConfiguration, request, queryArgs);

        setPagination(request, queryArgs, discoveryConfiguration);

        if (enableFacet
                && !"submit_export_metadata".equals(UIUtil.getSubmitButton(
                        request, "submit")))
        {
            setFacet(context, request, scope, queryArgs,
                    discoveryConfiguration, userFilters, discoveryConfiguration
                    .getTagCloudFacetConfiguration().getTagCloudFacets(), TYPE_TAGCLOUD);
        }

        return queryArgs;
    }
    
    /**
     * Build the DiscoverQuery object for an autocomplete search using
     * parameters in the request
     * 
     * @param context
     * @param request
     * @param scope
     * @return the query.
     */
    public static DiscoverQuery getDiscoverAutocomplete(Context context,
            HttpServletRequest request, DSpaceObject scope)
    {
        DiscoverQuery queryArgs = new DiscoverQuery();
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration();
        
        setupBasicQuery(context, discoveryConfiguration, request, queryArgs);
        String autoIndex = request.getParameter("auto_idx");
        String autoQuery = request.getParameter("auto_query");
        String sort = request.getParameter("auto_sort");
        String autoType = request.getParameter("auto_type");
        if ("contains".equals(autoType) || "notcontains".equals(autoType))
        {
            autoType = DiscoveryConfigurationParameters.TYPE_STANDARD;
        }
        else if ("authority".equals(autoType) || "notauthority".equals(autoType))
        {
            autoType = DiscoveryConfigurationParameters.TYPE_AUTHORITY;
        }
        else
        {
            autoType = DiscoveryConfigurationParameters.TYPE_AC;
        }
        DiscoveryConfigurationParameters.SORT sortBy = DiscoveryConfigurationParameters.SORT.VALUE;
        if (StringUtils.isNotBlank(sort))
        {
            if ("count".equalsIgnoreCase(sort))
            {
                sortBy = DiscoveryConfigurationParameters.SORT.COUNT;
            }
            else
            {
                sortBy = DiscoveryConfigurationParameters.SORT.VALUE;
            }
        }
        // no user choices... default for autocomplete should be alphabetic 
        // sorting in all cases except empty query where count is preferable
        else if ("".equals(autoQuery))
        {
           sortBy = DiscoveryConfigurationParameters.SORT.COUNT; 
        }
        if (autoIndex == null)
        {
            autoIndex = "all";
        }
        if (autoQuery == null)
        {
            autoQuery = "";
        }
        
        int limit = UIUtil.getIntParameter(request, "autocomplete.limit");
        if (limit == -1)
        {
            limit = 10;
        }
        DiscoverFacetField autocompleteField = new DiscoverFacetField(autoIndex, 
                autoType, 
                limit, sortBy, autoQuery.toLowerCase());
        queryArgs.addFacetField(autocompleteField);
        queryArgs.setMaxResults(0);
        queryArgs.setFacetMinCount(1);
        return queryArgs;
    }

    /**
     * Setup the basic query arguments: the main query and all the filters
     * (default + user). Return the list of user filter
     * 
     * @param context
     * @param request
     * @param queryArgs
     *            the query object to populate
     * @return the list of user filer (as filter query)
     */
    private static List<String> setupBasicQuery(Context context,
            DiscoveryConfiguration discoveryConfiguration,
            HttpServletRequest request, DiscoverQuery queryArgs)
    {
        // Get the query
        String query = request.getParameter("query");
        if (StringUtils.isNotBlank(query))
        {
            // Escape any special characters in this user-entered query
            query = escapeQueryChars(query);
            queryArgs.setQuery(query);
        }

        List<String> defaultFilterQueries = discoveryConfiguration
                .getDefaultFilterQueries();
        if (defaultFilterQueries != null)
        {
            for (String f : defaultFilterQueries)
            {
                queryArgs.addFilterQueries(f);
            }
        }
        List<String[]> filters = getFilters(request);
        List<String> userFilters = new ArrayList<String>();
        for (String[] f : filters)
        {
            try
            {
            String newFilterQuery = null;
            if (StringUtils.isNotBlank(f[0]) && StringUtils.isNotBlank(f[2]))
            {
                newFilterQuery = SearchUtils.getSearchService()
                        .toFilterQuery(context, f[0], f[1], f[2])
                        .getFilterQuery();
            }
            if (newFilterQuery != null)
            {
                queryArgs.addFilterQueries(newFilterQuery);
                userFilters.add(newFilterQuery);
            }
            }
            catch (SQLException e)
            {
                log.error(LogManager.getHeader(context,
                        "Error in discovery while setting up user facet query",
                        "filter_field: " + f[0] + ",filter_type:"
                                + f[1] + ",filer_value:"
                                + f[2]), e);
            }

        }

        return userFilters;

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
    private static String escapeQueryChars(String query)
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

    private static void setPagination(HttpServletRequest request,
            DiscoverQuery queryArgs,
            DiscoveryConfiguration discoveryConfiguration)
    {
        int start = UIUtil.getIntParameter(request, "start");
        // can't start earlier than 0 in the results!
        if (start < 0)
        {
            start = 0;
        }

        String sortBy = request.getParameter("sort_by");
        String sortOrder = request.getParameter("order");

        DiscoverySortConfiguration searchSortConfiguration = discoveryConfiguration
                .getSearchSortConfiguration();
        if (sortBy == null)
        {
            // Attempt to find the default one, if none found we use SCORE
            sortBy = "score";
            if (searchSortConfiguration != null)
            {
                for (DiscoverySortFieldConfiguration sortFieldConfiguration : searchSortConfiguration
                        .getSortFields())
                {
                    if (sortFieldConfiguration.equals(searchSortConfiguration
                            .getDefaultSort()))
                    {
                        sortBy = SearchUtils
                                .getSearchService()
                                .toSortFieldIndex(
                                        sortFieldConfiguration
                                                .getMetadataField(),
                                        sortFieldConfiguration.getType());
                    }
                }
            }
        }

        if (sortOrder == null && searchSortConfiguration != null)
        {
            sortOrder = searchSortConfiguration.getDefaultSortOrder()
                    .toString();
        }
        if (sortBy != null)
        {
            if ("asc".equalsIgnoreCase(sortOrder))
            {
                queryArgs.setSortField(sortBy, SORT_ORDER.asc);
            }
            else
            {
                queryArgs.setSortField(sortBy, SORT_ORDER.desc);
            }
        }

        int rpp = UIUtil.getIntParameter(request, "rpp");
        // Override the page setting if exporting metadata
        if ("submit_export_metadata".equals(UIUtil.getSubmitButton(request,
                "submit")))
        {
            queryArgs.setStart(0);
            queryArgs.setMaxResults(Integer.MAX_VALUE);
            // search only for items other objects are not exported
            queryArgs.addFilterQueries("search.resourcetype:2");
        }
        else
        {
            // String groupBy = request.getParameter("group_by");
            //
            // // Enable groupBy collapsing if designated
            // if (groupBy != null && !groupBy.equalsIgnoreCase("none")) {
            // /** Construct a Collapse Field Query */
            // queryArgs.addProperty("collapse.field", groupBy);
            // queryArgs.addProperty("collapse.threshold", "1");
            // queryArgs.addProperty("collapse.includeCollapsedDocs.fl",
            // "handle");
            // queryArgs.addProperty("collapse.facet", "before");
            //
            // //queryArgs.a type:Article^2
            //
            // // TODO: This is a hack to get Publications (Articles) to always
            // be at the top of Groups.
            // // TODO: I think the can be more transparently done in the solr
            // solrconfig.xml with DISMAX and boosting
            // /** sort in groups to get publications to top */
            // queryArgs.setSortField("dc.type", DiscoverQuery.SORT_ORDER.asc);
            //
            // }

            if (rpp > 0 && rpp <= discoveryConfiguration.getMaxRpp())
            {
                queryArgs.setMaxResults(rpp);
            }
            else if (rpp > 0 && rpp > discoveryConfiguration.getMaxRpp())
            {
                queryArgs.setMaxResults(discoveryConfiguration.getMaxRpp());
            }
            else
            {
                queryArgs.setMaxResults(discoveryConfiguration.getDefaultRpp());
            }
            queryArgs.setStart(start);
        }
    }

    private static void setFacet(Context context, HttpServletRequest request,
            DSpaceObject scope, DiscoverQuery queryArgs,
            DiscoveryConfiguration discoveryConfiguration,
            List<String> userFilters, List<DiscoverySearchFilterFacet> facets, int type)
    {
  
        log.info("facets for scope, " + scope + ": "
                + (facets != null ? facets.size() : null));
        if (facets != null)
        {
            queryArgs.setFacetMinCount(1);
        }

        /** enable faceting of search results */
        if (facets != null)
        {
            queryArgs.setFacetMinCount(1);
            for (DiscoverySearchFilterFacet facet : facets)
            {
                if (facet.getType().equals(
                        DiscoveryConfigurationParameters.TYPE_DATE))
                {
                    String dateFacet = facet.getIndexFieldName() + ".year";
                    List<String> filterQueriesList = queryArgs
                            .getFilterQueries();
                    String[] filterQueries = new String[0];
                    if (filterQueriesList != null)
                    {
                        filterQueries = new String[filterQueries.length];
                        filterQueries = filterQueriesList
                                .toArray(filterQueries);
                    }
                    try
                    {
                        // Get a range query so we can create facet
                        // queries
                        // ranging from out first to our last date
                        // Attempt to determine our oldest & newest year
                        // by
                        // checking for previously selected filters
                        int oldestYear = -1;
                        int newestYear = -1;

                        for (String filterQuery : filterQueries)
                        {
                            if (filterQuery.startsWith(dateFacet + ":"))
                            {
                                // Check for a range
                                Pattern pattern = Pattern
                                        .compile("\\[(.*? TO .*?)\\]");
                                Matcher matcher = pattern.matcher(filterQuery);
                                boolean hasPattern = matcher.find();
                                if (hasPattern)
                                {
                                    filterQuery = matcher.group(0);
                                    // We have a range
                                    // Resolve our range to a first &
                                    // endyear
                                    int tempOldYear = Integer
                                            .parseInt(filterQuery.split(" TO ")[0]
                                                    .replace("[", "").trim());
                                    int tempNewYear = Integer
                                            .parseInt(filterQuery.split(" TO ")[1]
                                                    .replace("]", "").trim());

                                    // Check if we have a further filter
                                    // (or
                                    // a first one found)
                                    if (tempNewYear < newestYear
                                            || oldestYear < tempOldYear
                                            || newestYear == -1)
                                    {
                                        oldestYear = tempOldYear;
                                        newestYear = tempNewYear;
                                    }

                                }
                                else
                                {
                                    if (filterQuery.indexOf(" OR ") != -1)
                                    {
                                        // Should always be the case
                                        filterQuery = filterQuery.split(" OR ")[0];
                                    }
                                    // We should have a single date
                                    oldestYear = Integer.parseInt(filterQuery
                                            .split(":")[1].trim());
                                    newestYear = oldestYear;
                                    // No need to look further
                                    break;
                                }
                            }
                        }
                        // Check if we have found a range, if not then
                        // retrieve our first & last year by using solr
                        if (oldestYear == -1 && newestYear == -1)
                        {

                            DiscoverQuery yearRangeQuery = new DiscoverQuery();
                            yearRangeQuery.setFacetMinCount(1);
                            yearRangeQuery.setMaxResults(1);
                            // Set our query to anything that has this
                            // value
                            yearRangeQuery.addFieldPresentQueries(dateFacet);
                            // Set sorting so our last value will appear
                            // on
                            // top
                            yearRangeQuery.setSortField(dateFacet + "_sort",
                                    DiscoverQuery.SORT_ORDER.asc);
                            yearRangeQuery.addFilterQueries(filterQueries);
                            yearRangeQuery.addSearchField(dateFacet);
                            DiscoverResult lastYearResult = SearchUtils
                                    .getSearchService().search(context, scope,
                                            yearRangeQuery);

                            if (0 < lastYearResult.getDspaceObjects().size())
                            {
                                java.util.List<DiscoverResult.SearchDocument> searchDocuments = lastYearResult
                                        .getSearchDocument(lastYearResult
                                                .getDspaceObjects().get(0));
                                if (0 < searchDocuments.size()
                                        && 0 < searchDocuments
                                                .get(0)
                                                .getSearchFieldValues(dateFacet)
                                                .size())
                                {
                                    oldestYear = Integer
                                            .parseInt(searchDocuments
                                                    .get(0)
                                                    .getSearchFieldValues(
                                                            dateFacet).get(0));
                                }
                            }
                            // Now get the first year
                            yearRangeQuery.setSortField(dateFacet + "_sort",
                                    DiscoverQuery.SORT_ORDER.desc);
                            DiscoverResult firstYearResult = SearchUtils
                                    .getSearchService().search(context, scope,
                                            yearRangeQuery);
                            if (0 < firstYearResult.getDspaceObjects().size())
                            {
                                java.util.List<DiscoverResult.SearchDocument> searchDocuments = firstYearResult
                                        .getSearchDocument(firstYearResult
                                                .getDspaceObjects().get(0));
                                if (0 < searchDocuments.size()
                                        && 0 < searchDocuments
                                                .get(0)
                                                .getSearchFieldValues(dateFacet)
                                                .size())
                                {
                                    newestYear = Integer
                                            .parseInt(searchDocuments
                                                    .get(0)
                                                    .getSearchFieldValues(
                                                            dateFacet).get(0));
                                }
                            }
                            // No values found!
                            if (newestYear == -1 || oldestYear == -1)
                            {
                                continue;
                            }

                        }

                        int gap = 1;
                        // Attempt to retrieve our gap by the algorithm
                        // below
                        int yearDifference = newestYear - oldestYear;
                        if (yearDifference != 0)
                        {
                            while (10 < ((double) yearDifference / gap))
                            {
                                gap *= 10;
                            }
                        }
                        // We need to determine our top year so we can
                        // start
                        // our count from a clean year
                        // Example: 2001 and a gap from 10 we need the
                        // following result: 2010 - 2000 ; 2000 - 1990
                        // hence
                        // the top year
                        int topYear = (int) (Math.ceil((float) (newestYear)
                                / gap) * gap);

                        if (gap == 1)
                        {
                            // We need a list of our years
                            // We have a date range add faceting for our
                            // field
                            // The faceting will automatically be
                            // limited to
                            // the 10 years in our span due to our
                            // filterquery
                            queryArgs.addFacetField(new DiscoverFacetField(
                                    facet.getIndexFieldName(), facet.getType(),
                                    10, facet.getSortOrderSidebar()));
                        }
                        else
                        {
                            java.util.List<String> facetQueries = new ArrayList<String>();
                            // Create facet queries but limit then to 11
                            // (11
                            // == when we need to show a show more url)
                            for (int year = topYear; year > oldestYear
                                    && (facetQueries.size() < 11); year -= gap)
                            {
                                // Add a filter to remove the last year
                                // only
                                // if we aren't the last year
                                int bottomYear = year - gap;
                                // Make sure we don't go below our last
                                // year
                                // found
                                if (bottomYear < oldestYear)
                                {
                                    bottomYear = oldestYear;
                                }

                                // Also make sure we don't go above our
                                // newest year
                                int currentTop = year;
                                if ((year == topYear))
                                {
                                    currentTop = newestYear;
                                }
                                else
                                {
                                    // We need to do -1 on this one to
                                    // get a
                                    // better result
                                    currentTop--;
                                }
                                facetQueries.add(dateFacet + ":[" + bottomYear
                                        + " TO " + currentTop + "]");
                            }
                            for (String facetQuery : facetQueries)
                            {
                                queryArgs.addFacetQuery(facetQuery);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        log.error(
                                LogManager
                                        .getHeader(
                                                context,
                                                "Error in discovery while setting up date facet range",
                                                "date facet: " + dateFacet), e);
                    }
                }
                else
                {
                    int facetLimit = type==TYPE_FACETS?facet.getFacetLimit():-1;

                    int facetPage = UIUtil.getIntParameter(request,
                            facet.getIndexFieldName() + "_page");
                    if (facetPage < 0)
                    {
                        facetPage = 0;
                    }
                    // at most all the user filters belong to this facet
                    int alreadySelected = userFilters.size();

                    // Add one to our facet limit to make sure that if
                    // we
                    // have more then the shown facets that we show our
                    // show
                    // more url
                    // add the already selected facet so to have a full
                    // top list
                    // if possible
                    int limit = 0;
                    if (type==TYPE_FACETS){
                    	limit = facetLimit + 1 + alreadySelected;
                    }
                    else 
                    	limit = facetLimit;
                    
                    queryArgs.addFacetField(new DiscoverFacetField(facet
                            .getIndexFieldName(),
                            DiscoveryConfigurationParameters.TYPE_TEXT,
                           limit, facet
                                    .getSortOrderSidebar(), facetPage * facetLimit));
                }
            }
        }
    }

    public static List<String[]> getFilters(HttpServletRequest request)
    {
        String submit = UIUtil.getSubmitButton(request, "submit");
        int ignore = -1;
        if (submit.startsWith("submit_filter_remove_"))
        {
            ignore = Integer.parseInt(submit.substring("submit_filter_remove_".length()));
        }
        List<String[]> appliedFilters = new ArrayList<String[]>();
        
        List<String> filterValue = new ArrayList<String>();
        List<String> filterOp = new ArrayList<String>();
        List<String> filterField = new ArrayList<String>();
        for (int idx = 1; ; idx++)
        {
            String op = request.getParameter("filter_type_"+idx);
            if (StringUtils.isBlank(op))
            {
                break;
            }
            else if (idx != ignore)
            {
                filterOp.add(op);
                filterField.add(request.getParameter("filter_field_"+idx));
                filterValue.add(request.getParameter("filter_value_"+idx));
            }
        }
        
        String op = request.getParameter("filtertype");
        if (StringUtils.isNotBlank(op))
        {
            filterOp.add(op);
            filterField.add(request.getParameter("filtername"));
            filterValue.add(request.getParameter("filterquery"));
        }
        
        for (int idx = 0; idx < filterOp.size(); idx++)
        {
            appliedFilters.add(new String[] { filterField.get(idx),
                    filterOp.get(idx), filterValue.get(idx) });
        }
        return appliedFilters;
    }

    // /**
    // * Build the query from the advanced search form
    // *
    // * @param request
    // * @return
    // */
    // public static String buildQuery(HttpServletRequest request)
    // {
    // int num_field = UIUtil.getIntParameter(request, "num_search_field");
    // if (num_field <= 0)
    // {
    // num_field = 3;
    // }
    // StringBuffer query = new StringBuffer();
    // buildQueryPart(query, request.getParameter("field"),
    // request.getParameter("query"), null);
    // for (int i = 1; i < num_field; i++)
    // {
    // buildQueryPart(query, request.getParameter("field" + i),
    // request.getParameter("query" + i),
    // request.getParameter("conjuction" + i));
    // }
    // return query.toString();
    // }
    //
    // private static void buildQueryPart(StringBuffer currQuery, String field,
    // String queryPart, String conjuction)
    // {
    // if (StringUtils.isBlank(queryPart))
    // {
    // return;
    // }
    // else
    // {
    // StringBuffer tmp = new StringBuffer(queryPart);
    // if (StringUtils.isNotBlank(field))
    // {
    // tmp.insert(0, field + ":(").append(")");
    // }
    //
    // if (StringUtils.isNotBlank(conjuction) && currQuery.length() > 0)
    // {
    // currQuery.append(conjuction);
    // }
    // currQuery.append(tmp);
    // }
    // }

}
