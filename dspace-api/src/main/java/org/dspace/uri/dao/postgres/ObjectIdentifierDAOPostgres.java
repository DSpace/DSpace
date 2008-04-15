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
import org.dspace.uri.dao.ObjectIdentifierStorageException;

import java.sql.SQLException;
import java.util.UUID;

/**
 * DAO implementation to persist ObjectIdentifier objects in a PostgreSQL database
 *
 * @author Richard Jones
 */
public class ObjectIdentifierDAOPostgres extends ObjectIdentifierDAO
{
    /** log4j logger */
    private Logger log = Logger.getLogger(ObjectIdentifierDAOPostgres.class);

    /** SQL to retrieve a record based on UUID */
    private String retrieveUUID = "SELECT * FROM uuid WHERE uuid = ?";

    /** SQL to retrieve a record based on resource type and storage id */
    private String retrieveID = "SELECT * FROM uuid WHERE resource_type = ? AND resource_id = ?";

    /** SQL to retrieve a record based on all its properties (i.e. existance test) */
    private String existing = "SELECT * FROM uuid WHERE uuid = ? AND resource_type = ? AND resource_id = ?";

    /** SQL to insert a record into the database */
    private String insertSQL = "INSERT INTO uuid (uuid, resource_type, resource_id) VALUES (?, ?, ?)";

    /** SQL to delete a record from the database based on resource type and storage id */
    private String deleteObjectSQL = "DELETE FROM uuid WHERE resource_type = ? AND resource_id = ?";

    /**
     * Construct a new ObjectIdentifierDAOPostgres with the given DSpace Context
     *
     * @param context
     */
    public ObjectIdentifierDAOPostgres(Context context)
    {
        super(context);
    }

    /**
     * Create a persistent record of the given ObjectIdentifier
     *
     * @param oid
     */
    public void create(ObjectIdentifier oid)
            throws ObjectIdentifierStorageException
    {
        try
        {
            Object[] params = { oid.getUUID().toString(), new Integer(oid.getResourceTypeID()), new Integer(oid.getResourceID()) };
            DatabaseManager.updateQuery(context, insertSQL, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ObjectIdentifierStorageException(e);
        }
    }

    /**
     * Retrieve the ObjectIdentifier associated with the given DSpaceObject
     *
     * @param uuid
     * @return
     */
    public ObjectIdentifier retrieve(UUID uuid)
            throws ObjectIdentifierStorageException
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
            throw new ObjectIdentifierStorageException(e);
        }
    }

    /**
     * Retrieve the ObjectIdentifier associated with the given DSpace object type and
     * storage layer id
     *
     * @param type
     * @param id
     * @return
     */
    public ObjectIdentifier retrieve(int type, int id)
            throws ObjectIdentifierStorageException
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
            throw new ObjectIdentifierStorageException(e);
        }
    }

    /**
     * Update the record of the given ObjectIdentifier
     *
     * @param oid
     */
    public void update(ObjectIdentifier oid)
            throws ObjectIdentifierStorageException
    {
        try
        {
            // find out if the ObjectIdentifier exists at all, and if not create it
            if (!exists(oid))
            {
                create(oid);
            }

            // FIXME: this means that you could potentially have multiple UUIDs pointing to a single
            // item.  This could be a "feature" or a "bug".  You decide!
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ObjectIdentifierStorageException(e);
        }
    }

    /**
     * Delete all record of the given ObjectIdentifier
     *
     * @param oid
     */
    public void delete(ObjectIdentifier oid)
            throws ObjectIdentifierStorageException
    {
        // delete all the identifiers associated with the DSpace Object
        try
        {
            Object[] params = { new Integer(oid.getResourceTypeID()), new Integer(oid.getResourceID()) };
            DatabaseManager.updateQuery(context, deleteObjectSQL, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ObjectIdentifierStorageException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ///////////////////////////////////////////////////////////////////

    /**
     * Test whether the given ObjectIdentifier exists in the database
     * 
     * @param oid
     * @return
     * @throws SQLException
     */
    private boolean exists(ObjectIdentifier oid)
            throws SQLException
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
}
