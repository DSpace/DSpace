/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

public class MetadataMatcher {

    private MetadataMatcher() { }

    public static Matcher<? super Object> matchMetadata(String key, String value) {
        return hasJsonPath("$.['" + key + "'][*].value", contains(value));
    }

    public static Matcher<? super Object> matchMetadata(String key, String value, int position) {
        return hasJsonPath("$.['" + key + "'][" + position + "].value", is(value));
    }
}
