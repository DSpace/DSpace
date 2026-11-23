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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.Matcher;

public class SubmissionCCLicenseMatcher {

    private SubmissionCCLicenseMatcher() {
    }

    public static Matcher<? super Object> matchLicenseEntry(
            String id,
            String name,
            int[] enumsPerField
    ) {
        return allOf(
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.name", is(name)),
                matchFields(enumsPerField)
        );
    }

    private static Matcher<? super Object> matchFields(int[] enumsPerField) {
        List<Matcher<? super Object>> matchers = new LinkedList<>();

        for (int i = 0; i < enumsPerField.length; i++) {
            int enumCount = enumsPerField[i];

            matchers.add(allOf(
                    hasJsonPath("$.id"),
                    hasJsonPath("$.label"),
                    hasJsonPath("$.enums.length()", is(enumCount))
            ));
        }

        return hasJsonPath("$.fields", containsInAnyOrder(matchers));
    }
}
