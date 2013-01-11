/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.orm.entity.content.DSpaceObjectType;
import org.springframework.beans.factory.annotation.Configurable;

@Entity
@Table(name = "bitstreamformatregistry")
@Configurable
@SequenceGenerator(name="bitstreamformatregistry_gen", sequenceName="bitstreamformatregistry_seq")
public class BitstreamFormat extends DSpaceObject {
    private String mimetype;
    private String shortDescription;
    private String description;
    private Integer supportLevel;
    private boolean internal;

    @Id
    @Column(name = "bitstream_format_id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="bitstreamformatregistry_gen")
    public int getID() {
        return id;
    }

    @Column(name = "mimetype", unique = true)
    public String getMimetype() {
        return mimetype;
    }

    @Column(name = "short_description", nullable = true)
    public String getShortDescription() {
        return shortDescription;
    }

    @Column(name = "description", nullable = true)
    public String getDescription() {
        return description;
    }

    @Column(name = "support_level")
    public Integer getSupportLevel() {
        return supportLevel;
    }

    @Column(name = "internal")
    public boolean isInternal() {
        return internal;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSupportLevel(Integer supportLevel) {
        this.supportLevel = supportLevel;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

	@Override
	@Transient
	public DSpaceObjectType getType() {
		return DSpaceObjectType.BITSTREAM_FORMAT;
	}
}
