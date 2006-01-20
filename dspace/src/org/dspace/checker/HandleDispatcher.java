/*
 * Copyright (c) 2004-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * A BitstreamDispatcher that checks all the bitstreams contained within an
 * item, collection or community referred to by Handle.
 * 
 * @author Jim Downing
 */
public class HandleDispatcher implements BitstreamDispatcher
{
    /** SQL query to retrieve bitstreams for a given item. */
    private static final String ITEM_BITSTREAMS = "SELECT b2b.bitstream_id "
            + "FROM bundle2bitstream b2b, item2bundle i2b WHERE "
            + "b2b.bundle_id=i2b.bundle_id AND i2b.item_id=?";

    /** SQL query to retrieve bitstreams for a given collection. */
    private static final String COLLECTION_BITSTREAMS = "SELECT b2b.bitstream_id "
            + "FROM bundle2bitstream b2b, item2bundle i2b, collection2item c2i WHERE "
            + "b2b.bundle_id=i2b.bundle_id AND c2i.item_id=i2b.item_id AND c2i.collection_id=?";

    /** SQL query to retrieve bitstreams for a given community. */
    private static final String COMMUNITY_BITSTREAMS = "SELECT b2b.bitstream_id FROM bundle2bitstream b2b, item2bundle i2b, collection2item c2i, community2collection c2c WHERE b2b.bundle_id=i2b.bundle_id AND c2i.item_id=i2b.item_id AND c2c.collection_id=c2i.collection_id AND c2c.community_id=?";

    /** Log 4j logger. */
    private static final Logger LOG = Logger.getLogger(HandleDispatcher.class);

    /** Handle to retrieve bitstreams from. */
    String handle = null;

    /** Has the type of object the handle refers to been determined. */
    Boolean init = Boolean.FALSE;

    /** the delegate to dispatch to. */
    ListDispatcher delegate = null;

    /**
     * Blanked off, no-op constructor.
     */
    private HandleDispatcher()
    {
        ;
    }

    /**
     * Main constructor.
     * 
     * @param hdl
     *            the handle to get bitstreams from.
     */
    public HandleDispatcher(String hdl)
    {
        handle = hdl;
    }

    /**
     * Private initialization routine.
     * 
     * @throws SQLException
     *             if database access fails.
     */
    private void init() throws SQLException
    {
        Context context = null;

        try
        {
            context = new Context();
            DSpaceObject dso = HandleManager.resolveToObject(context, handle);
            context.abort();

            List ids = new ArrayList();

            switch (dso.getType())
            {
            case Constants.BITSTREAM:
                ids.add(new Integer(dso.getID()));

                break;

            case Constants.ITEM:
                ids = getItemIds(dso.getID());

                break;

            case Constants.COLLECTION:
                ids = getCollectionIds(dso.getID());

                break;

            case Constants.COMMUNITY:
                ids = getCommunityIds(dso.getID());
            }

            delegate = new ListDispatcher(ids);
            init = Boolean.TRUE;
        }
        finally
        {
            // Abort the context if it's still valid
            if ((context != null) && context.isValid())
            {
                context.abort();
            }
        }
    }

    /**
     * Initializes this dispatcher on first execution.
     * 
     * @see org.dspace.checker.BitstreamDispatcher#next()
     */
    public int next() throws SQLException
    {
        synchronized (init)
        {
            if (init == Boolean.FALSE)
            {
                init();
            }
        }

        return delegate.next();
    }

    /**
     * Utility query method to get item ids.
     * 
     * @param id
     *            the item id
     * @return a list of bitstream ids for items.
     * @throws SQLException
     *             if database access fails
     */
    private List getItemIds(int id) throws SQLException
    {
        return getIdList(id, ITEM_BITSTREAMS);
    }

    /**
     * Utility query method.
     * 
     * @param id
     *            Collection id
     * @return a list of bitstream ids for collection
     * @throws SQLException
     *             if database access error occurs.
     */
    private List getCollectionIds(int id) throws SQLException
    {
        return getIdList(id, COLLECTION_BITSTREAMS);
    }

    /**
     * Utility query method.
     * 
     * @param id
     *            the community id
     * @return the bitstream ids.
     * @throws SQLException
     *             if a database access error occurs.
     */
    private List getCommunityIds(int id) throws SQLException
    {
        return getIdList(id, COMMUNITY_BITSTREAMS);
    }

    /**
     * Utility query method.
     * 
     * @param arg
     *            community/collection/item id.
     * @param query
     *            query to be excuted
     * @return list of bitstream ids.
     * @throws SQLException
     *             if database access occurs.
     */
    private List getIdList(int arg, String query) throws SQLException
    {
        List ids = new ArrayList();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        Context ctx = new Context();

        try
        {
            conn = ctx.getDBConnection();
            ps = conn.prepareStatement(query);
            ps.setInt(1, arg);

            rs = ps.executeQuery();

            while (rs.next())
            {
                ids.add(new Integer(rs.getInt(1)));
            }

            LOG.debug("Returned " + ids.size() + " ids for handle " + handle);
        }
        finally
        {
            if (rs != null)
            {
                rs.close();
            }

            if (ps != null)
            {
                ps.close();
            }

            ctx.complete();
        }

        return ids;
    }
}
