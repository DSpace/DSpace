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
// Generated on: 2012.08.02 at 04:23:52 PM BST 
//

package org.orcid.jaxb.model.record_v2;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
* 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType( propOrder = { "contributorRole" })
@XmlRootElement(name = "contributorAttributes", namespace = "http://www.orcid.org/ns/funding")
public class FundingContributorAttributes implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @XmlElement(required = true, namespace = "http://www.orcid.org/ns/funding", name = "contributor-role")
    protected FundingContributorRole contributorRole;

    /**
     * Gets the value of the contributorRole property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public FundingContributorRole getContributorRole() {
        return contributorRole;
    }

    /**
     * Sets the value of the contributorRole property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContributorRole(FundingContributorRole value) {
        this.contributorRole = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FundingContributorAttributes)) {
            return false;
        }

        FundingContributorAttributes that = (FundingContributorAttributes) o;

        if (contributorRole != that.contributorRole) {
            return false;
        }        

        return true;
    }

    @Override
    public int hashCode() {
        int result = 31 * (contributorRole != null ? contributorRole.hashCode() : 0);
        return result;
    }
}
