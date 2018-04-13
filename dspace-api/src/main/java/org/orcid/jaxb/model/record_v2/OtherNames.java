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

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType( propOrder = { "lastModifiedDate", "otherNames" })
@XmlRootElement(name = "other-names", namespace = "http://www.orcid.org/ns/other-name")
public class OtherNames implements Serializable {        
    private static final long serialVersionUID = 6312730308815255894L;
    
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "last-modified-date")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(name = "other-name", namespace = "http://www.orcid.org/ns/other-name")
    List<OtherName> otherNames;

    @XmlAttribute
    protected String path;
    
    public List<OtherName> getOtherNames() {
        if(otherNames == null) {
            otherNames = new ArrayList<>();
         }
        return otherNames;
    }

    public void setOtherNames(List<OtherName> otherNames) {
        this.otherNames = otherNames;
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
        result = prime * result + ((otherNames == null) ? 0 : otherNames.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OtherNames other = (OtherNames) obj;
        if (otherNames == null) {
            if (other.otherNames != null)
                return false;
        } else if (!otherNames.equals(other.otherNames))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }	 
        
	public LastModifiedDate getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(LastModifiedDate lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
}
