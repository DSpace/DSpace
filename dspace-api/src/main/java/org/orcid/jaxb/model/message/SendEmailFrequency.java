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

/**
 * 
 * @author Will Simpson
 * 
 */
public enum SendEmailFrequency {

    IMMEDIATELY("0.0"), DAILY("1.0"), WEEKLY("7.0"), QUARTERLY("91.3105"), NEVER(String.valueOf(Float.MAX_VALUE));

    // Value in days
    String value;

    private SendEmailFrequency(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
