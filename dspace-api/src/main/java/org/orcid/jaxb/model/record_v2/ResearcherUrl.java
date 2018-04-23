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
import org.orcid.jaxb.model.common_v2.*;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "createdDate", "lastModifiedDate", "source", "urlName", "url" })
@XmlRootElement(name = "researcher-url", namespace = "http://www.orcid.org/ns/researcher-url")
public class ResearcherUrl implements Filterable, Serializable, Comparable<ResearcherUrl>, SourceAware {
    private static final long serialVersionUID = 1047027166285177589L;    
    @XmlElement(name = "url-name", namespace = "http://www.orcid.org/ns/researcher-url")
    protected String urlName;
    @XmlElement(namespace = "http://www.orcid.org/ns/researcher-url")
    protected Url url;
    @XmlElement(namespace = "http://www.orcid.org/ns/common")
    protected Source source;
    @XmlAttribute(name = "put-code")
    @ApiModelProperty(hidden = true)
    protected Long putCode;
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "last-modified-date")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "created-date")
    protected CreatedDate createdDate;
    @XmlAttribute
    protected Visibility visibility;
    @XmlAttribute
    protected String path;
    @XmlAttribute(name = "display-index")
    protected Long displayIndex;    
    
    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public String getUrlName() {
        return urlName;
    }

    public void setUrlName(String urlName) {
        this.urlName = urlName;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
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
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((urlName == null) ? 0 : urlName.hashCode());
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
        ResearcherUrl other = (ResearcherUrl) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (urlName == null) {
            if (other.urlName != null)
                return false;
        } else if (!urlName.equals(other.urlName))
            return false;
        if (visibility != other.visibility)
            return false;
        return true;
    }

    @Override
    public String toString(){
        return "ResearcherUrl{" +
                "name='" + getUrlName() + '\'' +
                ", url='" + ((url==null)?"":url.getValue()) + '\'' +
                '}';
    }

    @Override
    public String retrieveSourcePath() {
        if (source != null) {
            return source.retrieveSourcePath();
        }
        return null;
    }

    @Override
    public int compareTo(ResearcherUrl o) {
        if(o == null || o.getUrlName() == null) {
            return 1;
        }
                
        if(getUrlName() == null) {
            return -1;
        }
        int urlNameComp = this.getUrlName().compareTo(o.getUrlName());
        
        if(urlNameComp == 0) {
            if(o.getUrl() == null) {
                return 1;
            }
                    
            if(this.getUrl() == null) {
                return -1;
            }
            
            return this.getUrl().compareTo(o.getUrl());
        }
        
        return 0;
    }    
}
