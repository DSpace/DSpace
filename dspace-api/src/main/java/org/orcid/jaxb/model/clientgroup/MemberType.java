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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>
 * Java class for client-type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="client-type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="basic"/>
 *     &lt;enumeration value="premium"/>
 *     &lt;enumeration value="basic-institution"/>
 *     &lt;enumeration value="premium-institution"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "group-type")
@XmlEnum
public enum MemberType {
    
    //@formatter:off
    @XmlEnumValue("basic")
    BASIC("basic"), 
    @XmlEnumValue("premium")
    PREMIUM("premium"), 
    @XmlEnumValue("basic-institution")
    BASIC_INSTITUTION("basic-institution"),
    @XmlEnumValue("premium-institution")
    PREMIUM_INSTITUTION("premium-institution");
    //@formatter:on
    
    private final String value;

    MemberType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MemberType fromValue(String v) {
        for (MemberType c : MemberType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
