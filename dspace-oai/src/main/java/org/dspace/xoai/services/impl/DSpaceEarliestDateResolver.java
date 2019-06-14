/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl;

import java.sql.SQLException;
import java.util.Date;

import javax.persistence.NoResultException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.services.api.EarliestDateResolver;
import org.dspace.xoai.services.api.FieldResolver;
import org.dspace.xoai.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceEarliestDateResolver implements EarliestDateResolver {
    private static final Logger log = LogManager.getLogger(DSpaceEarliestDateResolver.class);

    @Autowired
    private FieldResolver fieldResolver;

    @Override
    public Date getEarliestDate(Context context) throws InvalidMetadataFieldException, SQLException {
        MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
        MetadataValue minimum = null;
        try {
            minimum = metadataValueService.getMinimum(context,
                                                      fieldResolver.getFieldID(context, "dc.date.available"));
        } catch (NoResultException e) {
          // This error only occurs if no metadataFields of this type exist (i.e. no minimum exists)
          // It can be safely ignored in this scenario, as it implies the DSpace is empty.
        }

        if (null != minimum) {
            String str = minimum.getValue();
            try {
                Date d = DateUtils.parse(str);
                if (d != null) {
                    return d;
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return new Date();
    }
}
