package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

/**
 * Created by jonas - jonas@atmire.com on 01/12/17.
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
