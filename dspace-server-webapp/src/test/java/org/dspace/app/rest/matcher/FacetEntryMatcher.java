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
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Matcher;
import org.hamcrest.core.AnyOf;

public class FacetEntryMatcher {

    private FacetEntryMatcher() {
    }

    // List of facet matches for discovery configurations
    // Add more configurations if needed for further tests here
    public static Matcher<? super Object>[] defaultFacetMatchers =
            getFacetMatchersForConfig("defaultConfiguration");
    public static Matcher<? super Object>[] workflowFacetMatchers =
            getFacetMatchersForConfig("workflowConfiguration");
    public static Matcher<? super Object>[] workspaceFacetMatchers =
            getFacetMatchersForConfig("workspaceConfiguration");
    public static Matcher<? super Object>[] supervisionFacetMatchers =
            getFacetMatchersForConfig("supervisionConfiguration");
    public static Matcher<? super Object>[] workflowAdminFacetMatchers =
            getFacetMatchersForConfig("workflowAdminConfiguration");
    public static Matcher<? super Object>[] notifyOutgoingFacetMatchers =
            getFacetMatchersForConfig("NOTIFY.outgoing");
    public static Matcher<? super Object>[] notifyIncomingFacetMatchers =
            getFacetMatchersForConfig("NOTIFY.incoming");


    public static Matcher<? super Object>[] getFacetMatchersForConfig(String configName) {
        DiscoveryConfiguration config =
                DSpaceServicesFactory.getInstance()
                                     .getServiceManager()
                                     .getServiceByName(configName, DiscoveryConfiguration.class);
        List<DiscoverySearchFilterFacet> sidebarFacets = config.getSidebarFacets();
        return createFacetMatchers(sidebarFacets);
    }

    public static Matcher<? super Object> matchFacet(DiscoverySearchFilterFacet facet) {
        return allOf(
                hasJsonPath("$.name", is(facet.getIndexFieldName())),
                hasJsonPath("$.facetType", is(facet.getType())),
                hasJsonPath("$.facetLimit", any(Integer.class)),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/" + facet.getIndexFieldName())),
                hasJsonPath("$._links", matchNextLink("api/discover/facets/" + facet.getIndexFieldName()))
        );
    }

    public static Matcher<? super Object>[] createFacetMatchers(List<DiscoverySearchFilterFacet> facets) {
        return facets.stream()
                     .map(FacetEntryMatcher::matchFacet)
                     .toArray(Matcher[]::new);
    }

