/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
