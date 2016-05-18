/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

/**
 *    Exception class for BitstreamFilters
 *
 */
public class BitstreamFilterException extends Exception 
{
	
	private static final long serialVersionUID = 1L;
	
	public BitstreamFilterException() {}
        /**
         * 
         * @param msg exception message
         */
	public BitstreamFilterException(String msg)
	{
		super(msg);
	}
        /**
         * 
         * @param e exception
         */
	public BitstreamFilterException(Exception e)
	{
		super(e);
	}
	
}
