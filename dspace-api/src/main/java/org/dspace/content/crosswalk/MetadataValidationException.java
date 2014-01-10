/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

/**
 *
 * This indicates a problem with the input metadata (for submission) or
 * item state (dissemination).  It is invalid or incomplete, or simply
 * unsuitable to be crosswalked.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class MetadataValidationException extends CrosswalkException
{
    public MetadataValidationException()
    {
        super();
    }

    public MetadataValidationException(String arg0, Throwable arg1)
    {
        super(arg0, arg1);
    }

    public MetadataValidationException(String arg0)
    {
        super(arg0);
    }

    public MetadataValidationException(Throwable arg0)
    {
        super(arg0);
    }
}
