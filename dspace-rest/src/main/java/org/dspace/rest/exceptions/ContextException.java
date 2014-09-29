package org.dspace.rest.exceptions;

/**
 * Simple exception which only encapsulate classic exception. This exception is
 * only for exceptions caused by creating context.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
public class ContextException extends Exception
{

    private static final long serialVersionUID = 1L;

    Exception causedBy;

    public ContextException(String message, Exception causedBy)
    {
        super(message);
        this.causedBy = causedBy;
    }

    public Exception getCausedBy()
    {
        return causedBy;
    }

}
