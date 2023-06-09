/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

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

    public static String findLastFrequency(String frequency) {
        String startDate = "";
        String endDate = "";
        Calendar cal = Calendar.getInstance();
        // Full ISO 8601 is e.g.
        SimpleDateFormat fullIsoStart = new SimpleDateFormat("yyyy-MM-dd'T'00:00:00'Z'");
        SimpleDateFormat fullIsoEnd = new SimpleDateFormat("yyyy-MM-dd'T'23:59:59'Z'");
        switch (frequency) {
            case "D":
                cal.add(Calendar.DAY_OF_MONTH, -1);
                endDate = fullIsoEnd.format(cal.getTime());
                startDate = fullIsoStart.format(cal.getTime());
                break;
            case "M":
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                cal.add(Calendar.DAY_OF_MONTH, -dayOfMonth);
                endDate = fullIsoEnd.format(cal.getTime());
                cal.add(Calendar.MONTH, -1);
                cal.add(Calendar.DAY_OF_MONTH, 1);
                startDate = fullIsoStart.format(cal.getTime());
                break;
            case "W":
                cal.add(Calendar.DAY_OF_WEEK, -1);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
                cal.add(Calendar.DAY_OF_WEEK, -dayOfWeek);
                endDate = fullIsoEnd.format(cal.getTime());
                cal.add(Calendar.DAY_OF_WEEK, -6);
                startDate = fullIsoStart.format(cal.getTime());
                break;
            default:
                return null;
        }
        return "[" + startDate + " TO " + endDate + "]";
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
