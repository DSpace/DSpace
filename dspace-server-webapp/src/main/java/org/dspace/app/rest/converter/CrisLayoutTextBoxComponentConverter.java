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

import org.dspace.app.rest.model.CrisLayoutSectionRest;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.dspace.layout.CrisLayoutTextBoxComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
@Component
public class CrisLayoutTextBoxComponentConverter implements CrisLayoutSectionComponentConverter {

    private final CrisLayoutTextRowComponentConverter textKeyComponentConverter;

    @Autowired
    public CrisLayoutTextBoxComponentConverter(
        CrisLayoutTextRowComponentConverter textKeyComponentConverter) {
        this.textKeyComponentConverter = textKeyComponentConverter;
    }

    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutTextBoxComponent;
    }

    @Override
    public CrisLayoutSectionRest.CrisLayoutSectionComponentRest convert(CrisLayoutSectionComponent component) {

        CrisLayoutTextBoxComponent textBoxComponent = (CrisLayoutTextBoxComponent) component;

        List<CrisLayoutSectionRest.CrisLayoutTextRowComponentRest> rows =
            textBoxComponent.getTextRows()
            .stream()
                .map(comp -> (CrisLayoutSectionRest.CrisLayoutTextRowComponentRest)
                    textKeyComponentConverter.convert(comp))
                .sorted()
                .collect(Collectors.toList());

        return new CrisLayoutSectionRest.CrisLayoutTextBoxComponentRest(rows,
            textBoxComponent.getStyle());
    }
}
