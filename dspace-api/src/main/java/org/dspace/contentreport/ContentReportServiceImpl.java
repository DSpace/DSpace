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

import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.contentreport.service.ContentReportService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class ContentReportServiceImpl implements ContentReportService {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ContentReportServiceImpl.class);

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ItemService itemService;

    /**
     * Retrieves item statistics per collection according to a set of Boolean filters.
     * @param context DSpace context
     * @param filters Set of filters
     * @return a list of collections with the requested statistics for each of them
     */
    @Override
    public List<FilteredCollection> getFilteredCollections(Context context, Set<Filter> filters) {
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

}
