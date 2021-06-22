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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.dspace.content.Site;
import org.hamcrest.Matcher;

public class SiteMatcher {

    private SiteMatcher() { }

    public static Matcher<? super Object> matchEntry(Site site) {
        return allOf(
            hasJsonPath("$.uuid", is(site.getID().toString())),
            hasJsonPath("$.name", is(site.getName())),
            hasJsonPath("$.type", is("site")),
            hasJsonPath("$._links.self.href", containsString("/api/core/sites/" + site.getID()))

        );
    }
}
