/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.util.Arrays;

/**
 * Enum that model all the allowed values for an item access security
 * configuration.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum CrisSecurity {

    NONE("0"),
    ADMIN("1"),
    OWNER("2"),
    ADMIN_OWNER("3"),
    CUSTOM("4"),
    ITEM_ADMIN("5"),
    SUBMITTER("6"),
    SUBMITTER_GROUP("7"),
    GROUP("8");

    private final String value;

    private CrisSecurity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CrisSecurity getByValue(String value) {
        return Arrays.stream(CrisSecurity.values())
            .filter(security -> security.getValue().equals(value))
            .findFirst()
            .orElse(CrisSecurity.NONE);
    }

}
