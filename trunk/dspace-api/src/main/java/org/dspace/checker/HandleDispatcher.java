/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

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
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public class HandleDispatcher implements BitstreamDispatcher
{

    /** Log 4j logger. */
    private static final Logger LOG = Logger.getLogger(HandleDispatcher.class);

    /** Handle to retrieve bitstreams from. */
    private String handle = null;

    /** Has the type of object the handle refers to been determined. */
    private boolean init = false;

    /** the delegate to dispatch to. */
    private ListDispatcher delegate = null;

    /**
     * Database access for retrieving bitstreams
     */
    BitstreamInfoDAO bitstreamInfoDAO;

    /**
     * Blanked off, no-op constructor.
     */
    private HandleDispatcher()
    {
    }

    /**
     * Main constructor.
     * 
     * @param hdl
     *            the handle to get bitstreams from.
     */
    public HandleDispatcher(BitstreamInfoDAO bitInfoDAO, String hdl)
    {
        bitstreamInfoDAO = bitInfoDAO;
        handle = hdl;
    }

    /**
     * Private initialization routine.
     * 
     * @throws SQLException
     *             if database access fails.
     */
    private synchronized void init()
    {
        if (!init)
        {
            Context context = null;
            int dsoType = -1;

            int id = -1;
            try
            {
                context = new Context();
                DSpaceObject dso = HandleManager.resolveToObject(context, handle);
                id = dso.getID();
                dsoType = dso.getType();
                context.abort();

            }
            catch (SQLException e)
            {
                LOG.error("init error " + e.getMessage(), e);
                throw new IllegalStateException("init error" + e.getMessage(), e);

            }
            finally
            {
                // Abort the context if it's still valid
                if ((context != null) && context.isValid())
                {
                    context.abort();
                }
            }

            List<Integer> ids = new ArrayList<Integer>();

            switch (dsoType)
            {
            case Constants.BITSTREAM:
                ids.add(Integer.valueOf(id));
                break;

            case Constants.ITEM:
                ids = bitstreamInfoDAO.getItemBitstreams(id);
                break;

            case Constants.COLLECTION:
                ids = bitstreamInfoDAO.getCollectionBitstreams(id);
                break;

            case Constants.COMMUNITY:
                ids = bitstreamInfoDAO.getCommunityBitstreams(id);
                break;
            }

            delegate = new ListDispatcher(ids);
            init = true;
        }
    }

    /**
     * Initializes this dispatcher on first execution.
     * 
     * @see org.dspace.checker.BitstreamDispatcher#next()
     */
    public int next()
    {
        if (!init)
        {
            init();
        }

        return delegate.next();
    }
}
