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
import java.util.Date;

/**
 * <p>
 * A delegating dispatcher that puts a time limit on the operation of another
 * dispatcher.
 * </p>
 * 
 * <p>
 * Unit testing this class would be possible by abstracting the system time into
 * an abstract clock. We decided this was not worth the candle.
 * </p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public class LimitedDurationDispatcher implements BitstreamDispatcher
{
    /**
     * The delegate dispatcher that will actually dispatch the jobs.
     */
    private BitstreamDispatcher delegate;

    /**
     * Milliseconds since epoch after which this dispatcher will stop returning
     * values.
     */
    private long end;

    /**
     * Blanked off constructor - do not use.
     */
    private LimitedDurationDispatcher()
    {
        end = 0L;
        delegate = null;
    }

    /**
     * Main constructor.
     * 
     * @param dispatcher
     *            Delegate dispatcher that will do the heavy lifting of the
     *            dispatching work.
     * @param endTime
     *            when this dispatcher will stop returning valid bitstream ids.
     */
    public LimitedDurationDispatcher(BitstreamDispatcher dispatcher,
            Date endTime)
    {
        delegate = dispatcher;
        end = endTime.getTime();
    }

    /**
     * @throws SQLException if database error
     * @see org.dspace.checker.BitstreamDispatcher#next()
     */
    @Override
    public Bitstream next() throws SQLException {
        return (System.currentTimeMillis() > end) ? null : delegate.next();
    }
}
