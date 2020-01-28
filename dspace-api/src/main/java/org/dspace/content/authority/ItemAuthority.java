/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.factory.ItemAuthorityServiceFactory;
import org.dspace.content.authority.service.ItemAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;

/**
 * Sample authority to link a dspace item with another (i.e a publication with
 * the corresponding dataset or viceversa)
 *
 * @author Andrea Bollini
 * @author Giusdeppe Digilio
 * @version $Revision $
 */
public class ItemAuthority implements ChoiceAuthority {
    private static final Logger log = Logger.getLogger(ItemAuthority.class);

    private DSpace dspace = new DSpace();

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private SearchService searchService = dspace.getServiceManager().getServiceByName(
        "org.dspace.discovery.SearchService", SearchService.class);

    private ItemAuthorityServiceFactory itemAuthorityServiceFactory = dspace.getServiceManager().getServiceByName(
            "itemAuthorityServiceFactory", ItemAuthorityServiceFactory.class);

    // punt!  this is a poor implementation..
    @Override
    public Choices getBestMatch(String field, String text, Collection collection, String locale) {
        return getMatches(field, text, collection, 0, 2, locale);
    }

    /**
     * Match a proposed value against existend DSpace item applying an optional
     * filter query to limit the scope only to specific item types
     */
    @Override
    public Choices getMatches(String field, String text, Collection collection, int start, int limit, String locale) {
        Context context = null;
        if (limit <= 0) {
            limit = 20;
        }

        ItemAuthorityService itemAuthorityService = itemAuthorityServiceFactory.getInstance(field);
        String luceneQuery = itemAuthorityService.getSolrQuery(text);

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(Item.class.getSimpleName());

        String relationshipType = ConfigurationManager.getProperty("cris", "ItemAuthority."
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
                choiceList.add(new Choice(dso.getID().toString(), dso.getName(), dso.getName()));
            }

            Choice[] results = new Choice[choiceList.size()];
            results = choiceList.toArray(results);
            return new Choices(results, 0, results.length, Choices.CF_AMBIGUOUS,
                               resultSearch.getTotalSearchResults() > (start + limit), 0);

        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
            return new Choices(Choices.CF_UNSET);
        }
    }

    @Override
    public String getLabel(String field, String key, String locale) {
        String title = key;
        if (key != null) {
            Context context = null;
            try {
                context = new Context();
                DSpaceObject dso = itemService.find(context, UUIDUtils.fromString(key));
                if (dso != null) {
                    title = dso.getName();
                }
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
                return key;
            }
        }
        return title;
    }
}
