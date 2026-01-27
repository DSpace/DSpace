/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.customurl.consumer;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.versioning.VersionHistoryServiceImpl;

/**
 * Consumer that automatically generates and assigns custom URLs to DSpace items
 * based on their entity type and configured metadata fields.
 * <p>
 * This consumer processes items during installation or modification events and generates
 * a custom URL by concatenating values from specified metadata fields. The custom URL
 * is normalized by removing accents, converting to lowercase, and replacing spaces with hyphens.
 * </p>
 * <p>
 * Configuration is controlled by two main properties:
 * <ul>
 *   <li>{@code dspace.custom-url.consumer.supported-entities}: Defines which entity types will have
 *       custom URLs generated</li>
 *   <li>{@code dspace.custom-url.consumer.entity-metadata-mapping.{EntityType}}: Defines metadata fields
 *       to use for each entity type</li>
 * </ul>
 * </p>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CustomUrlConsumer implements Consumer {

    private static final Logger log = LogManager.getLogger(CustomUrlConsumer.class);

    /**
     * Configuration property for defining supported entity types
     */
    private static final String CONFIG_SUPPORTED_ENTITIES = "dspace.custom-url.consumer.supported-entities";

    /**
     * Configuration property prefix for entity metadata mapping
     */
    private static final String CONFIG_ENTITY_MAPPING_PREFIX = "dspace.custom-url.consumer.entity-metadata-mapping.";

    /**
     * Separator for multiple metadata fields in configuration
     */
    private static final String METADATA_FIELD_SEPARATOR = ";";

    /**
     * Set to track items already processed in current transaction to avoid duplicates
     */
    private final Set<Item> itemsAlreadyProcessed = new HashSet<>();

    private Map<String, List<String>> entityToMetadataMapping = new HashMap<>();

    private ItemService itemService;
    private CustomUrlService customUrlService;
    private ConfigurationService configurationService;
    private VersionHistoryServiceImpl versionHistoryService;

    /**
     * Initializes the consumer by setting up required services.
     *
     * @throws Exception if initialization fails
     */
    @Override
    public void initialize() throws Exception {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.customUrlService = new DSpace().getSingletonService(CustomUrlService.class);
        this.versionHistoryService = new DSpace().getSingletonService(VersionHistoryServiceImpl.class);
        this.entityToMetadataMapping = buildEntityToMetadataFieldsMapping();
    }

    /**
     * Called when consumer processing is finished. No specific cleanup required.
     *
     * @param ctx DSpace context
     * @throws Exception if an error occurs
     */
    @Override
    public void finish(Context ctx) throws Exception {
        // No cleanup required
    }

    /**
     * Processes an event to potentially generate a custom URL for an item.
     * Only processes archived items that haven't been processed in this transaction.
     *
     * @param context DSpace context
     * @param event   The event containing the item to process
     * @throws Exception if processing fails
     */
    @Override
    public void consume(Context context, Event event) throws Exception {
        Item item = (Item) event.getSubject(context);

        if (!shouldProcessItem(item) || !isItemLastVersion(context, item)) {
            return;
        }

        markItemAsProcessed(item);

        context.turnOffAuthorisationSystem();
        try {
            processItemForCustomUrl(context, item);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /**
     * Checks whether the specified item is the latest version in its version history.
     *
     * @param context DSpace context
     * @param item    the item to check
     * @return true if the item is the last version, false otherwise
     */
    private boolean isItemLastVersion(Context context, Item item) throws SQLException {
        return versionHistoryService.isLastVersion(context, item);
    }

    /**
     * Checks whether an item should be processed for custom URL generation.
     *
     * @param item the item to check
     * @return true if the item should be processed, false otherwise
     */
    private boolean shouldProcessItem(Item item) {
        return item != null
            && !itemsAlreadyProcessed.contains(item)
            && item.isArchived();
    }

    /**
     * Marks an item as already processed in this transaction to prevent duplicate processing.
     *
     * @param item the item to mark as processed
     */
    private void markItemAsProcessed(Item item) {
        itemsAlreadyProcessed.add(item);
    }

    /**
     * Main processing logic for generating and assigning a custom URL to an item.
     * Checks if the item's entity type is supported and if it doesn't already have a custom URL.
     *
     * @param context DSpace context
     * @param item    the item to process
     */
    private void processItemForCustomUrl(Context context, Item item) {
        if (!isEntityTypeSupported(item)) {
            log.debug("Skipping custom URL generation for item {} - entity type not supported",
                      item.getID());
            return;
        }

        if (hasExistingCustomUrl(item)) {
            log.debug("Skipping custom URL generation for item {} - already has custom URL",
                      item.getID());
            return;
        }

        generateCustomUrlForItem(context, item)
            .ifPresent(customUrl -> assignCustomUrlToItem(context, item, customUrl));
    }

    /**
     * Checks if the item's entity type is supported for custom URL generation.
     *
     * @param item the item to check
     * @return true if the entity type is supported, false otherwise
     */
    private boolean isEntityTypeSupported(Item item) {
        String entityType = getItemEntityType(item);
        return entityType != null && contains(getSupportedEntityTypes(), entityType);
    }

    /**
     * Gets the entity type label for an item.
     *
     * @param item the item
     * @return the entity type label or null if not found
     */
    private String getItemEntityType(Item item) {
        return itemService.getEntityTypeLabel(item);
    }

    /**
     * Checks if an item already has a custom URL assigned.
     *
     * @param item the item to check
     * @return true if the item has a custom URL, false otherwise
     */
    private boolean hasExistingCustomUrl(Item item) {
        return customUrlService.getCustomUrl(item).isPresent();
    }

    /**
     * Generates a custom URL for an item based on its entity type and configured metadata fields.
     * Concatenates values from all configured metadata fields and normalizes the result.
     * If the generated URL already exists, creates a progressive version (e.g., "base-url-2").
     *
     * @param context the DSpace context
     * @param item    the item to generate a custom URL for
     * @return an Optional containing the generated custom URL, or empty if generation fails
     */
    private Optional<String> generateCustomUrlForItem(Context context, Item item) {
        String entityType = getItemEntityType(item);
        if (entityType == null) {
            log.debug("Cannot generate custom URL for item {} - no entity type found", item.getID());
            return Optional.empty();
        }

        List<String> metadataFields = getConfiguredMetadataFieldsForEntity(entityType);
        if (metadataFields.isEmpty()) {
            log.debug("Cannot generate custom URL for item {} - no metadata fields configured for entity type '{}'",
                      item.getID(), entityType);
            return Optional.empty();
        }

        String concatenatedValues = extractAndConcatenateMetadataValues(item, metadataFields);
        if (isBlank(concatenatedValues)) {
            log.debug("Cannot generate custom URL for item {} - no values found in configured metadata fields",
                      item.getID());
            return Optional.empty();
        }

        // Check if the generated URL already exists and generate a progressive version if needed
        String finalUrl = customUrlService.generateUniqueCustomUrl(context, concatenatedValues);

        log.debug("Generated custom URL '{}' for item {}", finalUrl, item.getID());
        return Optional.of(finalUrl);
    }

    /**
     * Extracts values from specified metadata fields and concatenates them with spaces.
     *
     * @param item           the item to extract metadata from
     * @param metadataFields the list of metadata field names to extract
     * @return concatenated metadata values separated by spaces
     */
    private String extractAndConcatenateMetadataValues(Item item, List<String> metadataFields) {
        StringBuilder valueBuilder = new StringBuilder();

        for (String metadataFieldName : metadataFields) {
            String metadataValue = extractMetadataValue(item, metadataFieldName);

            if (!isBlank(metadataValue)) {
                if (!valueBuilder.isEmpty()) {
                    valueBuilder.append(" ");
                }
                valueBuilder.append(metadataValue);
            }
        }

        return valueBuilder.toString();
    }

    /**
     * Extracts the first value of a specified metadata field from an item.
     *
     * @param item              the item to extract metadata from
     * @param metadataFieldName the name of the metadata field (e.g., "dc.title" or "person.familyName")
     * @return the metadata value or null if not found or invalid field format
     */
    private String extractMetadataValue(Item item, String metadataFieldName) {
        try {
            MetadataFieldName fieldName = new MetadataFieldName(metadataFieldName);
            return itemService.getMetadataFirstValue(item, fieldName, Item.ANY);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid metadata field format '{}' - skipping", metadataFieldName);
            return null;
        }
    }

    /**
     * Assigns a custom URL to an item using the CustomUrlService.
     *
     * @param context   DSpace context
     * @param item      the item to assign the URL to
     * @param customUrl the custom URL to assign
     */
    private void assignCustomUrlToItem(Context context, Item item, String customUrl) {
        log.info("Assigning custom URL '{}' to item {}", customUrl, item.getID());
        customUrlService.replaceCustomUrl(context, item, customUrl);
    }



    /**
     * Gets the array of supported entity types from configuration.
     *
     * @return array of supported entity type names
     */
    private String[] getSupportedEntityTypes() {
        return configurationService.getArrayProperty(CONFIG_SUPPORTED_ENTITIES, new String[0]);
    }

    /**
     * Builds a mapping of entity types to their configured metadata fields.
     * This mapping is constructed by reading configuration properties for each supported entity type.
     *
     * @return map where keys are entity type names and values are lists of metadata field names
     */
    private Map<String, List<String>> buildEntityToMetadataFieldsMapping() {
        Map<String, List<String>> mapping = new HashMap<>();
        String[] supportedEntityTypes = getSupportedEntityTypes();

        for (String entityType : supportedEntityTypes) {
            String trimmedEntityType = getTrimmedEntityType(entityType);
            if (trimmedEntityType == null) {
                continue;
            }

            List<String> metadataFields = parseMetadataFieldsFromConfiguration(trimmedEntityType);
            if (!metadataFields.isEmpty()) {
                mapping.put(trimmedEntityType, metadataFields);
            }
        }

        return mapping;
    }

    /**
     * Gets and trims an entity type, returning null if it's null or empty after trimming.
     *
     * @param entityType the raw entity type string
     * @return trimmed entity type or null if invalid
     */
    private String getTrimmedEntityType(String entityType) {
        if (entityType == null) {
            return null;
        }

        String trimmed = entityType.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Parses metadata field configuration for a specific entity type.
     *
     * @param entityType the entity type to get metadata fields for
     * @return list of metadata field names for the entity type
     */
    private List<String> parseMetadataFieldsFromConfiguration(String entityType) {
        String configKey = CONFIG_ENTITY_MAPPING_PREFIX + entityType;
        String metadataFieldsConfig = configurationService.getProperty(configKey);

        if (isBlank(metadataFieldsConfig)) {
            log.debug("No metadata field configuration found for entity type '{}' at key '{}'",
                      entityType, configKey);
            return List.of();
        }

        return parseMetadataFieldList(metadataFieldsConfig);
    }

    /**
     * Parses a semicolon-separated list of metadata field names.
     *
     * @param metadataFieldsConfig the configuration string containing field names
     * @return list of trimmed, non-empty metadata field names
     */
    private List<String> parseMetadataFieldList(String metadataFieldsConfig) {
        String[] fieldArray = metadataFieldsConfig.split(METADATA_FIELD_SEPARATOR);
        List<String> fields = new ArrayList<>();

        for (String field : fieldArray) {
            String trimmedField = field.trim();
            if (!trimmedField.isEmpty()) {
                fields.add(trimmedField);
            }
        }

        return fields;
    }

    /**
     * Gets the configured metadata fields for a specific entity type.
     *
     * @param entityType the entity type to get metadata fields for
     * @return list of metadata field names, empty if none configured
     */
    private List<String> getConfiguredMetadataFieldsForEntity(String entityType) {
        return entityToMetadataMapping.getOrDefault(entityType, List.of());
    }

    /**
     * Clears the set of processed items when the consumer transaction ends.
     *
     * @param ctx DSpace context
     * @throws Exception if an error occurs
     */
    @Override
    public void end(Context ctx) throws Exception {
        itemsAlreadyProcessed.clear();
    }
}
