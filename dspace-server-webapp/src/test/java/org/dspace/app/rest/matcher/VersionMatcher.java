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

import org.dspace.versioning.Version;
import org.hamcrest.Matcher;

public class VersionMatcher {

    private VersionMatcher() {
    }

    public static Matcher<? super Object> matchEntry(Version version) {
        return allOf(
            hasJsonPath("$.id", is(version.getID())),
            hasJsonPath("$.version", is(version.getVersionNumber())),
            hasJsonPath("$.summary", is(version.getSummary())),
            hasJsonPath("$.type", is("version"))

        );
    }
}
