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
package org.orcid.jaxb.model.record.summary_v2;

import org.orcid.jaxb.model.common_v2.*;
import org.orcid.jaxb.model.record_v2.*;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "createdDate", "lastModifiedDate", "source", "title", "externalIdentifiers", "type", "startDate", "endDate","organization" })
@XmlRootElement(name = "summary", namespace = "http://www.orcid.org/ns/funding")
public class FundingSummary implements VisibilityType, Activity, GroupableActivity, Serializable, SourceAware, OrganizationHolder {

    private static final long serialVersionUID = 7489792970949538708L;
    @XmlElement(namespace = "http://www.orcid.org/ns/funding", required = true)
    protected FundingType type;
    @XmlElement(required = true, namespace = "http://www.orcid.org/ns/funding")
    protected FundingTitle title;
    @XmlElement(name="external-ids", namespace = "http://www.orcid.org/ns/common")
    protected ExternalIDs externalIdentifiers;
    @XmlElement(name="start-date", namespace = "http://www.orcid.org/ns/common")
    protected FuzzyDate startDate;
    @XmlElement(name="end-date", namespace = "http://www.orcid.org/ns/common")
    protected FuzzyDate endDate;
    @XmlElement(namespace = "http://www.orcid.org/ns/common")
    protected Source source;
    @XmlElement(name="last-modified-date", namespace = "http://www.orcid.org/ns/common")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(name="created-date", namespace = "http://www.orcid.org/ns/common")
    protected CreatedDate createdDate;
    @XmlElement(required = true, namespace = "http://www.orcid.org/ns/funding")
    protected Organization organization;

    @XmlAttribute(name="put-code")
    protected Long putCode;
    @XmlAttribute(name="path")
    protected String path;
    @XmlAttribute
    protected Visibility visibility;
    @XmlAttribute(name = "display-index")
    protected String displayIndex;

    public FundingType getType() {
        return type;
    }

    public void setType(FundingType type) {
        this.type = type;
    }

    public FundingTitle getTitle() {
        return title;
    }

    public void setTitle(FundingTitle title) {
        this.title = title;
    }

    public ExternalIDs getExternalIdentifiers() {
        return externalIdentifiers;
    }

    public void setExternalIdentifiers(ExternalIDs externalIdentifiers) {
        this.externalIdentifiers = externalIdentifiers;
    }

    public FuzzyDate getStartDate() {
        return startDate;
    }

    public void setStartDate(FuzzyDate startDate) {
        this.startDate = startDate;
    }

    public FuzzyDate getEndDate() {
        return endDate;
    }

    public void setEndDate(FuzzyDate endDate) {
        this.endDate = endDate;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public LastModifiedDate getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LastModifiedDate lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public CreatedDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(CreatedDate createdDate) {
        this.createdDate = createdDate;
    }
    
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Long getPutCode() {
        return putCode;
    }

    public void setPutCode(Long putCode) {
        this.putCode = putCode;
    }

    /**
     * Gets the value of the path property.
     * 
     * @return possible object is {@link Object }
     * 
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the value of the path property.
     * 
     * @param path
     *            allowed object is {@link Object }
     * 
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public String getDisplayIndex() {
        return displayIndex;
    }

    public void setDisplayIndex(String displayIndex) {
        this.displayIndex = displayIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
        result = prime * result + ((externalIdentifiers == null) ? 0 : externalIdentifiers.hashCode());
        result = prime * result + ((lastModifiedDate == null) ? 0 : lastModifiedDate.hashCode());
        result = prime * result + ((putCode == null) ? 0 : putCode.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
        result = prime * result + ((organization == null) ? 0 : organization.hashCode());
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
        FundingSummary other = (FundingSummary) obj;
        if (createdDate == null) {
            if (other.createdDate != null)
                return false;
        } else if (!createdDate.equals(other.createdDate))
            return false;
        if (endDate == null) {
            if (other.endDate != null)
                return false;
        } else if (!endDate.equals(other.endDate))
            return false;
        if (externalIdentifiers == null) {
            if (other.externalIdentifiers != null)
                return false;
        } else if (!externalIdentifiers.equals(other.externalIdentifiers))
            return false;
        if (lastModifiedDate == null) {
            if (other.lastModifiedDate != null)
                return false;
        } else if (!lastModifiedDate.equals(other.lastModifiedDate))
            return false;
        if (putCode == null) {
            if (other.putCode != null)
                return false;
        } else if (!putCode.equals(other.putCode))
            return false;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        if (startDate == null) {
            if (other.startDate != null)
                return false;
        } else if (!startDate.equals(other.startDate))
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        if (type != other.type)
            return false;
        if (visibility != other.visibility)
            return false;
        if (organization == null) {
            if (other.organization != null)
                return false;
        } else if (!organization.equals(other.organization))
            return false;
        
        return true;
    }

    @Override
    public String retrieveSourcePath() {
        if (source == null) {
            return null;
        }
        return source.retrieveSourcePath();
    }
    
    @Override
    public int compareTo(GroupableActivity activity) {
        Long index = Long.valueOf(this.getDisplayIndex() == null ? "0" : this.getDisplayIndex());
        Long otherIndex = Long.valueOf(activity.getDisplayIndex() == null ? "0" : activity.getDisplayIndex());
        if (index == null) {
            if (otherIndex == null) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (otherIndex == null) {
                return 1;
            } else if (index instanceof Comparable) {
                //Return opposite, since higher index goes first
                return  index.compareTo(otherIndex) * -1;
            } else {
                return 0;
            }
        }
    }    
}
