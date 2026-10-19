/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreport;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.contentreport.service.ContentReportService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class ContentReportServiceImpl implements ContentReportService {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ContentReportServiceImpl.class);

    @Autowired
    protected ConfigurationService configurationService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private MetadataFieldService metadataFieldService;

    /**
     * Returns <code>true<</code> if Content Reports are enabled.
     * @return <code>true<</code> if Content Reports are enabled
     */
    @Override
    public boolean getEnabled() {
        return configurationService.getBooleanProperty("contentreport.enable");
    }

    /**
     * Retrieves item statistics per collection according to a set of Boolean filters.
     * @param context DSpace context
     * @param filters Set of filters
     * @return a list of collections with the requested statistics for each of them
     */
    @Override
    public List<FilteredCollection> findFilteredCollections(Context context, java.util.Collection<Filter> filters) {
        List<FilteredCollection> colls = new ArrayList<>();
        try {
            List<Collection> collections = collectionService.findAll(context);
            for (Collection collection : collections) {
                FilteredCollection coll = new FilteredCollection();
                coll.setHandle(collection.getHandle());
                coll.setLabel(collection.getName());
                Community community = collection.getCommunities().stream()
                        .findFirst()
                        .orElse(null);
                if (community != null) {
                    coll.setCommunityLabel(community.getName());
                    coll.setCommunityHandle(community.getHandle());
                }
                colls.add(coll);

                Iterator<Item> items = itemService.findAllByCollection(context, collection);
                int nbTotalItems = 0;
                while (items.hasNext()) {
                    Item item = items.next();
                    nbTotalItems++;
                    boolean matchesAllFilters = true;
                    for (Filter filter : filters) {
                        if (filter.testItem(context, item)) {
                            coll.addValue(filter, 1);
                        } else {
                            // This ensures the requested filter is present in the collection record
                            // even when there are no matching items.
                            coll.addValue(filter, 0);
                            matchesAllFilters = false;
                        }
                    }
                    if (matchesAllFilters) {
                        coll.addAllFiltersValue(1);
                    }
                }
                coll.setTotalItems(nbTotalItems);
                coll.seal();
            }
        } catch (SQLException e) {
            log.error("SQLException trying to receive filtered collections statistics", e);
        }
        return colls;
    }

    /**
     * Retrieves a list of items according to a set of criteria.
     * @param context DSpace context
     * @param query structured query to find items against
     * @return a list of items filtered according to the provided query
     */
    @Override
    public FilteredItems findFilteredItems(Context context, FilteredItemsQuery query) {
        FilteredItems report = new FilteredItems();

        List<QueryPredicate> predicates = query.getQueryPredicates();
        List<UUID> collectionUuids = getUuidsFromStrings(query.getCollections());
        Set<Filter> filters = query.getFilters();

        try {
            List<Item> items = itemService.findByMetadataQuery(context, predicates, collectionUuids,
                    query.getOffset(), query.getPageLimit());
            items.stream()
                    .filter(item -> filters.stream().allMatch(f -> f.testItem(context, item)))
                    .forEach(report::addItem);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        try {
            long count = itemService.countForMetadataQuery(context, predicates, collectionUuids);
            report.setItemCount(count);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return report;
    }

    /**
     * Converts a metadata field name to a list of {@link MetadataField} instances
     * (one if no wildcards are used, possibly more otherwise).
     * @param context DSpace context
     * @param metadataField field to search for
     * @return a corresponding list of {@link MetadataField} entries
     */
    @Override
    public List<MetadataField> getMetadataFields(org.dspace.core.Context context, String metadataField)
            throws SQLException {
        List<MetadataField> fields = new ArrayList<>();
        if ("*".equals(metadataField)) {
            return fields;
        }
        String schema = "";
        String element = "";
        String qualifier = null;
        String[] parts = metadataField.split("\\.");
        if (parts.length > 0) {
            schema = parts[0];
        }
        if (parts.length > 1) {
            element = parts[1];
        }
        if (parts.length > 2) {
            qualifier = parts[2];
        }

        if (Item.ANY.equals(qualifier)) {
            fields.addAll(metadataFieldService.findFieldsByElementNameUnqualified(context, schema, element));
        } else {
            MetadataField mf = metadataFieldService.findByElement(context, schema, element, qualifier);
            if (mf != null) {
                fields.add(mf);
            }
        }
        return fields;
    }

    private static List<UUID> getUuidsFromStrings(List<String> collSel) {
        List<UUID> uuids = new ArrayList<>();
        for (String s: collSel) {
            try {
                uuids.add(UUID.fromString(s));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid collection UUID: " + s);
            }
        }
        return uuids;
    }

}
