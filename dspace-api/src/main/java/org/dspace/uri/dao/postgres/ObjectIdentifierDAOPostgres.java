/*
 * ObjectIdentifierDAOPostgres.java
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

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.dao.ObjectIdentifierDAO;

import java.sql.SQLException;
import java.util.UUID;

public class ObjectIdentifierDAOPostgres extends ObjectIdentifierDAO
{
    protected Logger log = Logger.getLogger(ObjectIdentifierDAOPostgres.class);

    private String retrieveUUID = "SELECT * FROM uuid WHERE uuid = ?";

    private String retrieveID = "SELECT * FROM uuid WHERE resource_type = ? AND resource_id = ?";

    private String existing = "SELECT * FROM uuid WHERE uuid = ? AND resource_type = ? AND resource_id = ?";

    private String insertSQL = "INSERT INTO uuid (uuid, resource_type, resource_id) VALUES (?, ?, ?)";

    private String deleteObjectSQL = "DELETE FROM uuid WHERE resource_type = ?, resource_id = ?";

    public ObjectIdentifierDAOPostgres(Context context)
    {
        super(context);
    }
    
    public void create(UUID uuid, DSpaceObject dso)
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "item");
            row.setColumn("uuid", uuid.toString());
            row.setColumn("resource_type", dso.getType());
            row.setColumn("resource_id", dso.getID());
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
    }

    public ObjectIdentifier retrieve(UUID uuid)
    {
        try
        {
            Object[] params = { uuid.toString() };
            TableRowIterator tri = DatabaseManager.query(context, retrieveUUID, params);
            if (!tri.hasNext())
            {
                tri.close();
                return null;
            }
            TableRow row = tri.next();
            ObjectIdentifier oid = new ObjectIdentifier(uuid, row.getIntColumn("resource_type"), row.getIntColumn("resource_id"));
            tri.close();
            return oid;
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
    }

    public ObjectIdentifier retrieve(int type, int id)
    {
        try
        {
            Object[] params = { new Integer(type), new Integer(id) };
            TableRowIterator tri = DatabaseManager.query(context, retrieveID, params);
            if (!tri.hasNext())
            {
                tri.close();
                return null;
            }
            TableRow row = tri.next();
            ObjectIdentifier oid = new ObjectIdentifier(row.getStringColumn("uuid"), type, id);
            tri.close();
            return oid;
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
    }

    public void update(ObjectIdentifier oid)
    {
        // find out if the ObjectIdentifier exists at all, and if not create it
        if (!exists(oid))
        {
            insertOID(oid);
        }

        // FIXME: this means that you could potentially have multiple UUIDs pointing to a single
        // item.  This could be a "feature" or a "bug".  You decide!
    }

    public void delete(DSpaceObject dso)
    {
        // delete all the identifiers associated with the DSpace Object
        try
        {
            Object[] params = { new Integer(dso.getType()), new Integer(dso.getID()) };
            DatabaseManager.updateQuery(context, deleteObjectSQL, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
    }

    private boolean exists(ObjectIdentifier oid)
    {
        try
        {
            Object[] params = { oid.getUUID().toString(), new Integer(oid.getResourceTypeID()), new Integer(oid.getResourceID()) };
            TableRowIterator tri = DatabaseManager.query(context, existing, params);
            if (!tri.hasNext())
            {
                tri.close();
                return false;
            }
            tri.close();
            return true;
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
    }

    private void insertOID(ObjectIdentifier oid)
    {
        try
        {
            Object[] params = { oid.getUUID().toString(), new Integer(oid.getResourceTypeID()), new Integer(oid.getResourceID()) };
            DatabaseManager.updateQuery(context, insertSQL, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
    }
}
