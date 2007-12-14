/*
 * CollectionDAOPostgres.java
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
package org.dspace.content.dao.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class CollectionDAOPostgres extends CollectionDAO
{
    public CollectionDAOPostgres(Context context)
    {
        super(context);
    }

    @Override
    public Collection create() throws AuthorizeException
    {
        // UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context, "collection");
            //row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("collection_id");
            Collection collection = new Collection(context, id);
            // collection.setIdentifier(new ObjectIdentifier(uuid));
            
            return collection;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Collection retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context, "collection", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Collection retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context, "collection",
                    "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(Collection collection) throws AuthorizeException
    {
        try
        {
            TableRow row =
                DatabaseManager.find(context, "collection", collection.getID());

            if (row != null)
            {
                populateTableRowFromCollection(collection, row);
                DatabaseManager.update(context, row);
            }
            else
            {
                throw new RuntimeException("Didn't find collection " +
                        collection.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        try
        {
            // remove subscriptions - hmm, should this be in Subscription.java?
            DatabaseManager.updateQuery(context,
                    "DELETE FROM subscription WHERE collection_id = ? ", id);

            // Delete collection row
            DatabaseManager.delete(context, "collection", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Collection> getCollections()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "collection",
                    "SELECT collection_id FROM collection ORDER BY name");

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Collection> getParentCollections(Item item)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "collection",
                    "SELECT collection_id " +
                    "FROM collection2item " +
                    "WHERE item_id = ? ",
                    item.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Collection> getChildCollections(Community community)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "collection",
                    "SELECT c.collection_id, c.name " +
                    "FROM collection c, community2collection c2c " +
                    "WHERE c2c.collection_id = c.collection_id " +
                    "AND c2c.community_id= ? " +
                    "ORDER BY c.name",
                    community.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public int itemCount(Collection collection)
    {
        try
        {
            String query = "SELECT count(*) FROM collection2item, item WHERE "
                + "collection2item.collection_id =  ? "
                + "AND collection2item.item_id = item.item_id "
                + "AND in_archive ='1' AND item.withdrawn='0' ";

            PreparedStatement statement =
                context.getDBConnection().prepareStatement(query);
            statement.setInt(1, collection.getID());
            
            ResultSet rs = statement.executeQuery();
            
            rs.next();
            int itemcount = rs.getInt(1);

            statement.close();

            return itemcount;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void link(Collection collection, Item item)
        throws AuthorizeException
    {
        if (!linked(collection, item))
        {
            try
            {
                TableRow row =
                    DatabaseManager.create(context, "collection2item");

                row.setColumn("collection_id", collection.getID());
                row.setColumn("item_id", item.getID());

                DatabaseManager.update(context, row);
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    @Override
    public void unlink(Collection collection, Item item)
        throws AuthorizeException
    {
        if (linked(collection, item))
        {
            try
            {
                DatabaseManager.updateQuery(context,
                        "DELETE FROM collection2item WHERE collection_id= ? " +
                        "AND item_id= ? ",
                        collection.getID(), item.getID());
            }
            catch (SQLException sqle)
            {
                throw new RuntimeException(sqle);
            }
        }
    }

    @Override
    public boolean linked(Collection collection, Item item)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT id FROM collection2item " +
                    "WHERE collection_id = ? AND item_id = ? ",
                    collection.getID(), item.getID());

            boolean result = tri.hasNext();
            tri.close();

            return result;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private Collection retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }

        int id = row.getIntColumn("collection_id");
        Collection collection = new Collection(context, id);
        populateCollectionFromTableRow(collection, row);

        // FIXME: I'd like to bump the rest of this up into the superclass
        // so we don't have to do it for every implementation, but I can't
        // figure out a clean way of doing this yet.
        List<ExternalIdentifier> identifiers =
                identifierDAO.getExternalIdentifiers(collection);
        collection.setExternalIdentifiers(identifiers);

        return collection;
    }

    private List<Collection> returnAsList(TableRowIterator tri)
            throws SQLException
    {
        List<Collection> collections = new ArrayList<Collection>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("collection_id");
            collections.add(retrieve(id));
        }

        return collections;
    }

    private void populateTableRowFromCollection(Collection collection,
            TableRow row)
    {
        int id = collection.getID();
        Bitstream logo = collection.getLogo();
        Item templateItem = collection.getTemplateItem();
        Group admins = collection.getAdministrators();
        Group[] workflowGroups = collection.getWorkflowGroups();

        if (logo == null)
        {
            row.setColumnNull("logo_bitstream_id");
        }
        else
        {
            row.setColumn("logo_bitstream_id", logo.getID());
        }

        if (templateItem == null)
        {
            row.setColumnNull("template_item_id");
        }
        else
        {
            row.setColumn("template_item_id", templateItem.getID());
        }

        if (admins == null)
        {
            row.setColumnNull("admin");
        }
        else
        {
            row.setColumn("admin", admins.getID());
        }

        for (int i = 1; i <= workflowGroups.length; i++)
        {
            Group g = workflowGroups[i - 1];
            if (g == null)
            {
                row.setColumnNull("workflow_step_" + i);
            }
            else
            {
                row.setColumn("workflow_step_" + i, g.getID());
            }
        }

        // Now loop over all allowed metadata fields and set the value into the
        // TableRow.
        for (CollectionMetadataField field : CollectionMetadataField.values())
        {
            String value = collection.getMetadata(field.toString());
            if (value == null)
            {
                row.setColumnNull(field.toString());
            }
            else
            {
                row.setColumn(field.toString(), value);
            }
        }

        row.setColumn("uuid", collection.getIdentifier().getUUID().toString());
    }

    private void populateCollectionFromTableRow(Collection c, TableRow row)
    {
        Bitstream logo = null;
        Item templateItem = null;

        // Get the logo bitstream
        if (!row.isColumnNull("logo_bitstream_id"))
        {
            int id = row.getIntColumn("logo_bitstream_id"); 
            logo = bitstreamDAO.retrieve(id);
        }

        // Get the template item
        if (!row.isColumnNull("template_item_id"))
        {
            templateItem =
                itemDAO.retrieve(row.getIntColumn("template_item_id"));
        }

        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));
        c.setIdentifier(new ObjectIdentifier(uuid));

        c.setLogoBitstream(logo);
        c.setTemplateItem(templateItem);

        c.setWorkflowGroup(1, groupFromColumn(row, "workflow_step_1"));
        c.setWorkflowGroup(2, groupFromColumn(row, "workflow_step_2"));
        c.setWorkflowGroup(3, groupFromColumn(row, "workflow_step_3"));

        c.setSubmitters(groupFromColumn(row, "submitter"));
        c.setAdministrators(groupFromColumn(row, "admin"));

        for (CollectionMetadataField field : CollectionMetadataField.values())
        {
            String value = row.getStringColumn(field.toString());
            if (value == null)
            {
                c.setMetadata(field.toString(), "");
            }
            else
            {
                c.setMetadata(field.toString(), value);
            }
        }
    }

    /**
     * Utility method for reading in a group from a group ID in a column. If the
     * column is null, null is returned.
     * 
     * @param col
     *            the column name to read
     * @return the group referred to by that column, or null
     * @throws SQLException
     */
    private Group groupFromColumn(TableRow row, String col)
    {
        if (row.isColumnNull(col))
        {
            return null;
        }

        return groupDAO.retrieve(row.getIntColumn(col));
    }
}
