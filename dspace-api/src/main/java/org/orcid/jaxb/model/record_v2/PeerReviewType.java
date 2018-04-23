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
package org.orcid.jaxb.model.record_v2;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlType(name = "type")
@XmlEnum
public enum PeerReviewType implements Serializable {
    @XmlEnumValue("review")
    REVIEW("review"),
    @XmlEnumValue("evaluation")
    EVALUATION("evaluation");
        
    private final String value;

    PeerReviewType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }
    
    public static PeerReviewType fromValue(String v) {
        for (PeerReviewType c : PeerReviewType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
