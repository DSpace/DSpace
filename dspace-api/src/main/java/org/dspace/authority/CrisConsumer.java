/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * Consumer to store item related entities when an item submission/modification
 * occurs.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisConsumer implements Consumer {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(CrisConsumer.class);

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ChoiceAuthorityService choiceAuthorityService;

    private ItemService itemService;

    private WorkspaceItemService workspaceItemService;

    private InstallItemService installItemService;

    @Override
    public void initialize() throws Exception {
        choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    }

    @Override
    public void finish(Context context) throws Exception {

    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        Item item = (Item) event.getSubject(context);
        if (item == null || itemsAlreadyProcessed.contains(item) || !item.isArchived()) {
            return;
        }

        itemsAlreadyProcessed.add(item);

        List<MetadataValue> metadataValues = item.getMetadata();

        for (MetadataValue metadata : metadataValues) {

            String authority = metadata.getAuthority();
            if (StringUtils.isNotBlank(authority) && !authority.startsWith(AuthorityValueService.GENERATE)) {
                continue;
            }

            String metadataValue = metadata.getValue();
            String fieldKey = getFieldKey(metadata);

            if (!choiceAuthorityService.isChoicesConfigured(fieldKey)) {
                continue;
            }

            String relationshipType = choiceAuthorityService.getLinkedItemType(fieldKey);

            Item relatedItem = findRelatedItemByCrisSourceId(context, metadataValue, relationshipType);
            if (relatedItem == null) {
                log.debug("Creation of item with relationship.type = {} related to item {}",
                        relationshipType, item.getID());
                relatedItem = buildRelatedItem(context, item, metadata, relationshipType);
            }

            metadata.setAuthority(relatedItem.getID().toString());
            metadata.setConfidence(Choices.CF_ACCEPTED);
        }

    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    private Item findRelatedItemByCrisSourceId(Context context, String crisSourceId,
            String relationshipType) throws Exception {

        Iterator<Item> iterator = itemService.findByMetadataField(context, "cris", "sourceId", null, crisSourceId);

        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (itemHasRelationshipTypeMetadataEqualsTo(item, relationshipType)) {
                return item;
            }
        }

        return null;

    }

    private String getFieldKey(MetadataValue metadata) {
        String schema = metadata.getSchema();
        String element = metadata.getElement();
        String qualifier = metadata.getQualifier();

        String fieldKey = schema + "_" + element;
        return StringUtils.isBlank(qualifier) ? fieldKey : fieldKey + "_" + qualifier;
    }

    private boolean itemHasRelationshipTypeMetadataEqualsTo(Item item, String relationshipType) {
        return item.getMetadata().stream().anyMatch(metadataValue -> {
            return metadataValue.getMetadataField().toString('.').equals("relationship.type") &&
                    metadataValue.getValue().equals(relationshipType);
        });
    }

    private Item buildRelatedItem(Context context, Item item, MetadataValue metadata,
            String relationshipType) throws Exception {

        WorkspaceItem workspaceItem = workspaceItemService.create(context, item.getOwningCollection(), false);

        Item relatedItem = workspaceItem.getItem();
        relatedItem.setOwningCollection(item.getOwningCollection());
        relatedItem.setSubmitter(item.getSubmitter());
        itemService.addMetadata(context, relatedItem, "cris", "sourceId", null, null, metadata.getValue());
        itemService.addMetadata(context, relatedItem, "relationship", "type", null, null, relationshipType);

        return installItemService.installItem(context, workspaceItem);

    }

}
