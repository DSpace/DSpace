/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import org.dspace.content.DSpaceObject;

/**
 * Exception indicating the current user of the context does not have permission
 * to perform a particular action.
 * 
 * @author David Stuve
 * @version $Revision$
 */
public class AuthorizeException extends Exception
{
    private int myaction; // action attempted, or -1

    private DSpaceObject myobject; // object action attempted on or null

    /**
     * Create an empty authorize exception
     */
    public AuthorizeException()
    {
        super();

        myaction = -1;
        myobject = null;
    }

    /**
     * create an exception with only a message
     * 
     * @param message
     */
    public AuthorizeException(String message)
    {
        super(message);

        myaction = -1;
        myobject = null;
    }

    public AuthorizeException(Throwable throwable)
    {
        super(throwable);

        myaction = -1;
        myobject = null;
    }

    /**
     * Create an authorize exception with a message
     * 
     * @param message
     *            the message
     * @param o object
     * @param a actionID
     */
    public AuthorizeException(String message, DSpaceObject o, int a)
    {
        super(message);

        myobject = o;
        myaction = a;
    }

    public int getAction()
    {
        return myaction;
    }

    public DSpaceObject getObject()
    {
        return myobject;
    }
}
