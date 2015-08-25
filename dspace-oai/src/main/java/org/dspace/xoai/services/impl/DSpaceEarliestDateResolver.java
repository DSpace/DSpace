/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.services.api.EarliestDateResolver;
import org.dspace.xoai.services.api.FieldResolver;
import org.dspace.xoai.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Date;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataValueService;

public class DSpaceEarliestDateResolver implements EarliestDateResolver {
    private static final Logger log = LogManager.getLogger(DSpaceEarliestDateResolver.class);

    @Autowired
    private FieldResolver fieldResolver;

    @Override
    public Date getEarliestDate(Context context) throws InvalidMetadataFieldException, SQLException {
        String query = "SELECT MIN(text_value) as value FROM metadatavalue WHERE metadata_field_id = ?";

        MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
        MetadataValue minimum = metadataValueService.getMinimum(context,
                fieldResolver.getFieldID(context, "dc.date.available"));
        if (null != minimum)
        {
            String str = minimum.getValue();
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
