/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

/**
 * Just a quick SortException class to give us the relevant data type
 */
public class SortException extends Exception
{

    public SortException()
    {
        super();
    }

    public SortException(String message)
    {
        super(message);
    }

	public SortException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public SortException(Throwable cause)
	{
		super(cause);
	}

}
