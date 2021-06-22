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

import org.dspace.versioning.VersionHistory;
import org.hamcrest.Matcher;

public class VersionHistoryMatcher {

    private VersionHistoryMatcher() {
    }

    public static Matcher<? super Object> matchEntry(VersionHistory versionHistory) {
        return allOf(
            hasJsonPath("$.id", is(versionHistory.getID())),
            hasJsonPath("$.type", is("versionhistory"))

        );
    }
}
