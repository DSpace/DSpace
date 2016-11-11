/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Alexey Maslov
 */

@Entity
@Table(name="harvested_collection")
public class HarvestedCollection implements ReloadableEntity<Integer>
{
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="harvested_collection_seq")
    @SequenceGenerator(name="harvested_collection_seq", sequenceName="harvested_collection_seq", allocationSize = 1)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @Column(name = "harvest_type")
    private int harvestType;

    @Column(name = "oai_source")
    private String oaiSource;

    @Column(name = "oai_set_id")
    private String oaiSetId;

    @Column(name = "harvest_message")
    private String harvestMessage;

    @Column(name = "metadata_config_id")
    private String metadataConfigId;

    @Column(name = "harvest_status")
    private int harvestStatus;

    @Column(name = "harvest_start_time", columnDefinition="timestamp with time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date harvestStartTime;

    @Column(name = "last_harvested", columnDefinition="timestamp with time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastHarvested;

    @Transient
    public static final int TYPE_NONE = 0;
    @Transient
    public static final int TYPE_DMD = 1;
    @Transient
    public static final int TYPE_DMDREF = 2;
    @Transient
    public static final int TYPE_FULL = 3;

    @Transient
    public static final int STATUS_READY = 0;
    @Transient
    public static final int STATUS_BUSY = 1;
    @Transient
    public static final int STATUS_QUEUED = 2;
    @Transient
    public static final int STATUS_OAI_ERROR = 3;
    @Transient
    public static final int STATUS_UNKNOWN_ERROR = -1;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.harvest.service.HarvestedCollectionService#create(Context, Collection)}
     *
     */
    protected HarvestedCollection()
    {
    }

    public Integer getID() {
        return id;
    }

    /** 
     * A function to set all harvesting-related parameters at once 
     * @param type
     *     harvest type (TYPE_NONE, TYPE_DMD, TYPE_DMDREF, TYPE_FULL)
     * @param oaiSource
     *     base URL of the OAI-PMH server
     * @param oaiSetId
     *     OAI set identifier
     * @param mdConfigId
     *     harvest metadata config ID
     */
    public void setHarvestParams(int type, String oaiSource, String oaiSetId, String mdConfigId) {
        setHarvestType(type);
        setOaiSource(oaiSource);
        setOaiSetId(oaiSetId); 
        setHarvestMetadataConfig(mdConfigId);
    }     

    /*
     * Setters for the appropriate harvesting-related columns
     *
     * @param type
     *     harvest type (TYPE_NONE, TYPE_DMD, TYPE_DMDREF, TYPE_FULL
     */
    public void setHarvestType(int type) {
        this.harvestType = type;
    }
    
    /** 
     * Sets the current status of the collection.
     *    
     * @param    status    a HarvestInstance.STATUS_... constant (STATUS_READY, STATUS_BUSY, STATUS_QUEUED, STATUS_OAI_ERROR, STATUS_UNKNOWN_ERROR)
     */
    public void setHarvestStatus(int status) {
        this.harvestStatus = status;
    }

    /** 
     * Sets the base URL of the OAI-PMH server.
     *    
     * @param oaiSource base URL of the OAI-PMH server
     *
     */
    public void setOaiSource(String oaiSource) {
        this.oaiSource = oaiSource;
    }

    /** 
     * Sets the OAI set to harvest.
     *    
     * @param oaiSetId OAI set to harvest
     *
     */
    public void setOaiSetId(String oaiSetId) {
        this.oaiSetId = oaiSetId;
    }

    public void setHarvestMetadataConfig(String mdConfigId) {
        this.metadataConfigId = mdConfigId;
    }

    public void setLastHarvested(Date lastHarvested) {
        this.lastHarvested = lastHarvested;
    }

    public void setHarvestMessage(String message) {
        this.harvestMessage = message;
    }
    
    public void setHarvestStartTime(Date date) {
        this.harvestStartTime = date;
    }
    

    /* Getting for the appropriate harvesting-related columns */
    public Collection getCollection() {
        return collection;
    }

    void setCollection(Collection collection) {
        this.collection = collection;
    }

    public int getHarvestType() {
        return harvestType;
    }
    
    public int getHarvestStatus() {
        return harvestStatus;
    }

    public String getOaiSource() {
        return oaiSource;
    }

    public String getOaiSetId() {
        return oaiSetId;
    }

    public String getHarvestMetadataConfig() {
        return metadataConfigId;
    }
    
    public String getHarvestMessage() {
        return harvestMessage;
    }

    public Date getHarvestDate() {
        return lastHarvested;
    }
    
    public Date getHarvestStartTime() {
        return harvestStartTime;
    }
}
