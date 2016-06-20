/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl;

import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.services.api.FieldResolver;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class DSpaceFieldResolver implements FieldResolver {
    private MetadataFieldCache metadataFieldCache = null;

    private static final MetadataFieldService metadataFieldService
            = ContentServiceFactory.getInstance().getMetadataFieldService();

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

                MetadataField metadataField = metadataFieldService.findByElement(context, schema, element, qualifier);
                if (null != metadataField)
                {
                    metadataFieldCache.add(field, metadataField.getID());
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
