package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for an item
 *
 * @author Andrew Wood (AndrewZDemouraAtmire at gmail dot com)
 */
    public class ProjectionsMatcher {

    public ProjectionsMatcher() { }


    /**
     * Check that the full set of embeds are included for ItemRest
     */
    public static Matcher<? super Object> matchItemEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.relationships._embedded.relationships"),
                hasJsonPath("$._embedded.owningCollection"),
                hasJsonPath("$._embedded.bundles._embedded.bundles"),
                hasJsonPath("$._embedded.templateItemOf"),
                hasJsonPath("$._embedded.length()", equalTo(4))
        );
    }

    /**
     * Check that the full set of links are included for ItemRest
     */
    public static Matcher<? super Object> matchItemLinks() {
        return allOf(
                hasJsonPath("$._links.bundles.href"),
                hasJsonPath("$._links.mappedCollections.href"),
                hasJsonPath("$._links.owningCollection.href"),
                hasJsonPath("$._links.relationships.href"),
                hasJsonPath("$._links.templateItemOf.href"),
                hasJsonPath("$._links.self.href", containsString("/api/core/items")),
                hasJsonPath("$._links.length()", equalTo(6))
        );
    }

    /**
     * Check that the full set of embeds are included for CommunityRest
     */
    public static Matcher<? super Object> matchCommunityEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.collections._embedded.collections"),
                hasJsonPath("$._embedded.logo"),
                hasJsonPath("$._embedded.length()", equalTo(2))
        );
    }

    /**
     * Check that the full set of links are included for CommunityRest
     */
    public static Matcher<? super Object> matchCommunityLinks() {
        return allOf(
                hasJsonPath("$._links.collections.href"),
                hasJsonPath("$._links.logo.href"),
                hasJsonPath("$._links.subcommunities.href"),
                hasJsonPath("$._links.self.href", containsString("/api/core/communities")),
                hasJsonPath("$._links.length()", equalTo(4))
        );
    }

    /**
     * Check that the full set of embeds are included for CollectionRest
     */
    public static Matcher<? super Object> matchCollectionEmbeds() {
        return allOf(
                hasJsonPath("$._embedded.logo"),
                hasJsonPath("$._embedded.defaultAccessConditions._embedded.defaultAccessConditions"),
                hasJsonPath("$._embedded.length()", equalTo(2))
        );
    }

    /**
     * Check that the full set of links are included for CollectionRest
     */
    public static Matcher<? super Object> matchCollectionLinks() {
        return allOf(
                hasJsonPath("$._links.harvester.href"),
                hasJsonPath("$._links.itemtemplate.href"),
                hasJsonPath("$._links.defaultAccessConditions.href"),
                hasJsonPath("$._links.license.href"),
                hasJsonPath("$._links.logo.href"),
                hasJsonPath("$._links.self.href", containsString("/api/core/collections")),
                hasJsonPath("$._links.length()", equalTo(7))
        );
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

    /**
     * Check that there is no top level _embedded node in the a rest response
     */
    public static Matcher<? super Object> matchNoEmbeds() {
        return allOf(
                hasNoJsonPath("$._embedded")
        );
    }
}
