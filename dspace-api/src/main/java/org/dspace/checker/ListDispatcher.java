/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.util.Collections;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

/**
 * Really simple dispatcher that just iterates over a pre-defined list of ids.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public class ListDispatcher implements BitstreamDispatcher
{
    /**
     * List of Integer ids.
     */
    Stack<Integer> bitstreams = new Stack<Integer>();

    /**
     * Blanked off, no-op constructor. Do not use.
     */
    private ListDispatcher()
    {
    }

    /**
     * Main constructor.
     * 
     * @param bitstreamIds
     */
    public ListDispatcher(List<Integer> bitstreamIds)
    {
        Collections.reverse(bitstreamIds);
        bitstreams.addAll(bitstreamIds);
    }

    /**
     * @see org.dspace.checker.BitstreamDispatcher#next()
     */
    public synchronized int next()
    {
        try
        {
            return bitstreams.pop().intValue();
        }
        catch (EmptyStackException e)
        {
            return SENTINEL;
        }
    }
}
