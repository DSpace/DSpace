/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.dspace.app.rest.model.DynamicLayoutSectionRest;
import org.dspace.app.rest.model.DynamicLayoutSectionRest.DynamicLayoutSectionComponentRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.layout.DynamicLayoutSection;
import org.dspace.layout.DynamicLayoutSectionComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the DynamicLayoutSection in the DSpace API data
 * model and the REST data model.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class DynamicLayoutSectionConverter implements DSpaceConverter<DynamicLayoutSection, DynamicLayoutSectionRest> {

    @Autowired
    private List<DynamicLayoutSectionComponentConverter> componentConverters;

    @Override
    public DynamicLayoutSectionRest convert(DynamicLayoutSection modelObject, Projection projection) {
        DynamicLayoutSectionRest rest = new DynamicLayoutSectionRest();
        List<DynamicLayoutSectionRest> dynamicLayoutSectionRests = new ArrayList<>();

        rest.setProjection(projection);
        rest.setId(modelObject.getId());

        if (!Objects.isNull(modelObject.getSectionComponents())) {
            for (List<DynamicLayoutSectionComponent> components : modelObject.getSectionComponents()) {
                List<DynamicLayoutSectionComponentRest> componentsRest = new LinkedList<>();
                for (DynamicLayoutSectionComponent component : components) {
                    convertComponent(component).ifPresent(componentsRest::add);
                }
                rest.getComponentRows().add(componentsRest);
            }
        }

        if (!Objects.isNull(modelObject.getNestedSections())) {
            for (DynamicLayoutSection dynamicLayoutSection : modelObject.getNestedSections()) {
                dynamicLayoutSectionRests.add(this.convert(dynamicLayoutSection, projection));
            }
            rest.setNestedSections(dynamicLayoutSectionRests);
        }

        return rest;
    }

    private Optional<DynamicLayoutSectionComponentRest> convertComponent(DynamicLayoutSectionComponent component) {
        for (DynamicLayoutSectionComponentConverter converter : componentConverters) {
            if (converter.support(component)) {
                return Optional.of(converter.convert(component));
            }
        }
        return Optional.empty();
    }

    @Override
    public Class<DynamicLayoutSection> getModelClass() {
        return DynamicLayoutSection.class;
    }

}
