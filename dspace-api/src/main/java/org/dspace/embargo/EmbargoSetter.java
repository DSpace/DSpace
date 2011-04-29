/*
 * EmbargoSetter.java
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2009/08/05 21:59:34 $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
package org.dspace.embargo;

import java.util.Date;
import java.sql.SQLException;
import java.io.IOException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Plugin interface for the embargo setting function.
 *
 * @author Larry Stone
 * @author Richard Rodgers
 */
public interface EmbargoSetter
{
    /**
     * Get lift date of embargo from the "terms" supplied in the
     * metadata (or other available state) of this Item.  Return null
     * if it is clear this should not be under embargo -- that is to be
     * expected since this method is invoked on all newly-archived Items.
     * <p>
     * Note that the value (if any) of the metadata field configured to
     * contain embargo terms is passed explicitly, but this method is
     * free to explore other metadata fields, and even Bitstream contents,
     * to determine the embargo status and lift date.
     * <p>
     * Expect this method to be called at the moment before the Item is
     * installed into the archive (i.e. after workflow).  This may be
     * significant if the embargo lift date is computed relative to the present.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     * @param terms value of the metadata field configured as embargo terms, if any.
     * @return absolute date on which the embargo is to be lifted, or null if none
     */
    public DCDate parseTerms(Context context, Item item, String terms)
        throws SQLException, AuthorizeException, IOException;

    /**
     * Enforce embargo by e.g. turning off all read access to bitstreams in
     * this Item.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    public void setEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException;

    /**
     * Check that embargo is properly set on Item, e.g. no read access
     * to bitstreams.  It is expected to report any noteworthy
     * discrepencies by writing on the stream System.err, although
     * logging is also encouraged.  Only report conditions that
     * constitute a risk of exposing Bitstreams that should be under
     * embargo -- e.g. readable Bitstreams or ORIGINAL bundles.  A
     * readable bundle named "TEXT" does not constitute a direct risk so
     * long as its member Bitstreams are not readable.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    public void checkEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException;
}
