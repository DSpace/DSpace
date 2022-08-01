/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.model.TemplateItemRest;
import org.dspace.app.rest.model.wrapper.TemplateItem;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the TemplateItem in the DSpace API data model and the
 * REST data model
 */
@Component
public class TemplateItemConverter
    implements DSpaceConverter<TemplateItem, TemplateItemRest> {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    ConverterService converter;

    @Autowired
    private ItemService itemService;

    @Override
    public TemplateItemRest convert(TemplateItem templateItem, Projection projection) {
        TemplateItemRest templateItemRest = new TemplateItemRest();
        templateItemRest.setProjection(projection);
        if (templateItem.getID() != null) {
            templateItemRest.setId(templateItem.getID());
            templateItemRest.setUuid(templateItem.getID());
        }

        templateItemRest.setLastModified(templateItem.getLastModified());
        Collection templateItemOf = templateItem.getTemplateItemOf();
        if (templateItemOf != null) {
            templateItemRest.setTemplateItemOf(converter.toRest(templateItemOf, projection));
        }

        List<MetadataValue> fullList =
            itemService.getMetadata(templateItem.getItem(), Item.ANY, Item.ANY, Item.ANY, Item.ANY, true);
        MetadataValueList metadataValues = new MetadataValueList(fullList);
        templateItemRest.setMetadata(converter.toRest(metadataValues, projection));

        return templateItemRest;
    }

    @Override
    public Class<TemplateItem> getModelClass() {
        return TemplateItem.class;
    }
}
