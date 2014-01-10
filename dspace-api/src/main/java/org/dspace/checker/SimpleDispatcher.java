/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.util.Date;

/**
 * An implementation of the selection strategy that selects bitstreams in the
 * order that they were last checked, looping endlessly.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public class SimpleDispatcher implements BitstreamDispatcher
{

    /**
     * Should this dispatcher keep on dispatching around the collection?
     */
    private boolean loopContinuously = false;

    /**
     * Date this dispatcher started dispatching.
     */
    private Date processStartTime = null;

    /**
     * Access for bitstream information
     */
    private BitstreamInfoDAO bitstreamInfoDAO;

    /**
     * Creates a new SimpleDispatcher.
     * 
     * @param startTime
     *            timestamp for beginning of checker process
     * @param looping
     *            indicates whether checker should loop infinitely through
     *            most_recent_checksum table
     */
    public SimpleDispatcher(BitstreamInfoDAO bitstreamInfoDAO, Date startTime, boolean looping)
    {
        this.bitstreamInfoDAO = bitstreamInfoDAO;
        this.processStartTime = (startTime == null ? null : new Date(startTime.getTime()));
        this.loopContinuously = looping;
    }

    /**
     * Blanked off, no-op constructor. Do not use.
     */
    private SimpleDispatcher()
    {
    }

    /**
     * Selects the next candidate bitstream.
     * 
     * @see org.dspace.checker.BitstreamDispatcher#next()
     */
    public synchronized int next()
    {
        // should process loop infinitely through the
        // bitstreams in most_recent_checksum table?
        if (!loopContinuously && (processStartTime != null))
        {
            return bitstreamInfoDAO.getOldestBitstream(new java.sql.Timestamp(
                    processStartTime.getTime()));
        }
        else
        {
            return bitstreamInfoDAO.getOldestBitstream();
        }

    }
}
