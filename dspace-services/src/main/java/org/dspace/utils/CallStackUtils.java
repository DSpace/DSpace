/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.utils;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

import java.lang.StackWalker.StackFrame;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Utility methods for manipulating call stacks.
 *
 * @author mwood
 */
public class CallStackUtils {
    private CallStackUtils() {}

    /**
     * Log the class, method and line of the caller's caller.
     *
     * @param log logger to use.
     * @param level log at this level, if enabled.
     */
    static public void logCaller(Logger log, Level level) {
        if (log.isEnabled(level)) {
            StackWalker stack = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);
            StackFrame caller = stack.walk(stream -> stream.skip(2)
                    .findFirst()
                    .get());
            String callerClassName = caller.getDeclaringClass().getCanonicalName();
            String callerMethodName = caller.getMethodName();
            int callerLine = caller.getLineNumber();
            log.log(level, "Called from {}.{} line {}.",
                    callerClassName, callerMethodName, callerLine);
        }
    }
}
