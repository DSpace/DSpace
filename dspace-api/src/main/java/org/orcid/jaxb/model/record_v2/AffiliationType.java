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
package org.orcid.jaxb.model.record_v2;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * @author Declan Newman (declan) Date: 31/07/2012
 */
@XmlType(name = "type")
@XmlEnum
public enum AffiliationType implements Serializable {

    @XmlEnumValue("education")
    EDUCATION("education"),

    @XmlEnumValue("employment")
    EMPLOYMENT("employment");
    
    private final String value;

    AffiliationType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }       

    public static AffiliationType fromValue(String v) {
        for (AffiliationType c : AffiliationType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
