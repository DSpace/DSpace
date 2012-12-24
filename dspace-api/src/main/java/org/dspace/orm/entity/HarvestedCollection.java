/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.dspace.core.Constants;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */


@Entity
@Table(name = "harvested_collection")
public class HarvestedCollection extends DSpaceObject{
    private int id;
    private Collection collection;
    private Integer harvestType;
    private String oaiSource;

    private Integer oaiSet;
    private String harvestMessage;

    private Integer harvestStatus;
    private String metadataConfig;
    
    private Date harvestStartTime;
    private Date lastHarvested;
    
    
    @Id
    @Column(name = "id")
    @GeneratedValue
    public int getID() {
        return id;
    }
    
    public int setID(int id) {
        return this.id= id;
    }
    
    @Override
    @Transient
    public int getType()
    {
    	return Constants.HARVESTEDITEM;
    }

    @Column(name = "last_harvested", nullable = true)
	public Date getLastHarvested() {
		return lastHarvested;
	}

	public void setLastHarvested(Date lastHarvested) {
		this.lastHarvested = lastHarvested;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = true)
	public Collection getCollection() {
		return collection;
	}

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

	 @Column(name = "harvest_start_time", nullable = true)
	public Date getHarvestStartTime() {
		return harvestStartTime;
	}

	public void setHarvestStartTime(Date harvestStartTime) {
		this.harvestStartTime = harvestStartTime;
	}

	@Column(name = "harvest_type", nullable = true)
	public Integer getHarvestType() {
		return harvestType;
	}

	public void setHarvestType(Integer harvestType) {
		this.harvestType = harvestType;
	}

	@Column(name = "oai_source", nullable = true)
	public String getOaiSource() {
		return oaiSource;
	}

	public void setOaiSource(String oaiSource) {
		this.oaiSource = oaiSource;
	}

	@Column(name = "oai_set_id", nullable = true)
	public Integer getOaiSet() {
		return oaiSet;
	}

	public void setOaiSet(Integer oaiSet) {
		this.oaiSet = oaiSet;
	}

	@Column(name = "harvest_message", nullable = true)
	public String getHarvestMessage() {
		return harvestMessage;
	}

	public void setHarvestMessage(String harvestMessage) {
		this.harvestMessage = harvestMessage;
	}

	@Column(name = "metadata_config_id", nullable = true)
	public Integer getHarvestStatus() {
		return harvestStatus;
	}

	public void setHarvestStatus(Integer harvestStatus) {
		this.harvestStatus = harvestStatus;
	}

	@Column(name = "harvest_status", nullable = true)
	public String getMetadataConfig() {
		return metadataConfig;
	}

	public void setMetadataConfig(String metadataConfig) {
		this.metadataConfig = metadataConfig;
	}
	
}
