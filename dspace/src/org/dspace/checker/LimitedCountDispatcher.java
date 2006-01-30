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

import java.sql.SQLException;

import org.dspace.core.PluginManager;

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
     * Default constructor uses PluginManager
     */
    public LimitedCountDispatcher()
    {
        this((BitstreamDispatcher) PluginManager
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
     * @return the bitstream id
     * @throws SQLException
     *             if database error occurs.
     */
    public int next()
    {
        if (remaining > 0)
        {
            remaining--;

            return delegate.next();
        }
        else
        {
            return BitstreamDispatcher.SENTINEL;
        }
    }
}
