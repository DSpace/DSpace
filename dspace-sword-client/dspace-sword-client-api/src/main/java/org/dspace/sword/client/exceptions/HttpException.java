/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.sword.client.exceptions;

/**
 * User: Robin Taylor
 * Date: 15/02/11
 * Time: 21:12
 */

public class HttpException extends Exception
{

	public HttpException()
	{
		super();
	}

	public HttpException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

	public HttpException(String arg0)
	{
		super(arg0);
	}

	public HttpException(Throwable arg0)
	{
		super(arg0);
	}

}