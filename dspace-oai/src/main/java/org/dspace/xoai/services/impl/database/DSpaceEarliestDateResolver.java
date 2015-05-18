/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.database;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.services.api.database.EarliestDateResolver;
import org.dspace.xoai.services.api.database.FieldResolver;
import org.dspace.xoai.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Date;

public class DSpaceEarliestDateResolver implements EarliestDateResolver {
    private static final Logger log = LogManager.getLogger(DSpaceEarliestDateResolver.class);

    @Autowired
    private FieldResolver fieldResolver;

    @Override
    public Date getEarliestDate(Context context) throws InvalidMetadataFieldException, SQLException {
        String query = "SELECT MIN(text_value) as value FROM metadatavalue WHERE metadata_field_id = ?";
        boolean postgres = ! DatabaseManager.isOracle();

        if (!postgres) {
            query = "SELECT MIN(TO_CHAR(text_value)) as value FROM metadatavalue WHERE metadata_field_id = ?";
        }

        TableRowIterator iterator = DatabaseManager
                .query(context,
                        query,
                        fieldResolver.getFieldID(context, "dc.date.available"));

        if (iterator.hasNext())
        {
            String str = iterator.next().getStringColumn("value");
            try
            {
                Date d = DateUtils.parse(str);
                if (d != null) return d;
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
            }
        }

        return new Date();
    }
}
