/*
 * InstallItem.java
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

/**
 * Support to install item in the archive
 *
 * @author   dstuve
 * @version  $Revision$
 */
public class InstallItem
{
    public static Item installItem(Context c, InProgressSubmission is)
        throws SQLException, IOException, AuthorizeException
    {
        return installItem(c, is, c.getCurrentUser());
    }

    public static Item installItem(Context c, InProgressSubmission is, EPerson e)
        throws SQLException, IOException, AuthorizeException
    {
        Item item = is.getItem();
        
        // create accession date
        DCDate now = DCDate.getCurrent();
        item.addDC("date", "accessioned", null, now.toString());
        item.addDC("date", "available",   null, now.toString());

        // create issue date if not present
        DCValue[] currentDateIssued = item.getDC("date", "issued", null);
        
        if(currentDateIssued.length == 0)
        {
            item.addDC("date", "issued", null, now.toString());
        }
        
        // create handle
        String handle = HandleManager.createHandle(c, item);

        // Add handle as identifier.uri DC value
        item.addDC("identifier", "uri", null, handle);

        // Add format.mimetype and format.extent DC values
        Bitstream[] bitstreams = item.getNonInternalBitstreams();
        
        for (int i = 0; i < bitstreams.length; i++)
        {
            BitstreamFormat bf = bitstreams[i].getFormat();
            item.addDC("format",
                "extent",
                null,
                String.valueOf(bitstreams[i].getSize()));
            item.addDC("format", "mimetype", null, bf.getMIMEType());
        }

        String provDescription = "Made available in DSpace on " + now +
            " (GMT). " + getBitstreamProvenanceMessage(item);

        if (currentDateIssued.length != 0)
        {
            DCDate d = new DCDate(currentDateIssued[0].value);
            provDescription = provDescription + "  Previous issue date: " +
                d.toString();
        }

        // Add provenance description
        item.addDC("description", "provenance", "en", provDescription);

        // create collection2item mapping
        is.getCollection().addItem(item);

        // create date.available

        // set in_archive=true
        item.setArchived(true);

        // save changes ;-)
        item.update();


        // add item to search and browse indices
        DSIndexer.indexItem(c, item);
        Browse.itemAdded(c, item);
        
        // remove in-progress submission
        is.deleteWrapper();

        // remove the submit authorization policies
        // and replace them with the collection's READ
        // policies
        // FIXME: this is an inelegant hack, but out of time!
        TableRowIterator tri = DatabaseManager.query(c,
            "resourcepolicy",
            "SELECT * FROM resourcepolicy WHERE " +
            "resource_type_id=" + Constants.COLLECTION       + " AND " +
            "resource_id="      + is.getCollection().getID() + " AND " +
            "action_id="      + Constants.READ );
        item.replaceAllPolicies(tri);

        return item;
    }


    /** generate provenance-worthy description of the bitstreams
     * contained in an item
     */
    public static String getBitstreamProvenanceMessage(Item myitem)
    {
        // Get non-internal format bitstreams
        Bitstream[] bitstreams = myitem.getNonInternalBitstreams();

        // Create provenance description
        String mymessage = "No. of bitstreams: " + bitstreams.length + "\n";

        // Add sizes and checksums of bitstreams
        for (int j = 0; j < bitstreams.length; j++)
        {
            mymessage = mymessage + bitstreams[j].getName() + ": " +
                bitstreams[j].getSize() + " bytes, checksum: " +
                bitstreams[j].getChecksum() + " (" + 
                bitstreams[j].getChecksumAlgorithm() + ")\n";
        }
        
        return mymessage;
    }
}
