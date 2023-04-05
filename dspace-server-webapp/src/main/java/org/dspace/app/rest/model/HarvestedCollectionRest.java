/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The HarvestCollection REST Resource
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
public class HarvestedCollectionRest extends BaseObjectRest<Integer> {

    public static final String NAME = "collections";
    public static final String CATEGORY = "core";

    @JsonProperty("harvest_type")
    private HarvestTypeEnum harvestType;

    @JsonProperty("oai_source")
    private String oaiSource;

    @JsonProperty("oai_set_id")
    private String oaiSetId;

    @JsonProperty("harvest_message")
    private String harvestMessage;

    @JsonProperty("metadata_config_id")
    private String metadataConfigId;

    @JsonProperty("harvest_status")
    private HarvestStatusEnum harvestStatus;

    @JsonProperty("harvest_start_time")
    private Date harvestStartTime;

    @JsonProperty("last_harvested")
    private Date lastHarvested;

    private HarvesterMetadataRest metadata_configs;

    private CollectionRest collectionRest;

    @JsonIgnore
    @Override
    public Integer getId() {
        return id;
    }

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return HarvestedCollectionRest.class;
    }

    @JsonIgnore
    public String getType() {
        return NAME;
    }

    @JsonIgnore
    public CollectionRest getCollectionRest() {
        return this.collectionRest;
    }

    public void setCollection(CollectionRest collectionRest) {
        this.collectionRest = collectionRest;
    }

    @JsonIgnore
    public int getHarvestType() {
        return harvestType.ordinal();
    }

    @JsonGetter("harvest_type")
    public String getHarvestTypeAsString() {
        return harvestType.name();
    }

    public void setHarvestType(HarvestTypeEnum harvestType) {
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


    public String getHarvestMessage() {
        return harvestMessage;
    }

    public void setHarvestMessage(String harvestMessage) {
        this.harvestMessage = harvestMessage;
    }

    @JsonIgnore
    public HarvestStatusEnum getHarvestStatus() {
        return harvestStatus;
    }

    @JsonGetter("harvest_status")
    public String getHarvestStatusAsString() {
        return harvestStatus == null ? null : harvestStatus.name();
    }

    public void setHarvestStatus(HarvestStatusEnum harvestStatus) {
        this.harvestStatus = harvestStatus;
    }

    public Date getHarvestStartTime() {
        return harvestStartTime;
    }

    public void setHarvestStartTime(Date harvestStartTime) {
        this.harvestStartTime = harvestStartTime;
    }

    public Date getLastHarvested() {
        return lastHarvested;
    }

    public void setLastHarvested(Date lastHarvested) {
        this.lastHarvested = lastHarvested;
    }

    @LinkRest(name = "harvestermetadata")
    @JsonIgnore
    public HarvesterMetadataRest getMetadataConfigs() {
        return metadata_configs;
    }

    public void setMetadataConfigs(HarvesterMetadataRest metadata_configs) {
        this.metadata_configs = metadata_configs;
    }


}
