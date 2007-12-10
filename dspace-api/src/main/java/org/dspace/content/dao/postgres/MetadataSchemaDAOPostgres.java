/*
 * MetadataSchemaDAOPostgres.java
 *
 * Version: $Revision: 427 $
 *
 * Date: $Date: 2007-08-07 17:32:39 +0100 (Tue, 07 Aug 2007) $
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.content.dao.MetadataSchemaDAO;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class MetadataSchemaDAOPostgres extends MetadataSchemaDAO
{
    public MetadataSchemaDAOPostgres(Context context)
    {
        super(context);
    }

    @Override
    public MetadataSchema create() throws AuthorizeException
    {
        // FIXME: This isn't used anywhere yet (though it is stored).
        UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context,
                    "metadataschemaregistry");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("metadata_schema_id");
            MetadataSchema schema = new MetadataSchema(context, id);

            return schema;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public MetadataSchema retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "metadataschemaregistry", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public MetadataSchema retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "metadataschemaregistry", "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public MetadataSchema retrieveByName(String name)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "metadataschemaregistry", "short_id", name);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public MetadataSchema retrieveByNamespace(String namespace)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "metadataschemaregistry", "namespace", namespace);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(MetadataSchema schema) throws AuthorizeException
    {
        try
        {
            int id = schema.getID();
            TableRow row = DatabaseManager.find(context,
                    "metadataschemaregistry", id);

            if (row == null)
            {
                log.warn("Couldn't find metadata schema " + id);
            }
            else
            {
                String namespace = schema.getNamespace();
                String name = schema.getName();

                if ((namespace == null) || (namespace.equals("")))
                {
                    throw new RuntimeException("namespace cannot be null");
                }
                else
                {
                    row.setColumn("namespace", namespace);
                }
                if ((name == null) || (name.equals("")))
                {
                    throw new RuntimeException("name cannot be null");
                }
                else
                {
                    row.setColumn("short_id", name);
                }

                DatabaseManager.update(context, row);
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
            DatabaseManager.delete(context, "metadataschemaregistry", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    protected boolean uniqueShortName(int id, String name)
    {
        try
        {
            Connection con = context.getDBConnection();
            
            String query = "SELECT COUNT(*) FROM metadataschemaregistry " +
                    "WHERE metadata_schema_id != ? AND short_id = ?";
            
            PreparedStatement statement = con.prepareStatement(query);
            statement.setInt(1, id);
            statement.setString(2, name);
            
            ResultSet rs = statement.executeQuery();

            int count = 0;
            if (rs.next())
            {
                count = rs.getInt(1);
            }

            return (count == 0);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    protected boolean uniqueNamespace(int id, String namespace)
    {
        try
        {
            Connection con = context.getDBConnection();
            
            String query = "SELECT COUNT(*) FROM metadataschemaregistry " +
                    "WHERE metadata_schema_id != ? " + 
                    "AND namespace= ? ";

            PreparedStatement statement = con.prepareStatement(query);
            statement.setInt(1,id);
            statement.setString(2,namespace);
            
            ResultSet rs = statement.executeQuery();

            int count = 0;
            if (rs.next())
            {
                count = rs.getInt(1);
            }

            return (count == 0);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<MetadataSchema> getMetadataSchemas()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT metadata_schema_id FROM metadataschemaregistry");

            List<MetadataSchema> schemas = new ArrayList<MetadataSchema>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("metadata_schema_id");
                schemas.add(retrieve(id));
            }

            return schemas;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private MetadataSchema retrieve(TableRow row) throws SQLException
    {
        if (row == null)
        {
            return null;
        }

        int id = row.getIntColumn("metadata_schema_id");
        String namespace = row.getStringColumn("namespace");
        String name = row.getStringColumn("short_id");

        MetadataSchema schema = new MetadataSchema(context, id);
        schema.setNamespace(namespace);
        schema.setName(name);

        return schema;
    }
}
