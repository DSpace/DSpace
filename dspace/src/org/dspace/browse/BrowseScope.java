/*
 * BrowseScope.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Object which describes the desired parameters for a browse. A scope object
 * contains the following:
 * 
 * <dl>
 * <dt>scope</dt>
 * <dd>A {@link org.dspace.content.Community}, a
 * {@link org.dspace.content.Collection}, or null. If the scope is a community
 * or collection, browses return only objects within the community or
 * collection.</dd>
 * 
 * <dt>focus</dt>
 * <dd>The point at which a Browse begins. This can be a String, an
 * {@link org.dspace.content.Item}(given by either the Item object or its id),
 * or null. <br>
 * If a String, Browses begin with values lexicographically greater than or
 * equal to the String. <br>
 * If an Item, Browses begin with the value of the Item in the corresponding
 * browse index. If the item has multiple values in the index, the behavior is
 * undefined. <br>
 * If null, Browses begin at the start of the index.</dd>
 * 
 * <dt>total</dt>
 * <dd>The total number of results returned from a Browse. A total of -1 means
 * to return all results.</dd>
 * 
 * <dt>numberBefore</dt>
 * <dd>The maximum number of results returned previous to the focus.</dd>
 * </dl>
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class BrowseScope implements Cloneable
{
    /** The DSpace context */
    private Context context;

    /** The scope */
    private Object scope;

    /** The String or Item at which to start the browse. */
    private Object focus;

    /** Total results to return. -1 indicates all results. */
    private int total;

    /** Maximum number of results previous to the focus */
    private int numberBefore;

    // Internal variables used by Browse

    /** The type of browse */
    private int browseType;

    /** Whether results should be ascending or descending */
    private boolean ascending;

    /** Whether results should be sorted */
    private Boolean sort;

    /**
     * Create a browse scope with the given context. The default scope settings
     * are:
     * <ul>
     * <li>Include results from all of DSpace
     * <li>Start from the beginning of the given index
     * <li>Return 21 total results
     * <li>Return 3 values previous to focus
     * </ul>
     * 
     * @param context
     *            The DSpace context.
     */
    public BrowseScope(Context context)
    {
        this.context = context;
        scope = null;
        focus = null;
        total = 21;
        numberBefore = 3;
    }

    /**
     * Clone this object
     */
    public Object clone()
    {
        BrowseScope clone = new BrowseScope(context);

        clone.scope = scope;
        clone.focus = focus;
        clone.total = total;
        clone.numberBefore = numberBefore;
        clone.browseType = browseType;
        clone.ascending = ascending;
        clone.sort = sort;

        return clone;
    }

    /**
     * Set the browse scope to all of DSpace.
     */
    public void setScopeAll()
    {
        scope = null;
    }

    /**
     * Limit the browse to a community.
     * 
     * @param community
     *            The community to browse.
     */
    public void setScope(Community community)
    {
        scope = community;
    }

    /**
     * Limit the browse to a collection.
     * 
     * @param collection
     *            The collection to browse.
     */
    public void setScope(Collection collection)
    {
        scope = collection;
    }

    /**
     * Browse starts at item i. Note that if the item has more than one value
     * for the given browse, the results are undefined.
     * 
     * This setting is ignored for itemsByAuthor, byAuthor, and lastSubmitted
     * browses.
     * 
     * @param item
     *            The item to begin the browse at.
     */
    public void setFocus(Item item)
    {
        focus = item;
    }

    /**
     * Browse starts at value. If value is null, Browses begin from the start of
     * the index.
     * 
     * This setting is ignored for itemsByAuthor and lastSubmitted browses.
     * 
     * @param value
     *            The value to begin the browse at.
     */
    public void setFocus(String value)
    {
        focus = value.toLowerCase();
    }

    /**
     * Browse starts at the item with the given id. Note that if the item has
     * more than one value for the given browse index, the results are
     * undefined.
     * 
     * This setting is ignored for itemsByAuthor, byAuthor, and lastSubmitted
     * browses.
     * 
     * @param item_id
     *            The item to begin the browse at.
     */
    public void setFocus(int item_id)
    {
        focus = new Integer(item_id);
    }

    /**
     * Browse starts at beginning (default).
     */
    public void noFocus()
    {
        focus = null;
    }

    /**
     * Set the total returned to n. If n is -1, all results are returned.
     * 
     * @param n
     *            The total number of results to return
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
     * Set the maximum number of results to return previous to the focus.
     * 
     * @param n
     *            The maximum number of results to return previous to the focus.
     */
    public void setNumberBefore(int n)
    {
        this.numberBefore = n;
    }

    ////////////////////////////////////////
    // Accessor methods
    ////////////////////////////////////////

    /**
     * Return the context for the browse.
     * 
     * @return The context for the browse.
     */
    public Context getContext()
    {
        return context;
    }

    /**
     * Return the browse scope.
     * 
     * @return The browse scope.
     */
    public Object getScope()
    {
        return scope;
    }

    /**
     * Return the browse focus. This is either an
     * {@link org.dspace.content.Item}, an Integer (the Item id) or a String.
     * 
     * @return The focus of the browse.
     */
    public Object getFocus()
    {
        return focus;
    }

    /**
     * Return the maximum number of results to return. A total of -1 indicates
     * that all matching results should be returned.
     * 
     * @return The maximum number of results.
     */
    public int getTotal()
    {
        return total;
    }

    /**
     * Return the maximum number of results to return previous to the focus.
     * 
     * @return The maximum number of results previous to the focus.
     */
    public int getNumberBefore()
    {
        return numberBefore;
    }

    /**
     * Return true if there is no limit on the number of matches returned, false
     * otherwise.
     */
    public boolean hasNoLimit()
    {
        return total == -1;
    }

    /**
     * Return true if there is a focus for the browse, false otherwise.
     */
    public boolean hasFocus()
    {
        return focus != null;
    }

    ////////////////////////////////////////
    // Convenience methods at package scope
    ////////////////////////////////////////

    /**
     * Return true if the focus is an Item.
     */
    boolean focusIsItem()
    {
        return (focus instanceof Item) || (focus instanceof Integer);
    }

    /**
     * Return true if the focus is a String.
     */
    boolean focusIsString()
    {
        return (focus instanceof String);
    }

    /**
     * Return the focus item id.
     */
    int getFocusItemId()
    {
        if (!focusIsItem())
        {
            throw new IllegalArgumentException("Focus is not an Item");
        }

        if (focus instanceof Integer)
        {
            return ((Integer) focus).intValue();
        }

        return ((Item) focus).getID();
    }

    /**
     * Return the focus string value.
     */
    String getFocusValue()
    {
        return (String) focus;
    }

    /**
     * True if the scope is that of a collection.
     */
    boolean isCollectionScope()
    {
        if (scope == null)
        {
            return false;
        }

        return scope instanceof Collection;
    }

    /**
     * True if the scope is that of a community.
     */
    boolean isCommunityScope()
    {
        if (scope == null)
        {
            return false;
        }

        return scope instanceof Community;
    }

    /**
     * True if the scope is all of DSpace.
     */
    boolean isAllDSpaceScope()
    {
        return scope == null;
    }

    ////////////////////////////////////////
    // Internal browse fields
    ////////////////////////////////////////

    /**
     * Return the type of browse (one of the constants in Browse).
     */
    int getBrowseType()
    {
        return browseType;
    }

    void setBrowseType(int type)
    {
        browseType = type;
    }

    /**
     * If true, sort the results by title. If false, sort the results by date.
     * If null, do no sorting at all.
     */
    Boolean getSortByTitle()
    {
        return sort;
    }

    void setSortByTitle(Boolean s)
    {
        this.sort = s;
    }

    /**
     * If true, results are in ascending order. Otherwise, results are in
     * descending order.
     */
    boolean getAscending()
    {
        return ascending;
    }

    void setAscending(boolean a)
    {
        this.ascending = a;
    }

    /**
     * Return true if this BrowseScope is equal to another object, false
     * otherwise.
     * 
     * @param obj
     *            The object to compare to
     * @return True if this BrowseScope is equal to the other object, false
     *         otherwise.
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof BrowseScope))
        {
            return false;
        }

        BrowseScope other = (BrowseScope) obj;

        return _equals(scope, other.scope) && _equals(focus, other.focus)
                && _equals(sort, other.sort) && (total == other.total)
                && (browseType == other.browseType)
                && (ascending == other.ascending)
                && (numberBefore == other.numberBefore);
    }

    private boolean _equals(Object first, Object second)
    {
        if ((first == null) && (second == null))
        {
            return true;
        }

        if ((first != null) && (second == null))
        {
            return false;
        }

        if ((first == null) && (second != null))
        {
            return false;
        }

        return first.equals(second);
    }

    /*
     * Return a hashCode for this object.
     */
    public int hashCode()
    {
        return new StringBuffer().append(scope).append(focus).append(total)
                .append(numberBefore).append(browseType).append(ascending)
                .append(sort).toString().hashCode();
    }
}
