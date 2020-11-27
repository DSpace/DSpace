/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Utility class to handle exceptions.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class ExceptionMessageUtils {

    private ExceptionMessageUtils() {

    }

    /**
     * Returns the root cause message of the given exception without the Exception
     * class name.
     *
     * @param  ex the exception
     * @return    the root exception message
     */
    public static String getRootMessage(Exception ex) {
        String message = ExceptionUtils.getRootCauseMessage(ex);
        return isNotEmpty(message) ? message.substring(message.indexOf(":") + 1).trim() : "Generic error";
    }
}
