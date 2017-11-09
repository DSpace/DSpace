/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class MetadataFieldManager
{
    private static Logger log = LogManager
            .getLogger(MetadataFieldManager.class);

    private static MetadataFieldManager _manager = null;

    public static int getFieldID(Context context, String field)
            throws InvalidMetadataFieldException, SQLException
    {
        if (_manager == null)
            _manager = new MetadataFieldManager();
        if (!_manager.hasField(field))
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
                    log.debug("Query: " + query);
                    iterator = DatabaseManager.query(context, query, schema,
                            element);
                }
                else
                {
                    log.debug("Qualifier: " + qualifier);
                    query += " AND mfr.qualifier = ?";
                    log.debug("Query: " + query);
                    iterator = DatabaseManager.query(context, query, schema,
                            element, qualifier);
                }

                if (iterator.hasNext())
                {
                    _manager.add(field, iterator.next().getIntColumn("mid"));
                }
                else
                    throw new InvalidMetadataFieldException();

            }
            else
                throw new InvalidMetadataFieldException();
        }
        return _manager.getField(field);
    }

    private Map<String, Integer> _map;

    private MetadataFieldManager()
    {
        _map = new HashMap<String, Integer>();
    }

    public boolean hasField(String field)
    {
        return _map.containsKey(field);
    }

    public int getField(String field)
    {
        return _map.get(field).intValue();
    }

    public void add(String field, int id)
    {
        _map.put(field, new Integer(id));
    }
}
