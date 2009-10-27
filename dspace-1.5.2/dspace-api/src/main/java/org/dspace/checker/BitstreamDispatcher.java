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


/**
 * <p>
 * BitstreamDispatchers are strategy objects that hand bitstream ids out to
 * workers. Implementations must be threadsafe.
 * </p>
 * 
 * <p>
 * The rationale behind the use of the Sentinel pattern (rather than the more
 * traditional iterator pattern or a cursor c.f. java.sql.ResultSet): -
 * </p>
 * <ol>
 * <li>To make it easy to make implementations thread safe - multithreaded
 * invocation of the dispatchers is seen as a next step use case. In simple
 * terms, this requires that a single method does all the work.</li>
 * <li>Shouldn't an exception as the sentinel, as reaching the end of a loop is
 * not an exceptional condition.</li>
 * </ol>
 * 
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public interface BitstreamDispatcher
{
    /**
     * This value should be returned by <code>next()</code> to indicate that
     * there are no more values.
     */
    public static int SENTINEL = -1;

    /**
     * Returns the next id for checking, or a sentinel value if there are no
     * more to check.
     * 
     * @return the next bitstream id, or BitstreamDispatcher.SENTINEL if there
     *         isn't another value
     * 
     */
    public int next();
}
