/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.consumer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.factory.OrcidQueueServiceFactory;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
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

    private static final String SYNC_DISABLED = "DISABLED";

    private OrcidQueueService orcidQueueService;

    private ItemService itemService;

    private List<UUID> alreadyConsumedItems = new ArrayList<>();

    @Override
    public void initialize() throws Exception {
        this.orcidQueueService = OrcidQueueServiceFactory.getInstance().getOrcidQueueService();
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

        String relationshipType = getMetadataValue(item, "relationship.type");
        if (relationshipType == null) {
            return;
        }

        switch (relationshipType) {
            case "Person":
                consumePerson(context, item);
                break;
            case "Publication":
            case "Project":
                consumeItem(context, item, relationshipType);
                break;
            default:
                break;
        }

        alreadyConsumedItems.add(item.getID());

    }

    private void consumeItem(Context context, Item item, String relationshipType) throws SQLException {
        List<MetadataValue> metadataValues = item.getMetadata();

        for (MetadataValue metadata : metadataValues) {

            String authority = metadata.getAuthority();
            // ignore nested metadata with placeholder
            if (StringUtils.equals(metadata.getValue(), CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)) {
                continue;
            }
            if (StringUtils.isBlank(authority) || authority.startsWith(AuthorityValueService.GENERATE)) {
                continue;
            }

            UUID relatedItemUuid = UUIDUtils.fromString(authority);
            if (relatedItemUuid == null) {
                continue;
            }

            Item ownerItem = itemService.findByIdOrLegacyId(context, relatedItemUuid.toString());
            String ownerType = getMetadataValue(ownerItem, "relationship.type");
            String orcidId = getMetadataValue(ownerItem, "person.identifier.orcid");
            if (!"Person".equals(ownerType) || StringUtils.isEmpty(orcidId)) {
                continue;
            }

            if (!orcidQueueService.findByOwnerAndEntityId(context, relatedItemUuid, item.getID()).isEmpty()) {
                continue;
            }

            if (shouldBeSend(ownerItem, relationshipType)) {
                OrcidQueue orcidQueue = orcidQueueService.create(context, ownerItem, item);
                log.debug("Created ORCID queue record with id " + orcidQueue.getID());
            }

        }

    }

    private void consumePerson(Context context, Item item) throws SQLException {
        String orcidId = getMetadataValue(item, "person.identifier.orcid");
        List<OrcidQueue> queueRecords = orcidQueueService.findByOwnerAndEntityId(context, item.getID(), item.getID());
        if (StringUtils.isNotEmpty(orcidId) && queueRecords.isEmpty()) {
            OrcidQueue orcidQueue = orcidQueueService.create(context, item, item);
            log.debug("Created ORCID queue record with id " + orcidQueue.getID());
        }
    }

    private boolean shouldBeSend(Item item, String relatedItemType) {
        String syncSetting = "cris.orcid.sync-" + (relatedItemType.equals("Publication") ? "publications" : "projects");
        String syncSettingValue = getMetadataValue(item, syncSetting);
        return syncSettingValue != null && !syncSettingValue.equals(SYNC_DISABLED);
    }

    private String getMetadataValue(Item item, String metadataField) {
        return item.getMetadata().stream()
            .filter(metadata -> metadata.getMetadataField().toString('.').equals(metadataField))
            .map(metadata -> metadata.getValue())
            .findFirst()
            .orElse(null);
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
