/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

/**
 * This Exception class can be thrown by the internals of the
 * DSpace SWORD implementation
 *
 * @author Richard Jones
 *
 */
public class DSpaceSWORDException extends Exception
{

    public DSpaceSWORDException()
    {
        super();
    }

    public DSpaceSWORDException(String arg0, Throwable arg1)
    {
        super(arg0, arg1);
    }

    public DSpaceSWORDException(String arg0)
    {
        super(arg0);
    }

    public DSpaceSWORDException(Throwable arg0)
    {
        super(arg0);
    }

}
