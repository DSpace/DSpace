/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class ValuePairSupplier implements Supplier<List<String>> {

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Override
    public List<String> get() {
        return new ArrayList<>(choiceAuthorityService.getChoiceAuthoritiesNames());
    }
}
