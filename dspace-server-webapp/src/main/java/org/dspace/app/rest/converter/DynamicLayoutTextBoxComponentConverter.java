/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.rest.model.DynamicLayoutSectionRest;
import org.dspace.layout.DynamicLayoutSectionComponent;
import org.dspace.layout.DynamicLayoutTextBoxComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
@Component
public class DynamicLayoutTextBoxComponentConverter implements DynamicLayoutSectionComponentConverter {

    private final DynamicLayoutTextRowComponentConverter textKeyComponentConverter;

    @Autowired
    public DynamicLayoutTextBoxComponentConverter(
        DynamicLayoutTextRowComponentConverter textKeyComponentConverter) {
        this.textKeyComponentConverter = textKeyComponentConverter;
    }

    @Override
    public boolean support(DynamicLayoutSectionComponent component) {
        return component instanceof DynamicLayoutTextBoxComponent;
    }

    @Override
    public DynamicLayoutSectionRest.DynamicLayoutSectionComponentRest convert(DynamicLayoutSectionComponent component) {

        DynamicLayoutTextBoxComponent textBoxComponent = (DynamicLayoutTextBoxComponent) component;

        List<DynamicLayoutSectionRest.DynamicLayoutTextRowComponentRest> rows =
            textBoxComponent.getTextRows()
            .stream()
                .map(comp -> (DynamicLayoutSectionRest.DynamicLayoutTextRowComponentRest)
                    textKeyComponentConverter.convert(comp))
                .sorted()
                .collect(Collectors.toList());

        return new DynamicLayoutSectionRest.DynamicLayoutTextBoxComponentRest(rows,
            textBoxComponent.getStyle());
    }
}
