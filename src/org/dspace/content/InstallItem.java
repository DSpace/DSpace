/*
 * InstallItem.java
 *
 * $Id: InstallItem.java,v 1.21 2004/12/22 17:48:40 jimdowning Exp $
 *
 * Version: $Revision: 1.21 $
 *
 * Date: $Date: 2004/12/22 17:48:40 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSIndexer;

/**
 * Support to install item in the archive
 * 
 * @author dstuve
 * @version $Revision: 1.21 $
 */
public class InstallItem
{
    /**
     * <pre>
     * Revision History
     *
     *   2005/05/14: Ben
     *    - add ability to submit to multiple collections
     */
   
    /**
     * Take an InProgressSubmission and turn it into a fully-archived Item.
     * 
     * @param Context
     * @param InProgressSubmission
     */
    public static Item installItem(Context c, InProgressSubmission is)
            throws SQLException, IOException, AuthorizeException
    {
        return installItem(c, is, c.getCurrentUser());
    }

    /**
     * Take an InProgressSubmission and turn it into a fully-archived Item.
     * 
     * @param Context
     * @param InProgressSubmission
     * @param handle
     *            to assign instead of creating new one
     */
    public static Item installItem(Context c, InProgressSubmission is,
            String handle) throws SQLException, IOException, AuthorizeException
    {
        return installItem(c, is, c.getCurrentUser(), handle);
    }

    /**
     * Take an InProgressSubmission and turn it into a fully-archived Item.
     * 
     * @param Context
     * @param InProgressSubmission
     * @param EPerson
     *            (unused, should be removed from API)
     * @param previous
     *            handle
     */
    public static Item installItem(Context c, InProgressSubmission is,
            EPerson e2, String suppliedHandle) throws SQLException,
            IOException, AuthorizeException
    {
        Item item = is.getItem();
        String handle;

        // set the language to default if it's not set already
        DCValue[] dc = item.getDC("language", "iso", Item.ANY);

        if (dc.length < 1)
        {
            // Just set default language
            item.addDC("language", "iso", null, ConfigurationManager
                    .getProperty("default.language"));
        }

        // create accession date
        DCDate now = DCDate.getCurrent();
        item.addDC("date", "accessioned", null, now.toString());
        item.addDC("date", "available", null, now.toString());

        // create issue date if not present
        DCValue[] currentDateIssued = item.getDC("date", "issued", Item.ANY);

        if (currentDateIssued.length == 0)
        {
            item.addDC("date", "issued", null, now.toString());
        }

        // if no previous handle supplied, create one
        if (suppliedHandle == null)
        {
            // create handle
            handle = HandleManager.createHandle(c, item);
        }
        else
        {
            handle = HandleManager.createHandle(c, item, suppliedHandle);
        }

        String handleref = HandleManager.getCanonicalForm(handle);

        // Add handle as identifier.uri DC value
        item.addDC("identifier", "uri", null, handleref);

        // Add format.mimetype and format.extent DC values
        Bitstream[] bitstreams = item.getNonInternalBitstreams();

        for (int i = 0; i < bitstreams.length; i++)
        {
            BitstreamFormat bf = bitstreams[i].getFormat();
            item.addDC("format", "extent", null, String.valueOf(bitstreams[i]
                    .getSize())
                    + " bytes");
            item.addDC("format", "mimetype", null, bf.getMIMEType());
        }

        String provDescription = "Made available in DSpace on " + now
                + " (GMT). " + getBitstreamProvenanceMessage(item);

        if (currentDateIssued.length != 0)
        {
            DCDate d = new DCDate(currentDateIssued[0].value);
            provDescription = provDescription + "  Previous issue date: "
                    + d.toString();
        }

        // Add provenance description
        item.addDC("description", "provenance", "en", provDescription);

        // create collection2item mapping
        is.getCollection().addItem(item);

        // set owning collection
        item.setOwningCollection(is.getCollection());

	// add additional mapped collections
	Collection collections[] = is.getMapCollections();
	for (int i=0; i < collections.length; i++) {
	    collections[i].addItem(item);
	}

        // set in_archive=true
        item.setArchived(true);

        // save changes ;-)
        item.update();

        // add item to search and browse indices
        DSIndexer.indexContent(c, item);

        // remove in-progress submission
        is.deleteWrapper();

        // remove the item's policies and replace them with
        // the defaults from the collection
        item.inheritCollectionDefaultPolicies(is.getCollection());

        return item;
    }

    /**
     * Take an InProgressSubmission and turn it into a fully-archived Item, no
     * previous handle specified
     * 
     * @param Context
     * @param InProgressSubmission
     * @param EPerson
     *            (unused, should be removed from API)
     */
    public static Item installItem(Context c, InProgressSubmission is,
            EPerson e2) throws SQLException, IOException, AuthorizeException
    {
        return installItem(c, is, e2, null);
    }

    /**
     * generate provenance-worthy description of the bitstreams contained in an
     * item
     * 
     * @param item
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
            mymessage = mymessage + bitstreams[j].getName() + ": "
                    + bitstreams[j].getSize() + " bytes, checksum: "
                    + bitstreams[j].getChecksum() + " ("
                    + bitstreams[j].getChecksumAlgorithm() + ")\n";
        }

        return mymessage;
    }
}
