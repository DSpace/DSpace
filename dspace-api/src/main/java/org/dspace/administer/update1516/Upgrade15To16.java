package org.dspace.administer.update1516;

import org.dspace.authorize.AuthorizeException;

/**
 * Created by IntelliJ IDEA.
 * User: richard
 * Date: Dec 13, 2007
 * Time: 4:06:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class Upgrade15To16
{
    public static void main(String[] args)
                throws Exception, AuthorizeException
    {
        MigrateUUID uuid = new MigrateUUID();
        uuid.migrate();
    }
}
