/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.dspace.content.Bitstream;

import java.util.*;

/**
 * Really simple dispatcher that just iterates over a pre-defined list of ids.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public class IteratorDispatcher implements BitstreamDispatcher
{
    /**
     * List of Integer ids.
     */
    protected Iterator<Bitstream> bitstreams = null;

    /**
     * Blanked off, no-op constructor. Do not use.
     */
    private IteratorDispatcher()
    {
    }

    /**
     * Main constructor.
     * 
     * @param bitstreams bitstream iterator
     */
    public IteratorDispatcher(Iterator<Bitstream> bitstreams)
    {
        this.bitstreams = bitstreams;
    }

    /**
     * @see org.dspace.checker.BitstreamDispatcher#next()
     */
    @Override
    public synchronized Bitstream next()
    {
        if(bitstreams != null && bitstreams.hasNext())
        {
            return bitstreams.next();
        }else{
            return null;
        }
    }
}
