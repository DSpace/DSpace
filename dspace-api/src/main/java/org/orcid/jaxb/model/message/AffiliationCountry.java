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
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.08.19 at 01:44:12 PM BST 
//

package org.orcid.jaxb.model.message;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * ISO 3611 country
 * 
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.orcid.org/ns/orcid>iso-3166-country">
 *       &lt;attGroup ref="{http://www.orcid.org/ns/orcid}visibility"/>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType( propOrder = { "value" })
@XmlRootElement(name = "affiliation-country")
public class AffiliationCountry implements Serializable {

    private final static long serialVersionUID = 1L;
    @XmlValue
    protected Iso3166Country value;

    public AffiliationCountry() {
        super();
    }

    public AffiliationCountry(Iso3166Country value) {
        super();
        this.value = value;
    }

    /**
     * ISO 3166 country codes
     * 
     * @return possible object is {@link org.orcid.jaxb.model.message.Iso3166Country }
     *
     */
    public Iso3166Country getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value
     *            allowed object is {@link org.orcid.jaxb.model.message.Iso3166Country }
     * 
     */
    public void setValue(Iso3166Country value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        AffiliationCountry other = (AffiliationCountry) obj;
        if (value != other.value)
            return false;
        return true;
    }

}
