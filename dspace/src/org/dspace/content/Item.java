/*
 * Item.java
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

package org.dspace.content;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;


/**
 * Class representing an item in DSpace
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class Item
{
    /**
     * Wild card character for Dublin Core metadata qualifiers/languages
     */
    public final static char ANY = '*';


    /**
     * Get an item from the database.  The item, its Dublin Core metadata,
     * and the bundle and bitstream metadata are all loaded into memory.
     *
     * @param  context  DSpace context object
     * @param  handle   Handle of the item
     *   
     * @return  the item, or null if the Handle is invalid.
     */
    public static Item find(Context context, String handle)
        throws SQLException
    {
        return null;
    }

    
    /**
     * Create a new item, with a new ID.  Not inserted in database.
     *
     * @param  context  DSpace context object
     *
     * @return  the newly created item
     */
    static Item create(Context context)
        throws AuthorizeException
    {
        return null;
    }


    /**
     * Find out if the item is part of the main archive
     *
     * @return  true if the item is in the main archive
     */
    public boolean isArchived()
    {
        return false;
    }


    /**
     * Get Dublin Core metadata for the item.  Passing in a <code>null</code>
     * value only matches Dublin Core fields where that qualifier or languages
     * is actually <code>null</code>.  Passing in <code>Item.ANY</code>
     * retrieves all metadata fields with any value for the qualifier or
     * language, including <code>null</code>
     * <P>
     * Examples:
     * <P>
     *   Return values of the unqualified "title" field, in any language.
     *   Qualified title fields (e.g. "title.uniform" are NOT returned<P>
     *      <code>item.getDC( "title", null, Item.ANY );</code>
     * <P>
     *   Return all US English values of the "title" element, with any qualifier
     *   (including unqualified.)<P>
     *      <code>item.getDC( "title", Item.ANY, "en_US" );</code>
     *
     * @param  element    the Dublin Core element.  Must be specified.
     * @param  qualifier  the qualifier.  <code>null</code> means unqualified,
     *                    and <code>Item.ANY</code> means any qualifier
     *                    (including unqualified.)
     * @param  lang       the ISO639 language code, optionally followed by
     *                    an underscore and the ISO3166 country code.
     *                    <code>null</code> means only values with no language
     *                    are returned, and <code>Item.ANY</code> means values
     *                    with any country code or no country code are returned.
     *
     * @return  values of the Dublin Core fields that match the parameters.
     */
    public String[] getDC(String element, String qualifier, String lang)
    {
        return null;
    }
    
    
    /**
     * Add Dublin Core metadata fields.  These are appended to existing values.
     * Use <code>clearDC</code> to remove values.
     *
     * @param  element    the Dublin Core element
     * @param  qualifier  the Dublin Core qualifer, or <code>null</code> for
     *                    unqualified
     * @param  lang       the ISO639 language code, optionally followed by
     *                    an underscore and the ISO3166 country code.
     *                    <code>null means the value has no language (e.g.
     *                    a date).
     * @param  values     the values to add.
     */
    public void addDC(String element,
                      String qualifier,
                      String lang,
                      String[] values )
        throws AuthorizeException
    {
    }


    /**
     * Clear Dublin Core metadata values.  As with <code>addDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.  <code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>item.clearDC(Item.ANY, Item.ANY, Item.ANY)</code>
     * will remove all Dublin Core metadata associated with an item.
     *
     * @param element     the Dublin Core element to remove, or
     *                    <code>Item.ANY</code>
     * @param  qualifier  the qualifier.  <code>null</code> means unqualified,
     *                    and <code>Item.ANY</code> means any qualifier
     *                    (including unqualified.)
     * @param  lang       the ISO639 language code, optionally followed by
     *                    an underscore and the ISO3166 country code.
     *                    <code>null</code> means only values with no language
     *                    are removed, and <code>Item.ANY</code> means values
     *                    with any country code or no country code are removed.
     */
    public void clearDC(String element, String qualifier, String lang)
        throws AuthorizeException
    {
    }
    

    /**
     * Get the handle of this item.  Returns <code>null</code> if no handle has
     * been assigned.
     *
     * @return  the handle
     */
    public String getHandle()
    {
        return null;
    }
    

    /**
     * Get the collections this item is in.
     *
     * @return the collections this item is in, if any.
     */
    public List getCollections()
        throws SQLException
    {
        return null;
    }
    

    /**
     * Get the bundles in this item
     *
     * @return the bundles
     */
    public List getBundles()
    {
        return null;
    }
    

    /**
     * Add a bundle
     *
     * @param b  the bundle to add
     */
    public void addBundle(Bundle b)
        throws AuthorizeException
    {
    }

    
    /**
     * Remove a bundle
     *
     * @param b  the bundle to remove
     */
    public void removeBundle(Bundle b)
        throws AuthorizeException
    {
    }
    

    /**
     * Add a single bitstream in a new bundle.  A bundle is created, and the
     * bitstream passed in made the single bitstream.
     *
     * @param bitstream  the bitstream to add
     */
    public void addSingleBitstream(Bitstream bitstream)
        throws AuthorizeException
    {
    }


    /**
     * Update the item, including Dublin Core metadata, and the bundle
     * and bitstream metadata, in the database.  Inserts if this is a new
     * item.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
    }
    

    /**
     * Delete the item.  Bundles and bitstreams are also deleted if they are
     * not also included in another item.  The Dublin Core metadata is deleted,
     * as are any associations with collections.
     */
    public void delete()
        throws SQLException, AuthorizeException
    {
    }
}
