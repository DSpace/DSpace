/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.dav;


/**
 * Container for all exceptions thrown by DSpace LNI SOAP methods.
 */
public class LNIRemoteException extends java.rmi.RemoteException
{
    
    /**
     * Instantiates a new LNI remote exception.
     */
    protected LNIRemoteException()
    {
        super();
    }

    /**
     * Instantiates a new LNI remote exception.
     * 
     * @param message the message
     */
    protected LNIRemoteException(String message)
    {
        super(message);
    }

    /**
     * Instantiates a new LNI remote exception.
     * 
     * @param message the message
     * @param thrown the thrown
     */
    protected LNIRemoteException(String message, Throwable thrown)
    {
        super(message, thrown);
    }
}
