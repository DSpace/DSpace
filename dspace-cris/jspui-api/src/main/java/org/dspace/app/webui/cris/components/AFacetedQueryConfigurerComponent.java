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
import java.util.List;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.webui.discovery.DiscoverUtility;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.services.RequestService;
import org.dspace.utils.DSpace;

public abstract class AFacetedQueryConfigurerComponent<T extends DSpaceObject>
        extends ASolrConfigurerComponent<T, ICrisBeanComponent> implements
        IFacetedQueryConfigurerComponent
{
    
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(AFacetedQueryConfigurerComponent.class);

    private List<DiscoverySearchFilterFacet> facets;
    
    @Override
    protected List<String[]> addActiveTypeInRequest(HttpServletRequest request, String currentType)
            throws Exception
    {
        ACrisObject cris = getCrisObject(request);
        Context c = UIUtil.obtainContext(request);
        List<String[]> subLinks = new ArrayList<String[]>();

        DiscoverResult docs = search(c, request, currentType, cris, 0, 0, null, true);
        if (docs.getTotalSearchResults() > 0)
        {
            for (String type : docs.getFacetQueryResults().keySet())
            {
                FacetResult facetResult = docs.getFacetQueryResults().get(type).get(0);
                Long size = facetResult
                        .getCount();
                String message = "";
                try
                {
                    message = I18nUtil.getMessage(
                            "jsp.layout.dspace.detail.fieldset-legend.component."
                                    + type,
                            c);
                }
                catch (MissingResourceException e)
                {
                    log.warn(
                            "Missing Resource: jsp.layout.dspace.detail.fieldset-legend.component."
                                    + type);
                    message = I18nUtil.getMessage(
                            "jsp.layout.dspace.detail.fieldset-legend.component.default",
                            c);
                }

                subLinks.add(new String[] { type,
                        MessageFormat.format(message, size), "" + size });
            }
        }

        request.setAttribute(
                "activeTypes" + getRelationConfiguration().getRelationName(),
                subLinks);

        return subLinks;
    }

    protected DiscoverResult search(Context context, HttpServletRequest request, String type, ACrisObject cris, int start,
            int rpp, String orderfield, boolean ascending)
            throws SearchServiceException
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

        DiscoverQuery discoveryQuery = new DiscoverQuery();
        discoveryQuery.setFacetMinCount(1);
        try
        {
            discoveryQuery.addFilterQueries(getTypeFilterQuery());
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
        discoveryQuery.setStart(start);
        discoveryQuery.setMaxResults(rpp);
        if (orderfield == null)
        {
            orderfield = "score";
        }
        discoveryQuery.setSortField(orderfield, ascending ? SORT_ORDER.asc
                : SORT_ORDER.desc);

        for (ICrisBeanComponent facetComponent : getTypes().values())
        {
            String format = MessageFormat.format(
                    facetComponent.getFacetQuery(), authority, uuid);
            discoveryQuery.addFacetQuery("{!ex=dt key="+facetComponent.getComponentIdentifier()+"}"+format);
            discoveryQuery.getNamedFacetQueries().put(facetComponent.getComponentIdentifier(), format);
        }

        for (DiscoverySearchFilterFacet facet : getFacets()) {

            discoveryQuery.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(),
                    facet.getType(), facet.getFacetLimit(), facet.getSortOrder(), false));
        }
        
        
        List<String> userFilters = new ArrayList<String>();
        if (request != null)
        {
            List<String[]> filters = DiscoverUtility.getFilters(request, getRelationConfiguration().getRelationName());
            for (String[] f : filters)
            {
                try
                {
                    String newFilterQuery = SearchUtils.getSearchService()
                            .toFilterQuery(context, f[0], f[1], f[2])
                            .getFilterQuery();
                    if (StringUtils.isNotBlank(newFilterQuery))
                    {
                        discoveryQuery.addFilterQueries("{!tag=dt}"+newFilterQuery);
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
        
        if(getCommonFilter()!=null) {            
            String format = MessageFormat.format(getCommonFilter(), getRelationConfiguration().getRelationName(),
                    cris.getUuid(), authority);
            discoveryQuery.addFilterQueries(format);
        }    
        
        List<String> filters = getFilters(type);

        if (filters != null)
        {
            for (String filter : filters)
            {
                discoveryQuery.addFilterQueries("{!tag=dt}"+MessageFormat.format(filter,
                        authority, uuid));
            }
        }
        return getSearchService().search(context, discoveryQuery);
    }
    
    
    @Override
    public List<String[]> sublinks(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        List<String[]> subLinks = (List<String[]>) request
                .getAttribute("activeTypes"+getRelationConfiguration().getRelationName());
        if (subLinks == null)
        {
            ACrisObject t = getCrisObject(request);
            if(t!=null) {
                return addActiveTypeInRequest(request, getType(request, t.getId()));    
            }
            else {
                return addActiveTypeInRequest(request, getType(request, null));
            }
            
        }
        return subLinks;
    }

    public long count(HttpServletRequest request, String type, Integer id)
    {
        Context context = null;

        try
        {
            if(request == null) {
                RequestService requestService = new DSpace().getServiceManager().getServiceByName(RequestService.class.getName(), RequestService.class);
                if(requestService != null && requestService.getCurrentRequest() != null){
                	request = requestService.getCurrentRequest().getHttpServletRequest();
                }else{
                	return -1;
                }
                
            }
            context = UIUtil.obtainContext(request);
            ACrisObject cris = getApplicationService().get(getTarget(), id);
            List<FacetResult> facetresults = search(context, request, type, cris, 0, 0, null, true).getFacetQueryResult(type);
            if(facetresults.isEmpty()) {
                return 0;    
            }
            return facetresults.get(0).getCount();
        }
        catch (Exception ex)
        {
            if(log.isDebugEnabled()) {
                log.error(ex.getMessage(), ex);
            }
        }
        return -1;
    }

    public List<DiscoverySearchFilterFacet> getFacets()
    {
        if(this.facets == null) {
            this.facets = new ArrayList<DiscoverySearchFilterFacet>();
        }
        return this.facets;
    }

    public void setFacets(List<DiscoverySearchFilterFacet> facets)
    {
        this.facets = facets;
    }


}
