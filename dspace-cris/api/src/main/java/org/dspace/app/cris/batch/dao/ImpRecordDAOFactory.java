/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
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
