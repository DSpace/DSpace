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
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

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

    protected Context context;

    /** Handle to retrieve bitstreams from. */
    protected String handle = null;

    /** Has the type of object the handle refers to been determined. */
    protected boolean init = false;

    /** the delegate to dispatch to. */
    protected IteratorDispatcher delegate = null;

    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    /**
     * Blanked off, no-op constructor.
     */
    private HandleDispatcher()
    {
    }

    /**
     * Main constructor.
     * 
     * @param context Context
     * @param hdl
     *            the handle to get bitstreams from.
     */
    public HandleDispatcher(Context context, String hdl)
    {
        this.context = context;
        handle = hdl;
    }

    /**
     * Private initialization routine.
     * 
     * @throws SQLException if database error
     *             if database access fails.
     */
    protected synchronized void init() throws SQLException {
        if (!init)
        {
            DSpaceObject dso = handleService.resolveToObject(context, handle);

            Iterator<Bitstream> ids = new ArrayList<Bitstream>().iterator();

            switch (dso.getType())
            {
                case Constants.BITSTREAM:
                    ids = Arrays.asList(((Bitstream) dso)).iterator();
                    break;

                case Constants.ITEM:
                    ids = bitstreamService.getItemBitstreams(context, (org.dspace.content.Item) dso);
                    break;

                case Constants.COLLECTION:
                    ids = bitstreamService.getCollectionBitstreams(context, (org.dspace.content.Collection) dso);
                    break;

                case Constants.COMMUNITY:
                    ids = bitstreamService.getCommunityBitstreams(context, (org.dspace.content.Community) dso);
                    break;
            }

            delegate = new IteratorDispatcher(ids);
            init = true;
        }
    }

    /**
     * Initializes this dispatcher on first execution.
     * 
     * @throws SQLException if database error
     * @see org.dspace.checker.BitstreamDispatcher#next()
     */
    @Override
    public Bitstream next() throws SQLException {
        if (!init)
        {
            init();
        }

        return delegate.next();
    }
}
