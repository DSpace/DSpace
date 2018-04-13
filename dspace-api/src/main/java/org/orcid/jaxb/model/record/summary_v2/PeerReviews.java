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
import org.orcid.jaxb.model.record_v2.Group;
import org.orcid.jaxb.model.record_v2.GroupsContainer;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "lastModifiedDate", "peerReviewGroup" })
@XmlRootElement(name = "peer-reviews", namespace = "http://www.orcid.org/ns/activities")
public class PeerReviews implements GroupsContainer, Serializable {

    private static final long serialVersionUID = 6779626621503362679L;
    @XmlElement(name = "last-modified-date", namespace = "http://www.orcid.org/ns/common")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(name = "group", namespace = "http://www.orcid.org/ns/activities")
    List<PeerReviewGroup> peerReviewGroup;
    @XmlAttribute
    protected String path;
    
    public List<PeerReviewGroup> getPeerReviewGroup() {
        if (peerReviewGroup == null)
            peerReviewGroup = new ArrayList<PeerReviewGroup>();
        return peerReviewGroup;
    }

    @Override
    public Collection<? extends Group> retrieveGroups() {
        return getPeerReviewGroup();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((peerReviewGroup == null) ? 0 : peerReviewGroup.hashCode());
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
        PeerReviews other = (PeerReviews) obj;
        if (peerReviewGroup == null) {
            if (other.peerReviewGroup != null)
                return false;
        } else if (!peerReviewGroup.equals(other.peerReviewGroup))
            return false;
        return true;
    }

    public LastModifiedDate getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LastModifiedDate lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
