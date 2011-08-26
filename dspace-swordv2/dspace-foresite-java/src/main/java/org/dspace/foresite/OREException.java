/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

/**
 * @Author Richard Jones
 */
public class OREException extends Exception
{
    public OREException()
    {
        super();
    }

    public OREException(String s)
    {
        super(s);
    }

    public OREException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public OREException(Throwable throwable)
    {
        super(throwable);
    }
}
