/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.crosswalk.SubscriptionDsoMetadataForEmailCompose;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.EPerson;
import org.dspace.subscriptions.service.SubscriptionGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation class of SubscriptionGenerator
 * which will handle the logic of sending the emails
 * in case of 'content' subscriptionType
 */
@org.springframework.stereotype.Component
@SuppressWarnings("rawtypes")
public class ContentGenerator implements SubscriptionGenerator<IndexableObject> {

    private final Logger log = LogManager.getLogger(ContentGenerator.class);

    @SuppressWarnings("unchecked")
    private Map<String, SubscriptionDsoMetadataForEmailCompose> entityType2Disseminator = new HashMap();

    @Autowired
    private ItemService itemService;

    private static final String NEW_ITEMS_LABEL = "New Items";
    private static final String MODIFIED_ITEMS_LABEL = "Modified Items";
    private static final int MAX_METADATA_VALUES = 3;
    private static final String COMMUNITY_NOTE_PREFIX = " (via community subscription to \"";
    private static final String COMMUNITY_NOTE_SUFFIX = " \")";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @Override
    public void notifyForSubscriptions(Context context, EPerson ePerson,
                                       Map<Community, List<IndexableObject>> commMap,
                                       Map<Collection, List<IndexableObject>> collMap) {
        try {
            if (Objects.nonNull(ePerson)) {
                Locale supportedLocale = I18nUtil.getEPersonLocale(ePerson);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "subscriptions_content"));
                email.addRecipient(ePerson.getEmail());

                // Create a map which holds the collections and their corresponding community names if we have updates
                // That originally came from a community subscription, so we can refer to this in the email
                Map<Collection, String> collectionToCommunityNameMap = buildCommunityCollectionMap(commMap);
                List<IndexableObject> allItems = new ArrayList<>();
                if (commMap != null) {
                    commMap.values().forEach(allItems::addAll);
                }
                if (collMap != null) {
                    collMap.values().forEach(allItems::addAll);
                }

                Map<Collection, List<IndexableObject>> allByCollection =
                        allItems.stream()
                                .filter(distinctByKey(IndexableObject::getID))
                                .collect(groupingBy(obj -> ((Item) obj.getIndexedObject()).getOwningCollection()));

                // Split into new and modified sections
                Map<Collection, List<IndexableObject>> newItemsByCollection = new HashMap<>();
                Map<Collection, List<IndexableObject>> modifiedItemsByCollection = new HashMap<>();
                allByCollection.forEach((collection, items) -> {
                    Map<Boolean, List<IndexableObject>> partitionedItems =
                            items.stream().collect(partitioningBy(obj -> isNewItem((Item) obj.getIndexedObject())));

                    newItemsByCollection.put(collection,  partitionedItems.get(true));
                    modifiedItemsByCollection.put(collection, partitionedItems.get(false));
                });

                String intro = buildIntro(newItemsByCollection, modifiedItemsByCollection);
                String combinedSection = buildCombinedSection(newItemsByCollection, modifiedItemsByCollection,
                        collectionToCommunityNameMap);

                if (combinedSection.equals(EMPTY)) {
                    log.debug("subscription(s) of eperson {} do(es) not match any new or modified items: " +
                            "nothing to send - exit silently", ePerson::getID);
                    return;
                }
                email.addArgument(intro);
                email.addArgument(combinedSection);
                email.send();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            log.warn("Cannot email user eperson_id: {} eperson_email: {}", ePerson::getID, ePerson::getEmail);
        }
    }

    private static Map<Collection, String> buildCommunityCollectionMap(Map<Community, List<IndexableObject>> commMap) {
        if (commMap == null) {
            return Collections.emptyMap();
        }
        Map<Collection, String> resultMap = new HashMap<>();
        commMap.forEach((community, items) -> items.forEach(obj -> {
            Item item = (Item) obj.getIndexedObject();
            Collection collection = item.getOwningCollection();
            resultMap.putIfAbsent(collection, community.getName());
        }));
        return resultMap;
    }

    private boolean isNewItem(Item item) {
        Optional<ZonedDateTime> createdDate =
                itemService.getMetadata(item, "dc", "date", "accessioned", Item.ANY)
                           .stream()
                           .map(MetadataValue::getValue)
                           .findFirst()
                           .map(val -> new DCDate(val).toDate().toInstant())
                           .map(instant -> ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));

        ZonedDateTime lastModified = ZonedDateTime.ofInstant(item.getLastModified(), ZoneOffset.UTC);

        return createdDate.map(createdZoned -> createdZoned.truncatedTo(ChronoUnit.SECONDS)
                                                           .equals(lastModified.truncatedTo(ChronoUnit.SECONDS)))
                          .orElse(false);
    }

    private String buildIntro(Map<Collection, List<IndexableObject>> newItems,
                              Map<Collection, List<IndexableObject>> modifiedItems) {
        if (!newItems.isEmpty() && !modifiedItems.isEmpty()) {
            return "New and modified items are available in the collections you have subscribed to:";
        } else if (!newItems.isEmpty()) {
            return "New items are available in the collections you have subscribed to:";
        } else if (!modifiedItems.isEmpty()) {
            return "Modified items are available in the collections you have subscribed to:";
        } else {
            return "";
        }
    }

    private String buildCombinedSection(Map<Collection, List<IndexableObject>> newItemsByCollection,
                                        Map<Collection, List<IndexableObject>> modifiedItemsByCollection,
                                        Map<Collection, String> collectionToCommunity) {
        if (newItemsByCollection.isEmpty() && modifiedItemsByCollection.isEmpty()) {
            return EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        Set<Collection> allCollections = new HashSet<>();
        allCollections.addAll(newItemsByCollection.keySet());
        allCollections.addAll(modifiedItemsByCollection.keySet());

        for (Collection collection : allCollections) {
            String header = buildCollectionHeader(collection, collectionToCommunity);
            sb.append(header);

            List<IndexableObject> newItems = newItemsByCollection.get(collection);
            List<IndexableObject> modifiedItems = modifiedItemsByCollection.get(collection);
            if (newItems != null && !newItems.isEmpty()) {
                sb.append(buildSectionHeader(NEW_ITEMS_LABEL, newItems.size()));
                sb.append(buildItemsBlock(newItems));
            }
            if (modifiedItems != null && !modifiedItems.isEmpty()) {
                sb.append(buildSectionHeader(MODIFIED_ITEMS_LABEL, modifiedItems.size()));
                sb.append(buildItemsBlock(modifiedItems));
            }
            sb.append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

    private String buildCollectionHeader(Collection collection, Map<Collection, String> collectionToCommunity) {
        String name = collection.getName();
        String communityNote = "";
        if (collectionToCommunity != null && collectionToCommunity.containsKey(collection)) {
            communityNote = COMMUNITY_NOTE_PREFIX + collectionToCommunity.get(collection) + COMMUNITY_NOTE_SUFFIX;
        }
        String fullHeader = name + communityNote + ":" + LINE_SEPARATOR;
        String underline = "-".repeat(Math.max(0, name.length() + communityNote.length())) + LINE_SEPARATOR;
        return fullHeader + underline;
    }

    private String buildSectionHeader(String label, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(label).append(" (").append(count).append("):")
          .append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        return sb.toString();
    }

    private String buildItemsBlock(List<IndexableObject> items) {
        StringBuilder sb = new StringBuilder();
        for (IndexableObject obj : items) {
            Item item = (Item) obj.getIndexedObject();
            sb.append(formatItem(item)).append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

    private String formatItem(Item item) {
        String entityType = itemService.getEntityTypeLabel(item);
        SubscriptionDsoMetadataForEmailCompose crosswalk = entityType2Disseminator.get(entityType);
        if (crosswalk == null) {
            crosswalk = entityType2Disseminator.get("Item"); // fallback
        }
        if (crosswalk == null) {
            throw new IllegalStateException("No crosswalk found for entity type: " + entityType);
        }
        StringBuilder sb = new StringBuilder();
        for (ImmutablePair<String, String> mdPair: crosswalk.getMetadata()) {
            String field = mdPair.getLeft();
            String label = mdPair.getRight();
            List<String> values = getAllMetadata(item, field);
            if (!values.isEmpty()) {
                addIfNotBlank(sb, label + ": ", values);
            }
        }
        return sb.toString();
    }

    private void addIfNotBlank(StringBuilder sb, String label, List<String> values) {
        if (values != null && !values.isEmpty()) {
            List<String> nonBlankValues = values.stream()
                                                .filter(StringUtils::isNotBlank)
                                                .collect(toList());

            if (!nonBlankValues.isEmpty()) {
                String joined = nonBlankValues.stream()
                                              .limit(MAX_METADATA_VALUES)
                                              .collect(joining(", "));
                sb.append("\t").append(label).append(joined);
                if (nonBlankValues.size() > MAX_METADATA_VALUES) {
                    sb.append(", ...");
                }
                sb.append(LINE_SEPARATOR);
            }
        }
    }

    private List<String> getAllMetadata(Item item, String field) {
        String[] fieldParts = field.split("\\.");
        String schema = fieldParts[0];
        String element = fieldParts[1];
        String qualifier = fieldParts.length > 2 ? fieldParts[2] : null;
        return itemService.getMetadata(item, schema, element, qualifier, Item.ANY)
                          .stream()
                          .map(MetadataValue::getValue)
                          .filter(Objects::nonNull)
                          .collect(toList());
    }

    public void setEntityType2Disseminator(Map<String, SubscriptionDsoMetadataForEmailCompose>
                                                   entityType2Disseminator) {
        this.entityType2Disseminator = entityType2Disseminator;
    }

}
