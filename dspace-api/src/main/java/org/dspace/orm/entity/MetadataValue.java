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
@Table(name = "metadatavalue")
@SequenceGenerator(name="metadatavalue_gen", sequenceName="metadatavalue_seq")
@Configurable
public class MetadataValue extends DSpaceObject {
    private MetadataFieldRegistry metadataField;
    private String textValue;
    private String textLang;
    private Integer place;
    private String authority;
    private Integer confidence;
    private int resource;
    private int resourceType;
    
    
    @Id
    @Column(name = "metadata_value_id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="metadatavalue_gen")
    public int getID() {
        return id;
    }
    
    @Override
    @Transient
    public DSpaceObjectType getType()
    {
    	return DSpaceObjectType.METADATA;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_field_id", nullable = true)
	public MetadataFieldRegistry getMetadataField() {
		return metadataField;
	}

	public void setMetadataField(MetadataFieldRegistry metadataField) {
		this.metadataField = metadataField;
	}


    @Column(name = "text_value", nullable = true)
	public String getTextValue() {
		return textValue;
	}

	public void setTextValue(String textValue) {
		this.textValue = textValue;
	}

    @Column(name = "text_lang", nullable = true)
	public String getTextLang() {
		return textLang;
	}

	public void setTextLang(String textLang) {
		this.textLang = textLang;
	}

    @Column(name = "place", nullable = true)
	public Integer getPlace() {
		return place;
	}

	public void setPlace(Integer place) {
		this.place = place;
	}

    @Column(name = "authority", nullable = true)
	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

    @Column(name = "confidence", nullable = true)
	public Integer getConfidence() {
		return confidence;
	}

	public void setConfidence(Integer confidence) {
		this.confidence = confidence;
	}

    @Column(name = "resource_id", nullable = true)
	public int getResource() {
		return resource;
	}

	public void setResource(int resource) {
		this.resource = resource;
	}

    @Column(name = "resource_type", nullable = true)
	public int getResourceType() {
		return resourceType;
	}

	public void setResourceType(int resourceType) {
		this.resourceType = resourceType;
	}

  
	public String toString () {
		return this.getTextValue();
	}
}
