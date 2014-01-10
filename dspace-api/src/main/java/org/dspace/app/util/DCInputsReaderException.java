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
 * importing or exporting a package.  E.g., unacceptable package format
 * or contents.  Implementations should throw one of the more specific
 * exceptions. This class is intended for declarations and catch clauses.
 *
 * @author Larry Stone
 * @version $Revision: 3761 $
 */
public class DCInputsReaderException extends Exception
{
    /**
     * No-args constructor.
     */
    public DCInputsReaderException()
    {
        super();
    }

    /**
     * Constructor for a given message.
     * @param message diagnostic message.
     */
    public DCInputsReaderException(String message)
    {
        super(message);
    }

    /**
     * Constructor for a given cause.
     * @param cause throwable that caused this exception
     */
    public DCInputsReaderException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructor to create a new exception wrapping it around another exception.
     * @param message diagnostic message.
     * @param cause throwable that caused this exception
     */
    public DCInputsReaderException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
