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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.02.01 at 04:40:47 PM GMT 
//

package org.orcid.jaxb.model.message;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}creation-method" minOccurs="0"/>
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}completion-date" minOccurs="0"/>
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}submission-date" minOccurs="0"/>
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}claimed" minOccurs="0"/>
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}source" minOccurs="0"/>
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}deactivation-date" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.orcid.org/ns/orcid}visibility"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType( propOrder = { "creationMethod", "completionDate", "submissionDate", "lastModifiedDate", "claimed", "source", "deactivationDate", "verifiedEmail", "verifiedPrimaryEmail" })
@XmlRootElement(name = "orcid-history")
public class OrcidHistory implements Serializable {

    private final static long serialVersionUID = 1L;
    @XmlElement(name = "creation-method")
    protected CreationMethod creationMethod;
    @XmlElement(name = "completion-date")
    protected CompletionDate completionDate;
    @XmlElement(name = "submission-date")
    protected SubmissionDate submissionDate;
    @XmlElement(name = "last-modified-date")
    protected LastModifiedDate lastModifiedDate;
    protected Claimed claimed;
    protected Source source;
    @XmlElement(name = "deactivation-date")
    protected DeactivationDate deactivationDate;
    @XmlElement(name = "verified-email")
    private VerifiedEmail verifiedEmail;
    @XmlElement(name = "verified-primary-email")
    private VerifiedPrimaryEmail verifiedPrimaryEmail;
    @XmlAttribute
    protected Visibility visibility;
    
    
    
    /**
     * Gets the value of the creationMethod property.
     * 
     * @return possible object is {@link CreationMethod }
     * 
     */
    public CreationMethod getCreationMethod() {
        return creationMethod;
    }

    /**
     * Sets the value of the creationMethod property.
     * 
     * @param value
     *            allowed object is {@link CreationMethod }
     * 
     */
    public void setCreationMethod(CreationMethod value) {
        this.creationMethod = value;
    }

    /**
     * Gets the value of the completionDate property.
     * 
     * @return possible object is {@link CompletionDate }
     * 
     */
    public CompletionDate getCompletionDate() {
        return completionDate;
    }

    /**
     * Sets the value of the completionDate property.
     * 
     * @param value
     *            allowed object is {@link CompletionDate }
     * 
     */
    public void setCompletionDate(CompletionDate value) {
        this.completionDate = value;
    }

    /**
     * Gets the value of the submissionDate property.
     * 
     * @return possible object is {@link SubmissionDate }
     * 
     */
    public SubmissionDate getSubmissionDate() {
        return submissionDate;
    }

    /**
     * Sets the value of the submissionDate property.
     * 
     * @param value
     *            allowed object is {@link SubmissionDate }
     * 
     */
    public void setSubmissionDate(SubmissionDate value) {
        this.submissionDate = value;
    }

    /**
     * Gets the value of the lastModifiedDate property.
     * 
     * @return possible object is {@link LastModifiedDate }
     * 
     */
    public LastModifiedDate getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the value of the lastModifiedDate property.
     * 
     * @param value
     *            allowed object is {@link LastModifiedDate }
     * 
     */
    public void setLastModifiedDate(LastModifiedDate value) {
        this.lastModifiedDate = value;
    }

    /**
     * Gets the value of the claimed property.
     * 
     * @return possible object is {@link org.orcid.jaxb.model.message.Claimed }
     * 
     */
    public Claimed getClaimed() {
        return claimed;
    }

    public Boolean isClaimed() {
        return claimed == null ? false : claimed.isValue();
    }

    /**
     * Sets the value of the claimed property.
     * 
     * @param value
     *            allowed object is {@link org.orcid.jaxb.model.message.Claimed }
     * 
     */
    public void setClaimed(Claimed value) {
        this.claimed = value;
    }

    /**
     * Gets the value of the source property.
     * 
     * @return possible object is {@link org.orcid.jaxb.model.message.Source }
     * 
     */
    public Source getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     * 
     * @param value
     *            allowed object is {@link org.orcid.jaxb.model.message.Source }
     * 
     */
    public void setSource(Source value) {
        this.source = value;
    }

    /**
     * Gets the value of the deactivationDate property.
     * 
     * @return possible object is {@link DeactivationDate }
     * 
     */
    public DeactivationDate getDeactivationDate() {
        return deactivationDate;
    }

    /**
     * Sets the value of the deactivationDate property.
     * 
     * @param value
     *            allowed object is {@link DeactivationDate }
     * 
     */
    public void setDeactivationDate(DeactivationDate value) {
        this.deactivationDate = value;
    }

    /**
     * Gets the value of the visibility property.
     * 
     * @return possible object is {@link org.orcid.jaxb.model.message.Visibility }
     * 
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Sets the value of the visibility property.
     * 
     * @param value
     *            allowed object is {@link org.orcid.jaxb.model.message.Visibility }
     * 
     */
    public void setVisibility(Visibility value) {
        this.visibility = value;
    }
    
    public VerifiedPrimaryEmail getVerifiedPrimaryEmail() {
        return verifiedPrimaryEmail;
    }

    public void setVerifiedPrimaryEmail(VerifiedPrimaryEmail verifiedPrimaryEmail) {
        this.verifiedPrimaryEmail = verifiedPrimaryEmail;
    }

    public VerifiedEmail getVerifiedEmail() {
        return verifiedEmail;
    }

    public void setVerifiedEmail(VerifiedEmail verifiedEmail) {
        this.verifiedEmail = verifiedEmail;
    }
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((claimed == null) ? 0 : claimed.hashCode());
        result = prime * result + ((completionDate == null) ? 0 : completionDate.hashCode());
        result = prime * result + ((creationMethod == null) ? 0 : creationMethod.hashCode());
        result = prime * result + ((deactivationDate == null) ? 0 : deactivationDate.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((submissionDate == null) ? 0 : submissionDate.hashCode());
        result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
        result = prime * result + ((verifiedEmail == null) ? 0 : verifiedEmail.hashCode());
        result = prime * result + ((verifiedPrimaryEmail == null) ? 0 : verifiedPrimaryEmail.hashCode());
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
        OrcidHistory other = (OrcidHistory) obj;
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
        if (visibility != other.visibility)
            return false;
        
        if (verifiedEmail == null) {
            if (other.verifiedEmail != null)
                return false;
        } else if (!verifiedEmail.equals(other.verifiedEmail))
            return false;

        if (verifiedPrimaryEmail == null) {
            if (other.verifiedPrimaryEmail != null)
                return false;
        } else if (!verifiedPrimaryEmail.equals(other.verifiedPrimaryEmail))
            return false;

        return true;
    }


}
