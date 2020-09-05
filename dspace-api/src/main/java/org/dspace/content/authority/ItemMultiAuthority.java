/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.factory.ItemAuthorityServiceFactory;
import org.dspace.content.authority.service.ItemAuthorityService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.ItemAuthorityUtils;
import org.dspace.utils.DSpace;
/**
 * Authority to aggregate "extra" value to single choice
 * 
 * @author Mykhaylo Boychuk (4Science.it)
 */
public class ItemMultiAuthority implements ChoiceAuthority {
    private static final Logger log = Logger.getLogger(ItemAuthority.class);

    /** the name assigned to the specific instance by the PluginService, @see {@link NameAwarePlugin} **/
    private String authorityName;

    /**
     * the metadata managed by the plugin instance, derived from its authority name
     * in the form schema_element_qualifier
     */
    private String field;

    private DSpace dspace = new DSpace();

    private SearchService searchService = dspace.getServiceManager().getServiceByName(
        "org.dspace.discovery.SearchService", SearchService.class);

    private ItemAuthorityServiceFactory itemAuthorityServiceFactory = dspace.getServiceManager().getServiceByName(
            "itemAuthorityServiceFactory", ItemAuthorityServiceFactory.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    // punt!  this is a poor implementation..
    @Override
    public Choices getBestMatch(String text, String locale) {
        return getMatches(text, 0, 2, locale);
    }

    /**
     * Match a proposed value against existend DSpace item applying an optional
     * filter query to limit the scope only to specific item types
     */
    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Context context = null;
        if (limit <= 0) {
            limit = 20;
        }

        ItemAuthorityService itemAuthorityService = itemAuthorityServiceFactory.getInstance(field);
        String luceneQuery = itemAuthorityService.getSolrQuery(text);

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(Item.class.getSimpleName());

        String relationshipType = configurationService.getProperty("cris.ItemAuthority."
                + field + ".relationshipType");
        if (StringUtils.isNotBlank(relationshipType)) {
            String filter = "relationship.type:" + relationshipType;
            discoverQuery.addFilterQueries(filter);
        }

        discoverQuery
            .setQuery(luceneQuery);
        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);

        DiscoverResult resultSearch;
        try {
            context = new Context();
            resultSearch = searchService.search(context, discoverQuery);
            List<Choice> choiceList = new ArrayList<Choice>();

            // Process results of query
            Iterator<IndexableObject> dsoIterator = resultSearch.getIndexableObjects().iterator();
            while (dsoIterator.hasNext()) {
                DSpaceObject dso = (DSpaceObject) dsoIterator.next().getIndexedObject();
                Item item = (Item) dso;
                choiceList.addAll(ItemAuthorityUtils.buildAggregateByExtra(getPluginInstanceName(), item));
            }
            Choice[] results = new Choice[choiceList.size()];
            results = choiceList.toArray(results);
            return new Choices(results, start, (int) resultSearch.getTotalSearchResults(), Choices.CF_AMBIGUOUS,
                               resultSearch.getTotalSearchResults() > (start + limit), 0);

        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
            return new Choices(Choices.CF_UNSET);
        }
    }

    @Override
    public String getLabel(String key, String locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPluginInstanceName(String name) {
        authorityName = name;
        for (Entry conf : configurationService.getProperties().entrySet()) {
            if (StringUtils.startsWith((String) conf.getKey(), ChoiceAuthorityServiceImpl.CHOICES_PLUGIN_PREFIX)
                    && StringUtils.equals((String) conf.getValue(), authorityName)) {
                field = ((String) conf.getKey()).substring(ChoiceAuthorityServiceImpl.CHOICES_PLUGIN_PREFIX.length())
                        .replace(".", "_");
                // exit the look immediately as we have found it
                break;
            }
        }
    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }
}
