/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

/**
 * @author Graham Triggs
 *
 * An exception to report any problems with registry exports
 */
public class RegistryExportException extends Exception
{
    /**
     * Create an empty authorize exception
     */
    public RegistryExportException()
    {
        super();
    }

    /**
     * create an exception with only a message
     * 
     * @param message exception message
     */
    public RegistryExportException(String message)
    {
        super(message);
    }
    
    /**
     * create an exception with an inner exception and a message
     * 
     * @param message exception message
     * @param e reference to Throwable
     */
    public RegistryExportException(String message, Throwable e)
    {
        super(message, e);
    }
    
    /**
     * create an exception with an inner exception
     * 
     * @param e reference to Throwable
     */
    public RegistryExportException(Throwable e)
    {
        super(e);
    }
    
}
