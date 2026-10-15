/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.dspace.app.rest.model.DynamicLayoutSectionRest.DynamicLayoutMultiColumnTopComponentRest.Column;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.rest.model.DynamicLayoutSectionRest.DynamicLayoutMultiColumnTopComponentRest;
import org.dspace.layout.DynamicLayoutMultiColumnTopComponent;
import org.dspace.layout.DynamicLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * extension of {@link DynamicLayoutTopComponentConverter} to convert
 * {@link org.dspace.layout.DynamicLayoutMultiColumnTopComponent} to rest.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */

@Component
public class DynamicLayoutMultiColumnTopComponentConverter implements DynamicLayoutSectionComponentConverter {

    @Override
    public boolean support(DynamicLayoutSectionComponent component) {
        return component instanceof DynamicLayoutMultiColumnTopComponent;
    }

    @Override
    public DynamicLayoutMultiColumnTopComponentRest convert(DynamicLayoutSectionComponent component) {

        DynamicLayoutMultiColumnTopComponent topComponent = (DynamicLayoutMultiColumnTopComponent) component;
        DynamicLayoutMultiColumnTopComponentRest rest = new DynamicLayoutMultiColumnTopComponentRest();
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
