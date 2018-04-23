/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * 
 * http://www.dspace.org/license/
 */
/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.jaxb.model.record_v2;

import org.orcid.jaxb.model.common_v2.LastModifiedDate;
import org.orcid.jaxb.model.record.util.RecordUtil;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "lastModifiedDate", "name", "otherNames", "biography" })
@XmlRootElement(name = "personal-details", namespace = "http://www.orcid.org/ns/personal-details")
public class PersonalDetails implements Serializable {
    private static final long serialVersionUID = 8496158434601501884L;
    
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "last-modified-date")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(name = "name", namespace = "http://www.orcid.org/ns/personal-details")
    private Name name;
    @XmlElement(name = "other-names", namespace = "http://www.orcid.org/ns/other-name")
    private OtherNames otherNames;
    @XmlElement(name = "biography", namespace = "http://www.orcid.org/ns/personal-details")
    private Biography biography;
    @XmlAttribute
    protected String path;
    
    
    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public OtherNames getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(OtherNames otherNames) {
        this.otherNames = otherNames;
    }

    public Biography getBiography() {
        return biography;
    }

    public void setBiography(Biography biography) {
        this.biography = biography;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((biography == null) ? 0 : biography.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((otherNames == null) ? 0 : otherNames.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PersonalDetails other = (PersonalDetails) obj;
        if (biography == null) {
            if (other.biography != null) {
                return false;
            }
        } else if (!biography.equals(other.biography)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (otherNames == null) {
            if (other.otherNames != null) {
                return false;
            }
        } else if (!otherNames.equals(other.otherNames)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return RecordUtil.convertToString(this);
    }
    
	public LastModifiedDate getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(LastModifiedDate lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
}
