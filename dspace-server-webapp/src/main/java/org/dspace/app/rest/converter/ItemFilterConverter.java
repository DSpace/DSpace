/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.ldn.ItemFilter;
import org.dspace.app.rest.model.ItemFilterRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This is the converter from the ItemFilter to the REST data model
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Component
public class ItemFilterConverter implements DSpaceConverter<ItemFilter, ItemFilterRest> {

    @Override
    public ItemFilterRest convert(ItemFilter obj, Projection projection) {
        ItemFilterRest itemFilterRest = new ItemFilterRest();
        itemFilterRest.setProjection(projection);
        itemFilterRest.setId(obj.getId());
        return itemFilterRest;
    }

    @Override
    public Class<ItemFilter> getModelClass() {
        return ItemFilter.class;
    }
}
