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

public class ItemDAOOracle extends ItemDAO
{
    ItemDAOOracle(Context ctx)
    {
        super(ctx);
    }

    public Bitstream getPrimaryBitstream(int itemId, String bundleName) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Bitstream getFirstBitstream(int itemId, String bundleName) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Bitstream getNamedBitstream(int itemId, String bundleName, String fileName) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
