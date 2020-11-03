/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ItemExportFormatRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.integration.crosswalks.service.ItemExportFormat;
import org.springframework.stereotype.Component;

/**
 * This converter deals with the conversion between ItemExportFormat objects and ItemExportFormatRest objects
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
@Component
public class ItemExportFormatRestConverter implements DSpaceConverter<ItemExportFormat, ItemExportFormatRest> {

    @Override
    public ItemExportFormatRest convert(ItemExportFormat modelObject, Projection projection) {
        ItemExportFormatRest itemExportFormatRest = new ItemExportFormatRest();
        itemExportFormatRest.setId(modelObject.getId());
        itemExportFormatRest.setMolteplicity(modelObject.getMolteplicity());
        itemExportFormatRest.setEntityType(modelObject.getEntityType());
        itemExportFormatRest.setMimeType(modelObject.getMimeType());
        return itemExportFormatRest;
    }

    @Override
    public Class<ItemExportFormat> getModelClass() {
        return ItemExportFormat.class;
    }

}
