/*
 * BrowseScope.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

package org.dspace.browse;

import org.dspace.core.Context;
import org.dspace.content.*;

/**
 * Scope object for browse. The scope object contains the following:
 *
 *  scope: A Community, a collection, or null. If a community or
 *    collection, browses return only objects within the
 *    community or collection.
 *
 *  focus: The point at which a Browse begins. This can be a String,
 *      an Item, or null. If a String, Browses begin with values
 *      lexicographically greater than or equal to the String.
 *      If an Item, Browses begin with values
 *      If null, Browses
 *
 *  total: The total number of results returned from a Browse.
 *
 *  numberBefore: The number of results
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class BrowseScope
{
    /** The DSpace context */
    private Context context;

    /** The scope */
    private Object scope;

    /** The String or Item at which to start the browse. */
    private Object focus;

    /** Total results to return. -1 indicates all results. */
    private int total;

    private int numberBefore;

    /**
     * Create a browse scope with the given context.
     * The default scope settings are:
     *   + Include results from all of DSpace
     *   + Start from the beginning of the given index
     *   + Return 0 total results
     *   + Return 0 values before results
     *
     * @param context - The DSpace context.
     */
    public BrowseScope(Context context)
    {
        this.context = context;
    }

    /**
     * Set the browse scope to all of DSpace
     */
    public void setScopeAll()
    {
        scope = null;
    }

    /**
     * Set the browse scope to COMMUNITY.
     *
     * @param community - The community to browse.
     */
   public void setScope(Community community)
    {
        scope = community;
    }

    /**
     * Set the browse scope to COLLECTION.
     *
     * @param collection - The collection to browse.
     */
   public void setScope(Collection collection)
    {
        scope = collection;
    }

    /**
     * Browse starts at item i. Note that if the item has more
     * than one value for the given browse, the results are undefined.
     * (FIXME -- do we want to specify this at some point??)
     *
     * This setting is ignored for itemsByAuthor, byAuthor, and
     * lastSubmitted browses.
     *
     * @param item - The item to begin the browse at.
     */
    public void setFocus(Item item)
    {
        focus = item;
    }

    /**
     * Browse starts at VALUE. If VALUE is null, Browses begin from
     * the start of the index.
     *
     * This setting is ignored for itemsByAuthor and
     * lastSubmitted browses.
     *
     * @param value - The value to begin the browse at.
     */
    public void setFocus(String value)
    {
        focus = value;
    }

    /**
     * Browse starts at beginning (default)
     */
    public void noFocus()
    {
        focus = null;
    }

    /**
     * Set the total returned to n.
     * If n is -1, all results are returned.
     */
    public void setTotal(int n)
    {
        total = n;
    }

    /**
     * Return all results from browse.
     */
    public void setTotalAll()
    {
        setTotal(-1);
    }

    /**
     * Set the point
     */
    public void setNumberBefore(int n)
    {
        this.numberBefore = n;
    }

    ////////////////////////////////////////
    // Package-level accessor methods
    ////////////////////////////////////////

    /**
     * Return the context for the browse.
     */
    Context getContext()
    {
        return context;
    }

    /**
     * Return the browse scope.
     */
    Object getScope()
    {
        return scope;
    }

    /**
     * Return the browse focus. This is either an Item or a String.
     */
    Object getFocus()
    {
        return focus;
    }

    /**
     * Return the total
     */
    int getTotal()
    {
        return total;
    }

    /**
     * Return the number before
     */
    int getNumberBefore()
    {
        return numberBefore;
    }
}
