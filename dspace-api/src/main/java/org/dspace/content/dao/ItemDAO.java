/*
 * ItemDAO.java
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

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.dao.CRUD;
import org.dspace.storage.dao.Link;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;

public abstract class ItemDAO extends ContentDAO<ItemDAO>
        implements CRUD<Item>, Link<Item, Bundle>
{
    protected Logger log = Logger.getLogger(ItemDAO.class);

    protected Context context;
    protected BundleDAO bundleDAO;
    protected BitstreamDAO bitstreamDAO;
    protected ExternalIdentifierDAO identifierDAO;

    protected ItemDAO childDAO;

    public ItemDAO()
    {
    }

    public ItemDAO(Context context)
    {
        this.context = context;

        bundleDAO = BundleDAOFactory.getInstance(context);
        bitstreamDAO = BitstreamDAOFactory.getInstance(context);
        identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
    }

    public ItemDAO getChild()
    {
        return childDAO;
    }

    public void setChild(ItemDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public Item create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public Item retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public Item retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public void update(Item item) throws AuthorizeException
    {
        childDAO.update(item);
    }

    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    public void decache(Item item)
    {
        childDAO.decache(item);
    }

    /**
     * Returns a list of items that are both in the archive and not withdrawn.
     */
    public List<Item> getItems()
    {
        return childDAO.getItems();
    }

    /**
     * This function primarily exists to service the Harvest class. See that
     * class for documentation on usage.
     */
    public List<Item> getItems(DSpaceObject scope,
            String startDate, String endDate,
            int offset, int limit,
            boolean items, boolean collections,
            boolean withdrawn)
            throws ParseException
    {
        return childDAO.getItems(scope, startDate, endDate, offset, limit,
                items, collections, withdrawn);
    }

    /**
     * This is a simple 'search' function that returns Items that are in the
     * archive, are not withdrawn, and match the given schema, field, and
     * value.
     */
    public List<Item> getItems(MetadataValue value)
    {
        return childDAO.getItems(value);
    }

    /**
     * The dates passed in here are used to limit the results by ingest date
     * (dc.date.accessioned).
     */
    public List<Item> getItems(MetadataValue value, Date startDate, Date endDate)
    {
        return childDAO.getItems(value, startDate, endDate);
    }

    public List<Item> getItemsByCollection(Collection collection)
    {
        return childDAO.getItemsByCollection(collection);
    }

    public List<Item> getItemsBySubmitter(EPerson eperson)
    {
        return childDAO.getItemsBySubmitter(eperson);
    }

    public List<Item> getParentItems(Bundle bundle)
    {
        return childDAO.getParentItems(bundle);
    }

    public void link(Item item, Bundle bundle) throws AuthorizeException
    {
        childDAO.link(item, bundle);
    }

    public void unlink(Item item, Bundle bundle) throws AuthorizeException
    {
        childDAO.unlink(item, bundle);
    }

    public boolean linked(Item item, Bundle bundle)
    {
        return childDAO.linked(item, bundle);
    }

    public void loadMetadata(Item item)
    {
        childDAO.loadMetadata(item);
    }

    public List<DCValue> getMetadata(Item item, String schema, String element,
            String qualifier, String lang)
    {
        return childDAO.getMetadata(item, schema, element, qualifier, lang);
    }
}
