/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.consumer;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.profile.service.ProfileOrcidSynchronizationService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The consumer to fill the ORCID queue.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueConsumer implements Consumer {

    private static Logger log = LoggerFactory.getLogger(OrcidQueueConsumer.class);

    private OrcidQueueService orcidQueueService;

    private OrcidHistoryService orcidHistoryService;

    private ProfileOrcidSynchronizationService orcidSynchronizationService;

    private ItemService itemService;

    private List<UUID> alreadyConsumedItems = new ArrayList<>();

    @Override
    public void initialize() throws Exception {
        this.orcidQueueService = OrcidServiceFactory.getInstance().getOrcidQueueService();
        this.orcidHistoryService = OrcidServiceFactory.getInstance().getOrcidHistoryService();
        this.orcidSynchronizationService = OrcidServiceFactory.getInstance().getOrcidSynchronizationService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
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

    private void consumeItem(Context context, Item item) throws SQLException {

        String entityType = itemService.getEntityType(item);
        if (entityType == null) {
            return;
        }

        switch (entityType) {
            case "Person":
                consumePerson(context, item);
                break;
            case "Publication":
            case "Project":
                consumeEntity(context, item);
                break;
            default:
                break;
        }

        alreadyConsumedItems.add(item.getID());

    }

    private void consumeEntity(Context context, Item entity) throws SQLException {
        List<MetadataValue> metadataValues = entity.getMetadata();

        for (MetadataValue metadata : metadataValues) {

            String authority = metadata.getAuthority();

            if (isNestedMetadataPlaceholder(metadata)) {
                continue;
            }

            UUID relatedItemUuid = UUIDUtils.fromString(authority);
            if (relatedItemUuid == null) {
                continue;
            }

            Item owner = itemService.find(context, relatedItemUuid);

            if (isNotPersonItem(owner) || isNotLinkedToOrcid(owner)) {
                continue;
            }

            if (shouldNotBeSynchronized(owner, entity) || isAlreadyQueued(context, owner, entity)) {
                continue;
            }

            OrcidQueue orcidQueue = createOrcidQueue(context, owner, entity);
            log.debug("Created ORCID queue record with id " + orcidQueue.getID());

        }

    }

    private void consumePerson(Context context, Item item) throws SQLException {

        if (isNotLinkedToOrcid(item) || isAlreadyQueued(context, item, item)) {
            return;
        }

        if (profileShouldNotBeSynchronized(item)) {
            return;
        }

        OrcidQueue orcidQueue = orcidQueueService.create(context, item, item);
        log.debug("Created ORCID queue record with id " + orcidQueue.getID());

    }

    private boolean isAlreadyQueued(Context context, Item owner, Item entity) throws SQLException {
        return isNotEmpty(orcidQueueService.findByOwnerAndEntityId(context, owner.getID(), entity.getID()));
    }

    private boolean isNotLinkedToOrcid(Item ownerItem) {
        return getMetadataValue(ownerItem, "cris.orcid.access-token") == null
            || getMetadataValue(ownerItem, "person.identifier.orcid") == null;
    }

    private boolean shouldNotBeSynchronized(Item owner, Item entity) {
        return !orcidSynchronizationService.isSynchronizationEnabled(owner, entity);
    }

    private boolean profileShouldNotBeSynchronized(Item item) {
        return !orcidSynchronizationService.isSynchronizationEnabled(item, item);
    }

    private boolean isNotPersonItem(Item ownerItem) {
        return !"Person".equals(itemService.getEntityType(ownerItem));
    }

    private boolean isNestedMetadataPlaceholder(MetadataValue metadata) {
        return StringUtils.equals(metadata.getValue(), CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
    }

    private OrcidQueue createOrcidQueue(Context context, Item owner, Item entity) throws SQLException {
        Optional<String> putCode = orcidHistoryService.findLastPutCode(context, owner, entity);
        if (putCode.isPresent()) {
            return orcidQueueService.create(context, owner, entity, putCode.get());
        } else {
            return orcidQueueService.create(context, owner, entity);
        }
    }

    private String getMetadataValue(Item item, String metadataField) {
        return itemService.getMetadataFirstValue(item, new MetadataFieldName(metadataField), Item.ANY);
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
