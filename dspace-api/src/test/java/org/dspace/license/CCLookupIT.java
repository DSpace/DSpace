/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


public class CCLookupIT {

    private static ConfigurationService config = null;
    private CCLookup ccLookup = new CCLookup();
    private static final String LICENSE_ID = "standard";

    @BeforeClass
    public static void setUpClass()
            throws Exception
    {
        // Find the configuration service
        config = DSpaceServicesFactory.getInstance().getConfigurationService();
        config.setProperty("cc.api.rooturl","http://api.creativecommons.org/rest/1.5");
    }

    @Test
    public void noLocaleCC4Link() throws Exception {
        String lang = "no";
        Map<String, String> answers = new HashMap<>();
        answers.put("commercial", "y");
        answers.put("derivatives", "y");
        answers.put("jurisdiction", "");

        ccLookup.issue(LICENSE_ID, answers, lang);

        assertThat("Operation success", ccLookup.isSuccess(), is(true));
        assertThat("License document exists", ccLookup.getLicenseDocument(), notNullValue());
        assertThat("CC version 4", ccLookup.getLicenseUrl(), containsString("4."));
        assertThat("CC locale language link", ccLookup.getLicenseUrl(), endsWith("deed."+lang));
        assertThat("CC licence name", ccLookup.getLicenseName().length(), not(0));
    }

    @Test
    public void defaultLocaleCC4Link() throws Exception {
        Map<String, String> answers = new HashMap<>();
        answers.put("commercial", "y");
        answers.put("derivatives", "y");
        answers.put("jurisdiction", "");

        ccLookup.issue(LICENSE_ID, answers, null);

        assertThat("Operation success", ccLookup.isSuccess(), is(true));
        assertThat("License document exists", ccLookup.getLicenseDocument(), notNullValue());
        assertThat("CC version 4", ccLookup.getLicenseUrl(), containsString("4."));
        assertThat("CC locale language link", ccLookup.getLicenseUrl(), endsWith("deed.en"));
        assertThat("CC licence name", ccLookup.getLicenseName().length(), not(0));
    }

    @Test
    public void noLocaleCC3Link() throws Exception {
        String lang = "no";
        Map<String, String> answers = new HashMap<>();
        answers.put("commercial", "y");
        answers.put("derivatives", "y");
        answers.put("jurisdiction", "no");

        ccLookup.issue(LICENSE_ID, answers, lang);

        assertThat("Operation success", ccLookup.isSuccess(), is(true));
        assertThat("License document exists", ccLookup.getLicenseDocument(), notNullValue());
        assertThat("CC version 3", ccLookup.getLicenseUrl(), containsString("3."));
        assertThat("CC locale language link", ccLookup.getLicenseUrl(), not(endsWith("deed."+lang)));
        assertThat("CC licence name", ccLookup.getLicenseName().length(), not(0));
    }

    @Test
    public void defaultLocaleCC3Link() throws Exception {
        Map<String, String> answers = new HashMap<>();
        answers.put("commercial", "y");
        answers.put("derivatives", "y");
        answers.put("jurisdiction", "no");

        ccLookup.issue(LICENSE_ID, answers, null);

        assertThat("Operation success", ccLookup.isSuccess(), is(true));
        assertThat("License document exists", ccLookup.getLicenseDocument(), notNullValue());
        assertThat("CC version 3", ccLookup.getLicenseUrl(), containsString("3."));
        assertThat("CC locale language link", ccLookup.getLicenseUrl(), not(containsString("deed.")));
        assertThat("CC licence name", ccLookup.getLicenseName().length(), not(0));
    }

    @Test
    public void lookupNoLocale4Link() throws Exception {
        String licenseUri = "http://creativecommons.org/licenses/by/4.0/";
        String localeLicenseUri = licenseUri + "deed.no";

        ccLookup.issue(localeLicenseUri);

        assertThat("Operation success", ccLookup.isSuccess(), is(true));
        assertThat("License document exists", ccLookup.getLicenseDocument(), notNullValue());
        assertThat("This CC operation does not support locale", ccLookup.getLicenseUrl(), equalTo(licenseUri));
        assertThat("CC licence name", ccLookup.getLicenseName().length(), not(0));
    }

    @Test
    public void lookupDefaultLocale4Link() throws Exception {
        String licenseUri = "http://creativecommons.org/licenses/by/4.0/";
        ccLookup.issue(licenseUri);

        assertThat("Operation success", ccLookup.isSuccess(), is(true));
        assertThat("License document exists", ccLookup.getLicenseDocument(), notNullValue());
        assertThat("This CC operation does not support locale", ccLookup.getLicenseUrl(), equalTo(licenseUri));
        assertThat("CC licence name", ccLookup.getLicenseName().length(), not(0));
    }

    @Test
    public void lookupNoLocale3Link() throws Exception {
        String licenseUri = "http://creativecommons.org/licenses/by/3.0/no/";
        ccLookup.issue(licenseUri);

        assertThat("Operation success", ccLookup.isSuccess(), is(true));
        assertThat("License document exists", ccLookup.getLicenseDocument(), notNullValue());
        assertThat("This CC operation does not support locale", ccLookup.getLicenseUrl(), equalTo(licenseUri));
        assertThat("CC licence name", ccLookup.getLicenseName().length(), not(0));
    }

    @Test
    public void lookupDefaultLocale3Link() throws Exception {
        String licenseUri = "http://creativecommons.org/licenses/by/3.0/";
        ccLookup.issue(licenseUri);

        assertThat("Operation success", ccLookup.isSuccess(), is(true));
        assertThat("License document exists", ccLookup.getLicenseDocument(), notNullValue());
        assertThat("This CC operation does not support locale", ccLookup.getLicenseUrl(), equalTo(licenseUri));
        assertThat("CC licence name", ccLookup.getLicenseName().length(), not(0));
    }
}