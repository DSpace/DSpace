/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.dspace.content.Bitstream;

import java.sql.SQLException;
import org.dspace.core.factory.CoreServiceFactory;

/**
 * Decorator that dispatches a specified number of bitstreams from a delegate
 * dispatcher.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public class LimitedCountDispatcher implements BitstreamDispatcher
{
    /** The remaining number of bitstreams to iterate through. */
    private int remaining = 1;

    /** The dispatcher to delegate to for retrieving bitstreams. */
    private BitstreamDispatcher delegate = null;

    /**
     * Default constructor uses LegacyPluginServiceImpl
     */
    public LimitedCountDispatcher()
    {
        this((BitstreamDispatcher) CoreServiceFactory.getInstance().getPluginService()
                .getSinglePlugin(BitstreamDispatcher.class));
    }

    /**
     * Constructor.
     * 
     * @param del
     *            The bitstream distpatcher to delegate to.
     * @param count
     *            the number of bitstreams to check.
     */
    public LimitedCountDispatcher(BitstreamDispatcher del, int count)
    {
        this(del);
        remaining = count;
    }

    /**
     * Constructor.
     * 
     * @param del
     *            The bitstream distpatcher to delegate to.
     */
    public LimitedCountDispatcher(BitstreamDispatcher del)
    {
        delegate = del;
    }

    /**
     * Retreives the next bitstream to be checked.
     * 
     * @return the bitstream
     * @throws SQLException if database error
     */
    @Override
    public Bitstream next() throws SQLException {
        if (remaining > 0)
        {
            remaining--;

            return delegate.next();
        }
        else
        {
            return null;
        }
    }
}
