/*
 * PolicySet.java - Command line hack for setting all of the
 *  policies for a collection's items
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

package org.dspace.authorize;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.browse.Browse;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSIndexer;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;


/**
 * Hack/Tool to set policies for items, bundles, and bitstreams
 *  to match their owning collection's READ policy
 *
 * To use: make sure a collection has a READ policy
 *  dsrun org.dspace.authorize.PolicySet ID
 *
 * Where ID is the ID of the collection
 *
 * Note, does not alter the policy of the collection's logo
 *  bitstream, if it has one
 *
 * This will be replaced soon by a much better tool.  ;-)
 *
 * @author   dstuve
 * @version  $Revision$
 */
public class PolicySet
{
    // invoke with collection id
    public static void main(String argv[])
        throws Exception
    {
        Context c = new Context();

        // turn off authorization
        c.setIgnoreAuthorization(true);

        int collection_id = Integer.parseInt(argv[0]);

        System.out.println("Collection ID: " + collection_id);


        Collection collection = Collection.find(c, collection_id);

        // find all items in a collection, and clone the collection's read policy
        ItemIterator items = collection.getItems();

        int mycount = 0;

        while( items.hasNext() )
        {
            mycount++;

            // now get the collection's read policies
            TableRowIterator tri = DatabaseManager.query(c,
                "resourcepolicy",
                "SELECT * FROM resourcepolicy WHERE " +
                "resource_type_id=" + Constants.COLLECTION + " AND " +
                "resource_id="      + collection_id        + " AND " +
                "action_id="        + Constants.READ       + ";" );

            Item i = items.next();
	        System.out.println(mycount + ": Replacing policy for item " + i.getID() );

            i.replaceAllPolicies(tri);
        }

    	c.complete();
    }
}
