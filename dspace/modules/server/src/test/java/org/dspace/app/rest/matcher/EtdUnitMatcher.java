package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.HalMatcher.matchEmbeds;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.hamcrest.Matcher;

public class EtdUnitMatcher {

    private EtdUnitMatcher() {
    }

    /**
     * Returns a Matcher based on the given UUID and name.
     * 
     * @param uuid the UUID of the EtdUnit being matched
     * @param name the name of the EtdUnit being matched
     * @return a Matcher based on the given UUID and name.
     */
    public static Matcher<? super Object> matchEtdUnitEntry(UUID uuid, String name) {
        return allOf(
                matchProperties(uuid, name));
    }

    /**
     * Returns a Matcher for a EtdUnit based on the given name
     * 
     * @param name the name of EtdUnit being matched
     * @return a Matcher for a EtdUnit based on the given name
     */
    public static Matcher<? super Object> matchEtdUnitWithName(String name) {
        return allOf(
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.type", is("etdunit")),
                hasJsonPath("$._links.self.href", containsString("/api/core/etdunits/")),
                hasJsonPath("$._links.collections.href", endsWith("/collections")));
    }

    /**
     * Returns a Matcher for all expected embeds when the full projection is
     * requested.
     *
     * @return a Matcher for all expected embeds
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "collections[]");
    }

    /**
     * Returns a Matcher for all expected links.
     *
     * @param uuid the UUID of the EtdUnit
     * @return a Matcher for all expected links
     */
    public static Matcher<? super Object> matchLinks(UUID uuid) {
        return HalMatcher.matchLinks(REST_SERVER_URL + "core/etdunits/" + uuid,
                "collections",
                "self");
    }

    /**
     * GReturns a Matcher for all expected properties, based on the given UUID
     * and name.
     *
     * @param uuid the UUID of the EtdUnit being matched
     * @param name the name of the EtdUnit being matched
     * @return a Matcher for all expected properties
     */
    private static Matcher<? super Object> matchProperties(UUID uuid, String name) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.type", is("etdunit")),
                hasJsonPath("$._links.self.href", containsString("/api/core/etdunits/" + uuid.toString())),
                hasJsonPath("$._links.collections.href", endsWith(uuid.toString() + "/collections")));
    }
}
