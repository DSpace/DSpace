/*
 * ExternalIdentifierDAOOracle.java
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
package org.dspace.uri.dao.oracle;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.uri.*;
import org.dspace.uri.dao.*;
import org.dspace.uri.dao.postgres.ExternalIdentifierDAOPostgres;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO implementation to persist ExternalIdentifier objects in an Oracle database
 *
 * FIXME: UNTESTED; identical to ExternalIdentifierDAOPostgres
 *
 * @author Richard Jones
 */
public class ExternalIdentifierDAOOracle extends ExternalIdentifierDAO
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(ExternalIdentifierDAOPostgres.class);

    /** SQL to create the record */
    private String createSQL = "INSERT INTO externalidentifier (namespace, identifier, resource_type_id, resource_id) " +
                               "VALUES (? ,?, ?, ?)";

    /** SQL to retrieve based on type and resource id */
    private String retrieveSQL = "SELECT * FROM externalidentifier WHERE resource_type_id = ? AND resource_id = ?";

    /** SQOL to obtain a record if all of the components exist in the database */
    private String existsSQL = "SELECT * FROM externalidentifier WHERE namespace = ? AND identifier = ? " +
                               "AND resource_type_id = ? AND resource_id = ?";

    /** delete row which meets all criteria */
    private String deleteSQL = "DELETE FROM externalidentifier WHERE namespace = ? AND identifier = ? " +
                               "AND resource_type_id = ? AND resource_id = ?";

    /** set up a tombstone reference in the database */
    private String tombSQL = "UPDATE externalidentifier SET namespace = ?, identifier = ?, resource_type_id = null, " +
                             "resource_id = null, tombstone = 1";

    /** retrieve a record based on its namespace and identifier value */
    private String nsValueSQL = "SELECT * FROM externalidentifier WHERE namespace = ? AND identifier = ?";

    /** retrieve a record based on its namespace and a substring of the identifier value */
    private String substringSQL = "SELECT * FROM externalidentifier WHERE namespace = ? AND identifier LIKE ?";

    /**
     * Construct a new ExternalIdentifierDAOOracle object with the given DSpace context
     * @param context
     */
    public ExternalIdentifierDAOOracle(Context context)
    {
        super(context);
    }

    /**
     * Create a persistent record of the given ExternalIdentifier to the given DSpaceObject
     *
     * @param eid
     */
    public void create(ExternalIdentifier eid)
            throws ExternalIdentifierStorageException
    {
        try
        {
            Object[] params = { eid.getType().getNamespace(), eid.getValue(),
                                eid.getObjectIdentifier().getResourceTypeID(),
                                eid.getObjectIdentifier().getResourceID() };

            DatabaseManager.updateQuery(context, createSQL, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ExternalIdentifierStorageException(e);
        }
    }

    /**
     * Retrieve all ExternalIdentifier objects associated with the given DSpaceObject
     *
     * @param dso
     * @return
     */
    public List<ExternalIdentifier> retrieve(DSpaceObject dso)
            throws ExternalIdentifierStorageException, IdentifierException
    {
        try
        {
            List<ExternalIdentifier> eids = new ArrayList<ExternalIdentifier>();
            Object[] params = {new Integer(dso.getType()), new Integer(dso.getID())};
            TableRowIterator tri = DatabaseManager.query(context, retrieveSQL, params);
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                String value = row.getStringColumn("identifier");
                ExternalIdentifier eid = ExternalIdentifierService.get(context, row.getStringColumn("namespace"), value);
                eid.setObjectIdentifier(dso.getIdentifier());
                eids.add(eid);
            }
            tri.close();
            return eids;
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ExternalIdentifierStorageException(e);
        }
    }

    /**
     * Retrieve an ExternalIdentifier object (if one exists) for the given identifier
     * type and value
     *
     * @param type
     * @param value
     * @return
     */
    public ExternalIdentifier retrieve(ExternalIdentifierType type, String value)
            throws ExternalIdentifierStorageException
    {
        try
        {
            Object[] params = {type.getNamespace(), value};
            TableRowIterator tri = DatabaseManager.query(context, nsValueSQL, params);
            if (!tri.hasNext())
            {
                return null;
            }
            TableRow row = tri.next();

            ObjectIdentifierDAO oidDAO = ObjectIdentifierDAOFactory.getInstance(context);
            ObjectIdentifier oid = oidDAO.retrieve(row.getIntColumn("resource_type_id"), row.getIntColumn("resource_id"));
            ExternalIdentifier eid = ExternalIdentifierService.get(context, type, value, oid);

            tri.close();
            return eid;
        }
        catch (ObjectIdentifierStorageException e)
        {
            log.error("caught exception: ", e);
            throw new ExternalIdentifierStorageException(e);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ExternalIdentifierStorageException(e);
        }
    }

    /**
     * Update the record of the given ExternalIdentifier
     *
     * @param eid
     */
    public void update(ExternalIdentifier eid)
            throws ExternalIdentifierStorageException
    {
        try
        {
            // find out if the ExternalIdentifier exists at all, and if not create it
            if (!exists(eid))
            {
                create(eid);
            }

            // Since we can't be sure, if the identifier has changed, which part
            // of the identifier we should be updating, we only ever create new
            // rows.

            // FIXME: is there any cleverness we can do with checking for the
            // identifier value, or checking for the resource type and resource id
            // or checking the underlying object identifier to see what the caller
            // is actually trying to do?
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ExternalIdentifierStorageException(e);
        }
    }

    /**
     * Delete all record of the given ExternalIdentifier
     *
     * @param eid
     */
    public void delete(ExternalIdentifier eid)
            throws ExternalIdentifierStorageException
    {
        try
        {
            Object[] params = { eid.getType().getNamespace(), eid.getValue(),
                                eid.getObjectIdentifier().getResourceTypeID(),
                                eid.getObjectIdentifier().getResourceID() };

            DatabaseManager.updateQuery(context, deleteSQL, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ExternalIdentifierStorageException(e);
        }
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Create a tombstone record for the given external identifier
     *
     * @param eid
     */
    public void tombstone(ExternalIdentifier eid)
            throws ExternalIdentifierStorageException
    {
        try
        {
            Object[] params = { eid.getType().getNamespace(), eid.getValue() };
            DatabaseManager.updateQuery(context, tombSQL, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ExternalIdentifierStorageException(e);
        }
    }

    /**
     * Retrieve all ExternalIdentifiers of the given type whose values start with the given
     * string fragment
     *
     * @param type
     * @param startsWith
     * @return
     */
    public List<ExternalIdentifier> startsWith(ExternalIdentifierType type, String startsWith)
            throws ExternalIdentifierStorageException
    {
        try
        {
            List<ExternalIdentifier> eids = new ArrayList<ExternalIdentifier>();
            Object[] params = {type.getNamespace(), startsWith + "%"};
            TableRowIterator tri = DatabaseManager.query(context, substringSQL, params);
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                ObjectIdentifierDAO oidDAO = ObjectIdentifierDAOFactory.getInstance(context);
                ObjectIdentifier oid = oidDAO.retrieve(row.getIntColumn("resource_type_id"), row.getIntColumn("resource_id"));
                ExternalIdentifier eid = ExternalIdentifierService.get(context, type, row.getStringColumn("identifier"), oid);
                eids.add(eid);
            }

            tri.close();
            return eids;
        }
        catch (ObjectIdentifierStorageException e)
        {
            log.error("caught exception: ", e);
            throw new ExternalIdentifierStorageException(e);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new ExternalIdentifierStorageException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ///////////////////////////////////////////////////////////////////

    /**
     * Does the given ExternalIdentifier currently have a record in the database or not
     *
     * @param eid
     * @return
     */
    private boolean exists(ExternalIdentifier eid)
            throws SQLException
    {
        Object[] params = {
                eid.getType().getNamespace(), eid.getValue(),
                eid.getObjectIdentifier().getResourceTypeID(),
                eid.getObjectIdentifier().getResourceID()
        };

        TableRowIterator tri = DatabaseManager.query(context, existsSQL, params);
        if (!tri.hasNext())
        {
            tri.close();
            return false;
        }
        tri.close();
        return true;
    }
}