/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;

/**
 * This is a superclass for exceptions representing a failure when
 * importing or exporting a package.  E.g., unacceptable package format
 * or contents.  Implementations should throw one of the more specific
 * exceptions. This class is intended for declarations and catch clauses.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class PackageException extends Exception
{
    /**
     * Create a new exception with no message.
     */
    public PackageException()
    {
        super();
    }

    /**
     * Create a new exception with the given message.
     * @param message - message text.
     */
    public PackageException(String message)
    {
        super(message);
    }

    /**
     * Create a new exception wrapping the given underlying cause.
     * @param cause - exception specifying the cause of this failure.
     */
    public PackageException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Create a new exception wrapping it around another exception.
     * @param message - message text.
     * @param cause - exception specifying the cause of this failure.
     */
    public PackageException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Write details of this exception to the indicated logger.
     * Dump a stack trace to the log to aid in debugging.
     * @param log logger
     */
    public void log(Logger log)
    {
        log.error(toString());

        Throwable cause = getCause();
        if (cause != null)
        {
            if (cause.getCause() != null)
            {
                cause = cause.getCause();
            }
            StringWriter sw = new StringWriter();
            cause.printStackTrace(new PrintWriter(sw));
            log.error(sw.toString());
        }
    }

    public String toString()
    {
        String base = getClass().getName() + ": " + getMessage();
        return (getCause() == null) ? base :
            base + ", Reason: "+getCause().toString();
    }
}
