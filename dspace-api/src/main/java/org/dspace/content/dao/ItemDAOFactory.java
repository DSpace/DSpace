/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;

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
        if (DatabaseManager.isOracle())
        {
            return new ItemDAOOracle(context);
        }

        return new ItemDAOPostgres(context);
    }
}
