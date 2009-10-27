/*
 * PackageException.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
     * Create a new exception with the given message.
     * @param s - diagnostic message.
     */
    public PackageException()
    {
        super();
    }

    public PackageException(String message)
    {
        super(message);
    }

    public PackageException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Create a new exception wrapping it around another exception.
     * @param e - exception specifying the cause of this failure.
     */
    public PackageException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Write details of this exception to the indicated logger.
     * Dump a stack trace to the log to aid in debugging.
     */
    public void log(Logger log)
    {
        log.error(toString());

        Throwable cause = getCause();
        if (cause != null)
        {
            if (cause.getCause() != null)
                cause = cause.getCause();
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
