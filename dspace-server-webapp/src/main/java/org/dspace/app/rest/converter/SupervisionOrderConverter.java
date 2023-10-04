/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.Objects;

import org.dspace.app.rest.model.SupervisionOrderRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.eperson.Group;
import org.dspace.supervision.SupervisionOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This class is responsible to convert SupervisionOrder to its rest model
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
@Component
public class SupervisionOrderConverter
    implements DSpaceConverter<SupervisionOrder, SupervisionOrderRest> {

    @Lazy
    @Autowired
    private ConverterService converter;

    @Override
    public SupervisionOrderRest convert(SupervisionOrder modelObject, Projection projection) {

        SupervisionOrderRest rest = new SupervisionOrderRest();
        Item item = modelObject.getItem();
        Group group = modelObject.getGroup();

        rest.setId(modelObject.getID());

        if (Objects.nonNull(item)) {
            rest.setItem(converter.toRest(item, projection));
        }

        if (Objects.nonNull(group)) {
            rest.setGroup(converter.toRest(group, projection));
        }

        rest.setProjection(projection);

        return rest;
    }

    @Override
    public Class<SupervisionOrder> getModelClass() {
        return SupervisionOrder.class;
    }
}
