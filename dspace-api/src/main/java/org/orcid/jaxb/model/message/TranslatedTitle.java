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

import org.apache.commons.lang.StringUtils;

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
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}translated-title" minOccurs="1" maxOccurs="1"/>
 *         &lt;attGroup ref="{http://www.orcid.org/ns/orcid}language-code" minOccurs="1"/>
 *       &lt;/sequence> *    
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType( propOrder = { "content" })
@XmlRootElement(name = "translated-title")
public class TranslatedTitle implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @XmlValue
    protected String content;
    @XmlAttribute(name = "language-code", required = true)
    protected String languageCode;

    public TranslatedTitle() {

    }

    public TranslatedTitle(String content) {
        this.content = content;
    }

    public TranslatedTitle(String content, String languageCode) {
        this.content = content;
        this.languageCode = languageCode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        int result = 1;
        result = prime * result + (StringUtils.isEmpty(this.content) ? 0 : this.content.hashCode());
        result = prime * result + (StringUtils.isEmpty(this.languageCode) ? 0 : this.languageCode.hashCode());
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
        TranslatedTitle other = (TranslatedTitle) obj;
        if (this.content == null) {
            if (other.content != null)
                return false;
        } else if (!this.content.equals(other.content))
            return false;

        if (this.languageCode == null) {
            if (other.languageCode != null)
                return false;
        } else if (!this.languageCode.equals(other.languageCode))
            return false;

        return true;
    }
}
