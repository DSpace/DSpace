package org.dspace.app.rest.matcher;

import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class ClarinLicenseLabelMatcher {

    private ClarinLicenseLabelMatcher() {

    }

    public static Matcher<? super Object> matchClarinLicenseLabel(ClarinLicenseLabel clarinLicenseLabel) {
        return allOf(
                hasJsonPath("$.id", is(clarinLicenseLabel.getID())),
                hasJsonPath("$.definition", is(clarinLicenseLabel.getDefinition())),
                hasJsonPath("$.type", is(ClarinLicenseLabelRest.NAME)),
                hasJsonPath("$.title", is(clarinLicenseLabel.getTitle())),
                hasJsonPath("$.extended", is(clarinLicenseLabel.isExtended())),
                hasJsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/clarinlicenselabels/" + clarinLicenseLabel.getID()))
        );
    }
}
