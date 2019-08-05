/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.*;
import org.dspace.app.rest.CollectionHarvestSettingsController;
import org.dspace.content.Collection;
import org.dspace.harvest.HarvestedCollection;

import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * The HarvestCollection REST Resource
 *
 * @author Jelle Pelgrims (jelle.atmire at gmail.com)
 */
public class HarvestedCollectionRest extends BaseObjectRest<Integer> {

    public static final String NAME = "harvestedCollection";
    public static final String CATEGORY = "core";

    @JsonProperty("harvest_type")
    private HarvestedTypeEnum harvestType;

    @JsonProperty("oai_source")
    private String oaiSource;

    @JsonProperty("oai_set_id")
    private String oaiSetId;

    @JsonProperty("metadata_config_id")
    private String metadataConfigId;

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return CollectionHarvestSettingsController.class;
    }

    public String getType() {
        return NAME;
    }

    private Integer id;
    private Collection collection;

    public int getHarvestType() {
        return harvestType.ordinal();
    }

    public void setHarvestType(HarvestedTypeEnum harvestType) {
        this.harvestType = harvestType;
    }

    public String getOaiSource() {
        return oaiSource;
    }

    public void setOaiSource(String oaiSource) {
        this.oaiSource = oaiSource;
    }

    public String getOaiSetId() {
        return oaiSetId;
    }

    public void setOaiSetId(String oaiSetId) {
        this.oaiSetId = oaiSetId;
    }

    public String getMetadataConfigId() {
        return metadataConfigId;
    }

    public void setMetadataConfigId(String metadataConfigId) {
        this.metadataConfigId = metadataConfigId;
    }
}
