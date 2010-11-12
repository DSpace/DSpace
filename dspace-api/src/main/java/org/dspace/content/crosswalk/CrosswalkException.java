/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

/**
 * Superclass for more-specific crosswalk exceptions.
 * Use this class in declarations and catchers to simplify code
 * and allow for new exception types to be added.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class CrosswalkException extends Exception
{
    public CrosswalkException()
    {
        super();
    }

    public CrosswalkException(String s, Throwable t)
    {
        super(s, t);
    }

    public CrosswalkException(String s)
    {
        super(s);
    }

    public CrosswalkException(Throwable t)
    {
        super(t);
    }
}
