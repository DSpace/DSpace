/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.content.MetadataSchemaEnum.CRIS;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.filler.AuthorityImportFiller;
import org.dspace.authority.filler.AuthorityImportFillerService;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authority.service.ItemSearchService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
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

    public static final String CONSUMER_NAME = "crisconsumer";

    private final static String NO_ENTITY_TYPE_FOUND_MSG = "No dspace.entity.type found for field {}";

    private final static String ITEM_CREATION_MSG = "Creation of item with dspace.entity.type = {} related to item {}";

    private final static String NO_COLLECTION_FOUND_MSG = "No collection found with dspace.entity.type = {} "
            + "for item = {}. No related item will be created.";

    private final static String NO_ITEM_FOUND_BY_AUTHORITY_MSG = "No related item found by authority {}";

    private static Logger log = LogManager.getLogger(CrisConsumer.class);

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ChoiceAuthorityService choiceAuthorityService;

    private MetadataAuthorityService metadataAuthorityService;

    private ItemService itemService;

    private WorkspaceItemService workspaceItemService;

    private WorkflowService<XmlWorkflowItem> workflowService;

    private InstallItemService installItemService;

    private CollectionService collectionService;

    private ConfigurationService configurationService;

    private AuthorityImportFillerService authorityImportFillerService;

    private ItemSearchService itemSearchService;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {
        choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        metadataAuthorityService = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
        authorityImportFillerService = AuthorityServiceFactory.getInstance().getAuthorityImportFillerService();
        itemSearchService = new DSpace().getSingletonService(ItemSearchService.class);
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

        addEntityTypeIfNotExist(context, item);

        for (MetadataValue metadata : item.getMetadata()) {

            String fieldKey = getFieldKey(metadata);
            String authority = metadata.getAuthority();

            if (isMetadataSkippable(metadata)) {
                continue;
            }

            String entityType = choiceAuthorityService.getLinkedEntityType(fieldKey);
            if (entityType == null) {
                log.debug(NO_ENTITY_TYPE_FOUND_MSG, fieldKey);
                continue;
            }

            String crisSourceId = generateCrisSourceId(metadata);

            Item relatedItem = itemSearchService.search(context, crisSourceId, entityType, item);
            boolean relatedItemAlreadyPresent = relatedItem != null;

            if (!relatedItemAlreadyPresent && isNotBlank(authority) && isReferenceAuthority(authority)) {
                log.debug(NO_ITEM_FOUND_BY_AUTHORITY_MSG, metadata.getAuthority());
                metadata.setConfidence(Choices.CF_UNSET);
                continue;
            }

            if (!relatedItemAlreadyPresent) {
                Collection collection = collectionService.retrieveCollectionByEntityType(context, item, entityType);
                if (collection == null) {
                    log.warn(NO_COLLECTION_FOUND_MSG, entityType, item.getID());
                    continue;
                }
                collection = context.reloadEntity(collection);

                log.debug(ITEM_CREATION_MSG, entityType, item.getID());
                relatedItem = buildRelatedItem(context, item, collection, metadata, entityType, crisSourceId);

            }

            fillRelatedItem(context, metadata, relatedItem, relatedItemAlreadyPresent);

            choiceAuthorityService.setReferenceWithAuthority(metadata, relatedItem);

        }

    }

    private void addEntityTypeIfNotExist(Context context, Item item) throws SQLException {
        String entityType = itemService.getEntityType(item);
        if (StringUtils.isBlank(entityType)) {
            Collection collection = item.getOwningCollection();
            if (collection != null) {
                String collectionEntityType = collectionService.getEntityType(collection);
                if (StringUtils.isNotBlank(collectionEntityType)) {
                    itemService.addMetadata(context, item, "dspace", "entity", "type", null, collectionEntityType);
                }
            }
        }
    }

    private boolean isMetadataSkippable(MetadataValue metadata) {

        String authority = metadata.getAuthority();

        if (isNestedMetadataPlaceholder(metadata) || isAuthoritySet(authority) || isAuthorityNotAllowed(metadata)) {
            return true;
        }

        if (isBlank(authority) && (isBlank(metadata.getValue()) || isMetadataWithEmptyAuthoritySkippable(metadata))) {
            return true;
        }

        return false;

    }

    private boolean isAuthoritySet(String authority) {
        return isNotBlank(authority) && !isGenerateAuthority(authority) && !isReferenceAuthority(authority);
    }

    private boolean isNestedMetadataPlaceholder(MetadataValue metadata) {
        return StringUtils.equals(metadata.getValue(), CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
    }

    private boolean isGenerateAuthority(String authority) {
        return StringUtils.startsWith(authority, AuthorityValueService.GENERATE);
    }

    private boolean isReferenceAuthority(String authority) {
        return StringUtils.startsWith(authority, AuthorityValueService.REFERENCE);
    }

    private boolean isMetadataWithEmptyAuthoritySkippable(MetadataValue metadata) {

        boolean skipEmptyAuthority = configurationService.getBooleanProperty("cris-consumer.skip-empty-authority");

        if (isMetadataFieldConfiguredToReverseSkipEmptyAuthorityCondition(metadata)) {
            return !skipEmptyAuthority;
        } else {
            return skipEmptyAuthority;
        }

    }

    public boolean isMetadataFieldConfiguredToReverseSkipEmptyAuthorityCondition(MetadataValue metadata) {
        String metadataField = metadata.getMetadataField().toString('.');
        return ArrayUtils.contains(getSkipEmptyAuthorityMetadataFields(), metadataField);
    }

    private String[] getSkipEmptyAuthorityMetadataFields() {
        return configurationService.getArrayProperty("cris-consumer.skip-empty-authority.metadata", new String[] {});
    }

    private boolean isAuthorityNotAllowed(MetadataValue metadataValue) {
        String metadataFieldKey = getFieldKey(metadataValue);
        return !metadataAuthorityService.isAuthorityAllowed(metadataFieldKey, Constants.ITEM, null);
    }

    @Override
    public void end(Context context) throws Exception {
        itemsAlreadyProcessed.clear();
    }

    private String getFieldKey(MetadataValue metadata) {
        return metadata.getMetadataField().toString('_');
    }

    private Item buildRelatedItem(Context context, Item item, Collection collection, MetadataValue metadata,
        String entityType, String crisSourceId) throws Exception {

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, useOfTemplate(metadata));
        Item relatedItem = workspaceItem.getItem();
        itemService.addMetadata(context, relatedItem, CRIS.getName(), "sourceId", null, null, crisSourceId);
        if (!hasEntityType(relatedItem, entityType)) {
            log.error("Inconstent configuration the related item " + relatedItem.getID().toString() + ", created from "
                + item.getID().toString() + " (" + metadata.getMetadataField().toString('.') + ")"
                + " hasn't the expected [" + entityType + "] entityType");
        }

        if (isSubmissionEnabled(metadata)) {
            installItemService.installItem(context, workspaceItem);
        } else {
            workflowService.start(context, workspaceItem).getItem();
        }
        return relatedItem;
    }

    private String generateCrisSourceId(MetadataValue metadata) {
        if (isGenerateAuthority(metadata.getAuthority())) {
            return metadata.getAuthority().substring(AuthorityValueService.GENERATE.length());
        } else if (isReferenceAuthority(metadata.getAuthority())) {
            return metadata.getAuthority().substring(AuthorityValueService.REFERENCE.length());
        } else if (isUuidStrategyEnabled(metadata)) {
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

    private boolean useOfTemplate(MetadataValue value) {

        String useOfTemplateByMetadata = "cris.import.submission.enabled.entity."
                + getFieldKey(value) + ".use-template";
        if (configurationService.hasProperty(useOfTemplateByMetadata)) {
            return configurationService.getBooleanProperty(useOfTemplateByMetadata);
        }

        return configurationService.getBooleanProperty("cris.import.submission.enabled.entity.use-template");
    }

    private void fillRelatedItem(Context context, MetadataValue metadata, Item relatedItem, boolean alreadyPresent)
        throws SQLException {

        AuthorityImportFiller filler = authorityImportFillerService.getAuthorityImportFillerByMetadata(metadata);
        if (filler != null && (!alreadyPresent || filler.allowsUpdate(context, metadata, relatedItem))) {
            filler.fillItem(context, metadata, relatedItem);
        } else if (filler == null && !alreadyPresent) {
            itemService.addMetadata(context, relatedItem, "dc", "title", null, null, metadata.getValue());
        }

    }

    private boolean hasEntityType(DSpaceObject dsObject, String entityType) {
        return dsObject.getMetadata().stream().anyMatch(metadataValue -> {
            return "dspace.entity.type".equals(metadataValue.getMetadataField().toString('.')) &&
                entityType.equals(metadataValue.getValue());
        });
    }

}
