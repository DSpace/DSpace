/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.sql.SQLException;
import java.util.UUID;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Manipulate DSpace internal object Identifiers.
 *
 * @author mwood
 */
public class DSpaceProvider
        extends IdentifierProvider
{
    /** Database table to hold DSpace object identifiers. */
    private static final String ID_TABLE = "ObjectID";

    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return identifier.isAssignableFrom(DSpace.class);
    }

    @Override
    public boolean supports(String type)
    {
        return "dspace".equalsIgnoreCase(type);
    }

    @Override
    public String register(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        String id = mint(context, dso);
        try {
            TableRow row = DatabaseManager.row(ID_TABLE);
            row.setColumn("dspace_id", id);
            row.setColumn("object_id", dso.getID());
            row.setColumn("object_type", dso.getType());
            DatabaseManager.insert(context, row);
            context.commit();
        } catch (SQLException ex)
        {
            throw new IdentifierException("Unable to store a DSpace object identifier", ex);
        }
        return id;
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier)
            throws IdentifierException
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        return UUID.randomUUID().toString(); // XXX different type of UUID?
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier,
            String... attributes)
            throws IdentifierNotFoundException, IdentifierNotResolvableException
    {
        TableRow row;
        try {
            row = DatabaseManager.querySingleTable(context, ID_TABLE,
                    "SELECT object_id, object_type FROM objectid WHERE dspace_id = ?",
                    identifier);
        } catch (SQLException ex)
        {
            throw new IdentifierNotResolvableException("ID unknown", ex);
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
            row = DatabaseManager.querySingleTable(context, ID_TABLE,
                    "SELECT dspace_id from objectid WHERE object_id = ? AND object_type = ?",
                    object.getID(), object.getType());
        } catch (SQLException ex) {
            throw new IdentifierNotFoundException("Failed to fetch identifier for "
                    + object.getTypeText() + " with ID " + object.getID(), ex);
        }
        if (null == row)
            throw new IdentifierNotFoundException();
        else
            return row.getStringColumn("dspace_id");
    }

    @Override
    public void delete(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        try {
            DatabaseManager.querySingleTable(context, ID_TABLE,
                    "DELETE FROM objectid WHERE object_id = ? AND object_type = ?",
                    dso.getID(), dso.getType());
            context.commit();
        } catch (SQLException ex) {
            throw new IdentifierException("Could not delete " + dso.getTypeText()
                    + " with ID " + dso.getID(), ex);
        }
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
}
