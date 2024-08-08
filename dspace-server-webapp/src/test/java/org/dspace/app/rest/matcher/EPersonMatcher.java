/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.HalMatcher.matchEmbeds;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.lang.reflect.Array;
import java.util.UUID;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hibernate.LazyInitializationException;

public class EPersonMatcher {
    // todo: this may not work in all cases!
    public static final EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();

    private EPersonMatcher() { }

    public static Matcher<? super Object> matchEPersonEntry(EPerson ePerson) {
        return allOf(
           matchProperties(ePerson),
           matchLinks(ePerson.getID())
        );
    }

    public static Matcher<? super Object> matchEPersonOnEmail(String email) {
        return allOf(
            hasJsonPath("$.type", is("eperson")),
            hasJsonPath("$.email", is(email))
            );
    }

    public static Matcher<? super Object> matchEPersonWithGroups(String email, String... groups) {
        Matcher<? super Object>[] matchers =
                (Matcher<? super Object>[]) Array.newInstance(Matcher.class, groups.length);
        for (int i = 0; i < groups.length; i++) {
            matchers[i] = GroupMatcher.matchGroupWithName(groups[i]);
        }

        return allOf(
                hasJsonPath("$.type", is("eperson")),
                hasJsonPath("$.email", is(email)),
                hasJsonPath("$._embedded.groups._embedded.groups", containsInAnyOrder(
                        matchers)));
    }

    public static Matcher<? super Object> matchDefaultTestEPerson() {
        return allOf(
                hasJsonPath("$.type", is("eperson"))
        );
    }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "groups[]"
        );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks(UUID uuid) {
        return HalMatcher.matchLinks(REST_SERVER_URL + "eperson/epersons/" + uuid,
                "groups",
                "self"
        );
    }

    public static Matcher<? super Object> matchProperties(EPerson ePerson) {
        return allOf(
                hasJsonPath("$.uuid", is(ePerson.getID().toString())),
                hasJsonPath("$.name", is(ePerson.getName())),
                hasJsonPath("$.type", is("eperson")),
                hasJsonPath("$.canLogIn", not(empty())),
                hasJsonPath("$.metadata", Matchers.allOf(
                        // todo: this fails when matching against the eperson from AbstractIntegrationTestWithDatabase
                        MetadataMatcher.matchMetadata("eperson.firstname", epersonService.getFirstName(ePerson)),
                        MetadataMatcher.matchMetadata("eperson.lastname", epersonService.getLastName(ePerson))
                ))
        );
    }
}
