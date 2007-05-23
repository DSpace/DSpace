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
     * @see org.dspace.checker.BitstreamDispatcher#next()
     */
    public int next()
    {
        return (System.currentTimeMillis() > end) ? SENTINEL : delegate.next();
    }
}
