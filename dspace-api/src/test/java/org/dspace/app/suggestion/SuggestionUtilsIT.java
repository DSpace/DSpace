/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import static java.util.Optional.of;
import static org.dspace.app.suggestion.SuggestionUtils.getFirstEntryByMetadatum;
import static org.dspace.orcid.model.OrcidProfileSectionType.EXTERNAL_IDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.collections.CollectionUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.external.factory.ExternalServiceFactory;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.provider.impl.OrcidPublicationDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.dspace.kernel.ServiceManager;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.factory.OrcidServiceFactory;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.dspace.orcid.service.OrcidProfileSectionFactoryService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkBulk;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for suggestion utilities @see SuggestionUtils
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 */
public class SuggestionUtilsIT extends AbstractIntegrationTestWithDatabase  {

    private static ConfigurationService cfg;
    private static final String ORCID = "0000-1111-2222-3333";
    private static final String ACCESS_TOKEN = "32c83ccb-c6d5-4981-b6ea-6a34a36de8ab";
    private static final String BASE_XML_DIR_PATH = "org/dspace/app/orcid-works/";
    private OrcidPublicationDataProvider dataProvider;
    private SolrSuggestionProvider solrSuggestionProvider;
    private OrcidProfileSectionFactoryService profileSectionFactoryService;
    private ItemService itemService;
    private Collection collection;
    private ExternalDataProvider primaryProvider;
    private Collection persons;
    private OrcidConfiguration orcidConfiguration;
    private OrcidClient orcidClientMock;
    private OrcidClient orcidClient;
    private String originalClientId;

    @Autowired
    private SuggestionService suggestionService;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        persons = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .withName("Profiles")
            .build();

        profileSectionFactoryService = OrcidServiceFactory.getInstance().getOrcidProfileSectionFactoryService();
        itemService = ContentServiceFactory.getInstance().getItemService();

        context.restoreAuthSystemState();

        cfg = DSpaceServicesFactory.getInstance().getConfigurationService();

        ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        HashMap<String,SuggestionProvider> providers = serviceManager.getServiceByName("suggestionProviders",
            HashMap.class);
        solrSuggestionProvider = (SolrSuggestionProvider) providers.get("scopus");
        dataProvider = new DSpace().getServiceManager()
            .getServiceByName("orcidPublicationDataProvider", OrcidPublicationDataProvider.class);
        ExternalDataService externalDataService = ExternalServiceFactory.getInstance().getExternalDataService();
        primaryProvider = externalDataService.getExternalDataProvider("openaireFunding");

        orcidConfiguration = new DSpace().getServiceManager()
            .getServiceByName("org.dspace.orcid.client.OrcidConfiguration", OrcidConfiguration.class);

        orcidClientMock = mock(OrcidClient.class);
        orcidClient = dataProvider.getOrcidClient();

        dataProvider.setReadPublicAccessToken(null);
        dataProvider.setOrcidClient(orcidClientMock);

        originalClientId = orcidConfiguration.getClientId();
        orcidConfiguration.setClientId("DSPACE-CLIENT-ID");
        orcidConfiguration.setClientSecret("DSPACE-CLIENT-SECRET");

        when(orcidClientMock.getReadPublicAccessToken()).thenReturn(buildTokenResponse(ACCESS_TOKEN));

        when(orcidClientMock.getWorks(any(), eq(ORCID))).thenReturn(unmarshall("works.xml", Works.class));
        when(orcidClientMock.getWorks(eq(ORCID))).thenReturn(unmarshall("works.xml", Works.class));

        when(orcidClientMock.getObject(any(), eq(ORCID), any(), eq(Work.class)))
            .then((invocation) -> of(unmarshall("work-" + invocation.getArgument(2) + ".xml", Work.class)));
        when(orcidClientMock.getObject(eq(ORCID), any(), eq(Work.class)))
            .then((invocation) -> of(unmarshall("work-" + invocation.getArgument(1) + ".xml", Work.class)));

        when(orcidClientMock.getWorkBulk(any(), eq(ORCID), any()))
            .then((invocation) -> unmarshallWorkBulk(invocation.getArgument(2)));
        when(orcidClientMock.getWorkBulk(eq(ORCID), any()))
            .then((invocation) -> unmarshallWorkBulk(invocation.getArgument(1)));
    }

    @After
    public void after() {
        dataProvider.setOrcidClient(orcidClient);
        orcidConfiguration.setClientId(originalClientId);
    }

    @Test
    public void testGetAllEntriesByMetadatum() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, persons)
            .withTitle("Test profile")
            .withScopusAuthorIdentifier("SCOPUS-123456")
            .withResearcherIdentifier("R-ID-01")
            .build();
        context.restoreAuthSystemState();

        List<MetadataValue> values = List.of(getMetadata(item, "person.identifier.scopus-author-id", 0));

        Object firstOrcidObject = profileSectionFactoryService.createOrcidObject(context, values, EXTERNAL_IDS);
        Optional<ExternalDataObject> optional = dataProvider.getExternalDataObject(ORCID + "::277902");

        ExternalDataObject externalDataObject = optional.get();
        String openAireId = externalDataObject.getId();
        Suggestion suggestion = new Suggestion(solrSuggestionProvider.getSourceName(), item, openAireId);
        suggestion.getMetadata().add(
                new MetadataValueDTO("dc", "title", null, null, "dcTitle"));
        suggestion.setDisplay(getFirstEntryByMetadatum(externalDataObject, "dc", "title", null));
        suggestion.getMetadata().add(new MetadataValueDTO("dc", "date", "issued", null, new Date().toString()));
        suggestion.getMetadata().add(new MetadataValueDTO("dc", "description", "abstract", null, "description"));
        suggestion.setExternalSourceUri(cfg.getProperty("dspace.server.url")
                + "/api/integration/externalsources/" + primaryProvider.getSourceIdentifier() + "/entryValues/"
                + openAireId);
        List<String> result = SuggestionUtils.getAllEntriesByMetadatum(externalDataObject, "dc", "title", null);

        assertTrue(result != null && !result.isEmpty());

        assertTrue(CollectionUtils.isEqualCollection(
            SuggestionUtils.getAllEntriesByMetadatum(externalDataObject, "dc.title"),
            result));

        String firstResult = SuggestionUtils.getFirstEntryByMetadatum(externalDataObject, "dc", "title", null);
        assertTrue("Another cautionary tale.".equalsIgnoreCase(firstResult));
        firstResult = SuggestionUtils.getFirstEntryByMetadatum(externalDataObject, "dc.title");
        assertTrue("Another cautionary tale.".equalsIgnoreCase(firstResult));
    }

    private MetadataValue getMetadata(Item item, String metadataField, int place) {
        List<MetadataValue> values = itemService.getMetadataByMetadataString(item, metadataField);
        assertThat(values.size(), greaterThan(place));
        return values.get(place);
    }

    private OrcidTokenResponseDTO buildTokenResponse(String accessToken) {
        OrcidTokenResponseDTO response = new OrcidTokenResponseDTO();
        response.setAccessToken(accessToken);
        return response;
    }

    private WorkBulk unmarshallWorkBulk(List<String> putCodes) throws Exception {
        return unmarshall("workBulk-" + String.join("-", putCodes) + ".xml", WorkBulk.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T unmarshall(String fileName, Class<T> clazz) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        URL resource = getClass().getClassLoader().getResource(BASE_XML_DIR_PATH + fileName);
        if (resource == null) {
            throw new IllegalStateException("No resource found named " + BASE_XML_DIR_PATH + fileName);
        }
        return (T) unmarshaller.unmarshal(new File(resource.getFile()));
    }
}
