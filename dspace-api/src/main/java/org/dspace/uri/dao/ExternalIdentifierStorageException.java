package org.dspace.uri.dao;

public class ExternalIdentifierStorageException extends Exception
{
    public ExternalIdentifierStorageException()
    {
        super();
    }

    public ExternalIdentifierStorageException(String message)
    {
        super(message);
    }

    public ExternalIdentifierStorageException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ExternalIdentifierStorageException(Throwable cause)
    {
        super(cause);
    }
}
