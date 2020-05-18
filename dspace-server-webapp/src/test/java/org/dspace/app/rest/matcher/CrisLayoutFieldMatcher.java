/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import org.dspace.layout.CrisLayoutField;
import org.hamcrest.Matcher;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutFieldMatcher {

    private CrisLayoutFieldMatcher() {}

    public static Matcher<? super Object> matchField(CrisLayoutField field) {
        return allOf(
                hasJsonPath("$.id", is(field.getID())),
                hasJsonPath("$.bundle", is(field.getBundle())),
                hasJsonPath("$.rendering", is(field.getRendering())),
                hasJsonPath("$.row", is(field.getRow())),
                hasJsonPath("$.priority", is(field.getPriority())),
                hasJsonPath("$.label", is(field.getLabel())),
                hasJsonPath("$.style", is(field.getStyle()))
        );
    }

}
