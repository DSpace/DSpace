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
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
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
    private static final String DSPACE_SCHEMA = "dspace";
    private static final String METADATA_ELEMENT = "identifier";
    private static final String METADATA_QUALIFIER = "dspace";

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
        dso.addMetadata(DSPACE_SCHEMA, METADATA_ELEMENT, METADATA_QUALIFIER, null, id);
        try {
            dso.update();
        }
        catch (SQLException | AuthorizeException ex) {
            throw new IdentifierException("Unable to store a DSpace object identifier", ex);
        }
        return id;
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
            MetadataSchema dspace = MetadataSchema.find(context, DSPACE_SCHEMA);
            MetadataField identifier_dspace = MetadataField.findByElement(context,
                    dspace.getSchemaID(), METADATA_ELEMENT, METADATA_QUALIFIER);
            row = DatabaseManager.querySingleTable(context, "MetadataValue",
                    "SELECT resource_id, resource_type_id FROM Metadatavalue" +
                    " WHERE text_value = ? AND metadata_field_id = ?",
                    identifier, identifier_dspace.getFieldID());
        } catch (SQLException ex)
        {
            throw new IdentifierNotResolvableException("ID unknown", ex);
        }
        int type = row.getIntColumn("resource_type_id");
        int id = row.getIntColumn("resource_id");
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
        Metadatum[] identifiers = object.getMetadata(DSPACE_SCHEMA,
                METADATA_ELEMENT, METADATA_QUALIFIER, Item.ANY);
        return identifiers[0].value;
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
