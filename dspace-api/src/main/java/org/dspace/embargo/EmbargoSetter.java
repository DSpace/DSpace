/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.embargo;

import java.sql.SQLException;
import java.io.IOException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Plugin interface for the embargo setting function.
 *
 * @author Larry Stone
 * @author Richard Rodgers
 */
public interface EmbargoSetter
{
    /**
     * Get lift date of embargo from the "terms" supplied in the
     * metadata (or other available state) of this Item.  Return null
     * if it is clear this should not be under embargo -- that is to be
     * expected since this method is invoked on all newly-archived Items.
     * <p>
     * Note that the value (if any) of the metadata field configured to
     * contain embargo terms is passed explicitly, but this method is
     * free to explore other metadata fields, and even Bitstream contents,
     * to determine the embargo status and lift date.
     * <p>
     * Expect this method to be called at the moment before the Item is
     * installed into the archive (i.e. after workflow).  This may be
     * significant if the embargo lift date is computed relative to the present.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     * @param terms value of the metadata field configured as embargo terms, if any.
     * @return absolute date on which the embargo is to be lifted, or null if none
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public DCDate parseTerms(Context context, Item item, String terms)
        throws SQLException, AuthorizeException;

    /**
     * Enforce embargo by (for example) turning off all read access to
     * bitstreams in this Item.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public void setEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException;

    /**
     * Check that embargo is properly set on Item.  For example: no read access
     * to bitstreams.  It is expected to report any noteworthy
     * discrepancies by writing on the stream System.err, although
     * logging is also encouraged.  Only report conditions that
     * constitute a risk of exposing Bitstreams that should be under
     * embargo -- e.g. readable Bitstreams or ORIGINAL bundles.  A
     * readable bundle named "TEXT" does not constitute a direct risk so
     * long as its member Bitstreams are not readable.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public void checkEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException;
}
