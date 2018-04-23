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

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "lastModifiedDate", "externalIdentifiers" })
@XmlRootElement(name = "external-identifiers", namespace = "http://www.orcid.org/ns/external-identifier")
public class PersonExternalIdentifiers implements Serializable {
    private static final long serialVersionUID = -9182106466010694574L;
    
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "last-modified-date")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(name = "external-identifier", namespace = "http://www.orcid.org/ns/external-identifier")
    List<PersonExternalIdentifier> externalIdentifiers;
    @XmlAttribute
    protected String path;

    public List<PersonExternalIdentifier> getExternalIdentifiers() {
        if(externalIdentifiers == null) {
           externalIdentifiers = new ArrayList<>();
        }
        return externalIdentifiers;
    }

    public void setExternalIdentifiers(List<PersonExternalIdentifier> externalIdentifier) {
        this.externalIdentifiers = externalIdentifier;
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
        result = prime * result + ((externalIdentifiers == null) ? 0 : externalIdentifiers.hashCode());
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
        PersonExternalIdentifiers other = (PersonExternalIdentifiers) obj;
        if (externalIdentifiers == null) {
            if (other.externalIdentifiers != null)
                return false;
        } else if (!externalIdentifiers.equals(other.externalIdentifiers))
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
