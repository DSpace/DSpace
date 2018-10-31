/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.configuration.RelationConfiguration;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.integration.ICRISComponent;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.cris.dto.ComponentInfoDTO;
import org.dspace.app.webui.discovery.DiscoverUtility;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;

public abstract class ASolrConfigurerComponent<T extends DSpaceObject, IBC extends ICrisBeanComponent>
        implements ICRISComponent<IBC>
{

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(ASolrConfigurerComponent.class);

    private ApplicationService applicationService;

    private SearchService searchService;

    private RelationConfiguration relationConfiguration;

    private String commonFilter;

    private Integer relationObjectType;
    
    public ApplicationService getApplicationService()
    {
        if (applicationService == null)
        {
            DSpace dspace = new DSpace();
            applicationService = dspace.getServiceManager().getServiceByName(
                    "applicationService", ApplicationService.class);
        }
        return applicationService;
    }

    public SearchService getSearchService()
    {
        if (searchService == null)
        {
            DSpace dspace = new DSpace();
            searchService = dspace.getServiceManager().getServiceByName(
                    SearchService.class.getName(), CrisSearchService.class);
        }
        return searchService;
    }

    private Map<String, IBC> types = new HashMap<String, IBC>();

    private String shortName;

    private Class<ACrisObject> target;

    protected ACrisObject getCrisObject(HttpServletRequest request)
    {
        ACrisObject cris = (ACrisObject) request
                .getAttribute(this.getClass().getName() + "-"
                        + getRelationConfiguration().getRelationName());
        if (cris == null)
        {
            Integer entityID = (Integer) request.getAttribute("entityID");
            cris = getApplicationService().get(getTarget(), entityID);
        }
        return cris;
    }

    @Override
    public void evalute(HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        ACrisObject cris = getCrisObject(request);
        // Get the query from the box name
        String opentype = getType(request, cris.getId());
        String relationName = this.getShortName();        
        String type = getTypes().containsKey(opentype)?opentype:relationName;
        List<String[]> activeTypes = addActiveTypeInRequest(request, type);

        int start = 0;

        int sortBy = -1;
        String order = "";
        int rpp = -1;
        int etAl = -1;
        String orderfield = "";
        boolean ascending = false;
        int searchTime = 0;
        
        Context context = UIUtil.obtainContext(request);
        DiscoverResult docs = null;
        long docsNumFound = 0;

        if (types.keySet().contains(type))
        {
            List<String> extraFields = getExtraFields(type);
        	start = UIUtil.getIntParameter(request,
                    "start" + getTypes().get(type).getComponentIdentifier());
            // can't start earlier than 0 in the results!
            if (start < 0)
            {
                start = 0;
            }
            sortBy = getSortBy(request, type);
            order = getOrder(request, type);
            rpp = getRPP(request, type);
            etAl = getEtAl(request, type);
            orderfield = getOrderField(sortBy);
            ascending = SortOption.ASCENDING.equalsIgnoreCase(order);

            // Perform the search

            docs = search(context, request, type, cris, start, rpp, orderfield,
                    ascending, extraFields);
            if (docs != null)
            {
                docsNumFound = docs.getTotalSearchResults();
                searchTime = docs.getSearchTime();
            }
        }

        if ((docs == null || docsNumFound == 0) && activeTypes.size() > 0)
        {
        	List<String> extraFields = getExtraFields(type);
            type = activeTypes.get(0)[0];
            sortBy = getSortBy(request, type);
            order = getOrder(request, type);
            rpp = getRPP(request, type);
            etAl = getEtAl(request, type);
            orderfield = getOrderField(sortBy);
            ascending = SortOption.ASCENDING.equalsIgnoreCase(order);
            docs = search(context, request, type, cris, start, rpp, orderfield,
                    ascending, extraFields);
            if (docs != null)
            {
                docsNumFound = docs.getTotalSearchResults();
                searchTime = docs.getSearchTime();
            }
        }

        // Pass in some page qualities
        // total number of pages
        int pageTotal = 0;

        if (docs != null)
        {
            pageTotal = (int) (1 + ((docsNumFound - 1) / rpp));
        }
        // current page being displayed
        int pageCurrent = 1 + (start / rpp);

        // pageLast = min(pageCurrent+9,pageTotal)
        int pageLast = ((pageCurrent + 9) > pageTotal) ? pageTotal
                : (pageCurrent + 9);

        // pageFirst = max(1,pageCurrent-9)
        int pageFirst = ((pageCurrent - 9) > 1) ? (pageCurrent - 9) : 1;

        SortOption sortOption = null;
        if (sortBy > 0)
        {
            sortOption = SortOption.getSortOption(sortBy);
        }

        // Pass the results to the display JSP

        Map<String, ComponentInfoDTO<T>> componentInfoMap = (Map<String, ComponentInfoDTO<T>>) request
                .getAttribute("componentinfomap");
        if (componentInfoMap == null || componentInfoMap.isEmpty())
        {
            componentInfoMap = new HashMap<String, ComponentInfoDTO<T>>();
        }
        else
        {
            if (componentInfoMap.containsKey(getShortName()))
            {
                componentInfoMap.remove(getShortName());
            }
        }

        ComponentInfoDTO<T> componentInfo = buildComponentInfo(docs, context,
                type, start, order, rpp, etAl, docsNumFound, pageTotal,
				pageCurrent, pageLast, pageFirst, sortOption,
				searchTime);

        componentInfoMap.put(getShortName(), componentInfo);
        request.setAttribute("componentinfomap", componentInfoMap);

        if (AuthorizeManager.isAdmin(context))
        {
            // Set a variable to create admin buttons
            request.setAttribute("admin_button", new Boolean(true));
        }
        request.setAttribute(
                "facetsConfig" + getRelationConfiguration().getRelationName(),
                getFacets() != null ? getFacets()
                        : new ArrayList<DiscoverySearchFilterFacet>());
        request.setAttribute(
                "qResults" + getRelationConfiguration().getRelationName(),
                docs);
        List<String[]> appliedFilters = DiscoverUtility.getFilters(request, getRelationConfiguration().getRelationName());
        request.setAttribute(
                "appliedFilters" + getRelationConfiguration().getRelationName(),
                appliedFilters);
        List<String> appliedFilterQueries = new ArrayList<String>();
        for (String[] filter : appliedFilters)
        {
            appliedFilterQueries
                    .add(filter[0] + "::" + filter[1] + "::" + filter[2]);
        }
        request.setAttribute(
                "appliedFilterQueries"
                        + getRelationConfiguration().getRelationName(),
                appliedFilterQueries);
        request.setAttribute("count" + this.getShortName(), docsNumFound);
    }

	private String getOrderField(int sortBy) throws SortException {
		String orderfield;
		orderfield = sortBy != -1 ? "bi_sort_" + sortBy + "_sort" : null;
		if (orderfield != null && "extra".equalsIgnoreCase(SortOption.getSortOption(sortBy).getType())) {
			orderfield = SortOption.getSortOption(sortBy).getMetadata().substring("extra.".length());
		}
		return orderfield;
	}

    protected abstract List<String[]> addActiveTypeInRequest(
            HttpServletRequest request, String currentType) throws Exception;

    private ComponentInfoDTO<T> buildComponentInfo(DiscoverResult docs,
            Context context, String type, int start, String order, int rpp,
            int etAl, long docsNumFound, int pageTotal, int pageCurrent,
			int pageLast, int pageFirst, SortOption sortOption, int searchTime)
            throws Exception
    {
        ComponentInfoDTO<T> componentInfo = new ComponentInfoDTO<T>();
        if (docs != null)
        {
            componentInfo.setItems(getObjectFromSolrResult(docs, context));
        }

        componentInfo.setPagetotal(pageTotal);
        componentInfo.setPagecurrent(pageCurrent);
        componentInfo.setPagelast(pageLast);
        componentInfo.setPagefirst(pageFirst);
		componentInfo.setRelationName(getRelationConfiguration()
				.getRelationName());
        componentInfo.setOrder(order);
        componentInfo.setSo(sortOption);
        componentInfo.setStart(start);
        componentInfo.setRpp(rpp);
        componentInfo.setEtAl(etAl);
        componentInfo.setTotal(docsNumFound);
        componentInfo.setType(type);
		componentInfo.setSearchTime(searchTime);
		componentInfo.setBrowseType(getRelationConfiguration().getType());
        return componentInfo;
    }

    protected abstract T[] getObjectFromSolrResult(DiscoverResult docs,
            Context context) throws Exception;

    public DiscoverResult search(Context context, HttpServletRequest request, String type,
            ACrisObject cris, int start, int rpp, String orderfield,
            boolean ascending, List<String> extraFields) throws SearchServiceException
    {
        // can't start earlier than 0 in the results!
        if (start < 0)
        {
            start = 0;
        }
        String authority = cris.getCrisID();
        String uuid = cris.getUuid();
        String query = MessageFormat.format(getRelationConfiguration()
                .getQuery(), authority, uuid);
        
        List<String> filters = getFilters(type);

        DiscoverQuery discoveryQuery = new DiscoverQuery();
        if(getCommonFilter()!=null) {            
            String format = MessageFormat.format(getCommonFilter(), getRelationConfiguration().getRelationName(),
                    cris.getUuid(), authority);
            discoveryQuery.addFilterQueries(format);
        }
        try
        {
            discoveryQuery.addFilterQueries("NOT(withdrawn:true)",
                    getTypeFilterQuery());
        }
        catch (InstantiationException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error(e.getMessage(), e);
        }
        
        discoveryQuery.setQuery(query);
        
        if (extraFields != null) {
        	for (String f : extraFields) {
        	    discoveryQuery.addSearchField(f);
        	}
        }
        discoveryQuery.setStart(start);
        discoveryQuery.setMaxResults(rpp);
        if (orderfield == null)
        {
            orderfield = "score";
        }
        discoveryQuery.setSortField(orderfield, ascending ? SORT_ORDER.asc
                : SORT_ORDER.desc);

        if (filters != null)
        {
            for (String filter : filters)
            {
                discoveryQuery.addFilterQueries(MessageFormat.format(filter,
                        authority, uuid));
            }
        }
        List<String> userFilters = new ArrayList<String>();
        if (request != null)
        {
            List<String[]> httpfilters = DiscoverUtility.getFilters(request, getRelationConfiguration().getRelationName());
            for (String[] f : httpfilters)
            {
                try
                {
                    String newFilterQuery = SearchUtils.getSearchService()
                            .toFilterQuery(context, f[0], f[1], f[2])
                            .getFilterQuery();
                    if (StringUtils.isNotBlank(newFilterQuery))
                    {
                        discoveryQuery.addFilterQueries(newFilterQuery);
                        userFilters.add(("{!tag=dt}"+newFilterQuery));
                    }
                }
                catch (SQLException e)
                {
                    log.error(
                            LogManager.getHeader(context,
                                    "Error in discovery while setting up user facet query",
                                    "filter_field: " + f[0] + ",filter_type:"
                                            + f[1] + ",filer_value:" + f[2]),
                            e);
                }

            }
        }
        
        discoveryQuery.setFacetMinCount(1);
        for (DiscoverySearchFilterFacet facet : getFacets()) {
            
            if (facet.getType().equals(
                    DiscoveryConfigurationParameters.TYPE_DATE))
            {
                String dateFacet = facet.getIndexFieldName() + ".year";
                List<String> filterQueriesList = discoveryQuery
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
                                .getSearchService().search(context, cris,
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
                                .getSearchService().search(context, cris,
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
                        discoveryQuery.addFacetField(new DiscoverFacetField(
                                facet.getIndexFieldName(), facet.getType(),
                                10, facet.getSortOrder(),false));
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
                            discoveryQuery.addFacetQuery(facetQuery);
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
                int facetLimit = facet.getFacetLimit();

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
                discoveryQuery.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(),
                        facet.getType(), facetLimit + 1 + alreadySelected, facet
                                .getSortOrder(), facetPage * facetLimit, false));
            }

        }
        
        return getSearchService().search(context, discoveryQuery);

    }

    protected String getType(HttpServletRequest request, Integer id)
    {
        String type = request.getParameter("open");
        if (type == null)
        {
            type = types.keySet().iterator().next();
            for (String t : types.keySet())
            {

                if (count(request, t, id) > 0)
                {
                    type = t;
                    break;
                }
            }
        }
        return type;
    }

    private int getEtAl(HttpServletRequest request, String type)
    {
        int etAl = UIUtil.getIntParameter(request, "etAl" + type);
        if (etAl == -1)
        {
            if(getTypes()!=null && getTypes().containsKey(type)) {
                etAl = getTypes().get(type).getEtal();
            }
        }
        return etAl;
    }

    private int getRPP(HttpServletRequest request, String type)
    {
        int rpp = UIUtil.getIntParameter(request, "rpp" + type);
        if (rpp == -1)
        {
            if(getTypes()!=null && getTypes().containsKey(type)) {
                rpp = getTypes().get(type).getRpp();
            }
        }
        return rpp;
    }

    private String getOrder(HttpServletRequest request, String type)
    {
        String order = request.getParameter("order" + type);
        if (order == null)
        {
            if(getTypes()!=null && getTypes().containsKey(type)) {
                order = getTypes().get(type).getOrder();
            }
        }
        return order;
    }
    
    private List<String> getExtraFields(String type)
    {
        if(getTypes()!=null && getTypes().containsKey(type)) {
            return getTypes().get(type).getExtraFields();
        }
        return new ArrayList<String>();
    }

    private int getSortBy(HttpServletRequest request, String type)
    {
        int sortBy = UIUtil.getIntParameter(request, "sort_by" + type);
        if (sortBy == -1)
        {
            if(getTypes()!=null && getTypes().containsKey(type)) {
                sortBy = getTypes().get(type).getSortby();
            }
        }
        return sortBy;
    }

    protected List<String> getFilters(String type)
    {        
        if(getTypes()!=null && getTypes().containsKey(type)) {
            return getTypes().get(type).getFilters();
        }
        
        return new ArrayList<String>();
    }

    public Map<String, IBC> getTypes()
    {
        return types;
    }

    public void setTypes(Map<String, IBC> types)
    {
        this.types = types;
    }

    public void setShortName(String shortName)
    {
        this.shortName = shortName;
    }

    public String getShortName()
    {
        return this.shortName;
    }

    public Class<ACrisObject> getTarget()
    {
        return target;
    }

    public void setTarget(Class<ACrisObject> target)
    {
        this.target = target;
    }

    public RelationConfiguration getRelationConfiguration()
    {
        return relationConfiguration;
    }

    public void setRelationConfiguration(
            RelationConfiguration relationConfiguration)
    {
        this.relationConfiguration = relationConfiguration;
    }

    public void setCommonFilter(String commonFilter)
    {
        this.commonFilter = commonFilter;
    }

    public String getCommonFilter()
    {
        if (commonFilter == null)
        {
            return "";
        }
        return commonFilter;
    }

    protected String getTypeFilterQuery() throws InstantiationException,
            IllegalAccessException
    {
        if (ResearchObject.class.isAssignableFrom(getRelationConfiguration()
                .getRelationClass()))
        {
            return "search.resourcetype:["
                    + CrisConstants.CRIS_DYNAMIC_TYPE_ID_START + " TO "
                    + CrisConstants.CRIS_NDYNAMIC_TYPE_ID_START + "]";
        }
        return "search.resourcetype:"
                + CrisConstants.getEntityType(getRelationConfiguration()
                        .getRelationClass());
    }

    public void setRelationObjectType(Integer relationObjectType)
    {
        this.relationObjectType = relationObjectType;
    }
    
    public abstract List<DiscoverySearchFilterFacet> getFacets();
}
