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
     * @param context the DSpace context
     * @param item the Item on which to lift the embargo
     */
    public void liftEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException;
}
