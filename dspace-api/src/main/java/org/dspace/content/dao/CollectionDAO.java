/*
 * CollectionDAO.java
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

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.dao.CRUD;
import org.dspace.dao.Link;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.uri.dao.*;

import java.util.List;
import java.util.UUID;

/**
 * @author James Rutherford
 */
public abstract class CollectionDAO extends ContentDAO<CollectionDAO>
        implements CRUD<Collection>, Link<Collection, Item>
{
    protected static Logger log = Logger.getLogger(CollectionDAO.class);

    protected Context context;
    protected BitstreamDAO bitstreamDAO;
    protected ItemDAO itemDAO;
    protected GroupDAO groupDAO;
    protected ExternalIdentifierDAO identifierDAO;
    protected ObjectIdentifierDAO oidDAO;

    protected CollectionDAO childDAO;

    /**
     * The allowed metadata fields for Collections are defined in the following
     * enum. This should make reading / writing all metadatafields a lot less
     * error-prone, not to mention concise and tidy!
     */
    protected enum CollectionMetadataField
    {
        NAME ("name"),
        SHORT_DESCRIPTION ("short_description"),
        PROVENANCE_DESCRIPTION ("provenance_description"),
        LICENSE ("license"),
        COPYRIGHT_TEXT ("copyright_text"),
        SIDE_BAR_TEXT ("side_bar_text");

        private String name;

        private CollectionMetadataField(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }
    }

    public CollectionDAO(Context context)
    {
        try
        {
            this.context = context;

            bitstreamDAO = BitstreamDAOFactory.getInstance(context);
            itemDAO = ItemDAOFactory.getInstance(context);
            groupDAO = GroupDAOFactory.getInstance(context);
            identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
            oidDAO = ObjectIdentifierDAOFactory.getInstance(context);
        }
        catch (ExternalIdentifierStorageException e)
        {
            throw new RuntimeException(e);
        }
        catch (ObjectIdentifierStorageException e)
        {
            throw new RuntimeException(e);
        }
    }

    public CollectionDAO getChild()
    {
        return childDAO;
    }

    public void setChild(CollectionDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public Collection create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public Collection retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public Collection retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public void update(Collection collection) throws AuthorizeException
    {
        childDAO.update(collection);
    }

    /**
     * Delete the collection, including the metadata and logo. Items that are
     * then orphans are deleted. Groups associated with this collection
     * (workflow participants and submitters) are NOT deleted.
     */
    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    public List<Collection> getCollections()
    {
        return childDAO.getCollections();
    }

    /**
     * Returns a List of collections that user has a given permission on.
     * Useful for trimming 'select to collection' list, or figuring out which
     * collections a person is an editor for.
     */
    public List<Collection> getCollectionsByAuthority(Community parent,
                                                      int actionID)
    {
        return childDAO.getCollectionsByAuthority(parent, actionID);
    }

    public List<Collection> getParentCollections(Item item)
    {
        return childDAO.getParentCollections(item);
    }

    public List<Collection> getChildCollections(Community community)
    {
        return childDAO.getChildCollections(community);
    }

    /**
     * Returns a list of all the Collections that are *not* the parent of the
     * given Item. This would be unnecessary if we used Sets instead of Lists.
     *
     * @param item The Item
     * @return All Collections that are not parent to the given Item.
     */
    public List<Collection> getCollectionsNotLinked(Item item)
    {
        return childDAO.getCollectionsNotLinked(item);
    }

    /**
     * Create a storage layer association between the given Item and
     * Collection.
     */
    public void link(Collection collection, Item item)
            throws AuthorizeException
    {
        childDAO.link(collection, item);
    }

    /**
     * Remove any existing storage layer association between the given Item and
     * Collection.
     */
    public void unlink(Collection collection, Item item)
            throws AuthorizeException
    {
        childDAO.unlink(collection, item);
    }

    /**
     * Determine whether or not there is an established link between the given
     * Item and Collection in the storage layer.
     */
    public boolean linked(Collection collection, Item item)
    {
        return childDAO.linked(collection, item);
    }

    // Everything below this line is debatable & needs rethinking

    /**
     * Straightforward utility method for counting the number of Items in the
     * given Collection. There is probably a way to be smart about this. Also,
     * this strikes me as the kind of method that shouldn't really be in here.
     */
    public int itemCount(Collection collection)
    {
        return childDAO.itemCount(collection);
    }
}

