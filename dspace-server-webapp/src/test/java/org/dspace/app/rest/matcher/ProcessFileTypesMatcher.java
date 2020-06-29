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

import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class ProcessFileTypesMatcher {

    private ProcessFileTypesMatcher() {
    }

    public static Matcher<? super Object> matchProcessFileTypes(String id, List<String> filetypes) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.values", Matchers.containsInAnyOrder(
                filetypes.stream().map(Matchers::containsString)
                         .collect(Collectors.toList())
            ))
        );

    }
}
