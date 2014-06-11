/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.configuration.RelationConfiguration;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.integration.ICRISComponent;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.cris.dto.ComponentInfoDTO;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
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
        String type = getType(request);
        List<String[]> activeTypes = addActiveTypeInRequest(request);

        int start = 0;

        int sortBy = -1;
        String order = "";
        int rpp = -1;
        int etAl = -1;
        String orderfield = "";
        boolean ascending = false;

        Context context = UIUtil.obtainContext(request);
        DiscoverResult docs = null;
        long docsNumFound = 0;

        if (types.keySet().contains(type))
        {
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
            orderfield = sortBy != -1 ? "bi_sort_" + sortBy + "_sort" : null;
            ascending = SortOption.ASCENDING.equalsIgnoreCase(order);

            // Perform the search

            docs = search(context, type, cris, start, rpp, orderfield,
                    ascending);
            if (docs != null)
            {
                docsNumFound = docs.getTotalSearchResults();
            }
        }

        if ((docs == null || docsNumFound == 0) && activeTypes.size() > 0)
        {
            type = activeTypes.get(0)[0];
            sortBy = getSortBy(request, type);
            order = getOrder(request, type);
            rpp = getRPP(request, type);
            etAl = getEtAl(request, type);
            orderfield = sortBy != -1 ? "bi_sort_" + sortBy + "_sort" : null;
            ascending = SortOption.ASCENDING.equalsIgnoreCase(order);
            docs = search(context, type, cris, start, rpp, orderfield,
                    ascending);
            if (docs != null)
            {
                docsNumFound = docs.getTotalSearchResults();
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
                pageCurrent, pageLast, pageFirst, sortOption);

        componentInfoMap.put(getShortName(), componentInfo);
        request.setAttribute("componentinfomap", componentInfoMap);

        if (AuthorizeManager.isAdmin(context))
        {
            // Set a variable to create admin buttons
            request.setAttribute("admin_button", new Boolean(true));
        }
    }

    protected abstract List<String[]> addActiveTypeInRequest(
            HttpServletRequest request) throws Exception;

    private ComponentInfoDTO<T> buildComponentInfo(DiscoverResult docs,
            Context context, String type, int start, String order, int rpp,
            int etAl, long docsNumFound, int pageTotal, int pageCurrent,
            int pageLast, int pageFirst, SortOption sortOption)
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

        componentInfo.setOrder(order);
        componentInfo.setSo(sortOption);
        componentInfo.setStart(start);
        componentInfo.setRpp(rpp);
        componentInfo.setEtAl(etAl);
        componentInfo.setTotal(docsNumFound);
        componentInfo.setType(type);
        return componentInfo;
    }

    protected abstract T[] getObjectFromSolrResult(DiscoverResult docs,
            Context context) throws Exception;

    public DiscoverResult search(Context context, String type,
            ACrisObject cris, int start, int rpp, String orderfield,
            boolean ascending) throws SearchServiceException
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

        DiscoverQuery solrQuery = new DiscoverQuery();
        try
        {
            solrQuery.addFilterQueries("NOT(withdrawn:true)",
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
        solrQuery.setQuery(query);
        solrQuery.addSearchField("search.resourceid");
        solrQuery.addSearchField("search.resourcetype");
        solrQuery.setStart(start);
        solrQuery.setMaxResults(rpp);
        if (orderfield == null)
        {
            orderfield = "score";
        }
        solrQuery.setSortField(orderfield, ascending ? SORT_ORDER.asc
                : SORT_ORDER.desc);

        if (filters != null)
        {
            for (String filter : filters)
            {
                solrQuery.addFilterQueries(MessageFormat.format(filter,
                        authority, uuid));
            }
        }

        return getSearchService().search(context, solrQuery);

    }

    private String getType(HttpServletRequest request)
    {
        String type = request.getParameter("open");
        if (type == null)
        {
            type = types.keySet().iterator().next();
        }
        return type;
    }

    private int getEtAl(HttpServletRequest request, String type)
    {
        int etAl = UIUtil.getIntParameter(request, "etAl" + type);
        if (etAl == -1)
        {
            etAl = types.get(type).getEtal();
        }
        return etAl;
    }

    private int getRPP(HttpServletRequest request, String type)
    {
        int rpp = UIUtil.getIntParameter(request, "rpp" + type);
        if (rpp == -1)
        {
            rpp = getTypes().get(type).getRpp();
        }
        return rpp;
    }

    private String getOrder(HttpServletRequest request, String type)
    {
        String order = request.getParameter("order" + type);
        if (order == null)
        {
            order = getTypes().get(type).getOrder();
        }
        return order;
    }

    private int getSortBy(HttpServletRequest request, String type)
    {
        int sortBy = UIUtil.getIntParameter(request, "sort_by" + type);
        if (sortBy == -1)
        {
            sortBy = getTypes().get(type).getSortby();
        }
        return sortBy;
    }

    private List<String> getFilters(String type)
    {
        return getTypes().get(type).getFilters();
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
}
