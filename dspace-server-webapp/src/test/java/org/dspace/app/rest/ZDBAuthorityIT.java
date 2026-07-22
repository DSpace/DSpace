/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.matcher.ItemAuthorityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.ItemAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.zdb.ZDBAuthorityValue;
import org.dspace.content.authority.zdb.ZDBService;
import org.dspace.content.authority.zdb.ZDBServicesFactory;
import org.dspace.content.authority.zdb.ZDBServicesFactoryImpl;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class handles ZDBAuthority related IT.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science.it)
 */
public class ZDBAuthorityIT extends AbstractControllerIntegrationTest {

    public static final String ZDB_AUTHORITY =
        "org.dspace.content.authority.ZDBAuthority = ZDBAuthority";

    private static MockedStatic<ZDBServicesFactory> mockZDBServiceFactory;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    protected ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    protected PluginService pluginService;

    private ZDBService zdbService;

    @BeforeClass
    public static void init() {
        mockZDBServiceFactory = Mockito.mockStatic(ZDBServicesFactory.class);
    }

    @AfterClass
    public static void close() {
        mockZDBServiceFactory.close();
    }

    @Before
    public void setup() throws IOException, SubmissionConfigReaderException {
        choiceAuthorityService.getChoiceAuthoritiesNames();
        zdbService = Mockito.mock(ZDBService.class);

        // Create factory with mocked service - generators will be loaded from ServiceManager
        ZDBServicesFactoryImpl zdbServiceFactory = new ZDBServicesFactoryImpl(zdbService);

        Mockito.when(
                   zdbService.list(Mockito.eq("Acta AND Mathematica AND informatica"), Mockito.anyInt(),
                                   Mockito.anyInt()))
               .thenReturn(createMockResults());

        // Mock the static factory method to return our factory with mocked service
        mockZDBServiceFactory.when(ZDBServicesFactory::getInstance).thenReturn(zdbServiceFactory);

        // Register the authority plugin
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] {ZDB_AUTHORITY}
        );

        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
    }

    @After
    public void tearDown() throws Exception {
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void zdbAuthorityTest() throws Exception {
        // Configure ZDB authority source
        configurationService.setProperty("cris.ItemAuthority.ZDBAuthority.source", "zdb");

        // Reset authorities and trigger re-initialization with mock factory
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        DCInputAuthority.getPluginNames();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/ZDBAuthority/entries")
            .param("filter", "Acta AND Mathematica AND informatica"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries",
                Matchers.containsInAnyOrder(
                    ItemAuthorityMatcher.matchItemAuthorityWithTwoMetadataInOtherInformations(
                        "will be generated::zdb::1447228-4", "Acta mathematica et informatica",
                        "Acta mathematica et informatica", "vocabularyEntry", "data-dc_relation_ispartof",
                        "Acta mathematica et informatica::will be generated::zdb::1447228-4",
                        "data-dc_relation_issn", "", getSource()),
                    ItemAuthorityMatcher.matchItemAuthorityWithTwoMetadataInOtherInformations(
                        "will be generated::zdb::1194912-0",
                        "Acta mathematica Universitatis Ostraviensis",
                        "Acta mathematica Universitatis Ostraviensis", "vocabularyEntry",
                        "data-dc_relation_ispartof",
                        "Acta mathematica Universitatis Ostraviensis::will be generated::zdb::1194912-0",
                        "data-dc_relation_issn", "1211-4774", getSource()),
                    ItemAuthorityMatcher.matchItemAuthorityWithTwoMetadataInOtherInformations(
                        "will be generated::zdb::2618143-5",
                        "Acta mathematica Universitatis Ostraviensis",
                        "Acta mathematica Universitatis Ostraviensis", "vocabularyEntry",
                        "data-dc_relation_ispartof",
                        "Acta mathematica Universitatis Ostraviensis::will be generated::zdb::2618143-5",
                        "data-dc_relation_issn", "", getSource()))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
    }

    @Test
    public void zdbAuthorityEmptyQueryTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/ZDBAuthority/entries")
            .param("filter", ""))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void zdbAuthorityUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/submission/vocabularies/ZDBAuthority/entries")
            .param("filter", "Mathematica AND informatica"))
            .andExpect(status().isUnauthorized());
    }

    private String getSource() {
        return configurationService.getProperty(
            "cris.ItemAuthority.ZDBAuthority.source", ItemAuthority.DEFAULT);
    }

    private List<ZDBAuthorityValue> createMockResults() {
        List<ZDBAuthorityValue> results = new ArrayList<>();
        // Create the first entry
        ZDBAuthorityValue zdb1 = new ZDBAuthorityValue();
        zdb1.setServiceId("1447228-4");
        zdb1.setValue("Acta mathematica et informatica");
        zdb1.addOtherMetadata("journalZDBID", "1447228-4");
        zdb1.addOtherMetadata("journalTitle", "Acta mathematica et informatica");

        // Create the second entry
        ZDBAuthorityValue zdb2 = new ZDBAuthorityValue();
        zdb2.setServiceId("1194912-0");
        zdb2.setValue("Acta mathematica Universitatis Ostraviensis");
        zdb2.addOtherMetadata("journalZDBID", "1194912-0");
        zdb2.addOtherMetadata("journalTitle", "Acta mathematica Universitatis Ostraviensis");
        zdb2.addOtherMetadata("journalIssn", "1211-4774");

        // Create the third entry
        ZDBAuthorityValue zdb3 = new ZDBAuthorityValue();
        zdb3.setServiceId("2618143-5");
        zdb3.setValue("Acta mathematica Universitatis Ostraviensis");
        zdb3.addOtherMetadata("journalZDBID", "2618143-5");
        zdb3.addOtherMetadata("journalTitle", "Acta mathematica Universitatis Ostraviensis");

        results.add(zdb1);
        results.add(zdb2);
        results.add(zdb3);
        return results;
    }
}
