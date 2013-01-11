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
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */


@Entity
@Table(name = "versionitem")
@SequenceGenerator(name="versionitem_gen", sequenceName="versionitem_seq")
@Configurable
public class VersionItem extends DSpaceObject {
    private Item item;
    private Eperson eperson;
    private Integer versionNumber;
    private Date versionDate;
    private String versionSummary;
    private Integer versionhistory_id;
    
    @Id
    @Column(name = "versionitem_id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="versionitem_gen")
    public int getID() {
        return id;
    }
    
    
    @Override
    @Transient
    public DSpaceObjectType getType()
    {
    	return DSpaceObjectType.VERSION_ITEM;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = true)
	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_id", nullable = true)
	public Eperson getEperson() {
		return eperson;
	}

	public void setEperson(Eperson owner) {
		this.eperson = owner;
	}


    @Column(name = "version_number", nullable = true)
	public Integer getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(Integer versionNumber) {
		this.versionNumber = versionNumber;
	}

    @Column(name = "version_date", nullable = true)
	public Date getVersionDate() {
		return versionDate;
	}

	public void setVersionDate(Date versionDate) {
		this.versionDate = versionDate;
	}


    @Column(name = "version_summary", nullable = true)
	public String getVersionSummary() {
		return versionSummary;
	}

	public void setVersionSummary(String versionSummary) {
		this.versionSummary = versionSummary;
	}

    @Column(name = "versionhistory_id", nullable = true)
	public Integer getVersionhistory_id() {
		return versionhistory_id;
	}

	public void setVersionhistory_id(Integer versionhistory_id) {
		this.versionhistory_id = versionhistory_id;
	}
}
