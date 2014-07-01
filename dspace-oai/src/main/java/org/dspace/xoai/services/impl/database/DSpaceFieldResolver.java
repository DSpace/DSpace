/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.database;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.services.api.database.FieldResolver;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class DSpaceFieldResolver implements FieldResolver {
    private MetadataFieldCache metadataFieldCache = null;

    @Override
    public int getFieldID(Context context, String field) throws InvalidMetadataFieldException, SQLException {
        if (metadataFieldCache == null)
            metadataFieldCache = new MetadataFieldCache();
        if (!metadataFieldCache.hasField(field))
        {
            String[] pieces = field.split(Pattern.quote("."));
            if (pieces.length > 1)
            {
                String schema = pieces[0];
                String element = pieces[1];
                String qualifier = null;
                if (pieces.length > 2)
                    qualifier = pieces[2];
                String query = "SELECT mfr.metadata_field_id as mid FROM metadatafieldregistry mfr, "
                        + "metadataschemaregistry msr WHERE mfr.metadata_schema_id=mfr.metadata_schema_id AND "
                        + "msr.short_id = ? AND mfr.element = ?";

                TableRowIterator iterator;

                if (qualifier == null)
                {
                    query += " AND mfr.qualifier is NULL";
                    iterator = DatabaseManager.query(context, query, schema,
                            element);
                }
                else
                {
                    query += " AND mfr.qualifier = ?";
                    iterator = DatabaseManager.query(context, query, schema,
                            element, qualifier);
                }

                if (iterator.hasNext())
                {
                    metadataFieldCache.add(field, iterator.next().getIntColumn("mid"));
                }
                else
                    throw new InvalidMetadataFieldException();

            }
            else
                throw new InvalidMetadataFieldException();
        }
        return metadataFieldCache.getField(field);
    }
}
