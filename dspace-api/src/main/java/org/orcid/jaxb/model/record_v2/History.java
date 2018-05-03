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
import org.orcid.jaxb.model.common_v2.Source;
import org.orcid.jaxb.model.message.CreationMethod;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "creationMethod", "completionDate", "submissionDate", "lastModifiedDate", "claimed", "source", "deactivationDate", "verifiedEmail",
        "verifiedPrimaryEmail" })
@XmlRootElement(name = "history", namespace = "http://www.orcid.org/ns/history")
public class History implements Serializable, SourceAware {
    private static final long serialVersionUID = 5662067844899740318L;
    @XmlElement(namespace = "http://www.orcid.org/ns/history", name = "creation-method")
    protected CreationMethod creationMethod;
    @XmlElement(namespace = "http://www.orcid.org/ns/history", name = "completion-date")
    protected CompletionDate completionDate;
    @XmlElement(namespace = "http://www.orcid.org/ns/history", name = "submission-date")
    protected SubmissionDate submissionDate;
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "last-modified-date")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(namespace = "http://www.orcid.org/ns/history", name = "claimed")
    protected Boolean claimed;
    @XmlElement(namespace = "http://www.orcid.org/ns/common", name = "source")
    protected Source source;
    @XmlElement(namespace = "http://www.orcid.org/ns/history", name = "deactivation-date")
    protected DeactivationDate deactivationDate;
    @XmlElement(namespace = "http://www.orcid.org/ns/history", name = "verified-email")
    protected boolean verifiedEmail;
    @XmlElement(namespace = "http://www.orcid.org/ns/history", name = "verified-primary-email")
    protected boolean verifiedPrimaryEmail;

    public CreationMethod getCreationMethod() {
        return creationMethod;
    }

    public void setCreationMethod(CreationMethod creationMethod) {
        this.creationMethod = creationMethod;
    }

    public CompletionDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(CompletionDate completionDate) {
        this.completionDate = completionDate;
    }

    public SubmissionDate getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(SubmissionDate submissionDate) {
        this.submissionDate = submissionDate;
    }

    public LastModifiedDate getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LastModifiedDate lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Boolean getClaimed() {
        return claimed;
    }

    public void setClaimed(Boolean claimed) {
        this.claimed = claimed;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public DeactivationDate getDeactivationDate() {
        return deactivationDate;
    }

    public void setDeactivationDate(DeactivationDate deactivationDate) {
        this.deactivationDate = deactivationDate;
    }

    public boolean isVerifiedEmail() {
        return verifiedEmail;
    }

    public void setVerifiedEmail(boolean verifiedEmail) {
        this.verifiedEmail = verifiedEmail;
    }

    public boolean isVerifiedPrimaryEmail() {
        return verifiedPrimaryEmail;
    }

    public void setVerifiedPrimaryEmail(boolean verifiedPrimaryEmail) {
        this.verifiedPrimaryEmail = verifiedPrimaryEmail;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((claimed == null) ? 0 : claimed.hashCode());
        result = prime * result + ((completionDate == null) ? 0 : completionDate.hashCode());
        result = prime * result + ((creationMethod == null) ? 0 : creationMethod.hashCode());
        result = prime * result + ((deactivationDate == null) ? 0 : deactivationDate.hashCode());
        result = prime * result + ((lastModifiedDate == null) ? 0 : lastModifiedDate.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((submissionDate == null) ? 0 : submissionDate.hashCode());
        result = prime * result + (verifiedEmail ? 1231 : 1237);
        result = prime * result + (verifiedPrimaryEmail ? 1231 : 1237);
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
        History other = (History) obj;
        if (claimed == null) {
            if (other.claimed != null)
                return false;
        } else if (!claimed.equals(other.claimed))
            return false;
        if (completionDate == null) {
            if (other.completionDate != null)
                return false;
        } else if (!completionDate.equals(other.completionDate))
            return false;
        if (creationMethod != other.creationMethod)
            return false;
        if (deactivationDate == null) {
            if (other.deactivationDate != null)
                return false;
        } else if (!deactivationDate.equals(other.deactivationDate))
            return false;
        if (lastModifiedDate == null) {
            if (other.lastModifiedDate != null)
                return false;
        } else if (!lastModifiedDate.equals(other.lastModifiedDate))
            return false;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        if (submissionDate == null) {
            if (other.submissionDate != null)
                return false;
        } else if (!submissionDate.equals(other.submissionDate))
            return false;
        if (verifiedEmail != other.verifiedEmail)
            return false;
        if (verifiedPrimaryEmail != other.verifiedPrimaryEmail)
            return false;
        return true;
    }
}
