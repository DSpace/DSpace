/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions.dSpaceObjectsUpdates;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
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
import org.dspace.subscriptions.ContentGenerator;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;


/**
 * Class which will be used to find
 * all item objects updated related with subscribed DSO
 *
 * @author Alba Aliu
 */
public class ItemsUpdates implements DSpaceObjectUpdates {
    private final CollectionService collectionService;
    private final CommunityService communityService;
    private final ItemService itemService;
    private DiscoveryConfigurationService searchConfigurationService;
    private SearchService searchService;
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(ContentGenerator.class);


    @Override
    public List<IndexableObject> findUpdates(Context context, DSpaceObject dSpaceObject, String frequency) {
        List<IndexableObject> list = new ArrayList<>();
        // entity type found
        String inverseRelationName = "RELATION." + itemService.getEntityType((Item) dSpaceObject);
        List<DiscoveryConfiguration> discoveryConfigurationList =
                searchConfigurationService.getDiscoveryConfigurationWithPrefixName(inverseRelationName);
        DiscoverQuery discoverQuery = null;
        DiscoverResult searchResult = null;
        IndexableObject indexableObject = resolveScope(context, dSpaceObject.getID().toString());
        try {
            for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurationList) {
                discoverQuery = buildDiscoveryQuery(discoveryConfiguration, indexableObject);
                discoverQuery.addFilterQueries("lastModified_dt:" + this.findLastFrequency(frequency));
                searchResult = searchService.search(context, discoverQuery);
                list.addAll(searchResult.getIndexableObjects());
            }
        } catch (Exception e) {
            log.error(e);
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
            String message = "The given scope string " + trimToEmpty(scope) + " is not a UUID";
            log.error(message);
        } catch (SQLException ex) {
            String message = "Unable to retrieve DSpace Object with ID " + trimToEmpty(scope) + " from the database";
            log.error(message);
        }

        return scopeObj;
    }

    private DiscoverQuery buildDiscoveryQuery(DiscoveryConfiguration discoveryConfiguration,
                                              IndexableObject<?, ?> scope) throws SQLException {

        DiscoverQuery discoverQuery = buildBaseQuery(discoveryConfiguration, scope);
        discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
        configureSorting(discoverQuery, discoveryConfiguration.getSearchSortConfiguration(), scope);
        return discoverQuery;
    }


    private void configureSorting(DiscoverQuery queryArgs,
                                  DiscoverySortConfiguration searchSortConfiguration,
                                  final IndexableObject scope) {
        String sortBy = getDefaultSortField(searchSortConfiguration);
        String sortOrder = getDefaultSortDirection(searchSortConfiguration);
        //Update Discovery query
        DiscoverySortFieldConfiguration sortFieldConfiguration = searchSortConfiguration
                .getSortFieldConfiguration(sortBy);

        if (sortFieldConfiguration != null) {
            String sortField;

            if (DiscoverySortFunctionConfiguration.SORT_FUNCTION.equals(sortFieldConfiguration.getType())) {
                sortField = MessageFormat.format(
                        ((DiscoverySortFunctionConfiguration) sortFieldConfiguration).getFunction(scope.getID()),
                        scope.getID());
            } else {
                sortField = searchService
                        .toSortFieldIndex(
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
        String sortBy;// Attempt to find the default one, if none found we use SCORE
        sortBy = "score";
        if (Objects.nonNull(searchSortConfiguration.getSortFields()) &&
                !searchSortConfiguration.getSortFields().isEmpty()) {
            DiscoverySortFieldConfiguration defaultSort = searchSortConfiguration.getSortFields().get(0);
            if (org.apache.commons.lang.StringUtils.isBlank(defaultSort.getMetadataField())) {
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

    public ItemsUpdates(CollectionService collectionService, CommunityService communityService, ItemService itemService,
                        DiscoveryConfigurationService searchConfigurationService, SearchService searchService) {
        this.collectionService = collectionService;
        this.communityService = communityService;
        this.itemService = itemService;
        this.searchConfigurationService = searchConfigurationService;
        this.searchService = searchService;
    }
}
