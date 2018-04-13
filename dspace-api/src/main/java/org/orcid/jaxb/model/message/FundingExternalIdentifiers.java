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
package org.orcid.jaxb.model.message;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}funding-external-identifier" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "fundingExternalIdentifier" })
@XmlRootElement(name = "funding-external-identifiers")
public class FundingExternalIdentifiers implements Serializable {
    private static final long serialVersionUID = 1L;
    @XmlElement(name = "funding-external-identifier")
    protected List<FundingExternalIdentifier> fundingExternalIdentifier;

    /**
     * Gets the value of the fundingExternalIdentifier property.
     * 
     * <p>
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the contributor property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getContributor().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link org.orcid.jaxb.model.message.Contributor }
     * 
     * 
     */
    public List<FundingExternalIdentifier> getFundingExternalIdentifier() {
        if (fundingExternalIdentifier == null)
            fundingExternalIdentifier = new ArrayList<FundingExternalIdentifier>();
        return fundingExternalIdentifier;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fundingExternalIdentifier == null) ? 0 : fundingExternalIdentifier.hashCode());
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
        FundingExternalIdentifiers other = (FundingExternalIdentifiers) obj;
        if (fundingExternalIdentifier == null) {
            if (other.fundingExternalIdentifier != null)
                return false;
        } else {
            if (other.fundingExternalIdentifier == null)
                return false;
            else if (!(fundingExternalIdentifier.containsAll(other.fundingExternalIdentifier) && other.fundingExternalIdentifier.containsAll(fundingExternalIdentifier) && other.fundingExternalIdentifier
                    .size() == fundingExternalIdentifier.size())) {
                return false;
            }
        }
        return true;
    }

}
