/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class DiscoveryConfigurationUtilsService {

    private static Logger log = LogManager.getLogger(DiscoveryConfigurationUtilsService.class);

    @Autowired
    private ItemService itemService;
    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    /**
     * Counts the items related to the given item through the named relation without
     * resolving the related items themselves.
     * <p>
     * This runs the relation discovery query but requests zero rows, so no items are
     * fetched from the database, and reads the total match count from the search response.
     * It is intended for callers that only need to know whether (or how many) related
     * items exist.
     * </p>
     *
     * @param context the DSpace context
     * @param item the item whose related items are counted
     * @param relationName the name of the relation
     * @return the number of related items, {@code 0} if none is found
     */
    public long countByRelation(Context context, Item item, String relationName) {
        DiscoverQuery discoverQuery = buildRelationQuery(item, relationName);
        if (discoverQuery == null) {
            return 0;
        }

        // We only need the total match count, so avoid resolving any item from the database.
        discoverQuery.setMaxResults(0);

        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery, false).getTotalSearchResults();
    }

    private DiscoverQuery buildRelationQuery(Item item, String relationName) {
        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        if (entityType == null) {
            log.warn("The item with id {} has no dspace.entity.type. No related items is found.", item.getID());
            return null;
        }

        DiscoveryConfiguration discoveryConfiguration = findDiscoveryConfiguration(entityType, relationName);
        if (discoveryConfiguration == null) {
            log.warn(
                "No discovery configuration found for relation {} for item with id {} and type {}. " +
                    "No related items is found.",
                relationName, item.getID(), entityType);
            return null;
        }

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setDiscoveryConfigurationName(discoveryConfiguration.getId());
        discoverQuery.setScopeObject(new IndexableItem(item));

        if (discoveryConfiguration.getSearchSortConfiguration() == null ||
            discoveryConfiguration.getSearchSortConfiguration().getDefaultSortField() == null) {
            // No sorting configured - add default chronological sort for consistency
            discoverQuery.setSortField("dc.date.issued_dt", DiscoverQuery.SORT_ORDER.asc);
        } else {
            DiscoverySortFieldConfiguration sortField =
                discoveryConfiguration.getSearchSortConfiguration().getDefaultSortField();
            discoverQuery.setSortField(sortField.getMetadataField(),
                                       DiscoverQuery.SORT_ORDER.valueOf(sortField.getDefaultSortOrder().name()));
        }

        List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
        for (String defaultFilterQuery : defaultFilterQueries) {
            discoverQuery.addFilterQueries(MessageFormat.format(defaultFilterQuery, item.getID()));
        }

        return discoverQuery;
    }

    private DiscoveryConfiguration findDiscoveryConfiguration(String entityType, String relationName) {
        String configurationName = "RELATION." + entityType + "." + relationName;
        return searchConfigurationService.getDiscoveryConfigurationByName(configurationName);
    }

}