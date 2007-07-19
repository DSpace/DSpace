/*
 * Site.java
 *
 * Version: $Revision: 1.8 $
 *
 * Date: $Date: 2005/04/20 14:22:34 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content;

import java.sql.SQLException;
import java.net.URI;
import java.io.IOException;


import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.authorize.AuthorizeException;

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

    private static Site theSite = null;

    /**
     * Get the type of this object, found in Constants
     *
     * @return type of the object
     */
    public int getType()
    {
        return Constants.SITE;
    }

    /**
     * Get the internal ID (database primary key) of this object
     *
     * @return internal ID of object
     */
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
    public String getHandle()
    {
        return getSiteHandle();
    }

    /**
     * Static method to return site Handle without creating a Site.
     * @returns handle of the Site.
     */
    public static String getSiteHandle()
    {
        if (handle == null)
            handle = ConfigurationManager.getProperty("handle.prefix")+"/"+
                String.valueOf(SITE_ID);
        return handle;
    }

    /**
     * Get Site object corresponding to db id (which is ignroed).
     * @param context the context.
     * @param id integer database id, ignored.
     * @returns Site object.
     */
    public static DSpaceObject find(Context context, int id)
        throws SQLException
    {
        if (theSite == null)
            theSite = new Site();
        return theSite;
    }

    void delete()
        throws SQLException, AuthorizeException, IOException
    {
    }

    public void update()
        throws SQLException, AuthorizeException, IOException
    {
    }

    public String getName()
    {
        return ConfigurationManager.getProperty("dspace.name");
    }
}
