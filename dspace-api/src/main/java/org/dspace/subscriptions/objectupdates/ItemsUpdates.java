/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions.objectupdates;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.configuration.DiscoveryRelatedItemConfiguration;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFunctionConfiguration;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.FrequencyType;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class which will be used to find all item objects updated related with a
 * subscribed Item. A subscribed Item is treated as an entity (Person, OrgUnit,
 * Project, ...) and the related items are looked up through its inverse-relation
 * discovery configurations ({@code RELATION.<entityType>.*}).
 *
 * @author Alba Aliu
 */
public class ItemsUpdates implements DSpaceObjectUpdates {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Autowired
    private SearchService searchService;

    private static final Logger log = LogManager.getLogger(ItemsUpdates.class);

    @Override
    @SuppressWarnings("rawtypes")
    public List<IndexableObject> findUpdates(Context context, DSpaceObject dSpaceObject, String frequency) {
        List<IndexableObject> list = new ArrayList<>();
        // entity type of the subscribed item, used to resolve its inverse-relation configurations
        String inverseRelationName = "RELATION." + itemService.getEntityTypeLabel((Item) dSpaceObject);
        List<DiscoveryConfiguration> discoveryConfigurationList =
                searchConfigurationService.getDiscoveryConfigurationWithPrefixName(inverseRelationName);
        IndexableObject indexableObject = resolveScope(context, dSpaceObject.getID().toString());
        try {
            for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurationList) {
                DiscoverQuery discoverQuery = buildDiscoveryQuery(discoveryConfiguration, indexableObject);
                discoverQuery.addFilterQueries("lastModified_dt:" + FrequencyType.findLastFrequency(frequency));
                DiscoverResult searchResult = searchService.search(context, discoverQuery);
                list.addAll(searchResult.getIndexableObjects());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return list;
    }

    private IndexableObject<?, ?> resolveScope(Context context, String scope) {
        IndexableObject<?, ?> scopeObj = null;
        if (StringUtils.isBlank(scope)) {
            return scopeObj;
        }
        try {
            UUID uuid = UUID.fromString(scope);
            scopeObj = new IndexableCommunity(communityService.find(context, uuid));
            if (scopeObj.getIndexedObject() == null) {
                scopeObj = new IndexableCollection(collectionService.find(context, uuid));
            }
            if (scopeObj.getIndexedObject() == null) {
                scopeObj = new IndexableItem(itemService.find(context, uuid));
            }
        } catch (IllegalArgumentException ex) {
            log.error("The given scope string " + trimToEmpty(scope) + " is not a UUID");
        } catch (SQLException ex) {
            log.error("Unable to retrieve DSpace Object with ID " + trimToEmpty(scope) + " from the database");
        }
        return scopeObj;
    }

    private DiscoverQuery buildDiscoveryQuery(DiscoveryConfiguration discoveryConfiguration,
                                              IndexableObject<?, ?> scope) {
        DiscoverQuery discoverQuery = buildBaseQuery(discoveryConfiguration, scope);
        discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
        configureSorting(discoverQuery, discoveryConfiguration.getSearchSortConfiguration(), scope);
        return discoverQuery;
    }

    @SuppressWarnings("rawtypes")
    private void configureSorting(DiscoverQuery queryArgs,
                                  DiscoverySortConfiguration searchSortConfiguration,
                                  IndexableObject scope) {
        String sortBy = getDefaultSortField(searchSortConfiguration);
        String sortOrder = getDefaultSortDirection(searchSortConfiguration);
        DiscoverySortFieldConfiguration sortFieldConfiguration =
                searchSortConfiguration.getSortFieldConfiguration(sortBy);

        if (sortFieldConfiguration != null) {
            String sortField;
            if (DiscoverySortFunctionConfiguration.SORT_FUNCTION.equals(sortFieldConfiguration.getType())) {
                sortField = MessageFormat.format(
                        ((DiscoverySortFunctionConfiguration) sortFieldConfiguration).getFunction(scope.getID()),
                        scope.getID());
            } else {
                sortField = searchService.toSortFieldIndex(
                        sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());
            }

            if ("asc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.asc);
            } else if ("desc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.desc);
            } else {
                log.error(sortOrder + " is not a valid sort order");
            }
        } else {
            log.error(sortBy + " is not a valid sort field");
        }
    }

    private String getDefaultSortDirection(DiscoverySortConfiguration searchSortConfiguration) {
        return searchSortConfiguration.getSortFields().iterator().next().getDefaultSortOrder().toString();
    }

    private String getDefaultSortField(DiscoverySortConfiguration searchSortConfiguration) {
        String sortBy = "score";
        if (Objects.nonNull(searchSortConfiguration.getSortFields())
                && !searchSortConfiguration.getSortFields().isEmpty()) {
            DiscoverySortFieldConfiguration defaultSort = searchSortConfiguration.getSortFields().get(0);
            if (StringUtils.isBlank(defaultSort.getMetadataField())) {
                return sortBy;
            }
            sortBy = defaultSort.getMetadataField();
        }
        return sortBy;
    }

    private DiscoverQuery buildBaseQuery(DiscoveryConfiguration discoveryConfiguration, IndexableObject<?, ?> scope) {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        if (discoveryConfiguration == null) {
            return discoverQuery;
        }
        discoverQuery.setDiscoveryConfigurationName(discoveryConfiguration.getId());
        List<String> filterQueries = discoveryConfiguration.getDefaultFilterQueries();
        for (String filterQuery : filterQueries) {
            if (discoveryConfiguration instanceof DiscoveryRelatedItemConfiguration) {
                discoverQuery.addFilterQueries(MessageFormat.format(filterQuery, scope.getID()));
            } else {
                discoverQuery.addFilterQueries(filterQuery);
            }
        }
        return discoverQuery;
    }

}
