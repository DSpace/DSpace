/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.exceptions;


/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("serial")
public class CompilingException extends Exception
{

    public CompilingException()
    {
    }

    public CompilingException(String arg0)
    {
        super(arg0);
    }

    public CompilingException(Throwable arg0)
    {
        super(arg0);
    }

    public CompilingException(String arg0, Throwable arg1)
    {
        super(arg0, arg1);
    }

}
