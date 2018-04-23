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

import io.swagger.annotations.ApiModelProperty;
import org.orcid.jaxb.model.common_v2.Title;
import org.orcid.jaxb.model.common_v2.Url;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"externalIdentifiers","type","journalTitle","title","url"})
@XmlRootElement(name = "subject", namespace = "http://www.orcid.org/ns/peer-review")
public class Subject implements Serializable {    
    private static final long serialVersionUID = -3011951615514804083L;
    
    @XmlElement(name = "external-identifiers", namespace = "http://www.orcid.org/ns/common")
    protected ExternalIDs externalIdentifiers;
    @XmlElement(namespace = "http://www.orcid.org/ns/peer-review")
    protected WorkType type;
    @XmlElement(namespace = "http://www.orcid.org/ns/peer-review")
    protected WorkTitle title;
    @XmlElement( namespace = "http://www.orcid.org/ns/peer-review", name = "journal-title")
    protected Title journalTitle;
    @XmlElement(namespace = "http://www.orcid.org/ns/peer-review")
    protected Url url;
    @XmlAttribute(name = "put-code")
    @ApiModelProperty(hidden = true) 
    protected String putCode;

    public String getPutCode() {
        return putCode;
    }

    public void setPutCode(String putCode) {
        this.putCode = putCode;
    }
    
    public ExternalIDs getExternalIdentifiers() {
        return externalIdentifiers;
    }

    public void setExternalIdentifiers(ExternalIDs externalIdentifiers) {
        this.externalIdentifiers = externalIdentifiers;
    }

    public WorkType getType() {
        return type;
    }

    public void setType(WorkType type) {
        this.type = type;
    }

    public WorkTitle getTitle() {
        return title;
    }

    public void setTitle(WorkTitle title) {
        this.title = title;
    }

    public Title getJournalTitle() {
        return journalTitle;
    }

    public void setJournalTitle(Title journalTitle) {
        this.journalTitle = journalTitle;
    }

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((externalIdentifiers == null) ? 0 : externalIdentifiers.hashCode());
        result = prime * result + ((journalTitle == null) ? 0 : journalTitle.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
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
        Subject other = (Subject) obj;
        if (externalIdentifiers == null) {
            if (other.externalIdentifiers != null)
                return false;
        } else if (!externalIdentifiers.equals(other.externalIdentifiers))
            return false;
        if (journalTitle == null) {
            if (other.journalTitle != null)
                return false;
        } else if (!journalTitle.equals(other.journalTitle))
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        if (type != other.type)
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }    
}
