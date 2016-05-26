/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

/**
 * This exception indicates a fatal error when instantiating a plugin class.
 * <p>
 * It should only be thrown when something unexpected happens in the
 * course of instantiating a plugin, e.g. an access error, class not found,
 * etc.  Simply not finding a class in the configuration is not an exception.
 * <p>
 * This is a RuntimeException so it doesn't have to be declared, and can
 * be passed all the way up to a generalized fatal exception handler.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.core.service.PluginService
 */

public class PluginInstantiationException extends RuntimeException
{
    /**
     * @param msg Error message text.
     */
    public PluginInstantiationException(String msg)
    {
        super(msg);
    }

    /**
     * @param msg Error message text.
     * @param cause other exception that this one is wrapping.
     */
    public PluginInstantiationException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    /**
     * @param cause other exception that this one is wrapping.
     */
    public PluginInstantiationException(Throwable cause)
    {
        super(cause);
    }
}
