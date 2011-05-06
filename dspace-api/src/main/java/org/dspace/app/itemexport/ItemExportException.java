/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

/**
 * An exception that can be thrown when error occur during item export
 */
public class ItemExportException extends Exception
{
    public static final int EXPORT_TOO_LARGE = 0;

    private int reason;

    public ItemExportException(int r, String message)
    {
        super(message);
        reason = r;
    }

    public int getReason()
    {
        return reason;
    }
}