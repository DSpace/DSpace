/*
 * Bundle.java
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


/**
 * Class representing bundles of bitstreams stored in the DSpace system
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class Bundle
{
    /**
     * Get a bundle from the database.  The bundle and bitstream metadata are
     * all loaded into memory.
     *
     * @param  context  DSpace context object
     * @param  id       ID of the bundle
     *   
     * @return  the bundle, or null if the ID is invalid.
     */
    static Bundle find(Context context, int id)
        throws SQLException
    {
        return null;
    }
    
    /**
     * Create a new bundle, with a new ID.  Not inserted in database.
     *
     * @param  context  DSpace context object
     *
     * @return  the newly created bundle
     */
    public static Bundle create(Context context)
        throws AuthorizeException
    {
        return null;
    }


    /**
     * Get the bitstreams in this budnle
     *
     * @return the bitstreams
     */
    public List getBitstreams()
    {
        return null;
    }
    

    /**
     * Get the items this bundle appears in
     *
     * @return <code>List<code> of <code>Item</code>s this bundle appears
     *         in
     */
    public List getItems()
    {
        return null;
    }
    

    /**
     * Add a bitstream
     *
     * @param b  the bitstream to add
     */
    public void addBitstream( Bitstream b )
        throws AuthorizeException
    {
    }
    

    /**
     * Remove a bitstream
     *
     * @param b  the bitstream to remove
     */
    public void removeBitstream( Bitstream b )
        throws AuthorizeException
    {
    }


    /**
     * Update the bundle item, including any changes to bitstreams.
     */
    public void update()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Delete the bundle.  Any association between the bundle and bitstreams
     * or items are removed.  The bitstreams contained in the bundle are
     * NOT removed.
     */
    public void delete()
        throws SQLException, AuthorizeException
    {
    }


    /**
     * Delete the bundle, and any bitstreams it contains.  Any associations
     * with items are deleted.  However, bitstreams that are also contained
     * in other bundles are NOT deleted.
     */
    public void deleteWithContents()
        throws SQLException, AuthorizeException
    {
    }
}
