/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * Represents the root of the DSpace Archive.
 * By default, the handle suffix "0" represents the Site, e.g. "1721.1/0"
 */
public class Site extends DSpaceObject
{
    /** "database" identifier of the site */
    public static final int SITE_ID = 0;

    // cache for Handle that is persistent ID for entire site.
    private static String handle = null;

    private Site() { super(); }

    private Site(Context ctx) { super(ctx); }

    /**
     * Get the type of this object, found in Constants
     *
     * @return type of the object
     */
    @Override
    public int getType()
    {
        return Constants.SITE;
    }

    /**
     * Get the internal ID (database primary key) of this object
     *
     * @return internal ID of object
     */
    @Override
    public int getID()
    {
        return SITE_ID;
    }

    /**
     * Get the Handle of the object. This may return <code>null</code>
     *
     * @return Handle of the object, or <code>null</code> if it doesn't have
     *         one
     */
    @Override
    public String getHandle()
    {
        return getSiteHandle();
    }

    /**
     * Static method to return site Handle without creating a Site.
     * @return handle of the Site.
     */
    public static String getSiteHandle()
    {
        if (handle == null)
        {
            handle = HandleManager.getPrefix() + "/" + String.valueOf(SITE_ID);
        }
        return handle;
    }

    /**
     * Get Site object corresponding to db id (which is ignored).  Essentially
     * this is a factory method for a fresh instance of Site.
     *
     * @param context the context.
     * @param id integer database id, ignored.
     * @return Site object.
     */
    public static DSpaceObject find(Context context, int id)
        throws SQLException
    {
        return new Site(context);
    }

    void delete()
        throws SQLException, AuthorizeException, IOException
    {
    }

    @Override
    public void update()
        throws SQLException, AuthorizeException
    {
    }

    @Override
    public String getName()
    {
        return ConfigurationManager.getProperty("dspace.name");
    }

    @Override
    public void updateLastModified()
    {

    }

    public String getURL()
    {
        return ConfigurationManager.getProperty("dspace.url");
    }
}
