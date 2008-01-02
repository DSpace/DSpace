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
