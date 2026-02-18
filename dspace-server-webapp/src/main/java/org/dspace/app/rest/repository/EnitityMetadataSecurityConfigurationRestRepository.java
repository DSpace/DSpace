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
 * This is the repository that is responsible to manage
 * EntityMetadataSecurityConfiguration Rest objects.
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

    @Override
    public Page<EntityMetadataSecurityConfigurationRest> findAll(Context context, Pageable pageable) {
        throw new ResourceNotFoundException("No configurations found");
    }

    @Override
    public Class<EntityMetadataSecurityConfigurationRest> getDomainClass() {
        return EntityMetadataSecurityConfigurationRest.class;
    }
}
