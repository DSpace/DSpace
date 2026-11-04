/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CCLicenseConnectorServiceImpl}, based on the configured CC license CSV.
 */
public class CCLicenseConnectorServiceImplIT extends AbstractIntegrationTestWithDatabase {

    private static final String CC_30 = "license1";
    private static final String CC_40 = "license265";
    private static final String CC_ZERO = "license271";
    private static final String MARK = "license272";
    private static final String CERTIFICATION = "license273";
    private static final String SAMPLING = "license274";

    private CCLicenseConnectorService ccLicenseConnectorService;
    private ConfigurationService configurationService;

    @Before
    public void init() {
        ccLicenseConnectorService = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServicesByType(CCLicenseConnectorService.class).get(0);
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Test
    public void retrieveLicensesDefaultClassFilterTest() {
        // Default cc.license.classfilter = recombo, mark, certification
        Map<String, CCLicense> licenses = ccLicenseConnectorService.retrieveLicenses("en");

        assertThat(licenses.keySet(), containsInAnyOrder(CC_30, CC_40, CC_ZERO));
    }

    @Test
    public void retrieveLicensesWithoutClassFilterTest() {
        configurationService.setProperty("cc.license.classfilter", null);

        Map<String, CCLicense> licenses = ccLicenseConnectorService.retrieveLicenses("en");

        assertThat(licenses.keySet(), containsInAnyOrder(CC_30, CC_40, CC_ZERO, MARK, CERTIFICATION, SAMPLING));
    }

    @Test
    public void retrieveLicensesExcludeByCCVersionAndUnitTest() {
        configurationService.setProperty("cc.license.classfilter", new String[] {"CC3.0", "zero"});

        Map<String, CCLicense> licenses = ccLicenseConnectorService.retrieveLicenses("en");

        assertThat(licenses.keySet(), containsInAnyOrder(CC_40, MARK, CERTIFICATION, SAMPLING));
    }

    @Test
    public void retrieveLicensesExcludeStandardTest() {
        configurationService.setProperty("cc.license.classfilter", new String[] {"standard"});

        Map<String, CCLicense> licenses = ccLicenseConnectorService.retrieveLicenses("en");

        assertThat(licenses.keySet(), containsInAnyOrder(CC_ZERO, MARK, CERTIFICATION, SAMPLING));
    }

    @Test
    public void retrieveLicensesConfiguredJurisdictionTest() {
        configurationService.setProperty("cc.license.jurisdiction", "de");

        List<CCLicenseFieldEnum> enums = getJurisdictionEnums();

        assertThat(enums.stream().map(CCLicenseFieldEnum::getId).toList(), contains("de"));
    }

    @Test
    public void retrieveLicensesUnportedJurisdictionTest() {
        configurationService.setProperty("cc.license.jurisdiction", null);

        List<CCLicenseFieldEnum> enums = getJurisdictionEnums();

        assertThat(enums.stream().map(CCLicenseFieldEnum::getId).toList(), contains(""));
    }

    @Test
    public void retrieveLicensesUserSelectedJurisdictionTest() {
        configurationService.setProperty("cc.license.jurisdiction", "none");

        List<String> ids = getJurisdictionEnums().stream().map(CCLicenseFieldEnum::getId).toList();

        // generic/unported option first, followed by all 3.0 jurisdictions of the CSV
        assertThat(ids, hasSize(44));
        assertThat(ids.get(0), is(""));
        assertThat(ids, hasItems("us", "de", "nz"));
    }

    @Test
    public void retrieveRightsByQuestionTest() {
        assertThat(ccLicenseConnectorService.retrieveRightsByQuestion(CC_40, "en",
                        Map.of("commercial", "n", "derivatives", "sa")),
                is("https://creativecommons.org/licenses/by-nc-sa/4.0/"));
        assertThat(ccLicenseConnectorService.retrieveRightsByQuestion(CC_30, "en",
                        Map.of("commercial", "y", "derivatives", "n", "jurisdiction", "us")),
                is("https://creativecommons.org/licenses/by-nd/3.0/us/"));
        assertThat(ccLicenseConnectorService.retrieveRightsByQuestion(CC_ZERO, "en", Map.of()),
                is("https://creativecommons.org/publicdomain/zero/1.0/"));
    }

    @Test
    public void retrieveSamplingLicenseTest() {
        configurationService.setProperty("cc.license.classfilter", null);

        CCLicense sampling = ccLicenseConnectorService.retrieveLicenses("en").get(SAMPLING);

        assertThat(sampling.getCcLicenseFieldList(), hasSize(1));
        assertThat(sampling.getCcLicenseFieldList().get(0).getFieldEnum().stream()
                        .map(CCLicenseFieldEnum::getId).toList(),
                contains("sampling", "samplingplus", "ncsamplingplus"));
        assertThat(ccLicenseConnectorService.retrieveRightsByQuestion(SAMPLING, "en", Map.of("sampling", "sampling")),
                is("https://creativecommons.org/licenses/sampling/1.0/"));
        assertThat(ccLicenseConnectorService.retrieveRightsByQuestion(SAMPLING, "en",
                        Map.of("sampling", "ncsamplingplus")),
                is("https://creativecommons.org/licenses/nc-sampling+/1.0/"));
        assertThat(ccLicenseConnectorService.retrieveLicenseName("https://creativecommons.org/licenses/sampling+/1.0/"),
                is("Sampling Plus 1.0 Generic"));
    }

    @Test
    public void retrieveLicenseNameTest() {
        assertThat(ccLicenseConnectorService.retrieveLicenseName("https://creativecommons.org/licenses/by/4.0/"),
                is("Attribution 4.0 International"));
        assertThat(ccLicenseConnectorService.retrieveLicenseName("https://example.org/unknown-license"),
                nullValue());
    }

    private List<CCLicenseFieldEnum> getJurisdictionEnums() {
        CCLicense cc30 = ccLicenseConnectorService.retrieveLicenses("en").get(CC_30);
        return cc30.getCcLicenseFieldList().stream()
                .filter(field -> "jurisdiction".equals(field.getId()))
                .findFirst()
                .orElseThrow()
                .getFieldEnum();
    }
}
