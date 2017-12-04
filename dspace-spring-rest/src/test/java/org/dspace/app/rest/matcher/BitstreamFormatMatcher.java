/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Jonas Van Goolen - (jonas@atmire.com)
 */
public class BitstreamFormatMatcher {

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
