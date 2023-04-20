/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.dspace.external.provider.ExternalDataProvider;
import org.hamcrest.Matcher;

public class ExternalSourceMatcher {

    private ExternalSourceMatcher() {
    }

    /**
     * Matcher which checks if all external source providers are listed (in exact order), up to the maximum number
     * @param providers List of providers to check against
     * @param max maximum number to check
     * @return Matcher for this list of providers
     */
    public static Matcher<Iterable<? extends List<Matcher>>> matchAllExternalSources(
        List<ExternalDataProvider> providers, int max) {
        List<Matcher> matchers = new ArrayList<>();
        int count = 0;
        for (ExternalDataProvider provider: providers) {
            count++;
            if (count > max) {
                break;
            }
            matchers.add(matchExternalSource(provider));
        }

        // Make sure all providers exist in this exact order
        return contains(matchers.toArray(new Matcher[0]));
    }

    public static Matcher<? super Object> matchExternalSource(ExternalDataProvider provider) {
        return matchExternalSource(provider.getSourceIdentifier(), provider.getSourceIdentifier(), false);
    }

    public static Matcher<? super Object> matchExternalSource(String id, String name, boolean hierarchical) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.hierarchical", is(hierarchical)),
            hasJsonPath("$._links.entries.href", is(REST_SERVER_URL +
                                                            "integration/externalsources/" + name + "/entries"))
        );
    }
}
