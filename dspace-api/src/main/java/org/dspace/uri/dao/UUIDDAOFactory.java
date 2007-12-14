package org.dspace.uri.dao;

import org.dspace.core.Context;
import org.dspace.uri.dao.postgres.UUIDDAOPostgres;

/**
 * Created by IntelliJ IDEA.
 * User: richard
 * Date: Dec 13, 2007
 * Time: 12:03:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class UUIDDAOFactory
{
    public static UUIDDAO getInstance(Context context)
    {
        return new UUIDDAOPostgres(context);
    }
}
