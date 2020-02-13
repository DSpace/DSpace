/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;

/**
 * Utility class to construct matchers for HAL resources.
 *
 * @author Andrew Wood (AndrewZDemouraAtmire at gmail dot com)
 */
public class HalMatcher {

    private HalMatcher() { }

    /**
     * Gets a matcher for no _embedded property.
     */
    public static Matcher<? super Object> matchNoEmbeds() {
        return hasNoJsonPath("$._embedded");
    }

    /**
     * Gets a matcher for the given set of _embedded rels.
     *
     * The matcher checks that exactly the given set of embeds is included. It does not verify exact values;
     * just that a value is given for each specified rel name. Value verification, if needed, should use
     * a separate matcher.
     *
     * @param rels the names of the rels. If a given name ends with "[]", it is assumed to be a paged subresource
     *             and must therefore contain an embeded array with the same property name as the rel (without the []).
     */
    public static Matcher<? super Object> matchEmbeds(String... rels) {
        if (rels.length == 0) {
            return matchNoEmbeds();
        }
        List<Matcher<? super Object>> matchers = new ArrayList<>();
        for (String rel : rels) {
            if (rel.endsWith("[]")) {
                // paged
                rel = rel.replace("[]", "");
                matchers.add(hasJsonPath("$._embedded." + rel + "._embedded." + rel));
            } else {
                // non-paged
                matchers.add(hasJsonPath("$._embedded." + rel));
            }
        }
        matchers.add(hasJsonPath("$._embedded.length()", equalTo(rels.length)));
        return allOf(matchers);
    }

    /**
     * Gets a matcher for the given set of _link rels.
     *
     * The matcher checks that exactly the given set of links is included, and that each has the expected
     * href value.
     *
     * @param selfHref the href
     * @param rels the names of the rels, which are assumed to be subresources and thus have hrefs ending with
     *             "/rel"
     */
    public static Matcher<? super Object> matchLinks(String selfHref, String... rels) {
        List<Matcher<? super Object>> matchers = new ArrayList<>();
        for (String rel : rels) {
            String href = rel.equals("self") ? selfHref : selfHref + "/" + rel;
            matchers.add(hasJsonPath("$._links." + rel + ".href", is(href)));
        }
        matchers.add(hasJsonPath("$._links.length()", equalTo(rels.length)));
        return allOf(matchers);
    }
}
