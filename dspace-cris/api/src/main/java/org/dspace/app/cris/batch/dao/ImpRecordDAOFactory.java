package org.dspace.app.cris.batch.dao;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

public class ImpRecordDAOFactory
{
    public static ImpRecordDAO getInstance(Context context)
    {
        if (ConfigurationManager.getProperty("db.name").equalsIgnoreCase("oracle"))
        {
            return new ImpRecordDAOOracle(context);
        }

        return new ImpRecordDAOPostgres(context);
    }
}
