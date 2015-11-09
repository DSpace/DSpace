/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

/**
 * Utilities that are needed in XSL transformations.
 *
 * @author Art Lowel (art dot lowel at atmire dot com)
 */
public class XSLUtils {

    /*
     * Cuts off the string at the space nearest to the targetLength if there is one within
     * maxDeviation chars from the targetLength, or at the targetLength if no such space is
     * found
     */
    public static String shortenString(String string, int targetLength, int maxDeviation) {
        targetLength = Math.abs(targetLength);
        maxDeviation = Math.abs(maxDeviation);
        if (string == null || string.length() <= targetLength + maxDeviation)
        {
            return string;
        }


        int currentDeviation = 0;
        while (currentDeviation <= maxDeviation) {
            try {
                if (string.charAt(targetLength) == ' ')
                {
                    return string.substring(0, targetLength) + "...";
                }
                if (string.charAt(targetLength + currentDeviation) == ' ')
                {
                    return string.substring(0, targetLength + currentDeviation) + "...";
                }
                if (string.charAt(targetLength - currentDeviation) == ' ')
                {
                    return string.substring(0, targetLength - currentDeviation) + "...";
                }
            } catch (Exception e) {
                //just in case
            }

            currentDeviation++;
        }

        return string.substring(0, targetLength) + "...";

    }

    /**
     * Shorten a file name for display with a maximum length.
     * If the maxlen specified is less than 10, return a string with length of 10.
     * @param maxlen
     *
     * @return the name of the bitstream
     */
    public static String getShortFileName(String name, int maxlen)
    {
        if (name == null) {
            return null;
        }

        // If the maxlen specified is less than 10, return a string with length of 10.
        if (maxlen < 10) {
            maxlen = 10;
        }

        if (name.length() > maxlen) {
            String prefix = null;
            String suffix = null;
            // the plan is to split the name into two parts:
            // the suffix will be the file extension plus the two letters before: e.g., "ly.txt"
            // if there is no file extension (or the file extension is longer than six), the suffix is the last six letters.
            // the prefix will be the first part of the name, truncated to the length needed to make the whole thing be maxlen.
            // so the whole thing is prefix...suffix
            String[] parts = name.split("\\.");
            if (parts.length > 1) {
                // there is a file extension.
                suffix = parts[parts.length - 1];
                suffix = name.substring(name.length() - suffix.length() - 3, name.length() - suffix.length() - 1) + "." + suffix;
            }

            if ((suffix == null) || (suffix.length() > 6)){
                // if the file extension is longer than six or it doesn't have a file extension:
                // the suffix will be the last six letters.
                suffix = name.substring(name.length() - 5, name.length());
            }
            suffix = "..." + suffix;
            prefix = name.substring(0, maxlen - suffix.length());
            return prefix + suffix;
        } else {
            return name;
        }
    }
}
