package org.dspace.content.dao;

import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;

/**
 * Created by IntelliJ IDEA.
 * User: Graham
 * Date: 19-Dec-2007
 * Time: 13:13:51
 * To change this template use File | Settings | File Templates.
 */
public class ItemDAOFactory
{
    public static ItemDAO getInstance(Context context)
    {
        if (ConfigurationManager.getProperty("db.name").equalsIgnoreCase("oracle"))
        {
            return new ItemDAOOracle(context);
        }

        return new ItemDAOPostgres(context);
    }
}
