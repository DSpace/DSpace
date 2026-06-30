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

import org.dspace.app.rest.model.CrisLayoutSectionRest;
import org.dspace.app.rest.model.CrisLayoutSectionRest.CrisLayoutSectionComponentRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.layout.CrisLayoutSection;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the CrisLayoutSection in the DSpace API data
 * model and the REST data model.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class CrisLayoutSectionConverter implements DSpaceConverter<CrisLayoutSection, CrisLayoutSectionRest> {

    @Autowired
    private List<CrisLayoutSectionComponentConverter> componentConverters;

    @Override
    public CrisLayoutSectionRest convert(CrisLayoutSection modelObject, Projection projection) {
        CrisLayoutSectionRest rest = new CrisLayoutSectionRest();
        List<CrisLayoutSectionRest> crisLayoutSectionRests = new ArrayList<>();

        rest.setProjection(projection);
        rest.setId(modelObject.getId());

        if (!Objects.isNull(modelObject.getSectionComponents())) {
            for (List<CrisLayoutSectionComponent> components : modelObject.getSectionComponents()) {
                List<CrisLayoutSectionComponentRest> componentsRest = new LinkedList<>();
                for (CrisLayoutSectionComponent component : components) {
                    convertComponent(component).ifPresent(componentsRest::add);
                }
                rest.getComponentRows().add(componentsRest);
            }
        }

        if (!Objects.isNull(modelObject.getNestedSections())) {
            for (CrisLayoutSection crisLayoutSection : modelObject.getNestedSections()) {
                crisLayoutSectionRests.add(this.convert(crisLayoutSection, projection));
            }
            rest.setNestedSections(crisLayoutSectionRests);
        }

        return rest;
    }

    private Optional<CrisLayoutSectionComponentRest> convertComponent(CrisLayoutSectionComponent component) {
        for (CrisLayoutSectionComponentConverter converter : componentConverters) {
            if (converter.support(component)) {
                return Optional.of(converter.convert(component));
            }
        }
        return Optional.empty();
    }

    @Override
    public Class<CrisLayoutSection> getModelClass() {
        return CrisLayoutSection.class;
    }

}
