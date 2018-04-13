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
package org.orcid.jaxb.model.clientgroup;

import org.orcid.jaxb.model.message.ScopePathType;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>anyURI">
 *       &lt;attribute name="scope" type="{http://www.orcid.org/ns/orcid}scope-path-type" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "value" })
@XmlRootElement(name = "redirect-uri")
public class RedirectUri implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -1845281302337792818L;
    @XmlValue
    @XmlSchemaType(name = "anyURI")
    protected String value;
    @XmlAttribute
    protected List<ScopePathType> scope;

    @XmlAttribute(required = true)
    private RedirectUriType type;
    
    @XmlTransient
    private String actType;
    
    @XmlTransient
    private String geoArea;

    public RedirectUri() {
        super();
    }

    public RedirectUri(String value) {
        //error is here!
        super();
        setValue(value);
        setType(RedirectUriType.DEFAULT);
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the scope property.
     * 
     * @return
     *     possible object is
     *     {@link org.orcid.jaxb.model.message.ScopePathType }
     *
     */
    public List<ScopePathType> getScope() {
        return scope;
    }

    public String getScopeAsSingleString() {

        String allScopes = "";

        if (scope != null && !scope.isEmpty()) {
            allScopes = ScopePathType.getScopesAsSingleString(scope);
        }

        return allScopes;

    }

    /**
     * Sets the value of the scope property.
     *
     * @param value
     *     allowed object is
     *     {@link org.orcid.jaxb.model.message.ScopePathType }
     *     
     */
    public void setScope(List<ScopePathType> value) {
        this.scope = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        RedirectUri other = (RedirectUri) obj;
        if (scope != other.scope)
            return false;
        if (type != other.type)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    public RedirectUriType getType() {
        return type;
    }

    public void setType(RedirectUriType redirectUriType) {
        this.type = redirectUriType;
    }

	public String getActType() {
		return actType;
	}

	public void setActType(String actType) {
		this.actType = actType;
	}

	public String getGeoArea() {
		return geoArea;
	}

	public void setGeoArea(String geoArea) {
		this.geoArea = geoArea;
	}
}
