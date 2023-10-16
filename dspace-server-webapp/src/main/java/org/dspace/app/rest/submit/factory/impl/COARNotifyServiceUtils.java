/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.app.rest.exception.DSpaceBadRequestException;

/**
 * Utility class to reuse methods related to COAR Notify Patch Operations
 */
public class COARNotifyServiceUtils {

    private COARNotifyServiceUtils() { }

    public static int extractIndex(String path) {
        Pattern pattern = Pattern.compile("/(\\d+)$");
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            throw new DSpaceBadRequestException("Index not found in the path");
        }
    }

    public static String extractPattern(String path) {
        Pattern pattern = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            return matcher.group(3);
        } else {
            throw new DSpaceBadRequestException("Pattern not found in the path");
        }
    }

}
