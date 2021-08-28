/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions.dSpaceObjectsUpdates;


import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DiscoverQueryBuilder;
import org.dspace.content.DSpaceObject;
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
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.subscriptions.ContentGenerator;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Class which will be used to find
 * all item objects updated related with subscribed DSO
 *
 * @author Alba Aliu
 */
public class ItemsUpdates implements DSpaceObjectUpdates {
    @Autowired
    CollectionService collectionService;

    @Autowired
    CommunityService communityService;
    @Autowired
    ItemService itemService;
    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;
    @Autowired
    private DiscoverQueryBuilder discoverQueryBuilder;
    @Autowired
    private SearchService searchService;
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(ContentGenerator.class);

    @SuppressWarnings("checkstyle:TodoComment")
    @Override
    public List<IndexableObject> findUpdates(Context context, DSpaceObject dSpaceObject, String frequency) {
        List<IndexableObject> list = new ArrayList<>();
        // entity type found
        String inverseRelationName = "RELATION" + dSpaceObject.getName();
        List<DiscoveryConfiguration> discoveryConfigurationList = searchConfigurationService.getDiscoveryConfigurationWithPrefixName(inverseRelationName);
        DiscoverQuery discoverQuery = null;
        DiscoverResult searchResult = null;
        IndexableObject indexableObject = resolveScope(context, dSpaceObject.getID().toString());
        try {
            for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurationList) {
                discoverQuery = discoverQueryBuilder.buildQuery(context, indexableObject, discoveryConfiguration, null, null, "ITEM");
                searchResult = searchService.search(context, discoverQuery);
                list.addAll(searchResult.getIndexableObjects());
            }
        } catch (Exception e) {
            log.error(e);
        }
        return list;
    }


    public IndexableObject resolveScope(Context context, String scope) {
        IndexableObject scopeObj = null;
        if (StringUtils.isNotBlank(scope)) {
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
                log.warn("The given scope string " + StringUtils.trimToEmpty(scope) + " is not a UUID", ex);
            } catch (SQLException ex) {
                log.warn(
                        "Unable to retrieve DSpace Object with ID " + StringUtils.trimToEmpty(scope)
                                + " from the database",
                        ex);
            }
        }
        return scopeObj;
    }
}
