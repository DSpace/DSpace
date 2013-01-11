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
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.orm.entity.content.DSpaceObjectType;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */


@Entity
@Table(name = "harvested_item")
@SequenceGenerator(name="harvested_item_gen", sequenceName="harvested_item_seq")
public class HarvestedItem extends DSpaceObject{
    private int id;
    private Item item;
    private Date lastHarvested;
    private String oai;
    
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="harvested_item_gen")
    public int getID() {
        return id;
    }
    
    @Override
    @Transient
    public DSpaceObjectType getType()
    {
    	return DSpaceObjectType.HARVESTED_ITEM;
    }

    @Column(name = "oai_id", nullable = true)
	public String getOai() {
		return oai;
	}

	public void setOai(String oai) {
		this.oai = oai;
	}

    @Column(name = "last_harvested", nullable = true)
	public Date getLastHarvested() {
		return lastHarvested;
	}

	public void setLastHarvested(Date lastHarvested) {
		this.lastHarvested = lastHarvested;
	}

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = true)
	public Item getItem() {
		return item;
	}

	public void setItem (Item i) {
		this.item = i;
	}
}
