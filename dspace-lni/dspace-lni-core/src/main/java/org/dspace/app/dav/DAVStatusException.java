/*
 * DAVStatusException.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.dav;


/**
 * Encapsulate the components of an HTTP status response, namely an integer code
 * and a text message. This lets a method pass a complete HTTP status up its
 * chain of callers via the exception mechanism.
 * 
 * @author Larry Stone
 */
public class DAVStatusException extends Exception
{
    
    /** The status. */
    private int status;

    /** The message. */
    private String message;

    /**
     * Instantiates a new DAV status exception.
     * 
     * @param status the status
     * @param msg the msg
     */
    protected DAVStatusException(int status, String msg)
    {
        super(String.valueOf(status) + " - " + msg);
        this.status = status;
        this.message = msg;
    }

    /**
     * Instantiates a new DAV status exception.
     * 
     * @param status the status
     * @param msg the msg
     * @param cause the cause
     */
    protected DAVStatusException(int status, String msg, Throwable cause)
    {
        super(String.valueOf(status) + " - " + msg, cause);
        this.status = status;
        this.message = msg;
    }

    /**
     * Returns an HTTP-format status line.
     * 
     * @return string representing HTTP status line (w/o trailing newline)
     */
    protected String getStatusLine()
    {
        return "HTTP/1.1 " + String.valueOf(this.status) + " " + this.message;
    }

    /**
     * Return the status code.
     * 
     * @return status code set in this exception.
     */
    protected int getStatus()
    {
        return this.status;
    }

    /**
     * Return the status message.
     * 
     * @return status message set in this exception.
     */
    @Override
    public String getMessage()
    {
        return this.message;
    }

}
