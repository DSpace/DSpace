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
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "content" })
@XmlRootElement(name = "salesforce-id")
public class SalesforceId implements Serializable {

    private static final long serialVersionUID = 1L;
    @XmlValue
    protected String content;

    public SalesforceId() {
        
    }
    
    public SalesforceId(String content) {
        this.content = content;
    }
    
    /**
     * Gets the value of the content property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the value of the content property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setContent(String value) {
        this.content = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        SalesforceId that = (SalesforceId) o;

        if (content != null ? !content.equals(that.content) : that.content != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return content != null ? content.hashCode() : 0;
    }
}
