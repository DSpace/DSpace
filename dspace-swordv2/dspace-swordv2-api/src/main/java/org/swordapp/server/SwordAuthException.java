/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server;

public class SwordAuthException extends Exception
{
	private boolean retry = false;
	
    public SwordAuthException()
    {
        super();
    }

	public SwordAuthException(boolean retry)
    {
        super();
		this.retry = retry;
    }

    public SwordAuthException(String message)
    {
        super(message);
    }

    public SwordAuthException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SwordAuthException(Throwable cause)
    {
        super(cause);
    }

	public boolean isRetry()
	{
		return retry;
	}
}
