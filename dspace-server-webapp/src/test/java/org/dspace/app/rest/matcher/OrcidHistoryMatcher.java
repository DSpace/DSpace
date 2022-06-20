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

import org.dspace.orcid.OrcidHistory;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for an OrcidHistory
 *
 * @author Mykhaylo Boychuk (4science.it)
 *
 */
public class OrcidHistoryMatcher {

    private OrcidHistoryMatcher() {}

    public static Matcher<? super Object> matchOrcidHistory(OrcidHistory orcidHistory, int status, String putCode,
        String responseMessage) {
        return allOf(
                hasJsonPath("$.id", is(orcidHistory.getID())),
                hasJsonPath("$.profileItemId", is(orcidHistory.getProfileItem().getID().toString())),
                hasJsonPath("$.entityId", is(orcidHistory.getEntity().getID().toString())),
                hasJsonPath("$.status", is(status)),
                hasJsonPath("$.putCode", is(putCode)),
                hasJsonPath("$.responseMessage", is(responseMessage)),
                hasJsonPath("$.type", is("orcidhistory"))
        );
    }
}
