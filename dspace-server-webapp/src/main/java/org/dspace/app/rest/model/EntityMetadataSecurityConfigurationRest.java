/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.RestResourceController;


/**
 * Model class to offer security configuration levels foreach entity type
 *
 * @author Alba Aliu (alba.aliu@atis.al)
 */
public class EntityMetadataSecurityConfigurationRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "securitysetting";
    public static final String NAME_PLURAL = "securitysettings";
    public static final String CATEGORY = RestAddressableModel.CORE;
    /**
     * Default configuration level for entity type
     */
    private List<Integer> metadataSecurityDefault;
    /**
     * List of configuration levels for each metadata of entity type
     */
    private Map<String, List<Integer>> metadataCustomSecurity;

    public String getCategory() {
        return CATEGORY;
    }

    public Class<?> getController() {
        return RestResourceController.class;
    }

    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return NAME_PLURAL;
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

    public EntityMetadataSecurityConfigurationRest() {
    }

    public EntityMetadataSecurityConfigurationRest(List<Integer> metadataSecurityDefault,
                                                   HashMap<String, List<Integer>> metadataCustomSecurity) {
        this.metadataSecurityDefault = metadataSecurityDefault;
        this.metadataCustomSecurity = metadataCustomSecurity;
    }
}
