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
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-833 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.11.03 at 04:58:24 PM GMT 
//

package org.orcid.jaxb.model.message;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import java.io.Serializable;

/**
 * <p>
 * Java class for creation-method values..
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * <p>
 * 
 * <pre>
 * &lt;xs:simpleType>
 *   &lt;xs:restriction base="xs:string">
 *     &lt;xs:enumeration value="API"/>
 *     &lt;xs:enumeration value="manual">
 *   &lt;/xs:restriction>
 * &lt;/xs:simpleType>
 * </pre>
 * 
 */
@XmlEnum
public enum CreationMethod implements Serializable {

    /**
     * The profile was created using the Tier 2 API.
     * 
     */
    @XmlEnumValue("API")
    API("API"),

    /**
     * The user registered on the /register page.
     * 
     */
    @XmlEnumValue("Direct")
    DIRECT("Direct"),

    /**
     * The registered on the /oauth/signin.
     * 
     */
    @XmlEnumValue("Member-referred")
    MEMBER_REFERRED("Member-referred"),

    /**
     * The profile was created manually using the ORCID web user interface.
     * 
     */
    @XmlEnumValue("website")
    WEBSITE("website"),

    /**
     * The profile was created manually using the ORCID web user interface.
     * 
     */
    @XmlEnumValue("integration-test")
    INTEGRATION_TEST("integration-test");
    
    private final String value;

    CreationMethod(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CreationMethod fromValue(String v) {
        for (CreationMethod c : CreationMethod.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public static boolean isValid(String v) {
        for (CreationMethod c : CreationMethod.values()) {
            if (c.value.equals(v)) {
                return true;
            }
        }
        return false;
    }

}
