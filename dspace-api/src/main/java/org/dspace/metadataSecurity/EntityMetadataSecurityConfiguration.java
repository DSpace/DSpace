/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metadataSecurity;

import java.util.HashMap;
import java.util.List;

/**
 * Configuration class to offer security configuration levels foreach entity type
 *
 * @author Alba Aliu (alba.aliu@atis.al)
 */
public class EntityMetadataSecurityConfiguration {
    /**
     * Default configuration level for entity type
     */
    private List<Integer> metadataSecurityDefault;
    /**
     * List of configuration levels for each metadata of entity type
     */
    private HashMap<String, List<Integer>> metadataCustomSecurity;

    public EntityMetadataSecurityConfiguration(List<Integer> metadataSecurityDefault,
                                               HashMap<String, List<Integer>> metadataCustomSecurity) {
        this.metadataSecurityDefault = metadataSecurityDefault;
        this.metadataCustomSecurity = metadataCustomSecurity;
    }

    public EntityMetadataSecurityConfiguration() {
    }

    public List<Integer> getMetadataSecurityDefault() {
        return metadataSecurityDefault;
    }

    public void setMetadataSecurityDefault(List<Integer> metadataSecurityDefault) {
        this.metadataSecurityDefault = metadataSecurityDefault;
    }

    public HashMap<String, List<Integer>> getMetadataCustomSecurity() {
        return metadataCustomSecurity;
    }

    public void setMetadataCustomSecurity(HashMap<String, List<Integer>> metadataCustomSecurity) {
        this.metadataCustomSecurity = metadataCustomSecurity;
    }
}
