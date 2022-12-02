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

import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for a clarin license label
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseLabelMatcher {

    private ClarinLicenseLabelMatcher() {

    }

    public static Matcher<? super Object> matchClarinLicenseLabel(ClarinLicenseLabel clarinLicenseLabel) {
        return allOf(
                hasJsonPath("$.label", is(clarinLicenseLabel.getLabel())),
                hasJsonPath("$.type", is(ClarinLicenseLabelRest.NAME)),
                hasJsonPath("$.title", is(clarinLicenseLabel.getTitle())),
                hasJsonPath("$.extended", is(clarinLicenseLabel.isExtended()))
        );
    }

    public static Matcher<? super Object> matchExtendedClarinLicenseLabel(ClarinLicenseLabel clarinLicenseLabel) {
        return allOf(
                hasJsonPath("$.label", is(clarinLicenseLabel.getLabel())),
                hasJsonPath("$.type", is(ClarinLicenseLabelRest.NAME)),
                hasJsonPath("$.title", is(clarinLicenseLabel.getTitle())),
                hasJsonPath("$.extended", is(clarinLicenseLabel.isExtended()))
        );
    }
}
