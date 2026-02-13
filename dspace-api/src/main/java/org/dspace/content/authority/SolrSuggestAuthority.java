/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.SORT.VALUE;
import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.TYPE_STANDARD;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.web.ContextUtil;

/**
 * Implementation of {@link ChoiceAuthority} based on Solr documents.
 * Provide suggestions based on existing values present in the Solr document by faced field.
 * The query is based on the prefix of values in solr.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class SolrSuggestAuthority extends SolrAuthority {

    private static final Logger log = LogManager.getLogger(SolrSuggestAuthority.class);

    private Context getContext() {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return context != null ? context : new Context();
    }

    @Override
    public Choices getBestMatch(String query, String locale) {
        Choices matches = getMatches(query, 0, 1, locale);
        if (matches.values.length != 0 && !matches.values[0].value.equalsIgnoreCase(query)) {
            matches = new Choices(false);
        }
        return matches;
    }

    @Override
    public Choices getMatches(String query, int start, int limit, String locale) {

        Context context = getContext();
        String facetName = configurationService.getProperty(SolrSuggestAuthority.class.getSimpleName()
                           + "." + this.field + ".facetname") + "_ac";
        String facetPrefix = query.toLowerCase();
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addFacetField(new DiscoverFacetField(facetName, TYPE_STANDARD, -1, VALUE, facetPrefix));

        List<Choice> proposals = new ArrayList<Choice>();
        try {
            DiscoverResult discoverResult = getSolrSearchService().search(context, discoverQuery);
            List<FacetResult> facets = discoverResult.getFacetResult(facetName);
            for (FacetResult facet : facets) {
                var value = facet.getDisplayedValue();
                proposals.add(new Choice(null, value, value));
            }
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }

        Choice[] propArray = new Choice[proposals.size()];
        propArray = proposals.toArray(propArray);
        return new Choices(propArray, 0, proposals.size(), CF_ACCEPTED, false);
    }

    private SearchService getSolrSearchService() {
        ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();
        return manager.getServiceByName(SearchService.class.getName(), SearchService.class);
    }

    @Override
    public String getLabel(String key, String locale) {
        return StringUtils.EMPTY;
    }

}
