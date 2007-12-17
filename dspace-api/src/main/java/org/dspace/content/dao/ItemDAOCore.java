/*
 * ItemDAOCore.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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
package org.dspace.content.dao;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.uri.ObjectIdentifier;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author James Rutherford
 */
public class ItemDAOCore extends ItemDAO
{
    public ItemDAOCore(Context context)
    {
        super(context);
    }

    public Item create() throws AuthorizeException
    {
        // first create the item record
        Item item = childDAO.create();

        // now assign an object identifier
        ObjectIdentifier oid = new ObjectIdentifier(true);
        item.setIdentifier(oid);

        log.info(LogManager.getHeader(context, "create_item",
                "item_id=" + item.getID() + ",uuid=" + oid.getCanonicalForm()));

        item.setLastModified(new Date());
        update(item);

        return item;
    }

    public Item retrieve(int id)
    {
        Item item = (Item) context.fromCache(Item.class, id);

        if (item == null)
        {
            item = childDAO.retrieve(id);
        }

        return item;
    }

    public void update(Item item) throws AuthorizeException
    {
        // Check authorisation. We only do write authorization if user is
        // not an editor
        if (!item.canEdit())
        {
            AuthorizeManager.authorizeAction(context, item, Constants.WRITE);
        }

        MetadataValueDAO mvDAO = MetadataValueDAOFactory.getInstance(context);
        MetadataFieldDAO mfDAO = MetadataFieldDAOFactory.getInstance(context);
        MetadataSchemaDAO msDAO = MetadataSchemaDAOFactory.getInstance(context);

        log.info(LogManager.getHeader(context, "update_item", "item_id="
                + item.getID()));

        // Update the associated Bundles & Bitstreams
        Bundle[] bundles = item.getBundles();

        // Delete any Bundles that were removed from the in-memory list
        for (Bundle dbBundle : bundleDAO.getBundles(item))
        {
            boolean deleted = true;
            for (Bundle bundle : bundles)
            {
                if (bundle.equals(dbBundle))
                {
                    // If the bundle still exists in memory, don't delete
                    deleted = false;
                    break;
                }
            }

            if (deleted)
            {
                unlink(item, dbBundle);
            }
        }

        // Now that we've cleared up the db, we make the Item <-> Bundle
        // link concrete.
        for (Bundle bundle : bundles)
        {
            link(item, bundle);
        }

        // Set sequence IDs for bitstreams in item
        int sequence = 0;

        // find the highest current sequence number
        for (Bundle bundle : bundles)
        {
            for (Bitstream bitstream : bundle.getBitstreams())
            {
                if (bitstream.getSequenceID() > sequence)
                {
                    sequence = bitstream.getSequenceID();
                }
            }
        }

        // start sequencing bitstreams without sequence IDs
        sequence++;

        for (Bundle bundle : bundles)
        {
            for (Bitstream bitstream : bundle.getBitstreams())
            {
                if (bitstream.getSequenceID() < 0)
                {
                    bitstream.setSequenceID(sequence);
                    sequence++;
                    bitstreamDAO.update(bitstream);
                }
            }

            bundleDAO.update(bundle);
        }

        // Next we take care of the metadata

        // First, we figure out what's in memory, and what's in the database
        List<MetadataValue> dbMetadata = mvDAO.getMetadataValues(item);
        List<DCValue> memMetadata = item.getMetadata();

        // Now we have Lists of metadata values stored in-memory and in the
        // database, we can go about saving changes.

        // Step 1: remove any metadata that is no longer in memory (this
        // includes values that may have changed, but since we allow
        // multiple values for a given field for an object, we can't tell
        // what's changed and what's just gone.

        for (MetadataValue dbValue : dbMetadata)
        {
            boolean deleted = true;

            for (DCValue memValue : memMetadata)
            {
                if (dbValue.equals(memValue))
                {
                    deleted = false;
                }
            }

            if (deleted)
            {
                mvDAO.delete(dbValue.getID());
            }
        }

        // Step 2: go through the list of in-memory metadata and save it to
        // the database if it's not already there.

        // Map counting number of values for each MetadataField
        // Values are Integers indicating number of values written for a
        // given MetadataField. Keys are MetadataField IDs.
        Map<Integer, Integer> elementCount =
                new HashMap<Integer, Integer>();

        MetadataSchema schema;
        MetadataField field;

        for (DCValue memValue : memMetadata)
        {
            boolean exists = false;
            MetadataValue storedValue = null;

            schema = msDAO.retrieveByName(memValue.schema);

            if (schema == null)
            {
                schema = msDAO.retrieve(MetadataSchema.DC_SCHEMA_ID);
            }

            field = mfDAO.retrieve(
                    schema.getID(), memValue.element, memValue.qualifier);

            // Work out the place number for ordering
            int current = 0;

            Integer key = field.getID();
            Integer currentInteger = elementCount.get(key);

            if (currentInteger != null)
            {
                current = currentInteger;
            }

            current++;
            elementCount.put(key, current);

            for (MetadataValue dbValue : dbMetadata)
            {
                if (dbValue.equals(memValue))
                {
                    // If it already exists, we make a note of the fact and
                    // hold on to a copy of the object so we can update it
                    // later.
                    exists = true;
                    storedValue = dbValue;
                    break;
                }
            }

            if (!exists)
            {
                MetadataValue value = mvDAO.create();
                value.setFieldID(field.getID());
                value.setItemID(item.getID());
                value.setValue(memValue.value);
                value.setLanguage(memValue.language);
                value.setPlace(current);
                mvDAO.update(value);
            }
            else
            {
                // Even if it already exists, the place may have changed.
                storedValue.setPlace(current);
                mvDAO.update(storedValue);
            }
        }

        // finally, deal with the item identifier/uuid
        ObjectIdentifier oid = item.getIdentifier();
        if (oid == null)
        {
            oid = new ObjectIdentifier(true);
            item.setIdentifier(oid);
        }
        oidDAO.update(item.getIdentifier());

        childDAO.update(item);
    }

