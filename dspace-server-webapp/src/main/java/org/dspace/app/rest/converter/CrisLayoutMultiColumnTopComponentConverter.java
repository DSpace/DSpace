/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.dspace.app.rest.model.CrisLayoutSectionRest.CrisLayoutMultiColumnTopComponentRest.Column;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.rest.model.CrisLayoutSectionRest.CrisLayoutMultiColumnTopComponentRest;
import org.dspace.layout.CrisLayoutMultiColumnTopComponent;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * extension of {@link CrisLayoutTopComponentConverter} to convert
 * {@link org.dspace.layout.CrisLayoutMultiColumnTopComponent} to rest.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */

@Component
public class CrisLayoutMultiColumnTopComponentConverter implements CrisLayoutSectionComponentConverter {

    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutMultiColumnTopComponent;
    }

    @Override
    public CrisLayoutMultiColumnTopComponentRest convert(CrisLayoutSectionComponent component) {

        CrisLayoutMultiColumnTopComponent topComponent = (CrisLayoutMultiColumnTopComponent) component;
        CrisLayoutMultiColumnTopComponentRest rest = new CrisLayoutMultiColumnTopComponentRest();
        rest.setDiscoveryConfigurationName(topComponent.getDiscoveryConfigurationName());
        rest.setOrder(topComponent.getOrder());
        rest.setSortField(topComponent.getSortField());
        rest.setStyle(component.getStyle());
        rest.setTitleKey(topComponent.getTitleKey());
        rest.setNumberOfItems(topComponent.getNumberOfItems());

        List<Column> columnList =  topComponent.getColumns().stream().map(Column::from)
            .collect(Collectors.toList());

        rest.setColumnList(columnList);

        return rest;
    }
}
