/*
 * HarvestedItemInfo.java
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

package org.dspace.search;

import java.util.List;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Simple container class containing information about a harvested DSpace item.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class HarvestedItemInfo
{
    /** Internal item ID (as opposed to item's OAI ID, which is the Handle) */
    public int itemID;

    /** The Handle, with no prefix */
    public String handle;

    /** The datestamp (ISO8601) */
    public String datestamp;
    
    /** The item.  Only filled out if requested */
    public Item item;
    
    /**
     * Containers (communities and collections).  Only filled out if
     * requested.  An example of how this is filled out:  Say the item
     * contained by collectionY in communityX, and collectionB in communityC.
     * This field is filled out as follows: <P>
     * <code>containers[0][0]</code> - ID of communityX <br>
     * <code>containers[0][1]</code> - ID of collectionY <br>
     * <code>containers[1][0]</code> - ID of communityA <br>
     * <code>containers[1][1]</code> - ID of collectionB <br>
     */
    public int[][] containers;
}
