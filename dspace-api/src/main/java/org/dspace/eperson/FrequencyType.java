/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.util.Arrays;

import org.apache.commons.codec.binary.StringUtils;

/**
 * This enum holds all the possible frequency types
 * that can be used in "subscription-send" script
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public enum FrequencyType {
    DAY("D"),
    WEEK("W"),
    MONTH("M");

    private String shortName;

    private FrequencyType(String shortName) {
        this.shortName = shortName;
    }

    public static boolean isSupportedFrequencyType(String value) {
        for (FrequencyType ft : Arrays.asList(FrequencyType.values())) {
            if (StringUtils.equals(ft.getShortName(), value)) {
                return true;
            }
        }
        return false;
    }

    public String getShortName() {
        return shortName;
    }

}
