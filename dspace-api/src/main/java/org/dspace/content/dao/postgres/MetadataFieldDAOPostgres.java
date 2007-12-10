/*
 * MetadataFieldDAOPostgres.java
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
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.dao.MetadataFieldDAO;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class MetadataFieldDAOPostgres extends MetadataFieldDAO
{
    public MetadataFieldDAOPostgres(Context context)
    {
        super(context);
    }

    @Override
    public MetadataField create() throws AuthorizeException
    {
        UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context,
                    "metadatafieldregistry");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("metadata_field_id");
            MetadataField field = new MetadataField(context, id);

            return field;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public MetadataField retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "metadatafieldregistry", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public MetadataField retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "metadatafieldregistry", "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public MetadataField retrieve(int schemaID, String element,
            String qualifier)
    {
        try
        {
            TableRowIterator tri = null;

            String query =
                "SELECT metadata_field_id FROM metadatafieldregistry " +
                "WHERE metadata_schema_id = ? " +
                "AND element = ? AND qualifier ";

            if (qualifier == null)
            {
                query = query + "is NULL";
                tri = DatabaseManager.queryTable(context,
                        "metadatafieldregistry", query,
                        schemaID, element);
            }
            else
            {
                query = query + "= ?";
                tri = DatabaseManager.queryTable(context,
                        "metadatafieldregistry", query,
                        schemaID, element, qualifier);
            }

            TableRow row = null;
            if (tri.hasNext())
            {
                row = tri.next();
            }
            tri.close();

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(MetadataField field) throws AuthorizeException
    {
        try
        {
            int id = field.getID();
            TableRow row = DatabaseManager.find(context,
                    "metadatafieldregistry", id);

            if (row == null)
            {
                log.warn("Couldn't find metadata field " + id);
            }
            else
            {
                int schemaID = field.getSchemaID();
                String element = field.getElement();
                String qualifier = field.getQualifier();
                String scopeNote = field.getScopeNote();

                if (schemaID <= 0)
                {
                    throw new RuntimeException("schema cannot be null");
                }
                else
                {
                    row.setColumn("metadata_schema_id", schemaID);
                }

                if ((element == null) || (element.equals("")))
                {
                    throw new RuntimeException("element cannot be null");
                }
                else
                {
                    row.setColumn("element", element);
                }

                if ((qualifier == null) || (qualifier.equals("")))
                {
                    row.setColumnNull("qualifier");
                }
                else
                {
                    row.setColumn("qualifier", qualifier);
                }

                if ((scopeNote == null) || (scopeNote.equals("")))
                {
                    row.setColumnNull("scope_note");
                }
                else
                {
                    row.setColumn("scope_note", scopeNote);
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
            DatabaseManager.delete(context, "metadatafieldregistry", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public boolean schemaChanged(MetadataField field)
    {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "metadatafieldregistry", field.getID());

            int schemaID = row.getIntColumn("metadata_schema_id");

            // Neither has been set yet, so they are effectively the same.
            if (schemaID <= 0 && field.getSchemaID() <= 0)
            {
                return false;
            }

            return schemaID != field.getSchemaID();
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<MetadataField> getMetadataFields()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
               "SELECT mfr.metadata_field_id " +
               "FROM metadatafieldregistry mfr, metadataschemaregistry msr " +
               "WHERE mfr.metadata_schema_id = msr.metadata_schema_id " +
               "ORDER BY msr.short_id, mfr.element, mfr.qualifier");

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<MetadataField> getMetadataFields(MetadataSchema schema)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
               "SELECT metadata_field_id FROM metadatafieldregistry " +
               "WHERE metadata_schema_id = ?", schema.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private MetadataField retrieve(TableRow row) throws SQLException
    {
        if (row == null)
        {
            return null;
        }

        int id = row.getIntColumn("metadata_field_id");
        int schemaID = row.getIntColumn("metadata_schema_id");
        String element = row.getStringColumn("element");
        String qualifier = row.getStringColumn("qualifier");
        String scopeNote = row.getStringColumn("scope_note");

        MetadataField field = new MetadataField(context, id);
        field.setSchemaID(schemaID);
        field.setElement(element);
        field.setQualifier(qualifier);
        field.setScopeNote(scopeNote);

        return field;
    }

    private List<MetadataField> returnAsList(TableRowIterator tri)
        throws SQLException
    {
        List<MetadataField> fields = new ArrayList<MetadataField>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("metadata_field_id");
            fields.add(retrieve(id));
        }

        return fields;
    }

    @Override
    protected boolean unique(int fieldID, int schemaID, String element,
            String qualifier)
    {
        try
        {
            Connection con = context.getDBConnection();
            TableRow reg = DatabaseManager.row("metadatafieldregistry");

            String qualifierClause = "";

            if (qualifier == null)
            {
                qualifierClause = "and qualifier is null";
            }
            else
            {
                qualifierClause = "and qualifier = ?";
            }

            String query = "SELECT COUNT(*) FROM " + reg.getTable()
                + " WHERE metadata_schema_id = ? "
                + " AND metadata_field_id != ? "
                + " AND element = ? " + qualifierClause;

            PreparedStatement statement = con.prepareStatement(query);
            statement.setInt(1, schemaID);
            statement.setInt(2, fieldID);
            statement.setString(3, element);

            if (qualifier != null)
            {
                statement.setString(4, qualifier);
            }

            ResultSet rs = statement.executeQuery();

            int count = 0;
            if (rs.next())
            {
                count = rs.getInt(1);
            }

            return count == 0;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }
}
