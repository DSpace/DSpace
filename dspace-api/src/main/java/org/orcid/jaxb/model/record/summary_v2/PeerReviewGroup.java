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
import org.orcid.jaxb.model.record_v2.ExternalIDs;
import org.orcid.jaxb.model.record_v2.Group;
import org.orcid.jaxb.model.record_v2.GroupableActivity;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "lastModifiedDate", "identifiers", "peerReviewSummary" })
@XmlRootElement(name = "peer-review-group", namespace = "http://www.orcid.org/ns/activities")
public class PeerReviewGroup implements Group, Serializable {
    private static final long serialVersionUID = 1L;

    @XmlElement(name = "last-modified-date", namespace = "http://www.orcid.org/ns/common")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(name = "external-ids", namespace = "http://www.orcid.org/ns/common")
    private ExternalIDs identifiers;
    @XmlElement(name = "peer-review-summary", namespace = "http://www.orcid.org/ns/peer-review")
    private List<PeerReviewSummary> peerReviewSummary;

    public ExternalIDs getIdentifiers() {
        if (identifiers == null)
            identifiers = new ExternalIDs();
        return identifiers;
    }

    public List<PeerReviewSummary> getPeerReviewSummary() {
        if (peerReviewSummary == null)
            peerReviewSummary = new ArrayList<PeerReviewSummary>();
        return peerReviewSummary;
    }

    @Override
    public Collection<? extends GroupableActivity> getActivities() {
        return getPeerReviewSummary();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((peerReviewSummary == null) ? 0 : peerReviewSummary.hashCode());
        result = prime * result + ((identifiers == null) ? 0 : identifiers.hashCode());
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
        PeerReviewGroup other = (PeerReviewGroup) obj;
        if (peerReviewSummary == null) {
            if (other.peerReviewSummary != null)
                return false;
        } else if (!peerReviewSummary.equals(other.peerReviewSummary))
            return false;
        if (identifiers == null) {
            if (other.identifiers != null)
                return false;
        } else if (!identifiers.equals(other.identifiers))
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
