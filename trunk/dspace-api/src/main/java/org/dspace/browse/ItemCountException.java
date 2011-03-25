/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

/**
 * Exception type to handle item count specific problems
 * 
 * @author Richard Jones
 *
 */
public class ItemCountException extends Exception
{

	public ItemCountException()
	{
	}

	public ItemCountException(String message)
	{
		super(message);
	}

	public ItemCountException(Throwable cause)
	{
		super(cause);
	}

	public ItemCountException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
