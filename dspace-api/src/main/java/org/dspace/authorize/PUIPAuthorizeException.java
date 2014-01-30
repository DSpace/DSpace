/*
 * PUIPAuthorizeException.java
 *
 */
package org.dspace.authorize;

import org.dspace.content.DSpaceObject;

/**
 * Exception indicating the current user of the context does not have permission
 * to perform a particular action because they are not on the Princeton campus network..
 * 
 * @author Mark Ratliff
 */
public class PUIPAuthorizeException extends AuthorizeException
{

    /**
     * Create an empty authorize exception
     */
    public PUIPAuthorizeException()
    {
        super();
    }

    /**
     * create an exception with only a message
     * 
     * @param message
     */
    public PUIPAuthorizeException(String message)
    {
        super(message);
    }

    /**
     * Create an authorize exception with a message
     * 
     * @param message
     *            the message
     */
    public PUIPAuthorizeException(String message, DSpaceObject o, int a)
    {
        super(message, o, a);
    }
}
