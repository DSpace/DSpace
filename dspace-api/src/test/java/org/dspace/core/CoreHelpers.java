/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

/**
 * Methods for breaking rules that hamper testing.  Not to be used in production.
 *
 * @author mwood
 */
public class CoreHelpers
{
    /**
     * Expose a Context's DBConnection.
     *
     * @param ctx
     * @return
     */
    static public DBConnection getDBConnection(Context ctx)
    {
        return ctx.getDBConnection();
    }
}
