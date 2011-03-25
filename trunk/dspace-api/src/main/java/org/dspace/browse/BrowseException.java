/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

/**
 * Just a quick BrowseException class to give us the relevant data type
 * 
 * @author Richard Jones
 */
public class BrowseException extends Exception
{

    public BrowseException()
    {
        super();
    }
    
    public BrowseException(String message)
    {
        super(message);
    }

	public BrowseException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public BrowseException(Throwable cause)
	{
		super(cause);
	}

}
