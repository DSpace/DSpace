/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.oai;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.util.Calendar;
import java.util.Date;

import com.lyncode.xoai.dataprovider.core.XOAIManager;
import com.lyncode.xoai.dataprovider.exceptions.ConfigurationException;
import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;
import com.lyncode.xoai.dataprovider.services.impl.BaseDateProvider;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.Configuration;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.ContextConfiguration;
import org.apache.commons.lang3.time.DateUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Community;
import org.dspace.services.ConfigurationService;
import org.dspace.xoai.services.api.EarliestDateResolver;
import org.dspace.xoai.services.api.cache.XOAICacheService;
import org.dspace.xoai.services.api.config.XOAIManagerResolver;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test to verify the /oai endpoint is responding as a valid OAI-PMH endpoint.
 * This tests that our dspace-oai module is running at this endpoint.
 * <P>
 * This is an AbstractControllerIntegrationTest because dspace-oai makes use of Controllers.
 *
 * @author Tim Donohue
 */
// Ensure the OAI SERVER IS ENABLED before any tests run.
// This annotation overrides default DSpace config settings loaded into Spring Context
@TestPropertySource(properties = {"oai.enabled = true"})
public class OAIpmhIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    // All OAI-PMH paths that we test against
    private final String ROOT_PATH = "/oai/";
    private final String DEFAULT_CONTEXT_PATH = "request";
    private final String DEFAULT_CONTEXT = ROOT_PATH + DEFAULT_CONTEXT_PATH;

    // Mock to ensure XOAI caching is disabled for all tests (see @Before method)
    @MockBean
    private XOAICacheService xoaiCacheService;

    // Spy on the current EarliestDateResolver bean, to allow us to change behavior in tests below
    @SpyBean
    private EarliestDateResolver earliestDateResolver;

    // XOAI's BaseDateProvider (used for date-based testing below)
    private static final BaseDateProvider baseDateProvider = new BaseDateProvider();

    // Spy on the current XOAIManagerResolver bean, to allow us to change behavior of XOAIManager in tests
    // See also: createMockXOAIManager() method
    @SpyBean
    private XOAIManagerResolver xoaiManagerResolver;

    // Beans required by createMockXOAIManager()
    @Autowired
    private ResourceResolver resourceResolver;
    @Autowired
    private DSpaceFilterResolver filterResolver;


    @Before
    public void onlyRunIfConfigExists() {
        // These integration tests REQUIRE that OAIWebConfig is found/available (as this class deploys OAI)
        // If this class is not available, the below "Assume" will cause all tests to be SKIPPED
        // NOTE: OAIWebConfig is provided by the 'dspace-oai' module
        try {
            Class.forName("org.dspace.app.configuration.OAIWebConfig");
        } catch (ClassNotFoundException ce) {
            Assume.assumeNoException(ce);
        }

        // Disable XOAI Caching for ALL tests
        when(xoaiCacheService.isActive()).thenReturn(false);
        when(xoaiCacheService.hasCache(anyString())).thenReturn(false);
    }

    @Test
    public void requestToRootShouldGiveListOfContextsWithBadRequestError() throws Exception {
        // Attempt to call the root endpoint
        getClient().perform(get(ROOT_PATH))
                   // Expect a 400 response code (OAI requires a context)
                   .andExpect(status().isBadRequest())
                   // Expect that a list of valid contexts is returned
                   .andExpect(model().attributeExists("contexts"))
        ;
    }

    @Test
    public void requestForUnknownContextShouldGiveListOfContextsWithBadRequestError() throws Exception {
        // Attempt to call an nonexistent OAI-PMH context
        getClient().perform(get(ROOT_PATH + "/nonexistentContext"))
                   // Expect a 400 response code (OAI requires a context)
                   .andExpect(status().isBadRequest())
                   // Expect that a list of valid contexts is returned
                   .andExpect(model().attributeExists("contexts"))
        ;
    }

    @Test
    public void requestForIdentifyWithoutRequiredConfigShouldFail() throws Exception {
        // Clear out the required "mail.admin" configuration
        configurationService.setProperty("mail.admin", null);

        // Attempt to make an Identify request to root context
        getClient().perform(get(DEFAULT_CONTEXT).param("verb", "Identify"))
                   // Expect a 500 response code (mail.admin MUST be set)
                   .andExpect(status().isInternalServerError())
        ;
    }

    @Test
    public void requestForIdentifyShouldReturnTheConfiguredValues() throws Exception {

        // Get current date/time and store as "now", then round to nearest second (as OAI-PMH ignores milliseconds)
        Date now = new Date();
        Date nowToNearestSecond = DateUtils.round(now, Calendar.SECOND);
        // Return "nowToNearestSecond" when "getEarliestDate()" is called for currently loaded EarliestDateResolver bean
        doReturn(nowToNearestSecond).when(earliestDateResolver).getEarliestDate(any());

        // Attempt to make an Identify request to root context
        getClient().perform(get(DEFAULT_CONTEXT).param("verb", "Identify"))
                   // Expect a 200 response code
                   .andExpect(status().isOk())
                   // Expect the content type to be "text/xml;charset=UTF-8"
                   .andExpect(content().contentType("text/xml;charset=UTF-8"))
                   // Expect <scheme>oai</scheme>
                   .andExpect(xpath("OAI-PMH/Identify/description/oai-identifier/scheme").string("oai"))
                   // Expect protocol version 2.0
                   .andExpect(xpath("OAI-PMH/Identify/protocolVersion").string("2.0"))
                   // Expect repositoryName to be the same as "dspace.name" config
                   .andExpect(xpath("OAI-PMH/Identify/repositoryName")
                                  .string(configurationService.getProperty("dspace.name")))
                   // Expect adminEmail to be the same as "mail.admin" config
                   .andExpect(xpath("OAI-PMH/Identify/adminEmail")
                                  .string(configurationService.getProperty("mail.admin")))
                   // Expect baseURL to be the same as our "oai.url" with the DEFAULT_CONTEXT_PATH appended
                   .andExpect(xpath("OAI-PMH/Identify/baseURL")
                                  .string(configurationService.getProperty("oai.url") + "/" + DEFAULT_CONTEXT_PATH))
                   // Expect earliestDatestamp to be "now", i.e. current date, (as mocked above)
                   .andExpect(xpath("OAI-PMH/Identify/earliestDatestamp")
                                  .string(baseDateProvider.format(nowToNearestSecond)))
        ;
    }

    @Test
    public void listSetsWithLessSetsThenMaxSetsPerPage() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        // Create a Community & a Collection
        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                                    .withName("Parent Community")
                                                    .build();
        CollectionBuilder.createCollection(context, parentCommunity)
                         .withName("Child Collection")
                         .build();
        context.restoreAuthSystemState();

        // Call ListSets verb, and verify both Collection & Community are listed as sets
        getClient().perform(get(DEFAULT_CONTEXT).param("verb", "ListSets"))
                   // Expect 200 response, with valid response date and verb=ListSets
                   .andExpect(status().isOk())
                   .andExpect(xpath("OAI-PMH/responseDate").exists())
                   .andExpect(xpath("OAI-PMH/request/@verb").string("ListSets"))
                   // Expect two Sets to be returned
                   .andExpect(xpath("//set").nodeCount(2))
                   // First setSpec should start with "com_" (Community)
                   .andExpect(xpath("(//set/setSpec)[1]").string(startsWith("com_")))
                   // First set name should be Community name
                   .andExpect(xpath("(//set/setName)[1]").string("Parent Community"))
                   // Second setSpec should start with "col_" (Collection)
                   .andExpect(xpath("(//set/setSpec)[2]").string(startsWith("col_")))
                   // Second set name should be Collection name
                   .andExpect(xpath("(//set/setName)[2]").string("Child Collection"))
                   // No resumption token should be returned
                   .andExpect(xpath("//resumptionToken").doesNotExist())
        ;
    }

    @Test
    public void listSetsWithMoreSetsThenMaxSetsPerPage() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        // Create 3 Communities (1 as a subcommunity) & 2 Collections
        Community firstCommunity = CommunityBuilder.createCommunity(context)
                                                   .withName("First Community")
                                                   .build();
        Community secondCommunity = CommunityBuilder.createSubCommunity(context, firstCommunity)
                                                    .withName("Second Community")
                                                    .build();
        CommunityBuilder.createCommunity(context)
                        .withName("Third Community")
                        .build();
        CollectionBuilder.createCollection(context, firstCommunity)
                         .withName("First Collection")
                         .build();
        CollectionBuilder.createCollection(context, secondCommunity)
                         .withName("Second Collection")
                         .build();
        context.restoreAuthSystemState();


        // Create a custom XOAI configuration, with maxListSetsSize = 3 for DEFAULT_CONTEXT requests
        // (This limits the number of sets returned in a single response)
        Configuration xoaiConfig =
            new Configuration().withMaxListSetsSize(3)
                               .withContextConfigurations(new ContextConfiguration(DEFAULT_CONTEXT_PATH));
        // When xoaiManagerResolver.getManager() is called, return a MockXOAIManager based on the above configuration
        doReturn(createMockXOAIManager(xoaiConfig)).when(xoaiManagerResolver).getManager();

        // Call ListSets verb, and verify all 5 Collections/Communities are listed as sets
        getClient().perform(get(DEFAULT_CONTEXT).param("verb", "ListSets"))
                   // Expect 200 response, with valid response date and verb=ListSets
                   .andExpect(status().isOk())
                   .andExpect(xpath("OAI-PMH/responseDate").exists())
                   .andExpect(xpath("OAI-PMH/request/@verb").string("ListSets"))
                   // Expect ONLY 3 (of 5) Sets to be returned
                   .andExpect(xpath("//set").nodeCount(3))
                   // Expect resumption token to exist and be equal to "////3"
                   .andExpect(xpath("//resumptionToken").string("////3"))
                   // Expect resumption token to have completeListSize=5
                   .andExpect(xpath("//resumptionToken/@completeListSize").number(Double.valueOf(5)))
        ;

        // Call ListSets verb, and verify all 5 Collections/Communities are listed as sets
        getClient().perform(get(DEFAULT_CONTEXT).param("verb", "ListSets"))
                   // Expect 200 response, with valid response date and verb=ListSets
                   .andExpect(status().isOk())
                   .andExpect(xpath("OAI-PMH/responseDate").exists())
                   .andExpect(xpath("OAI-PMH/request/@verb").string("ListSets"))
                   // Expect ONLY 3 (of 5) Sets to be returned
                   .andExpect(xpath("//set").nodeCount(3))
                   // Expect resumption token to exist and be equal to "////3"
                   .andExpect(xpath("//resumptionToken").string("////3"))
                   // Expect resumption token to have completeListSize=5
                   .andExpect(xpath("//resumptionToken/@completeListSize").number(Double.valueOf(5)))
        ;
    }

    /**
     * Create a fake/mock XOAIManager class based on the given xoaiConfig. May be used by above tests
     * to provide custom configurations to XOAI (overriding defaults in xoai.xml)
     * @param xoaiConfig XOAI Configuration
     * @return new XOAIManager initialized with the given Configuration
     * @throws ConfigurationException
     */
    private XOAIManager createMockXOAIManager(Configuration xoaiConfig) throws ConfigurationException {
        return new XOAIManager(filterResolver, resourceResolver, xoaiConfig);
    }
}
