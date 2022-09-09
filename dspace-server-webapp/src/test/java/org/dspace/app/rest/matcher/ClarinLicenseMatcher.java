package org.dspace.app.rest.matcher;

import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.content.MetadataValue;
import org.dspace.content.clarin.ClarinLicense;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

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
}
