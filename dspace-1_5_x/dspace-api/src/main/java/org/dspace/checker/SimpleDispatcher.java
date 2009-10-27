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
     * The next bitstreamId to be returned. -1 is a sentinel value used to
     * indicate that there are no (more) bitstreams to give.
     */
    private int bitstreamId = -1;

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
    public SimpleDispatcher(BitstreamInfoDAO bitstreamInfoDAO, Date startTime,
            boolean looping)
    {
        this.bitstreamInfoDAO = bitstreamInfoDAO;
        this.processStartTime = startTime;
        this.loopContinuously = looping;
    }

    /**
     * Blanked off, no-op constructor. Do not use.
     */
    private SimpleDispatcher()
    {
        ;
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
