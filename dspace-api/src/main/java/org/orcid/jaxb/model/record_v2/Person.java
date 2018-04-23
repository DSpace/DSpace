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

/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "lastModifiedDate", "name", "otherNames", "biography", "researcherUrls", "emails", "addresses", "keywords", "externalIdentifiers" })
@XmlRootElement(name = "person", namespace = "http://www.orcid.org/ns/person")
public class Person implements Serializable {
    private static final long serialVersionUID = 2200160976598223346L;
    
    @XmlElement(name = "name", namespace = "http://www.orcid.org/ns/person")
    Name name;
    @XmlElement(name = "other-names", namespace = "http://www.orcid.org/ns/other-name")
    OtherNames otherNames;
    @XmlElement(name = "biography", namespace = "http://www.orcid.org/ns/person")
    Biography biography;
    @XmlElement(name = "researcher-urls", namespace = "http://www.orcid.org/ns/researcher-url")
    ResearcherUrls researcherUrls;
    @XmlElement(name = "emails", namespace = "http://www.orcid.org/ns/email")
    Emails emails;
    @XmlElement(name = "addresses", namespace = "http://www.orcid.org/ns/address")
    Addresses addresses;
    @XmlElement(name = "keywords", namespace = "http://www.orcid.org/ns/keyword")
    Keywords keywords;
    @XmlElement(name = "external-identifiers", namespace = "http://www.orcid.org/ns/external-identifier")
    PersonExternalIdentifiers externalIdentifiers;
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "last-modified-date")
    protected LastModifiedDate lastModifiedDate;

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

    public ResearcherUrls getResearcherUrls() {
        return researcherUrls;
    }

    public void setResearcherUrls(ResearcherUrls researcherUrls) {
        this.researcherUrls = researcherUrls;
    }

    public Emails getEmails() {
        return emails;
    }

    public void setEmails(Emails emails) {
        this.emails = emails;
    }

    public Addresses getAddresses() {
        return addresses;
    }

    public void setAddresses(Addresses addresses) {
        this.addresses = addresses;
    }

    public Keywords getKeywords() {
        return keywords;
    }

    public void setKeywords(Keywords keywords) {
        this.keywords = keywords;
    }

    public PersonExternalIdentifiers getExternalIdentifiers() {
        return externalIdentifiers;
    }

    public void setExternalIdentifiers(PersonExternalIdentifiers externalIdentifiers) {
        this.externalIdentifiers = externalIdentifiers;
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
        result = prime * result + ((addresses == null) ? 0 : addresses.hashCode());
        result = prime * result + ((biography == null) ? 0 : biography.hashCode());
        result = prime * result + ((emails == null) ? 0 : emails.hashCode());
        result = prime * result + ((externalIdentifiers == null) ? 0 : externalIdentifiers.hashCode());
        result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((otherNames == null) ? 0 : otherNames.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((researcherUrls == null) ? 0 : researcherUrls.hashCode());
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
        Person other = (Person) obj;
        if (addresses == null) {
            if (other.addresses != null)
                return false;
        } else if (!addresses.equals(other.addresses))
            return false;
        if (biography == null) {
            if (other.biography != null)
                return false;
        } else if (!biography.equals(other.biography))
            return false;
        if (emails == null) {
            if (other.emails != null)
                return false;
        } else if (!emails.equals(other.emails))
            return false;
        if (externalIdentifiers == null) {
            if (other.externalIdentifiers != null)
                return false;
        } else if (!externalIdentifiers.equals(other.externalIdentifiers))
            return false;
        if (keywords == null) {
            if (other.keywords != null)
                return false;
        } else if (!keywords.equals(other.keywords))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
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
        if (researcherUrls == null) {
            if (other.researcherUrls != null)
                return false;
        } else if (!researcherUrls.equals(other.researcherUrls))
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
