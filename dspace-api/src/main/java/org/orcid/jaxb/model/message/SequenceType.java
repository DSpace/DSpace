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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * @author Declan Newman (declan)
 *         Date: 07/08/2012
 */
@XmlType(name = "sequence")
@XmlEnum
public enum SequenceType implements Serializable {

    @XmlEnumValue("first")
    FIRST("first"), @XmlEnumValue("additional")
    ADDITIONAL("additional");

    private final String value;

    SequenceType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SequenceType fromValue(String v) {
        for (SequenceType c : SequenceType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
