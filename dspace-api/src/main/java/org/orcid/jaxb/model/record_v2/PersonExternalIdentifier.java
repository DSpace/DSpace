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
import org.orcid.jaxb.model.common_v2.*;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "createdDate", "lastModifiedDate", "source", "type", "value", "url", "relationship" })
@XmlRootElement(name = "external-identifier", namespace = "http://www.orcid.org/ns/external-identifier")
public class PersonExternalIdentifier implements Serializable, Filterable, SourceAware {
    private static final long serialVersionUID = 8340033850223164314L;
    
    @XmlElement(name = "external-id-type", namespace = "http://www.orcid.org/ns/common", required = true)
    protected String type;
    @XmlElement(name = "external-id-value", namespace = "http://www.orcid.org/ns/common", required = true)
    protected String value;
    @XmlElement(name="external-id-relationship", namespace = "http://www.orcid.org/ns/common")
    protected Relationship relationship;
    @XmlElement(name="external-id-url", namespace = "http://www.orcid.org/ns/common")
    protected Url url;    
    @XmlElement(namespace = "http://www.orcid.org/ns/common")
    protected Source source;        
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "last-modified-date")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "created-date")
    protected CreatedDate createdDate;
    @XmlAttribute(name = "put-code")
    @ApiModelProperty(hidden = true)
    protected Long putCode;        
    @XmlAttribute
    protected Visibility visibility;
    @XmlAttribute
    protected String path;
    @XmlAttribute(name = "display-index")
    protected Long displayIndex;
    
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public Relationship getRelationship() {
        return relationship;
    }
    public void setRelationship(Relationship relationship) {
        this.relationship = relationship;
    }
    public Url getUrl() {
        return url;
    }
    public void setUrl(Url url) {
        this.url = url;
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
    public Long getPutCode() {
        return putCode;
    }
    public void setPutCode(Long putCode) {
        this.putCode = putCode;
    }
    public Visibility getVisibility() {
        return visibility;
    }
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public Long getDisplayIndex() {
        return displayIndex;
    }

    public void setDisplayIndex(Long displayIndex) {
        this.displayIndex = displayIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((lastModifiedDate == null) ? 0 : lastModifiedDate.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((putCode == null) ? 0 : putCode.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((relationship == null) ? 0 : relationship.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
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
        PersonExternalIdentifier other = (PersonExternalIdentifier) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (createdDate == null) {
            if (other.createdDate != null)
                return false;
        } else if (!createdDate.equals(other.createdDate))
            return false;
        if (lastModifiedDate == null) {
            if (other.lastModifiedDate != null)
                return false;
        } else if (!lastModifiedDate.equals(other.lastModifiedDate))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (putCode == null) {
            if (other.putCode != null)
                return false;
        } else if (!putCode.equals(other.putCode))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        if (relationship == null) {
            if (other.relationship != null)
                return false;
        } else if (!relationship.equals(other.relationship))
            return false;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (visibility != other.visibility)
            return false;
        return true;
    }
    @Override
    public String retrieveSourcePath() {
        if (source != null) {
            return source.retrieveSourcePath();
        }
        return null;
    }        
}
