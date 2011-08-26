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
