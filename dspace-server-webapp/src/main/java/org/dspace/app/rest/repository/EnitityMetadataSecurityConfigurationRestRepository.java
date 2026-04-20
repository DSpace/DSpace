/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.EntityMetadataSecurityConfigurationRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.metadataSecurity.EntityMetadataSecurityConfiguration;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;



/**
 * REST repository for retrieving entity-specific metadata security configurations.
 * <p>
 * This repository exposes metadata security settings that control which users can view specific
 * metadata fields on items of different entity types. The configuration supports both entity-wide
 * defaults and field-specific overrides.
 * <p>
 * <strong>REST Endpoint:</strong>
 * <pre>
 * GET /api/core/securitysettings/{entityType}
 * </pre>
 * <p>
 * <strong>Example Request:</strong>
 * <pre>
 *
 * // Get security configuration for Person entity
 * GET /api/core/securitysettings/person
 * Response:
 * {
 *   "entityType": "person",
 *   "metadataSecurityDefault": [0, 1, 2],
 *   "metadataCustomSecurity": {
 *     "person.email": [2]
 *   }
 * }
 * </pre>
 * <p>
 * <strong>Response Structure:</strong>
 * <ul>
 *   <li><strong>entityType</strong> - The entity type this configuration applies to</li>
 *   <li><strong>metadataSecurityDefault</strong> - Default security levels for all metadata fields
 *       (array of integers: 0=public, 1=group, 2=admin/owner)</li>
 *   <li><strong>metadataCustomSecurity</strong> - Map of metadata field → security levels for
 *       field-specific overrides</li>
 * </ul>
 *
 * <strong>Configuration Properties:</strong>
 * <pre>
 * # Default security levels for all entity types (fallback)
 * metadatavalue.visibility.settings = [0 1 2]
 *
 * # Entity-specific default (overrides fallback)
 * metadatavalue.visibility.publication.settings = [0 1 2]
 *
 * # Field-specific override (most specific)
 * metadatavalue.visibility.publication.dc.contributor.author.settings = [2]
 * </pre>
 *
 * <strong>Use Cases:</strong>
 * <ul>
 *   <li>REST clients querying what security levels apply to entity metadata</li>
 *   <li>Frontend forms determining which metadata fields have restricted visibility</li>
 *   <li>Administrative tools displaying metadata security configurations per entity</li>
 *   <li>Validation of metadata security settings before saving</li>
 * </ul>
 * <p>
 *
 * @see EntityMetadataSecurityConfiguration
 * @see org.dspace.content.service.MetadataSecurityEvaluation
 * @see org.dspace.content.service.DSpaceObjectService
 */
@Component(EntityMetadataSecurityConfigurationRest.CATEGORY + "."
        + EntityMetadataSecurityConfigurationRest.NAME_PLURAL)
