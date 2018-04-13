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

import org.orcid.jaxb.model.common_v2.LastModifiedDate;
import org.orcid.jaxb.model.record_v2.ActivitiesContainer;
import org.orcid.jaxb.model.record_v2.Activity;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "lastModifiedDate", "summaries" })
@XmlRootElement(name = "employments", namespace = "http://www.orcid.org/ns/activities")
public class Employments implements ActivitiesContainer, Serializable {

    private static final long serialVersionUID = 3293976926416154039L;
    @XmlElement(name = "last-modified-date", namespace = "http://www.orcid.org/ns/common")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(name = "employment-summary", namespace = "http://www.orcid.org/ns/employment")
    private List<EmploymentSummary> summaries;
    @XmlAttribute
    protected String path;
    
    public Employments() {
        
    }
    
    public Employments(List<EmploymentSummary> summaries) {
        this.summaries = summaries;
    }
    
    public List<EmploymentSummary> getSummaries() {
        if (summaries == null) {
            summaries = new ArrayList<>();
        }
        return summaries;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((summaries == null) ? 0 : summaries.hashCode());
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
        Employments other = (Employments) obj;
        if (summaries == null) {
            if (other.summaries != null)
                return false;
        } else if (!summaries.equals(other.summaries))
            return false;
        return true;
    }

    public LastModifiedDate getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LastModifiedDate lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public Map<Long, ? extends Activity> retrieveActivitiesAsMap() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Collection<? extends Activity> retrieveActivities() {
        return (Collection<? extends Activity>) summaries;
    }
    
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
