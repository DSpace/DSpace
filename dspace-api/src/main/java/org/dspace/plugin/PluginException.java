/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.plugin;

/**
 * General exception class for all code that runs as a plugin in DSpace
 * 
 * @author Richard Jones
 *
 */
public class PluginException extends Exception
{
	/**
	 * basic constructor
	 *
	 */
	public PluginException()
	{
		super();
	}
	
	/**
	 * Construct an exception with the passed message
	 * 
	 * @param message	a message for the exception
	 */
	public PluginException(String message)
	{
		super(message);
	}
	
	/**
	 * Construct an exception with the passed message to encapsulate
	 * the passed Throwable
	 * 
	 * @param message a message for the exception
	 * @param e	throwable which triggered this exception
	 */
	public PluginException(String message, Throwable e)
	{
		super(message, e);
	}
	
	/**
	 * Construct an exception to encapsulate the passed Throwable
	 * 
	 * @param e the throwable which triggered this exception
	 */
	public PluginException(Throwable e)
	{
		super(e);
	}
}