public class EnitityMetadataSecurityConfigurationRestRepository extends
        DSpaceRestRepository<EntityMetadataSecurityConfigurationRest, String> {
    /**
     * Metadata security configuration prefix.
     */
    static final String prefixPropertyBasicFallbackSecurityConfiguration
            = "metadatavalue.visibility";
    /**
     * Metadata security configuration suffix.
     */
    static final String suffixPropertyNameMetadataSecurityConfiguration
            = "settings";
    /**
     * Logger for the class EnitityMetadataSecurityConfigurationRestRepository.
     */
    private static final Logger log = LogManager.getLogger();
    /**
     * Configuration service injection to find metadata security configurations.
     */
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    /**
     * Utils class injection to use default utilities.
     */
    @Autowired
    private Utils utils;
    /**
     * Converter service injection to use convert method.
     */
    @Autowired
    private ConverterService converter;

    /**
     * Retrieves the metadata security configuration for a specific entity type.
     * <p>
     * This method reads configuration properties to determine:
     * <ul>
     *   <li>Default security levels for all metadata fields of this entity type</li>
     *   <li>Field-specific security level overrides</li>
     * </ul>
     * <p>
     * <strong>Configuration Lookup Process:</strong>
     * <ol>
     *   <li>Check {@code metadatavalue.visibility.{entityType}.settings} for entity-specific defaults</li>
     *   <li>If not found, check {@code metadatavalue.visibility.settings} for global defaults</li>
     *   <li>Scan all properties matching {@code metadatavalue.visibility.{entityType}.*} for field overrides</li>
     *   <li>Parse security level arrays from format {@code "[0 1 2]"} to integer lists</li>
     * </ol>
     *
     * <p>
     * <strong>Field-Specific Overrides:</strong>
     * The {@code metadataCustomSecurity} map only includes fields with custom settings that differ
     * from the entity default. Fields using the default security levels are not included in the map.
     * <p>
     * <strong>Missing Configuration:</strong>
     * If no configuration exists for the entity type or globally, the method returns a configuration
     * object with empty/null security levels. This indicates that metadata security is not enforced
     * for the entity type.
     *
     * @param context the DSpace context for database access (unused in this implementation)
     * @param entityType the entity type identifier (e.g., "publication", "person", "project")
     * @return the {@link EntityMetadataSecurityConfigurationRest} containing default and field-specific
     *         security level configurations for the entity type
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public EntityMetadataSecurityConfigurationRest findOne(final Context context, final String entityType) {
        EntityMetadataSecurityConfiguration entityMetadataSecurityConfiguration
            = new EntityMetadataSecurityConfiguration(entityType);
        List<Integer> securityLevelValuesFallBack = new ArrayList<>();
        String entitySecurityConfiguration;
        // find the security levels configuration for the entity type
        if (configurationService.hasProperty(
                prefixPropertyBasicFallbackSecurityConfiguration + "."
                        + entityType + "." + suffixPropertyNameMetadataSecurityConfiguration)) {
            entitySecurityConfiguration = configurationService.getProperty(
                    prefixPropertyBasicFallbackSecurityConfiguration + "."
                    + entityType + "." + suffixPropertyNameMetadataSecurityConfiguration);
        } else {
            // if not found look at the fallback configuration level
            if (configurationService.hasProperty(prefixPropertyBasicFallbackSecurityConfiguration
                    + "." + suffixPropertyNameMetadataSecurityConfiguration
            )) {
                // set as default security configuration level the fallback
                // level found
                entitySecurityConfiguration = configurationService.getProperty(
                        prefixPropertyBasicFallbackSecurityConfiguration
                        + "." + suffixPropertyNameMetadataSecurityConfiguration);
            } else {
                // if neither metadata.visibility found, set as null
                entitySecurityConfiguration = null;
                //  throw new ResourceNotFoundException("No such configuration property: " + entityType);
            }
        }
        try {
            if (entitySecurityConfiguration != null) {
                String listOfSecuritiesAsString = entitySecurityConfiguration.
                        substring(1, entitySecurityConfiguration.length() - 1);
                securityLevelValuesFallBack = Arrays.stream(listOfSecuritiesAsString.split(" "))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error parsing the configuration value of security metadata");
        }
        entityMetadataSecurityConfiguration.setMetadataSecurityDefault(securityLevelValuesFallBack);
        HashMap<String, List<Integer>> metadataCustomSecurity = new HashMap<>();
        Iterator<Map.Entry<Object, Object>> securityValuesIterator = configurationService.getPropertiesWithPrefix(
                prefixPropertyBasicFallbackSecurityConfiguration
                        + "." + entityType).entrySet().iterator();
        while (securityValuesIterator.hasNext()) {
            Map.Entry<Object, Object> next = securityValuesIterator.next();
            List<Integer> securityValueList = new ArrayList<>();
            String value = next.getValue().toString();
            String draftKey =  next.getKey().toString();
            // include only values different from the default configurations
            if (draftKey.equals(suffixPropertyNameMetadataSecurityConfiguration)
                    || Objects.equals(entitySecurityConfiguration, value)) {
                continue;
            }
            try {
                String key = draftKey.substring(0, draftKey.indexOf(".settings"));
                securityValueList = Arrays.stream(value.substring(1, value.length() - 1).split(" "))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                metadataCustomSecurity.put(key, securityValueList);
            } catch (Exception e) {
                log.error("Error parsing the configuration value of security metadata");
            }
        }
        entityMetadataSecurityConfiguration.setMetadataCustomSecurity(metadataCustomSecurity);
        // find security levels based on entity type for all of the metadata configured for the entity
        return converter.toRest(entityMetadataSecurityConfiguration, utils.obtainProjection());
    }

    /**
     * Not implemented - security configurations are always queried by specific entity type.
     * <p>
     * Metadata security configurations are entity-specific and defined in DSpace configuration
     * properties. There is no meaningful way to list "all security configurations" as they depend
     * on runtime configuration and available entity types.
     * <p>
     * Use {@link #findOne(Context, String)} with a specific entity type to retrieve the configuration.
     *
     * @param context the DSpace context (unused)
     * @param pageable pagination parameters (unused)
     * @return never returns normally
     * @throws ResourceNotFoundException always thrown with message "No configurations found"
     */
    @Override
    public Page<EntityMetadataSecurityConfigurationRest> findAll(Context context, Pageable pageable) {
        throw new ResourceNotFoundException("No configurations found");
    }

    /**
     * Returns the domain class managed by this repository.
     * <p>
     * This method is required by the DSpaceRestRepository interface to identify the REST resource
     * type handled by this repository.
     *
     * @return {@link EntityMetadataSecurityConfigurationRest}.class
     */
    @Override
    public Class<EntityMetadataSecurityConfigurationRest> getDomainClass() {
        return EntityMetadataSecurityConfigurationRest.class;
    }
}
