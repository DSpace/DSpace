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
 * @author Angel Montenegro 
 *         Date: 20/02/2014
 */
@XmlType(name = "contributorRole", namespace = "http://www.orcid.org/ns/funding")
@XmlEnum
public enum FundingContributorRole implements Serializable {

    @XmlEnumValue("lead")
    LEAD("lead"), @XmlEnumValue("co-lead")
    CO_LEAD("co_lead"), @XmlEnumValue("supported-by")
    SUPPORTED_BY("supported_by"), @XmlEnumValue("other-contribution")
    OTHER_CONTRIBUTION("other_contribution");

    private final String value;

    FundingContributorRole(String v) {
        value = v;
    }
    
    public String value() {
        return value;
    }
    
    public static FundingContributorRole fromValue(String v) {
        for (FundingContributorRole c : FundingContributorRole.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
