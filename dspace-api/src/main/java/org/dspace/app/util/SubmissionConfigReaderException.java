/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

/**
 * This is a superclass for exceptions representing a failure when
 * loading the item submission configuration.  E.g., missing mandatory 
 * information, inconsistent data, etc
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SubmissionConfigReaderException extends Exception
{
    /**
     * No-args constructor.
     */
    public SubmissionConfigReaderException()
    {
        super();
    }

    /**
     * Constructor for a given message.
     * @param message diagnostic message.
     */
    public SubmissionConfigReaderException(String message)
    {
        super(message);
    }

    /**
     * Constructor for a given cause.
     * @param cause throwable that caused this exception
     */
    public SubmissionConfigReaderException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructor to create a new exception wrapping it around another exception.
     * @param message diagnostic message.
     * @param cause throwable that caused this exception
     */
    public SubmissionConfigReaderException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
