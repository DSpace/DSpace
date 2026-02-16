/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.customurl.consumer;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class CustomUrlConsumerConfig {

    private static final Logger log = LogManager.getLogger(CustomUrlConsumerConfig.class);

    /**
     * Configuration property for defining supported entity types
     */
    private static final String CONFIG_SUPPORTED_ENTITIES = "dspace.custom-url.consumer.supported-entities";

    /**
     * Configuration property prefix for entity metadata mapping
     */
    private static final String CONFIG_ENTITY_MAPPING_PREFIX = "dspace.custom-url.consumer.entity-metadata-mapping.";

    /**
     * Internal cache mapping entity types to their respective list of metadata fields.
     * This map is rebuilt from the ConfigurationService via the {@link #reload()} method.
     */
    private volatile Map<String, List<String>> entityToMetadataMapping = new HashMap<>();

    private final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
                                                                                   .getConfigurationService();

    public CustomUrlConsumerConfig() {
        reload();
    }

    /**
     * Reload configuration from ConfigurationService and rebuild cache.
     * Thread-safe, atomic replacement.
     */
    public synchronized void reload() {
        entityToMetadataMapping = buildEntityToMetadataFieldsMapping();
    }

    /**
     * Builds a mapping of entity types to their configured metadata fields.
     * This mapping is constructed by reading configuration properties for each supported entity type.
     *
     * @return map where keys are entity type names and values are lists of metadata field names
     */
    private Map<String, List<String>> buildEntityToMetadataFieldsMapping() {
        Map<String, List<String>> mapping = new HashMap<>();
        String[] supportedEntityTypes = configurationService.getArrayProperty(
            CONFIG_SUPPORTED_ENTITIES, new String[0]);

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
        if (isBlank(entityType)) {
            return null;
        }
        return entityType.trim();
    }

    /**
     * Parses metadata field configuration for a specific entity type.
     * Uses getArrayProperty to read comma-separated values from configuration.
     *
     * @param entityType the entity type to get metadata fields for
     * @return list of metadata field names for the entity type
     */
    private List<String> parseMetadataFieldsFromConfiguration(String entityType) {
        String configKey = CONFIG_ENTITY_MAPPING_PREFIX + entityType;
        String[] metadataFieldsArray = configurationService.getArrayProperty(configKey, new String[0]);

        if (metadataFieldsArray.length == 0) {
            log.debug("No metadata field configuration found for entity type '{}' at key '{}'",
                      entityType, configKey);
            return List.of();
        }

        return parseMetadataFieldList(metadataFieldsArray);
    }

    /**
     * Parses an array of metadata field names, filtering out blank values.
     *
     * @param metadataFieldsArray the configuration array containing field names
     * @return list of trimmed, non-empty metadata field names
     */
    private List<String> parseMetadataFieldList(String[] metadataFieldsArray) {
        return Arrays.stream(metadataFieldsArray)
                     .map(String::trim)
                     .filter(field -> !isBlank(field))
                     .collect(Collectors.toList());
    }

    /**
     * Gets the configured metadata fields for a specific entity type.
     *
     * @param entityType the entity type to get metadata fields for
     * @return list of metadata field names, empty if none configured
     */
    public List<String> getConfiguredMetadataFieldsForEntity(String entityType) {
        return entityToMetadataMapping.getOrDefault(entityType, List.of());
    }

    /**
     * Gets the array of supported entity types from configuration.
     *
     * @return array of supported entity type names
     */
    public String[] getSupportedEntityTypes() {
        return configurationService.getArrayProperty(CONFIG_SUPPORTED_ENTITIES, new String[0]);
    }

}