    public static Matcher<? super Object> authorFacet() {
        return allOf(
            hasJsonPath("$.name", is("author")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/author")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/author"))
        );
    }

    public static Matcher<? super Object> languageFacet() {
        return allOf(
            hasJsonPath("$.name", is("language")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/language")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/language"))
        );
    }

    public static Matcher<? super Object> typesFacet() {
        return allOf(
            hasJsonPath("$.name", is("types")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/types")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/types"))
        );
    }

    public static Matcher<? super Object> authorFacetWithMinMax(String min, String max) {
        return allOf(
            hasJsonPath("$.name", is("author")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.minValue", is(min)),
            hasJsonPath("$.maxValue", is(max)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/author")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/author"))
        );
    }

    public static Matcher<? super Object> subjectFacet() {
        return allOf(
            hasJsonPath("$.name", is("subject")),
            hasJsonPath("$.facetType", is("hierarchical")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/subject")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/subject"))

        );
    }

    public static Matcher<? super Object> submitterFacet() {
        return allOf(
            hasJsonPath("$.name", is("submitter")),
            hasJsonPath("$.facetType", is("authority")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/submitter")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/submitter"))

        );
    }

    public static Matcher<? super Object> supervisedByFacet() {
        return allOf(
            hasJsonPath("$.name", is("supervisedBy")),
            hasJsonPath("$.facetType", is("authority")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/supervisedBy")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/supervisedBy"))

        );
    }

    public static Matcher<? super Object> dateIssuedFacet() {
        return allOf(
            hasJsonPath("$.name", is("dateIssued")),
            hasJsonPath("$.facetType", is("date")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/dateIssued"))
        );
    }

    public static Matcher<? super Object> dateIssuedFacetWithMinMax(String min, String max) {
        return allOf(
            hasJsonPath("$.name", is("dateIssued")),
            hasJsonPath("$.facetType", is("date")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.minValue", is(min)),
            hasJsonPath("$.maxValue", is(max)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/dateIssued"))
        );
    }

    public static Matcher<? super Object> hasContentInOriginalBundleFacet() {
        return allOf(
            hasJsonPath("$.name", is("has_content_in_original_bundle")),
            hasJsonPath("$.facetType", is("standard")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/has_content_in_original_bundle")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/has_content_in_original_bundle"))
        );
    }

    public static Matcher<? super Object> matchFacet(String name, String facetType) {
        return allOf(
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.facetType", is(facetType)),
                hasJsonPath("$.facetLimit", any(Integer.class)),
                hasJsonPath("$.openByDefault", any(Boolean.class)),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/" + name)),
                hasJsonPath("$._links", matchNextLink("api/discover/facets/" + name))
        );
    }


    /**
     * Check that a facet over the dc.type exists and match the default configuration
     *
     * @return a Matcher
     */
    public static Matcher<? super Object> typeFacet() {
        return allOf(
                hasJsonPath("$.name", is("itemtype")),
                hasJsonPath("$.facetType", is("hierarchical")),
                hasJsonPath("$.facetLimit", any(Integer.class)),
                hasJsonPath("$.openByDefault", any(Boolean.class)),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/itemtype")),
                hasJsonPath("$._links", matchNextLink("api/discover/facets/itemtype"))
            );
    }

    /**
     * Check that a facet over the object type (workspaceitem, workflowitem, etc.) exists and match the default
     * configuration
     *
     * @return a Matcher
     */
    public static Matcher<? super Object> resourceTypeFacet() {
        return allOf(
                hasJsonPath("$.name", is("namedresourcetype")),
                hasJsonPath("$.facetType", is("authority")),
                hasJsonPath("$.facetLimit", any(Integer.class)),
                hasJsonPath("$.openByDefault", any(Boolean.class)),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/namedresourcetype")),
                hasJsonPath("$._links", matchNextLink("api/discover/facets/namedresourcetype"))
            );
    }

    private static AnyOf<? super Object> matchNextLink(String path) {

        return anyOf(hasJsonPath("$.next.href", containsString(path)),
                           not(hasJsonPath("$.next.href", containsString(path))));
    }
    public static Matcher<? super Object> entityTypeFacet() {
        return allOf(
            hasJsonPath("$.name", is("entityType")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/entityType")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/entityType"))
        );
    }

    public static Matcher<? super Object> anyFacet(String name, String facetType) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.facetType", is(facetType)),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/" + name))
        );
    }

    public static Matcher<? super Object> relatedItemFacet() {
        return allOf(
            hasJsonPath("$.name", is("relateditem")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/relateditem")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/relateditem"))
        );
    }

    public static Matcher<? super Object> originFacet(boolean b) {
        return allOf(
            hasJsonPath("$.name", is("origin")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/origin")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/origin"))
        );
    }

    public static Matcher<? super Object> accessStatusFacet(boolean hasNext) {
        return allOf(
            hasJsonPath("$.name", is("access_status")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/access_status")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/access_status"))
        );
    }

    public static Matcher<? super Object> targetFacet() {
        return allOf(
            hasJsonPath("$.name", is("target")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/target")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/target"))
        );
    }

    public static Matcher<? super Object> queueStatusFacet() {
        return allOf(
            hasJsonPath("$.name", is("queue_status")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/queue_status")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/queue_status"))
        );
    }

    public static Matcher<? super Object> activityStreamTypeFacet() {
        return allOf(
            hasJsonPath("$.name", is("activity_stream_type")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/activity_stream_type")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/activity_stream_type"))
        );
    }

    public static Matcher<? super Object> coarNotifyTypeFacet() {
        return allOf(
            hasJsonPath("$.name", is("coar_notify_type")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/coar_notify_type")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/coar_notify_type"))
        );
    }

    public static Matcher<? super Object> notificationTypeFacet() {
        return allOf(
            hasJsonPath("$.name", is("notification_type")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.openByDefault", any(Boolean.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/notification_type")),
            hasJsonPath("$._links", matchNextLink("api/discover/facets/notification_type"))
        );
    }

}
