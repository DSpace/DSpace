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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.dspace.eperson.EPerson;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class EPersonMatcher {

    private EPersonMatcher() { }

    public static Matcher<? super Object> matchEPersonEntry(EPerson ePerson) {
        return allOf(
            hasJsonPath("$.uuid", is(ePerson.getID().toString())),
            hasJsonPath("$.name", is(ePerson.getName())),
            hasJsonPath("$.type", is("eperson")),
            hasJsonPath("$.canLogIn", not(empty())),
            hasJsonPath("$._links.self.href", containsString("/api/eperson/epersons/" + ePerson.getID().toString())),
            hasJsonPath("$.metadata", Matchers.allOf(
                MetadataMatcher.matchMetadata("eperson.firstname", ePerson.getFirstName()),
                MetadataMatcher.matchMetadata("eperson.lastname", ePerson.getLastName())
            ))
        );
    }


    public static Matcher<? super Object> matchEPersonOnEmail(String email) {
        return allOf(
            hasJsonPath("$.type", is("eperson")),
            hasJsonPath("$.email", is(email))
            );
    }

    public static Matcher<? super Object> matchDefaultTestEPerson() {
        return allOf(
                hasJsonPath("$.type", is("eperson"))
        );
    }
}
