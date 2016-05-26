/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

/**
 * @author Richard Jones
 *
 * An exception to report any problems with registry imports
 */
public class RegistryImportException extends Exception
{
    /**
     * Create an empty authorize exception
     */
    public RegistryImportException()
    {
        super();
    }

    /**
     * create an exception with only a message
     * 
     * @param message error message
     */
    public RegistryImportException(String message)
    {
        super(message);
    }
    
    /**
     * create an exception with an inner exception and a message
     * 
     * @param	message error message
     * @param	e throwable
     */
    public RegistryImportException(String message, Throwable e)
    {
    	super(message, e);
    }
    
    /**
     * create an exception with an inner exception
     * 
     * @param	e throwable
     */
    public RegistryImportException(Throwable e)
    {
    	super(e);
    }
    
}
