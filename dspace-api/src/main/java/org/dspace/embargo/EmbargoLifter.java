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
import org.dspace.content.Item;
import org.dspace.core.Context;


/**
 * Plugin interface for the embargo lifting function.
 *
 * @author Larry Stone
 * @author Richard Rodgers
 */
public interface EmbargoLifter
{
    /**
     * Implement the lifting of embargo in the "resource policies"
     * (access control) by (for example) turning on default read access to all
     * Bitstreams.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     the Item on which to lift the embargo
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public void liftEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException;
}
