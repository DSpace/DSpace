/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.consumer;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.Relationship;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.orcid.OrcidHistory;
import org.dspace.orcid.OrcidOperation;
import org.dspace.orcid.factory.OrcidServiceFactory;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.model.factory.OrcidProfileSectionFactory;
import org.dspace.orcid.service.OrcidHistoryService;
import org.dspace.orcid.service.OrcidProfileSectionFactoryService;
import org.dspace.orcid.service.OrcidQueueService;
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.orcid.service.OrcidTokenService;
import org.dspace.profile.OrcidProfileSyncPreference;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The consumer to fill the ORCID queue. The addition to the queue is made for
 * all archived items that meet one of these conditions:
 * <ul>
 * <li>are profiles already linked to orcid that have some modified sections to
 * be synchronized (based on the preferences set by the user)</li>
 * <li>are publications/fundings related to profile items linked to orcid (based
 * on the preferences set by the user)</li>
 * 
 * </ul>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueConsumer implements Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidQueueConsumer.class);

    private OrcidQueueService orcidQueueService;

    private OrcidHistoryService orcidHistoryService;

    private OrcidTokenService orcidTokenService;

    private OrcidSynchronizationService orcidSynchronizationService;

    private ItemService itemService;

    private OrcidProfileSectionFactoryService profileSectionFactoryService;

    private ConfigurationService configurationService;

    private RelationshipService relationshipService;

    private List<UUID> alreadyConsumedItems = new ArrayList<>();

    @Override
    public void initialize() throws Exception {

        OrcidServiceFactory orcidServiceFactory = OrcidServiceFactory.getInstance();

        this.orcidQueueService = orcidServiceFactory.getOrcidQueueService();
        this.orcidHistoryService = orcidServiceFactory.getOrcidHistoryService();
        this.orcidSynchronizationService = orcidServiceFactory.getOrcidSynchronizationService();
        this.orcidTokenService = orcidServiceFactory.getOrcidTokenService();
        this.profileSectionFactoryService = orcidServiceFactory.getOrcidProfileSectionFactoryService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.relationshipService = ContentServiceFactory.getInstance().getRelationshipService();

        this.itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        if (isOrcidSynchronizationDisabled()) {
            return;
        }

        DSpaceObject dso = event.getSubject(context);
        if (!(dso instanceof Item)) {
            return;
        }

        Item item = (Item) dso;
        if (!item.isArchived()) {
            return;
        }

        if (alreadyConsumedItems.contains(item.getID())) {
            return;
        }

        context.turnOffAuthorisationSystem();
        try {
            consumeItem(context, item);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    /**
     * Consume the item if it is a profile or an ORCID entity.
     */
    private void consumeItem(Context context, Item item) throws SQLException {

        String entityType = itemService.getEntityTypeLabel(item);
        if (entityType == null) {
            return;
        }

        if (OrcidEntityType.isValidEntityType(entityType)) {
            consumeEntity(context, item);
        } else if (entityType.equals(getProfileType())) {
            consumeProfile(context, item);
        }

        alreadyConsumedItems.add(item.getID());

    }

    /**
     * Search for all related items to the given entity and create a new ORCID queue
     * record if one of this is a profile linked with ORCID and the entity item must
     * be synchronized with ORCID.
     */
    private void consumeEntity(Context context, Item entity) throws SQLException {

        List<Item> relatedItems = findAllRelatedItems(context, entity);

        for (Item relatedItem : relatedItems) {

            if (isNotProfileItem(relatedItem) || isNotLinkedToOrcid(context, relatedItem)) {
                continue;
            }

            if (shouldNotBeSynchronized(relatedItem, entity) || isAlreadyQueued(context, relatedItem, entity)) {
                continue;
            }

            orcidQueueService.create(context, relatedItem, entity);

        }

    }

    private List<Item> findAllRelatedItems(Context context, Item entity) throws SQLException {
        return relationshipService.findByItem(context, entity).stream()
            .map(relationship -> getRelatedItem(entity, relationship))
            .collect(Collectors.toList());
    }

    private Item getRelatedItem(Item item, Relationship relationship) {
        return item.equals(relationship.getLeftItem()) ? relationship.getRightItem() : relationship.getLeftItem();
    }

    /**
     * If the given profile item is linked with ORCID recalculate all the ORCID
     * queue records of the configured profile sections that can be synchronized.
     */
    private void consumeProfile(Context context, Item item) throws SQLException {

        if (isNotLinkedToOrcid(context, item)) {
            return;
        }

        for (OrcidProfileSectionFactory factory : getAllProfileSectionFactories(item)) {

            String sectionType = factory.getProfileSectionType().name();

            orcidQueueService.deleteByEntityAndRecordType(context, item, sectionType);

            if (isProfileSectionSynchronizationDisabled(context, item, factory)) {
                continue;
            }

            List<String> signatures = factory.getMetadataSignatures(context, item);
            List<OrcidHistory> historyRecords = findSuccessfullyOrcidHistoryRecords(context, item, sectionType);

            createInsertionRecordForNewSignatures(context, item, historyRecords, factory, signatures);
            createDeletionRecordForNoMorePresentSignatures(context, item, historyRecords, factory, signatures);

        }

    }

    private boolean isProfileSectionSynchronizationDisabled(Context context,
        Item item, OrcidProfileSectionFactory factory) {
        List<OrcidProfileSyncPreference> preferences = this.orcidSynchronizationService.getProfilePreferences(item);
        return !preferences.contains(factory.getSynchronizationPreference());
    }

    /**
     * Add new INSERTION record in the ORCID queue based on the metadata signatures
     * calculated from the current item state.
     */
    private void createInsertionRecordForNewSignatures(Context context, Item item, List<OrcidHistory> historyRecords,
        OrcidProfileSectionFactory factory, List<String> signatures) throws SQLException {

        String sectionType = factory.getProfileSectionType().name();

        for (String signature : signatures) {

            if (isNotAlreadySynchronized(historyRecords, signature)) {
                String description = factory.getDescription(context, item, signature);
                orcidQueueService.createProfileInsertionRecord(context, item, description, sectionType, signature);
            }

        }

    }

    /**
     * Add new DELETION records in the ORCID queue for metadata signature presents
     * in the ORCID history no more present in the metadata signatures calculated
     * from the current item state.
     */
    private void createDeletionRecordForNoMorePresentSignatures(Context context, Item profile,
        List<OrcidHistory> historyRecords, OrcidProfileSectionFactory factory, List<String> signatures)
        throws SQLException {

        String sectionType = factory.getProfileSectionType().name();

        for (OrcidHistory historyRecord : historyRecords) {
            String storedSignature = historyRecord.getMetadata();
            String putCode = historyRecord.getPutCode();
            String description = historyRecord.getDescription();

            if (signatures.contains(storedSignature) || isAlreadyDeleted(historyRecords, historyRecord)) {
                continue;
            }

            if (StringUtils.isBlank(putCode)) {
                LOGGER.warn("The orcid history record with id {} should have a not blank put code",
                    historyRecord.getID());
                continue;
            }

            orcidQueueService.createProfileDeletionRecord(context, profile, description,
                sectionType, storedSignature, putCode);
        }

    }

    private List<OrcidHistory> findSuccessfullyOrcidHistoryRecords(Context context, Item item,
        String sectionType) throws SQLException {
        return orcidHistoryService.findSuccessfullyRecordsByEntityAndType(context, item, sectionType);
    }

    private boolean isNotAlreadySynchronized(List<OrcidHistory> records, String signature) {
        return getLastOperation(records, signature)
            .map(operation -> operation == OrcidOperation.DELETE)
            .orElse(Boolean.TRUE);
    }

    private boolean isAlreadyDeleted(List<OrcidHistory> records, OrcidHistory historyRecord) {

        if (historyRecord.getOperation() == OrcidOperation.DELETE) {
            return true;
        }

        return findDeletedHistoryRecordsBySignature(records, historyRecord.getMetadata())
            .anyMatch(record -> record.getTimestamp().after(historyRecord.getTimestamp()));
    }

    private Stream<OrcidHistory> findDeletedHistoryRecordsBySignature(List<OrcidHistory> records, String signature) {
        return records.stream()
            .filter(record -> signature.equals(record.getMetadata()))
            .filter(record -> record.getOperation() == OrcidOperation.DELETE);
    }

    private Optional<OrcidOperation> getLastOperation(List<OrcidHistory> records, String signature) {
        return records.stream()
            .filter(record -> signature.equals(record.getMetadata()))
            .sorted(comparing(OrcidHistory::getTimestamp, nullsFirst(naturalOrder())).reversed())
            .map(OrcidHistory::getOperation)
            .findFirst();
    }

    private boolean isAlreadyQueued(Context context, Item profileItem, Item entity) throws SQLException {
        return isNotEmpty(orcidQueueService.findByProfileItemAndEntity(context, profileItem, entity));
    }

    private boolean isNotLinkedToOrcid(Context context, Item profileItemItem) {
        return hasNotOrcidAccessToken(context, profileItemItem)
            || getMetadataValue(profileItemItem, "person.identifier.orcid") == null;
    }

    private boolean hasNotOrcidAccessToken(Context context, Item profileItemItem) {
        return orcidTokenService.findByProfileItem(context, profileItemItem) == null;
    }

    private boolean shouldNotBeSynchronized(Item profileItem, Item entity) {
        return !orcidSynchronizationService.isSynchronizationAllowed(profileItem, entity);
    }

    private boolean isNotProfileItem(Item profileItemItem) {
        return !getProfileType().equals(itemService.getEntityTypeLabel(profileItemItem));
    }

    private String getMetadataValue(Item item, String metadataField) {
        return itemService.getMetadataFirstValue(item, new MetadataFieldName(metadataField), Item.ANY);
    }

    private List<OrcidProfileSectionFactory> getAllProfileSectionFactories(Item item) {
        return this.profileSectionFactoryService.findByPreferences(asList(OrcidProfileSyncPreference.values()));
    }

    private String getProfileType() {
        return configurationService.getProperty("researcher-profile.entity-type", "Person");
    }

    private boolean isOrcidSynchronizationDisabled() {
        return !configurationService.getBooleanProperty("orcid.synchronization-enabled", true);
    }

    @Override
    public void end(Context context) throws Exception {
        alreadyConsumedItems.clear();
    }

    @Override
    public void finish(Context context) throws Exception {
        // nothing to do
    }

}
