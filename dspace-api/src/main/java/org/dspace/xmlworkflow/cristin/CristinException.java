/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.cristin;

public class CristinException extends Exception
{
    public CristinException()
    {
        super();
    }

    public CristinException(String s)
    {
        super(s);
    }

    public CristinException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public CristinException(Throwable throwable)
    {
        super(throwable);
    }
}
