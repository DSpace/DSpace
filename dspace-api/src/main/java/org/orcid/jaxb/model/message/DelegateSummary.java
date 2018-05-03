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
// Generated on: 2012.08.02 at 11:50:02 AM BST 
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
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}orcid"/>
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}credit-name" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "orcid", "orcidIdentifier", "lastModifiedDate", "creditName" })
@XmlRootElement(name = "delegate-summary")
public class DelegateSummary implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @XmlElement
    protected Orcid orcid;
    @XmlElement(name = "orcid-identifier")
    protected OrcidIdentifier orcidIdentifier;
    @XmlElement(name = "last-modified-date")
    protected LastModifiedDate lastModifiedDate;
    @XmlElement(name = "credit-name")
    protected CreditName creditName;

    public DelegateSummary() {
        super();
    }

    @Deprecated
    public DelegateSummary(Orcid orcid) {
        this.orcid = orcid;
        this.orcidIdentifier = new OrcidIdentifier(orcid.getValue());
    }

    public DelegateSummary(OrcidIdentifier orcidIdentifier) {
        this.orcidIdentifier = orcidIdentifier;
    }

    /**
     * Gets the value of the orcid property.
     * 
     * @return possible object is {@link org.orcid.jaxb.model.message.Orcid }
     * 
     */
    @Deprecated
    public Orcid getOrcid() {
        return orcid;
    }

    /**
     * Sets the value of the orcid property.
     * 
     * @param value
     *            allowed object is {@link org.orcid.jaxb.model.message.Orcid }
     * 
     */
    @Deprecated
    public void setOrcid(Orcid value) {
        this.orcid = value;
    }

    public OrcidIdentifier getOrcidIdentifier() {
        return orcidIdentifier;
    }

    public void setOrcidIdentifier(OrcidIdentifier orcidIdentifier) {
        this.orcidIdentifier = orcidIdentifier;
    }

    public LastModifiedDate getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LastModifiedDate lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Gets the value of the creditName property.
     * 
     * @return possible object is {@link org.orcid.jaxb.model.message.CreditName }
     * 
     */
    public CreditName getCreditName() {
        return creditName;
    }

    /**
     * Sets the value of the creditName property.
     * 
     * @param value
     *            allowed object is {@link org.orcid.jaxb.model.message.CreditName }
     * 
     */
    public void setCreditName(CreditName value) {
        this.creditName = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DelegateSummary)) {
            return false;
        }

        DelegateSummary that = (DelegateSummary) o;

        if (creditName != null ? !creditName.equals(that.creditName) : that.creditName != null) {
            return false;
        }
        if (orcid != null ? !orcid.equals(that.orcid) : that.orcid != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = orcid != null ? orcid.hashCode() : 0;
        result = 31 * result + (creditName != null ? creditName.hashCode() : 0);
        return result;
    }
}
