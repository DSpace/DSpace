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

import java.util.List;

import org.dspace.handle.external.Handle;
import org.hamcrest.Matcher;

/**
 * Compare the json from the response with the expected values
 */
public class ExternalHandleMatcher {

    private ExternalHandleMatcher() {
    }

    public static Matcher<? super Object> matchProperties(String url, String title, String repository,
                                                          String submitDate, String reporteMail, String subprefix,
                                                          String handle) {
        return allOf(
                hasJsonPath("$.url", is(url)),
                hasJsonPath("$.title", is(title)),
                hasJsonPath("$.repository", is(repository)),
                hasJsonPath("$.submitdate", is(submitDate)),
                hasJsonPath("$.reportemail", is(reporteMail)),
                hasJsonPath("$.subprefix", is(subprefix)),
                hasJsonPath("$.handle", is(handle))
        );
    }

    public static Matcher<? super Object> matchListOfExternalHandles(List<Handle> externalHandles) {
        return allOf(
                hasJsonPath("$[*].url", containsInAnyOrder(externalHandles.get(0).url,
                        externalHandles.get(1).url,
                        externalHandles.get(2).url )),
                hasJsonPath("$[*].title", containsInAnyOrder(externalHandles.get(0).title,
                        externalHandles.get(1).title,
                        externalHandles.get(2).title)),
                hasJsonPath("$[*].repository", containsInAnyOrder(externalHandles.get(0).repository,
                        externalHandles.get(1).repository,
                        externalHandles.get(2).repository)),
                hasJsonPath("$[*].submitdate", containsInAnyOrder(externalHandles.get(0).submitdate,
                        externalHandles.get(1).submitdate,
                        externalHandles.get(2).submitdate)),
                hasJsonPath("$[*].reportemail", containsInAnyOrder(externalHandles.get(0).reportemail,
                        externalHandles.get(1).reportemail,
                        externalHandles.get(2).reportemail)),
                hasJsonPath("$[*].subprefix", containsInAnyOrder(externalHandles.get(0).subprefix,
                        externalHandles.get(1).subprefix,
                        externalHandles.get(2).subprefix)),
                hasJsonPath("$[*].handle", containsInAnyOrder(externalHandles.get(0).getHandle(),
                        externalHandles.get(1).getHandle(),
                        externalHandles.get(2).getHandle()))
        );
    }
}
