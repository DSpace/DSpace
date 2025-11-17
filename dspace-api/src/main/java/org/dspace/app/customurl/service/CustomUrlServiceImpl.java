/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.customurl.service;

import static java.util.Optional.ofNullable;
import static org.dspace.content.Item.ANY;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link CustomUrlService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CustomUrlServiceImpl implements CustomUrlService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomUrlServiceImpl.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private SearchService searchService;

    @Override
    public Optional<String> getCustomUrl(Item item) {
        return ofNullable(itemService.getMetadataFirstValue(item, "dspace", "customurl", null, Item.ANY));
    }

    @Override
    public List<String> getOldCustomUrls(Item item) {
        return itemService.getMetadataByMetadataString(item, "dspace.customurl.old").stream()
                          .map(MetadataValue::getValue)
                          .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllCustomUrls(Item item) {
        return Stream.concat(getCustomUrl(item).stream(), getOldCustomUrls(item).stream())
                     .collect(Collectors.toList());
    }

    @Override
    public void replaceCustomUrl(Context context, Item item, String newUrl) {
        try {
            itemService.clearMetadata(context, item, "dspace", "customurl", null, ANY);
            itemService.addMetadata(context, item, "dspace", "customurl", null, null, newUrl);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void deleteAnyOldCustomUrlEqualsTo(Context context, Item item, String customUrl) {

        List<MetadataValue> redirectedUrls = getRedirectedUrlMetadataValuesWithValue(item, customUrl);
        if (CollectionUtils.isNotEmpty(redirectedUrls)) {
            deleteMetadataValues(context, item, redirectedUrls);
        }

    }

    @Override
    public void addOldCustomUrl(Context context, Item item, String url) {
        try {
            itemService.addMetadata(context, item, "dspace", "customurl", "old", null, url);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void deleteCustomUrl(Context context, Item item) {
        try {
            itemService.clearMetadata(context, item, "dspace", "customurl", null, ANY);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void deleteAllOldCustomUrls(Context context, Item item) {
        try {
            itemService.clearMetadata(context, item, "dspace", "customurl", "old", ANY);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public void deleteOldCustomUrlByIndex(Context context, Item item, int index) {

        List<MetadataValue> redirectedUrls = itemService.getMetadata(item, "dspace", "customurl", "old", ANY);

        if (index >= redirectedUrls.size()) {
            throw new IllegalArgumentException(
                "The provided index is not consistent with the cardinality of the old custom urls");
        }

        deleteMetadataValues(context, item, List.of(redirectedUrls.get(index)));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Optional<Item> findItemByCustomUrl(Context context, String customUrl) {
        if (StringUtils.isBlank(customUrl)) {
            return Optional.empty();
        }
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries("customurl:" + searchService.escapeQueryChars(customUrl));
        discoverQuery.setIncludeNotDiscoverableOrWithdrawn(true);

        List<IndexableObject> indexableObjects = findIndexableObjects(context, discoverQuery);

        if (CollectionUtils.isEmpty(indexableObjects)) {
            return Optional.empty();
        }

        if (indexableObjects.size() > 1) {
            LOGGER.error("Found many item with the same custom url {} - Ids: {}", customUrl, getIds(indexableObjects));
            throw new IllegalStateException("Found many item with the same custom url: " + customUrl);
        }

        return Optional.of(indexableObjects.get(0))
                       .map(indexableObject -> (Item) indexableObject.getIndexedObject());

    }

    @SuppressWarnings("rawtypes")
    private List<IndexableObject> findIndexableObjects(Context context, DiscoverQuery discoverQuery) {
        try {
            return searchService.search(context, discoverQuery).getIndexableObjects();
        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteMetadataValues(Context context, Item item, List<MetadataValue> metadataValues) {
        try {
            itemService.removeMetadataValues(context, item, metadataValues);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<MetadataValue> getRedirectedUrlMetadataValuesWithValue(Item item, String value) {
        return itemService.getMetadataByMetadataString(item, "dspace.customurl.old").stream()
                          .filter(metadataValue -> value.equals(metadataValue.getValue()))
                          .collect(Collectors.toList());
    }

    @SuppressWarnings("rawtypes")
    private List<Object> getIds(List<IndexableObject> indexableObjects) {
        return indexableObjects.stream()
                               .map(IndexableObject::getID)
                               .collect(Collectors.toList());
    }

}
