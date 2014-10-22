/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
