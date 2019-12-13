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
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import org.hamcrest.Matcher;

/**
 * @author Jonas Van Goolen - (jonas@atmire.com)
 */
public class BitstreamFormatMatcher {

    private BitstreamFormatMatcher() {
    }

    public static Matcher<? super Object> matchBitstreamFormat(int id, String mimetype, String description) {
        return allOf(
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.mimetype", is(mimetype)),
                hasJsonPath("$.description", is(description)),
                hasJsonPath("$.type", is("bitstreamformat")),
                hasJsonPath("$._links.self.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.self.href", endsWith("/api/core/bitstreamformats/" + id))
        );
    }

    public static Matcher<? super Object> matchBitstreamFormat(int id, String mimetype, String description,
                                                               String shortDescription) {
        return allOf(
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.description", is(description)),
                hasJsonPath("$.shortDescription", is(shortDescription)),
                hasJsonPath("$.mimetype", is(mimetype)),
                hasJsonPath("$.type", is("bitstreamformat")),
                hasJsonPath("$._links.self.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.self.href", endsWith("/api/core/bitstreamformats/" + id))
        );
    }
    public static Matcher<? super Object> matchBitstreamFormat(int id, String mimetype, String description,
                                                               String shortDescription, String supportLevel) {
        return allOf(
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.description", is(description)),
                hasJsonPath("$.shortDescription", is(shortDescription)),
                hasJsonPath("$.mimetype", is(mimetype)),
                hasJsonPath("$.supportLevel", is(supportLevel)),
                hasJsonPath("$.type", is("bitstreamformat")),
                hasJsonPath("$._links.self.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.self.href", endsWith("/api/core/bitstreamformats/" + id))
        );
    }

    public static Matcher<? super Object> matchBitstreamFormat(String mimetype, String description) {
        return allOf(
            hasJsonPath("$.mimetype", is(mimetype)),
            hasJsonPath("$.description", is(description)),
            hasJsonPath("$.type", is("bitstreamformat"))
        );
    }

    public static Matcher<? super Object> matchBitstreamFormatMimeType(String mimetype) {
        return allOf(
            hasJsonPath("$.mimetype", is(mimetype)),
            hasJsonPath("$.type", is("bitstreamformat"))
        );
    }

}