    public void delete(int id) throws AuthorizeException
    {
        Item item = retrieve(id);

        context.removeCached(item, id);

        log.info(LogManager.getHeader(context, "delete_item", "item_id=" + id));

        // Remove bundles
        for (Bundle bundle : item.getBundles())
        {
            item.removeBundle(bundle);
            unlink(item, bundle);
        }

        // remove all of our authorization policies
        AuthorizeManager.removeAllPolicies(context, item);

        // remove the object identifier
        oidDAO.delete(item);

        childDAO.delete(id);
    }

    public void decache(Item item)
    {
        // Remove item and it's submitter from cache
        context.removeCached(item, item.getID());
        EPerson submitter = item.getSubmitter();

        // FIXME: I don't think we necessarily want to do this.
        if (submitter != null)
        {
            context.removeCached(submitter, submitter.getID());
        }

        // Remove bundles & bitstreams from cache if they have been loaded
        for (Bundle bundle : item.getBundles())
        {
            context.removeCached(bundle, bundle.getID());
            for (Bitstream bitstream : bundle.getBitstreams())
            {
                context.removeCached(bitstream, bitstream.getID());
            }
        }

        childDAO.decache(item);
    }

    public List<Item> getItems(MetadataValue value)
    {
        return getItems(value, null, null);
    }

    public void link(Item item, Bundle bundle) throws AuthorizeException
    {
        if (linked(item, bundle))
        {
            return;
        }

        AuthorizeManager.authorizeAction(context, item, Constants.ADD);

        log.info(LogManager.getHeader(context, "add_bundle", "item_id="
                + item.getID() + ",bundle_id=" + bundle.getID()));

        childDAO.link(item, bundle);
    }

    public void unlink(Item item, Bundle bundle) throws AuthorizeException
    {
        if (!linked(item, bundle))
        {
            return;
        }

        AuthorizeManager.authorizeAction(context, item, Constants.REMOVE);

        childDAO.unlink(item, bundle);

        // If the bundle is now orphaned, delete it.
        if (getParentItems(bundle).size() == 0)
        {
            // make the right to remove the bundle explicit because the
            // implicit relation has been removed. This only has to concern the
            // currentUser because he started the removal process and he will
            // end it too. also add right to remove from the bundle to remove
            // it's bitstreams.
            AuthorizeManager.addPolicy(context, bundle, Constants.DELETE,
                    context.getCurrentUser());
            AuthorizeManager.addPolicy(context, bundle, Constants.REMOVE,
                    context.getCurrentUser());

            // The bundle is an orphan, delete it
            bundleDAO.delete(bundle.getID());
        }
    }
}
