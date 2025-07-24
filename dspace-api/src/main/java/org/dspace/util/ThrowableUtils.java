/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

/**
 * Things you wish {@link Throwable} or some logging package would do for you.
 *
 * @author mwood
 */
public class ThrowableUtils {
    /**
     * Utility class:  do not instantiate.
     */
    private ThrowableUtils() { }

    /**
     * Trace a chain of {@code Throwable}s showing only causes.
     * Less voluminous than a stack trace.  Useful if you just want to know
     * what caused third-party code to return an uninformative exception
     * message.
     *
     * @param throwable the exception or whatever.
     * @return list of messages from each {@code Throwable} in the chain,
     *          separated by '\n'.
     */
    static public String formatCauseChain(Throwable throwable) {
        StringBuilder trace = new StringBuilder();
        trace.append(throwable.getMessage());
        Throwable cause = throwable.getCause();
        while (null != cause) {
            trace.append("\nCaused by:  ")
                    .append(cause.getClass().getCanonicalName()).append(' ')
                    .append(cause.getMessage());
            cause = cause.getCause();
        }
        return trace.toString();
    }
}
