/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority;

import static org.dspace.content.MetadataSchemaEnum.CRIS;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.filler.AuthorityImportFiller;
import org.dspace.authority.filler.AuthorityImportFillerHolder;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Consumer to store item related entities when an item submission/modification
 * occurs.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisConsumer implements Consumer {

    public static final String SOURCE_INTERNAL = "INTERNAL-SUBMISSION";

    private final static String NO_RELATIONSHIP_TYPE_FOUND_MSG = "No relationship.type found for field {}";

    private final static String ITEM_CREATION_MSG = "Creation of item with relationship.type = {} related to item {}";

    private final static String NO_COLLECTION_FOUND_MSG = "No collection found with relationship.type = {} "
            + "for item = {}. No related item will be created.";

    private static Logger log = LogManager.getLogger(CrisConsumer.class);

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ChoiceAuthorityService choiceAuthorityService;

    private ItemService itemService;

    private WorkspaceItemService workspaceItemService;

    private WorkflowService<XmlWorkflowItem> workflowService;

    private InstallItemService installItemService;

    private CollectionService collectionService;

    private RelationshipService relationshipService;

    private ConfigurationService configurationService;

    private AuthorityImportFillerHolder authorityImportFillerHolder;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {
        choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
        authorityImportFillerHolder = AuthorityServiceFactory.getInstance().getAuthorityImportFillerHolder();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
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

        context.turnOffAuthorisationSystem();
        try {
            consumeItem(context, item);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    private void consumeItem(Context context, Item item) throws Exception {

        List<MetadataValue> metadataValues = item.getMetadata();

        for (MetadataValue metadata : metadataValues) {

            String authority = metadata.getAuthority();
            // ignore nested metadata with placeholder
            if (StringUtils.equals(metadata.getValue(), CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)) {
                continue;
            }
            if (StringUtils.isNotBlank(authority) && !authority.startsWith(AuthorityValueService.GENERATE)) {
                continue;
            }

            String fieldKey = getFieldKey(metadata);

            if (!choiceAuthorityService.isChoicesConfigured(fieldKey, null)) {
                continue;
            }

            String relationshipType = choiceAuthorityService.getRelationshipType(fieldKey);
            if (relationshipType == null) {
                log.warn(NO_RELATIONSHIP_TYPE_FOUND_MSG, fieldKey);
                continue;
            }

            String crisSourceId = generateCrisSourceId(metadata);

            Item relatedItem = findRelatedItemByCrisSourceId(context, crisSourceId, relationshipType);
            boolean relatedItemAlreadyPresent = relatedItem != null;

            if (!relatedItemAlreadyPresent) {

                Collection collection = collectionService.retrieveCollectionByRelationshipType(item, relationshipType);
                if (collection == null) {
                    log.warn(NO_COLLECTION_FOUND_MSG, relationshipType, item.getID());
                    continue;
                }
                collection = context.reloadEntity(collection);

                log.debug(ITEM_CREATION_MSG, relationshipType, item.getID());
                relatedItem = buildRelatedItem(context, item, collection, metadata, relationshipType, crisSourceId);

            }

            String authorityType = calculateAuthorityType(authority);
            AuthorityImportFiller filler = authorityImportFillerHolder.getFiller(authorityType);
            if (filler != null && (!relatedItemAlreadyPresent || filler.allowsUpdate(context, metadata, relatedItem))) {
                filler.fillItem(context, metadata, relatedItem);
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

        Iterator<Item> items = itemService.findByMetadataField(context, CRIS.getName(), "sourceId", null, crisSourceId);

        while (items.hasNext()) {
            Item item = items.next();
            if (relationshipService.hasRelationshipType(item, relationshipType)) {
                return item;
            }
        }

        return null;

    }

    private String getFieldKey(MetadataValue metadata) {
        return metadata.getMetadataField().toString('_');
    }



    private Item buildRelatedItem(Context context, Item item, Collection collection, MetadataValue metadata,
            String relationshipType, String crisSourceId) throws Exception {

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item relatedItem = workspaceItem.getItem();
        itemService.addMetadata(context, relatedItem, CRIS.getName(), "sourceId", null, null, crisSourceId);
        itemService.addMetadata(context, relatedItem, "dc", "title", null, null, metadata.getValue());
        if (!relationshipService.hasRelationshipType(relatedItem, relationshipType)) {
            log.error("Inconstent configuration the related item " + relatedItem.getID().toString() + ", created from "
                    + item.getID().toString() + " (" + metadata.getMetadataField().toString('.') + ")"
                    + " hasn't the expected [" + relationshipType + "] relationshipType");
        }

        if (isSubmissionEnabled(metadata)) {
            installItemService.installItem(context, workspaceItem);
        } else {
            workflowService.start(context, workspaceItem).getItem();
        }
        return relatedItem;
    }

    private String generateCrisSourceId(MetadataValue metadata) {
        if (isUuidStrategyEnabled(metadata)) {
            return UUID.randomUUID().toString();
        } else {
            return DigestUtils.md5Hex(metadata.getValue().toUpperCase());
        }
    }

    private boolean isUuidStrategyEnabled(MetadataValue value) {
        String property = "cris.import.submission.strategy.uuid." + getFieldKey(value);
        return configurationService.getBooleanProperty(property, false);
    }

    private boolean isSubmissionEnabled(MetadataValue value) {
        String property = "cris.import.submission.enabled.entity";
        String propertyWithMetadataField = property + "." + getFieldKey(value);
        if (configurationService.hasProperty(propertyWithMetadataField)) {
            return configurationService.getBooleanProperty(propertyWithMetadataField);
        } else {
            return configurationService.getBooleanProperty(property, true);
        }
    }

    private String calculateAuthorityType(String authority ) {
        if (StringUtils.isNotBlank(authority) && authority.startsWith(AuthorityValueService.GENERATE)) {
            String[] split = StringUtils.split(authority, AuthorityValueService.SPLIT);
            if (split.length > 1) {
                return split[1];
            }
        }
        return SOURCE_INTERNAL;
    }

}
