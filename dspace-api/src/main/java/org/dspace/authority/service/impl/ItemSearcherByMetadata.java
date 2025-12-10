/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service.impl;

import static org.apache.commons.collections4.IteratorUtils.chainedIterator;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authority.service.ItemReferenceResolver;
import org.dspace.authority.service.ItemSearcher;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableInProgressSubmission;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implementation of {@link ItemSearcher} and {@link ItemReferenceResolver} to
 * search the item by the configured metadata.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemSearcherByMetadata implements ItemSearcher, ItemReferenceResolver {

    private static final Logger log = LogManager.getLogger(ItemSearcherByMetadata.class);
    private final String metadata;
    private final String authorityPrefix;
    @Autowired
    private SearchService searchService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;
    private final ThreadLocal<Map<String, UUID>> valuesToItemIds = ThreadLocal.withInitial(() -> new HashMap<>());
    private final ThreadLocal<MultiValuedMap<String, UUID>> referenceResolutionAttempts =
        ThreadLocal.withInitial(() -> new ArrayListValuedHashMap<>());

    public ItemSearcherByMetadata(String metadata, String authorityPrefix) {
        this.metadata = metadata;
        this.authorityPrefix = authorityPrefix;
    }

    @Override
    public Item searchBy(Context context, String searchParam, Item source) {
        try {
            if (source != null) {
                referenceResolutionAttempts.get().get(searchParam).add(source.getID());
            }
            if (valuesToItemIds.get().containsKey(searchParam)) {
                Item foundInCache = itemService.find(context, valuesToItemIds.get().get(searchParam));
                if (foundInCache != null) {
                    return foundInCache;
                } else {
                    UUID removedUUID = valuesToItemIds.get().remove(searchParam);
                    log.info("No item with uuid: " + removedUUID + " was found");
                    log.info("Removing uuid: " + removedUUID + " from cache");
                    return performSearchByMetadata(context, searchParam);
                }
            } else {
                return performSearchByMetadata(context, searchParam);
            }
        } catch (SearchServiceException e) {
            throw new RuntimeException("An error occurs searching the item by metadata", e);
        } catch (SQLException e) {
            throw new RuntimeException("An error occurs retrieving the item by identifier", e);
        }
    }

    @Override
    public void resolveReferences(Context context, Item item) {

        List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadata);
        if (CollectionUtils.isEmpty(metadataValues)) {
            return;
        }

        try {
            context.turnOffAuthorisationSystem();
            resolveReferences(context, metadataValues, item);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException("An error occurs resolving references", e);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    @SuppressWarnings("rawtypes")
    private Item performSearchByMetadata(Context context, String searchParam) throws SearchServiceException {
        String query = metadata + ":" +
            ClientUtils.escapeQueryChars(searchParam);
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addDSpaceObjectFilter(IndexableWorkspaceItem.TYPE);
        discoverQuery.addDSpaceObjectFilter(IndexableWorkflowItem.TYPE);
        discoverQuery.addFilterQueries(query);

        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();

        if (CollectionUtils.isEmpty(indexableObjects)) {
            return null;
        }

        IndexableObject indexableObject = indexableObjects.get(0);
        if (indexableObject instanceof IndexableItem) {
            return ((IndexableItem) indexableObject).getIndexedObject();
        } else {
            return ((IndexableInProgressSubmission) indexableObject).getIndexedObject().getItem();
        }
    }

    private void resolveReferences(Context context, List<MetadataValue> metadataValues, Item item)
        throws SQLException, AuthorizeException {

        metadataValues.forEach(metadataValue -> {
            valuesToItemIds.get().put(metadataValue.getValue(), item.getID());
        });

        List<String> authorities = metadataValues.stream()
                                                 .map(MetadataValue::getValue)
                                                 .map(
                                                     value -> AuthorityValueService.REFERENCE + authorityPrefix + "::" +
                                                         value)
                                                 .collect(Collectors.toList());

        Iterator<Item> itemsIterator =
            itemService.findRelatedItemsByAuthorityControlledFields(context, item, authorities);

        Iterator<Item> cachedItemsIterator = getItemsFromResolutionAttemptsCache(context, metadataValues);

        Iterator<Item> itemsWithReferenceIterator = chainedIterator(itemsIterator, cachedItemsIterator);

        while (itemsWithReferenceIterator.hasNext()) {
            Item itemWithReference = itemsWithReferenceIterator.next();
            updateReferences(context, itemWithReference, item, authorities);
        }

    }

    private Iterator<Item> getItemsFromResolutionAttemptsCache(Context context, List<MetadataValue> metadataValues) {
        return metadataValues.stream()
                             .flatMap(metadataValue -> referenceResolutionAttempts.get().get(metadataValue.getValue())
                                                                                  .stream())
                             .flatMap(itemId -> findItemById(context, itemId).stream())
                             .iterator();
    }

    private void updateReferences(Context context, Item itemWithReference, Item item, List<String> authorities)
        throws SQLException, AuthorizeException {

        itemWithReference.getMetadata().stream()
                         .filter(metadataValue -> authorities.contains(metadataValue.getAuthority()))
                         .forEach(
                             metadataValue -> choiceAuthorityService.setReferenceWithAuthority(metadataValue, item));

        itemService.update(context, itemWithReference);
    }

    private Optional<Item> findItemById(Context context, UUID itemId) {
        try {
            return Optional.ofNullable(itemService.find(context, itemId));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearCache() {
        valuesToItemIds.get().clear();
        referenceResolutionAttempts.get().clear();
    }

    public String getMetadata() {
        return metadata;
    }

    public String getAuthorityPrefix() {
        return authorityPrefix;
    }

}
