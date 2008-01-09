package org.dspace.uri;

public class UnsupportedIdentifierException extends Exception
{
    public UnsupportedIdentifierException()
    {
        super();
    }

    public UnsupportedIdentifierException(String message)
    {
        super(message);
    }

    public UnsupportedIdentifierException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnsupportedIdentifierException(Throwable cause)
    {
        super(cause);
    }
}
