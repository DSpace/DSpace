/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.ItemAuthorityMatcher.matchItemAuthorityWithOtherInformations;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.core.service.PluginService;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.ror.service.RorImportMetadataSourceService;
import org.dspace.importer.external.ror.service.RorServicesFactory;
import org.dspace.importer.external.ror.service.RorServicesFactoryImpl;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class RorOrgUnitAuthorityIT extends AbstractControllerIntegrationTest {

    public static final String ROR_ORGUNIT_AUTHORITY =
        "org.dspace.content.authority.RorOrgUnitAuthority = OrgUnitAuthority";

    public static final String ROR_FILTER = "02z02cv32 OR 03vb2cr34";

    private static MockedStatic<RorServicesFactory> mockRorServiceFactory;

    @Autowired
    protected ChoiceAuthorityService choiceAuthorityService;
    @Autowired
    protected PluginService pluginService;
    @Autowired
    protected MetadataAuthorityService metadataAuthorityService;
    @Autowired
    protected ConfigurationService configurationService;
    @Mock
    private RorImportMetadataSourceService metadataSourceService;

    @BeforeClass
    public static void init() {
        mockRorServiceFactory = Mockito.mockStatic(RorServicesFactory.class);
    }

    @AfterClass
    public static void close() {
        mockRorServiceFactory.close();
    }

    @After
    public void tearDown() throws Exception {
        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
    }

    @Before
    public void setup() throws Exception {

        RorServicesFactoryImpl rorServiceFactory =
            new RorServicesFactoryImpl(this.metadataSourceService);

        Mockito.when(
            metadataSourceService.getRecords(
                ROR_FILTER, 0, 0
            )
        ).thenReturn(List.of(getImportRecord1(), getImportRecord2()));

        mockRorServiceFactory.when(RorServicesFactory::getInstance).thenReturn(rorServiceFactory);

        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] {ROR_ORGUNIT_AUTHORITY}
        );
    }

    @After
    @Override
    public void destroy() throws Exception {
        super.destroy();
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void testAuthority() throws Exception {

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.RorOrgUnitAuthority = OrgUnitAuthority"
                                         });

        configurationService.setProperty("cris.RorOrgUnitAuthority.country.display", "false");
        configurationService.setProperty("cris.ItemAuthority.OrgUnitAuthority.source", "ror");

        configurationService.setProperty("choices.plugin.crisrp.qualification", "OrgUnitAuthority");
        configurationService.setProperty("choices.presentation.crisrp.qualification", "suggest");
        configurationService.setProperty("authority.controlled.crisrp.qualification", "true");

        configurationService.setProperty("choices.plugin.crisrp.education", "OrgUnitAuthority");
        configurationService.setProperty("choices.presentation.crisrp.education", "suggest");
        configurationService.setProperty("authority.controlled.crisrp.education", "true");

        DCInputAuthority.reset();
        pluginService.clearNamedPluginClasses();

        choiceAuthorityService.getChoiceAuthoritiesNames();
        choiceAuthorityService.clearCache();
        DCInputAuthority.getPluginNames();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/OrgUnitAuthority/entries")
                                     .param("filter", ROR_FILTER))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.entries", hasSize(2)))
                        .andExpect(jsonPath("$._embedded.entries",
                                            hasItems(
                                                matchItemAuthorityWithOtherInformations(
                                                    "will be referenced::ROR-ID::https://ror.org/02z02cv32",
                                                    "Wind Energy Institute of Canada",
                                                    "Wind Energy Institute of Canada", "vocabularyEntry",
                                                    getExtrasRecord1()
                                                ),
                                                matchItemAuthorityWithOtherInformations(
                                                    "will be referenced::ROR-ID::https://ror.org/03vb2cr34",
                                                    "4Science", "4Science", "vocabularyEntry",
                                                    getExtrasRecord2()
                                                )
                                            )
                        ));
    }

    private ImportRecord getImportRecord1() {
        return new ImportRecord(
            List.of(
                new RorMetadatum("dc.title", "Wind Energy Institute of Canada"),
                new RorMetadatum("organization.identifier.ror", "https://ror.org/02z02cv32"),
                new RorMetadatum("oairecerif.identifier.url", "http://www.weican.ca/"),
                new RorMetadatum("dc.type", "Nonprofit"),
                new RorMetadatum("oairecerif.acronym", "IEEC"),
                new RorMetadatum("oairecerif.acronym", "WEICan"),
                new RorMetadatum("organization.address.addressCountry", "CA"),
                new RorMetadatum("organization.foundingDate", "1981")

            )
        );
    }

    private Map<String, String> getExtrasRecord1() {
        Map<String, String> extras = new HashMap<>();
        extras.put("data-ror_orgunit_id", "https://ror.org/02z02cv32");
        extras.put("ror_orgunit_id", "https://ror.org/02z02cv32");
        extras.put("data-ror_orgunit_type", "Nonprofit");
        extras.put("ror_orgunit_type", "Nonprofit");
        extras.put("data-ror_orgunit_acronym", "IEEC, WEICan");
        extras.put("ror_orgunit_acronym", "IEEC, WEICan");
        extras.put("data-ror_orgunit_country", "CA");
        extras.put("data-ror_orgunit_countryName", "CA");
        extras.put("ror_orgunit_countryName", "CA");
        return extras;
    }

    private ImportRecord getImportRecord2() {
        return new ImportRecord(
            List.of(
                new RorMetadatum("dc.title", "4Science"),
                new RorMetadatum("organization.identifier.ror", "https://ror.org/03vb2cr34"),
                new RorMetadatum("oairecerif.identifier.url", "https://www.4science.it/"),
                new RorMetadatum("dc.type", "Company"),
                new RorMetadatum("organization.address.addressCountry", "IT"),
                new RorMetadatum("organization.foundingDate", "2015")
            )
        );
    }

    private Map<String, String> getExtrasRecord2() {
        Map<String, String> extras = new HashMap<>();
        extras.put("data-ror_orgunit_id", "https://ror.org/03vb2cr34");
        extras.put("ror_orgunit_id", "https://ror.org/03vb2cr34");
        extras.put("data-ror_orgunit_type", "Company");
        extras.put("ror_orgunit_type", "Company");
        extras.put("data-ror_orgunit_countryName", "Italia");
        extras.put("ror_orgunit_countryName", "Italia");
        extras.put("data-ror_orgunit_country", "IT");
        return extras;
    }

    private final class RorMetadatum extends MetadatumDTO {
        public RorMetadatum(String field, String value) {
            String[] split = field.split("\\.");
            setSchema(split[0]);
            setElement(split[1]);
            if (split.length > 2) {
                setQualifier(split[2]);
            }
            setValue(value);
        }
    }

}
