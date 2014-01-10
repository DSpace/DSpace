/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.core.Context;
import org.dspace.content.Bitstream;

import java.sql.SQLException;

public abstract class ItemDAO
{
    protected Context context;

    protected ItemDAO(Context ctx)
    {
        context = ctx;
    }

    public abstract Bitstream getPrimaryBitstream(int itemId, String bundleName) throws SQLException;

    public abstract Bitstream getFirstBitstream(int itemId, String bundleName) throws SQLException;

    public abstract Bitstream getNamedBitstream(int itemId, String bundleName, String fileName) throws SQLException;
}
