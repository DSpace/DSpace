/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metadataSecurity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration class to offer security configuration levels foreach entity type
 *
 * @author Alba Aliu (alba.aliu@atis.al)
 */
public class EntityMetadataSecurityConfiguration {

    /**
     * The entity type
     */
    private String entityType;

    /**
     * Default configuration level for entity type
     */
    private List<Integer> metadataSecurityDefault;

    /**
     * List of configuration levels for each metadata of entity type
     */
    private Map<String, List<Integer>> metadataCustomSecurity;

    public EntityMetadataSecurityConfiguration(String entityType) {
        this.entityType = entityType;
        this.metadataSecurityDefault = new ArrayList<>();
        this.metadataCustomSecurity = new HashMap<>();
    }

    public List<Integer> getMetadataSecurityDefault() {
        return metadataSecurityDefault;
    }

    public void setMetadataSecurityDefault(List<Integer> metadataSecurityDefault) {
        this.metadataSecurityDefault = metadataSecurityDefault;
    }

    public Map<String, List<Integer>> getMetadataCustomSecurity() {
        return metadataCustomSecurity;
    }

    public void setMetadataCustomSecurity(Map<String, List<Integer>> metadataCustomSecurity) {
        this.metadataCustomSecurity = metadataCustomSecurity;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
}
