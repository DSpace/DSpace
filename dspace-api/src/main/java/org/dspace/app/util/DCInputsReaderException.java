/*
 * DCInputsReaderException.java
 *
 * Version: $Revision: 3761 $
 *
 * Date: $Date: 2009-05-07 00:18:02 -0400 (Thu, 07 May 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
