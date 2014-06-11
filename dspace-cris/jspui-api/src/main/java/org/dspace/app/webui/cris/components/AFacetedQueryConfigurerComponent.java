/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;

public abstract class AFacetedQueryConfigurerComponent<T extends DSpaceObject>
        extends ASolrConfigurerComponent<T, ICrisBeanComponent> implements
        IFacetedQueryConfigurerComponent
{
    
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(AFacetedQueryConfigurerComponent.class);

        
    @Override
    protected List<String[]> addActiveTypeInRequest(HttpServletRequest request)
            throws Exception
    {
        ACrisObject cris = getCrisObject(request);
        Context c = UIUtil.obtainContext(request);
        List<String[]> subLinks = new ArrayList<String[]>();

        DiscoverResult docs = search(c, cris, 0, 0, null, true);
        if (docs.getTotalSearchResults() > 0)
        {
            for (String type : docs.getFacetResults().keySet())
            {
                Long size = docs.getFacetResults().get(type).get(0).getCount();
                String message = "";
                try
                {
                    message = I18nUtil.getMessage(
                            "jsp.layout.dspace.detail.fieldset-legend.component."
                            + type, c);
                }
                catch (MissingResourceException e)
                {
                    log.warn("Missing Resource: jsp.layout.dspace.detail.fieldset-legend.component." + type);
                    message = I18nUtil.getMessage(
                            "jsp.layout.dspace.detail.fieldset-legend.component.default"
                            , c);
                }
                
                subLinks.add(new String[] {
                        type,
                        MessageFormat.format(message, size), "" + size });
            }
        }
        request.setAttribute("activeTypes"
                + getRelationConfiguration().getRelationName(), subLinks);
        return subLinks;
    }

    protected DiscoverResult search(Context context, ACrisObject cris, int start,
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

        DiscoverQuery solrQuery = new DiscoverQuery();
        solrQuery.setFacetMinCount(1);
        try
        {
            solrQuery.addFilterQueries(getTypeFilterQuery());
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

        for (ICrisBeanComponent facetComponent : getTypes().values())
        {
            String format = MessageFormat.format(
                    facetComponent.getFacetQuery(), authority, uuid);
            solrQuery.addFacetQuery(format);
            solrQuery.getNamedFacetQueries().put(format, facetComponent.getComponentIdentifier());
        }

        if(getCommonFilter()!=null) {            
            String format = MessageFormat.format(getCommonFilter(), getRelationConfiguration().getRelationName(),
                    cris.getUuid(), authority);
            solrQuery.addFilterQueries(format);
        }    
        
        return getSearchService().search(context, solrQuery);
    }
    
    
    @Override
    public List<String[]> sublinks(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        List<String[]> subLinks = (List<String[]>) request
                .getAttribute("activeTypes"+getRelationConfiguration().getRelationName());
        if (subLinks == null)
        {
            return addActiveTypeInRequest(request);
        }
        return subLinks;
    }

    public long count(String type, Integer id)
    {
        Context context = null;

        try
        {
            context = new Context();
            ACrisObject cris = getApplicationService().get(getTarget(), id);
            List<FacetResult> facetresults = search(context, cris, 0, 0, null, true).getFacetResult(type);
            if(facetresults.isEmpty()) {
                return 0;    
            }
            return facetresults.get(0).getCount();
        }

        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        return -1;
    }


}
