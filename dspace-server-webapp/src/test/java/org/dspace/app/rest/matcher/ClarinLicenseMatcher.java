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
import static org.hamcrest.Matchers.is;

import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.content.clarin.ClarinLicense;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Utility class to construct a Matcher for a clarin license
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseMatcher {

    private ClarinLicenseMatcher() {

    }

    public static Matcher<? super Object> matchClarinLicense(ClarinLicense clarinLicense) {
        return allOf(
                hasJsonPath("$.id", is(clarinLicense.getID())),
                hasJsonPath("$.name", is(clarinLicense.getName())),
                hasJsonPath("$.definition", is(clarinLicense.getDefinition())),
                hasJsonPath("$.type", is(ClarinLicenseRest.NAME)),
                hasJsonPath("$.confirmation", is(clarinLicense.getConfirmation())),
                hasJsonPath("$.requiredInfo", is(clarinLicense.getRequiredInfo())),
                hasJsonPath("$.bitstreams", Matchers.not(Matchers.empty())),
                hasJsonPath("$.clarinLicenseLabel", Matchers.not(Matchers.empty())),
                hasJsonPath("$.extendedClarinLicenseLabels", Matchers.not(Matchers.empty())),
                hasJsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/clarinlicenses/" + clarinLicense.getID()))
        );
    }

    // when the Clarin License is updated
    public static Matcher<? super Object> matchClarinLicenseWithoutId(ClarinLicense clarinLicense) {
        return allOf(
                hasJsonPath("$.name", is(clarinLicense.getName())),
                hasJsonPath("$.definition", is(clarinLicense.getDefinition())),
                hasJsonPath("$.type", is(ClarinLicenseRest.NAME)),
                hasJsonPath("$.confirmation", is(clarinLicense.getConfirmation())),
                hasJsonPath("$.requiredInfo", is(clarinLicense.getRequiredInfo())),
                hasJsonPath("$.bitstreams", Matchers.not(Matchers.empty())),
                hasJsonPath("$.clarinLicenseLabel", Matchers.not(Matchers.empty())),
                hasJsonPath("$.extendedClarinLicenseLabels", Matchers.not(Matchers.empty()))
        );
    }
}
