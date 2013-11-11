/*
 * Copyright 2013 Indiana University.  All rights reserved.
 *
 * Mark H. Wood, IUPUI University Library, Nov 11, 2013
 */

package org.dspace.identifier.dspace;

import java.sql.SQLException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.DSpace;
import org.dspace.identifier.Identifier;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Manipulate DSpace internal object Identifiers.
 *
 * @author mwood
 */
public class Provider
        extends IdentifierProvider
{
    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return identifier.isAssignableFrom(DSpace.class);
    }

    @Override
    public boolean supports(String identifier)
    {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    public String register(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        String id = mint(context, dso);
        try {
            TableRow row = DatabaseManager.create(context, "ObjectID");
            row.setColumn("dspace_id", id);
            row.setColumn("object_id", dso.getID());
            row.setColumn("object_type", dso.getType());
            DatabaseManager.insert(context, row);
        } catch (SQLException ex)
        {
            throw new IdentifierException("Unable to store a DSpace object identifier", ex);
        }
        return id;
    }

    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        TableRow row;
        try
        {
            row = DatabaseManager.querySingleTable(context,
                    "ObjectID", "SELECT max(dspace_id) FROM ObjectID");
        } catch (SQLException ex)
        {
            throw new IdentifierException(ex);
        }
        return String.valueOf(row.getIntColumn("dspace_id") + 1);
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier,
            String... attributes)
            throws IdentifierNotFoundException, IdentifierNotResolvableException
    {
        TableRow row;
        try {
            row = DatabaseManager.querySingleTable(context, "ObjectID",
                    "SELECT object_id, object_type FROM ObjectID WHERE dspace_id = ?",
                    Integer.parseInt(identifier));
        } catch (SQLException ex)
        {
            throw new IdentifierNotFoundException("ID unknown", ex);
        }
        int type = row.getIntColumn("object_type");
        int id = row.getIntColumn("object_id");
        DSpaceObject object;
        try
        {
            object = DSpaceObject.find(context, type, id);
        } catch (SQLException ex)
        {
            throw new IdentifierNotFoundException("Object not found", ex);
        }
        return object;
    }

    @Override
    public String lookup(Context context, DSpaceObject object)
            throws IdentifierNotFoundException, IdentifierNotResolvableException
    {
        TableRow row;
        try {
            row = DatabaseManager.querySingleTable(context, "ObjectID",
                    "SELECT dspace_id from ObjectID WHERE object_id = ? AND object_type = ?",
                    object.getID(), object.getType());
        } catch (SQLException ex) {
            throw new IdentifierNotFoundException(ex);
        }
        return String.valueOf(row.getIntColumn("dspace_id"));
    }

    @Override
    public void delete(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier)
            throws IdentifierException
    {
        throw new UnsupportedOperationException("Not supported.");
    }
}
