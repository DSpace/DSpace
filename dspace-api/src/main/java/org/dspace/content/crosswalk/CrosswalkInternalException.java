/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

/**
 * Something went wrong inside the crosswalk, not necessarily caused by
 * the input or state (although it could be an incorrectly handled pathological
 * case).  Most likely caused by a configuration problem.  It deserves its own
 * exception because many crosswalks are configuration-driven (e.g. the XSLT
 * crosswalks) so configuration errors are likely to be common enough that
 * they ought to be easy to identify and debug.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class CrosswalkInternalException extends CrosswalkException
{
    public CrosswalkInternalException(String s)
    {
        super(s);
    }

    public CrosswalkInternalException(String arg0, Throwable arg1)
    {
        super(arg0, arg1);
    }

    public CrosswalkInternalException(Throwable arg0)
    {
        super(arg0);
    }
}
