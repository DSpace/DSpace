/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.HarvesterMetadataController;

/**
 * The rest resource used for harvester metadata
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
public class HarvesterMetadataRest extends BaseObjectRest {

    public static final String CATEGORY = "config";
    public static final String NAME = "harvesterMetadata";

    private List<Map<String,String>> configs;

    @Override
    @JsonIgnore
    public Serializable getId() {
        return id;
    }

    @JsonIgnore
    public String getCategory() {
        return CATEGORY;
    }

    @JsonIgnore
    public String getType() {
        return NAME;
    }


    public Class getController() {
        return HarvesterMetadataController.class;
    }

    public List<Map<String,String>> getConfigs() {
        return configs;
    }

    public void setConfigs(List<Map<String,String>> configs) {
        this.configs = configs;
    }

}
