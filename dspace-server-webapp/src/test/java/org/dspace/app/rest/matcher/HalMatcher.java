package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
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

    public HalMatcher() { }

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
    public static Matcher<? super Object> hasLinks(String selfHref, String... rels) {
        List<Matcher<? super Object>> matchers = new ArrayList<>();
        for (String rel : rels) {
            String href = rel.equals("self") ? selfHref : selfHref + "/" + rel;
            matchers.add(hasJsonPath("$._links." + rel + ".href", is(href)));
        }
        matchers.add(hasJsonPath("$._links.length()", equalTo(rels.length)));
        return allOf(matchers);
    }

    /**
     * Check that the full set of embeds are included for BundleRest
     */
    public static Matcher<? super Object> matchBundleEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.primaryBitstream"),
                hasJsonPath("$._embedded.bitstreams._embedded.bitstreams"),
                hasJsonPath("$._links.length()", equalTo(2))
        );
    }

    /**
     * Check that the full set of links are included for BundleRest
     */
    public static Matcher<? super Object> matchBundleLinks() {
        return allOf(
                hasJsonPath("$._links.bitstreams.href"),
                hasJsonPath("$._links.primaryBitstream.href"),
                hasJsonPath("$._links.self.href", containsString("/api/core/bundles")),
                hasJsonPath("$._links.length()", equalTo(3))
        );
    }

    /**
     * Check that the full set of embeds are included for BitstreamRest
     */
    public static Matcher<? super Object> matchBitstreamEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.format"),
                hasJsonPath("$._embedded.length()", equalTo(1))
        );
    }

    /**
     * Check that the full set of links are included for BitstreamRest
     */
    public static Matcher<? super Object> matchBitstreamLinks() {
        return allOf(
                hasJsonPath("$._links.content.href"),
                hasJsonPath("$._links.bundle.href"),
                hasJsonPath("$._links.format.href"),
                hasJsonPath("$._links.self.href", containsString("/api/core/bitstreams")),
                hasJsonPath("$._links.length()", equalTo(4))
        );
    }

    /**
     * Check that the full set of embeds are included for EpersonRest
     */
    public static Matcher<? super Object> matchEpersonEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.groups"),
                hasJsonPath("$._embedded.length()", equalTo(1))
        );
    }

    /**
     * Check that the full set of links are included for EpersonRest
     */
    public static Matcher<? super Object> matchEpersonLinks() {
        return allOf(
                hasJsonPath("$._links.groups.href"),
                hasJsonPath("$._links.self.href", containsString("/api/eperson")),
                hasJsonPath("$._links.length()", equalTo(2))
        );
    }

    /**
     * Check that the full set of embeds are included for AuthorityRest
     */
    public static Matcher<? super Object> matchAuthorityEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.authorityEntries"),
                hasJsonPath("$._embedded.length()", equalTo(1))
        );
    }

    /**
     * Check that the full set of links are included for AuthorityRest
     */
    public static Matcher<? super Object> matchAuthorityLinks() {
        return allOf(
                hasJsonPath("$._links.self.href", containsString("/api/integration/authorities")),
                hasJsonPath("$._links.length()", equalTo(1))
        );
    }

    /**
     * Check that the full set of embeds are included for GroupRest
     */
    public static Matcher<? super Object> matchGroupsEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.groups"),
                hasJsonPath("$._embedded.length()", equalTo(1))
        );
    }

    /**
     * Check that the full set of links are included for GroupRest
     */
    public static Matcher<? super Object> matchGroupsLinks() {
        return allOf(
                hasJsonPath("$._links.self.href", containsString("/api/eperson/groups")),
                hasJsonPath("$._links.length()", equalTo(1))
        );
    }

    /**
     * Check that the full set of embeds are included for AuthenticationStatusRest
     */
    public static Matcher<? super Object> matchAuthStatusEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.eperson"),
                hasJsonPath("$._embedded.length()", equalTo(1))
        );
    }

    /**
     * Check that the full set of links are included for AuthenticationStatusRest
     */
    public static Matcher<? super Object> matchAuthStatusLinks() {
        return allOf(
                hasJsonPath("$._links.self.href", containsString("/api/authn/status")),
                hasJsonPath("$._links.length()", equalTo(2))
        );
    }


    /**
     * Check that the full set of embeds are included for HarvestedCollectionRest
     */
    public static Matcher<? super Object> matchHarvesterMetadataEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.harvestermetadata"),
                hasJsonPath("$._embedded.length()", equalTo(1))
        );
    }

    /**
     * Check that the full set of links are included for HarvestedCollectionRest
     */
    public static Matcher<? super Object> matchHarvesterMetadataLinks() {
        return allOf(
                hasJsonPath("$._links.self.href", containsString("/api/core/collections")),
                hasJsonPath("$._links.length()", equalTo(1))
        );
    }


    /**
     * Check that the full set of embeds are included for SubmissionDefinitionRest
     */
    public static Matcher<? super Object> matchSubmissionDefintionsEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.collections._embedded.collections"),
                hasJsonPath("$._embedded.sections._embedded.sections"),
                hasJsonPath("$._embedded.length()", equalTo(2))
        );
    }

    /**
     * Check that the full set of links are included for SubmissionDefinitionRest
     */
    public static Matcher<? super Object> matchSubmissionDefintionsLinks() {
        return allOf(
                hasJsonPath("$._links.collections.href"),
                hasJsonPath("$._links.sections.href"),
                hasJsonPath("$._links.self.href", containsString("/api/config/submissiondefinitions/traditional")),
                hasJsonPath("$._links.length()", equalTo(3))
        );
    }
}
