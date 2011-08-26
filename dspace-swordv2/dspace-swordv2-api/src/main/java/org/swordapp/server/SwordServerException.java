/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server;

public class SwordServerException extends Exception
{
    public SwordServerException()
    {
        super();
    }

    public SwordServerException(String message)
    {
        super(message);
    }

    public SwordServerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SwordServerException(Throwable cause)
    {
        super(cause);
    }
}
