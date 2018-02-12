/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import org.dspace.app.rest.model.hateoas.DSpaceCurieProvider;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.JUnitRestDocumentation;

/**
 * Abstract class to write documentation tests
 */
public abstract class AbstractDocumentationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private DSpaceCurieProvider curieProvider;

    protected abstract String getRestCategory();

    @Rule
    public JUnitRestDocumentation getRestDocumentation() {
        if(restDocumentation == null) {
            String curie = curieProvider.getCurieForCategory(getRestCategory());
            restDocumentation = new JUnitRestDocumentation(DOCUMENTATION_SNIPPETS_DIR + curie);
        }

        return restDocumentation;
    }

}
