/*
 * ExternalIdentifierDAO.java
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
package org.dspace.uri.dao.postgres;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierType;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 */
public class ExternalIdentifierDAOPostgres extends ExternalIdentifierDAO
{
    public ExternalIdentifierDAOPostgres(Context context)
    {
        this.context = context;
    }

    public ExternalIdentifier create(DSpaceObject dso, String value,
            ExternalIdentifierType type)
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "handle");

            value = type.getPrefix() + row.getIntColumn("handle_id");

            ExternalIdentifier identifier = getInstance(dso, type, value);

            row.setColumn("handle", value);
            row.setColumn("resource_type_id", dso.getType());
            row.setColumn("resource_id", dso.getID());
            row.setColumn("namespace", type.getNamespace());
            DatabaseManager.update(context, row);

            if (log.isDebugEnabled())
            {
                log.debug("Created new persistent identifier for "
                        + Constants.typeText[dso.getType()] + " " + value);
            }

            dso.addExternalIdentifier(identifier);

            return identifier;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public ExternalIdentifier retrieve(String canonicalForm)
    {
        if (canonicalForm.equals("") || canonicalForm == null)
        {
            return null;
        }

        Object[] bits = parseCanonicalForm(canonicalForm);

        if (bits == null)
        {
            return null;
        }

        ExternalIdentifierType type = (ExternalIdentifierType) bits[0];
        String value = (String) bits[1];

        return retrieve(type, value);
    }

    public ExternalIdentifier retrieve(ExternalIdentifierType type,
            String value)
    {
        DSpaceObject dso = null;

        int resourceID = -1;
        int resourceTypeID = -1;

        if (type == null)
        {
            throw new RuntimeException(type + " not a supported type");
        }

        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "externalidentifier",
                    "SELECT resource_id, resource_type_id " +
                    "FROM externalidentifier " +
                    "WHERE value = ? AND namespace = ?",
                    value, type.getNamespace());

            List<TableRow> list = tri.toList();
            if (list.size() == 1)
            {
                TableRow row = list.get(0);
                resourceID = row.getIntColumn("resource_id");
                resourceTypeID = row.getIntColumn("resource_type_id");
            }
            else
            {
                return null;
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

        ObjectIdentifier oi = new ObjectIdentifier(resourceID, resourceTypeID);
        dso = oi.getObject(context);

        return getInstance(dso, type, value);
    }

    public List<ExternalIdentifier> getExternalIdentifiers(DSpaceObject dso)
    {
        try
        {
            List<ExternalIdentifier> list =
                new ArrayList<ExternalIdentifier>();

            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "externalidentifier",
                    "SELECT namespace, value FROM externalidentifier " +
                    "WHERE resource_id = ? " +
                    "AND resource_type_id = ?",
                    dso.getID(), dso.getType());

            String value = null;

            for (TableRow row : tri.toList())
            {
                value = row.getStringColumn("value");
                String id = row.getStringColumn("namespace");

                // FIXME: Maybe throw an error if the value stored in the db
                // isn't in the enum?
                for (ExternalIdentifier pid : pids)
                {
                    if (pid.getType().getNamespace().equals(id))
                    {
                        list.add(getInstance(dso, pid.getType(), value));
                        break;
                    }
                }
            }

            return list;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<ExternalIdentifier>
        getExternalIdentifiers(ExternalIdentifierType type, String prefix)
    {
        try
        {
            List<ExternalIdentifier> list =
                new ArrayList<ExternalIdentifier>();

            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "externalidentifier",
                    "SELECT resource_id, resource_type_id, value " +
                    "FROM externalidentifier " +
                    "WHERE namespace = ? " +
                    "AND value LIKE ?",
                    type.getNamespace(), prefix + "%");

            for (TableRow row : tri.toList())
            {
                String value = row.getStringColumn("value");
                int resourceID = row.getIntColumn("resource_id");
                int resourceTypeID = row.getIntColumn("resource_type_id");

                ObjectIdentifier oi =
                    new ObjectIdentifier(resourceID, resourceTypeID);
                DSpaceObject dso = oi.getObject(context);

                for (ExternalIdentifier pid : pids)
                {
                    if (type.equals(pid.getType()))
                    {
                        list.add(getInstance(dso, pid.getType(), value));
                        break;
                    }
                }
            }

            return list;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }
}
