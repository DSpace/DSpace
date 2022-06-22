/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import static java.util.Optional.of;
import static org.dspace.app.matcher.LambdaMatcher.has;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidTokenBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkBulk;
import org.orcid.jaxb.model.v3.release.record.summary.Works;

/**
 * Integration tests for {@link OrcidPublicationDataProvider}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidPublicationDataProviderIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_XML_DIR_PATH = "org/dspace/app/orcid-works/";

    private static final String ACCESS_TOKEN = "32c83ccb-c6d5-4981-b6ea-6a34a36de8ab";

    private static final String ORCID = "0000-1111-2222-3333";

    private OrcidPublicationDataProvider dataProvider;

    private OrcidConfiguration orcidConfiguration;

    private OrcidClient orcidClient;

    private OrcidClient orcidClientMock;

    private String originalClientId;

    private Collection persons;

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

        context.restoreAuthSystemState();

        dataProvider = new DSpace().getServiceManager()
            .getServiceByName("orcidPublicationDataProvider", OrcidPublicationDataProvider.class);

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
    public void testSearchWithoutPagination() throws Exception {

        List<ExternalDataObject> externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, -1);
        assertThat(externalObjects, hasSize(3));

        ExternalDataObject firstObject = externalObjects.get(0);
        assertThat(firstObject.getDisplayValue(), is("The elements of style and the survey of ophthalmology."));
        assertThat(firstObject.getValue(), is("The elements of style and the survey of ophthalmology."));
        assertThat(firstObject.getId(), is(ORCID + "::277904"));
        assertThat(firstObject.getSource(), is("orcidWorks"));

        List<MetadataValueDTO> metadata = firstObject.getMetadata();
        assertThat(metadata, hasSize(7));
        assertThat(metadata, has(metadata("dc.date.issued", "2011")));
        assertThat(metadata, has(metadata("dc.source", "Test Journal")));
        assertThat(metadata, has(metadata("dc.language.iso", "it")));
        assertThat(metadata, has(metadata("dc.type", "Other")));
        assertThat(metadata, has(metadata("dc.identifier.doi", "10.11234.12")));
        assertThat(metadata, has(metadata("dc.contributor.author", "Walter White")));
        assertThat(metadata, has(metadata("dc.title", "The elements of style and the survey of ophthalmology.")));

        ExternalDataObject secondObject = externalObjects.get(1);
        assertThat(secondObject.getDisplayValue(), is("Another cautionary tale."));
        assertThat(secondObject.getValue(), is("Another cautionary tale."));
        assertThat(secondObject.getId(), is(ORCID + "::277902"));
        assertThat(secondObject.getSource(), is("orcidWorks"));

        metadata = secondObject.getMetadata();
        assertThat(metadata, hasSize(8));
        assertThat(metadata, has(metadata("dc.date.issued", "2011-05-01")));
        assertThat(metadata, has(metadata("dc.description.abstract", "Short description")));
        assertThat(metadata, has(metadata("dc.relation.ispartof", "Journal title")));
        assertThat(metadata, has(metadata("dc.contributor.author", "Walter White")));
        assertThat(metadata, has(metadata("dc.contributor.author", "John White")));
        assertThat(metadata, has(metadata("dc.contributor.editor", "Jesse Pinkman")));
        assertThat(metadata, has(metadata("dc.title", "Another cautionary tale.")));
        assertThat(metadata, has(metadata("dc.type", "Article")));

        ExternalDataObject thirdObject = externalObjects.get(2);
        assertThat(thirdObject.getDisplayValue(), is("Branch artery occlusion in a young woman."));
        assertThat(thirdObject.getValue(), is("Branch artery occlusion in a young woman."));
        assertThat(thirdObject.getId(), is(ORCID + "::277871"));
        assertThat(thirdObject.getSource(), is("orcidWorks"));

        metadata = thirdObject.getMetadata();
        assertThat(metadata, hasSize(3));
        assertThat(metadata, has(metadata("dc.date.issued", "1985-07-01")));
        assertThat(metadata, has(metadata("dc.title", "Branch artery occlusion in a young woman.")));
        assertThat(metadata, has(metadata("dc.type", "Article")));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).getWorks(ACCESS_TOKEN, ORCID);
        verify(orcidClientMock).getWorkBulk(ACCESS_TOKEN, ORCID, List.of("277904", "277902", "277871"));
        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testSearchWithInvalidOrcidId() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> dataProvider.searchExternalDataObjects("0000-1111-2222", 0, -1));

        assertThat(exception.getMessage(), is("The given ORCID ID is not valid: 0000-1111-2222"));

    }

    @Test
    public void testSearchWithStoredAccessToken() throws Exception {

        context.turnOffAuthorisationSystem();

        String accessToken = "95cb5ed9-c208-4bbc-bc99-aa0bd76e4452";

        Item profile = ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .withOrcidIdentifier(ORCID)
            .withDspaceObjectOwner(eperson.getEmail(), eperson.getID().toString())
            .build();

        OrcidTokenBuilder.create(context, eperson, accessToken)
            .withProfileItem(profile)
            .build();

        context.restoreAuthSystemState();

        List<ExternalDataObject> externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, -1);
        assertThat(externalObjects, hasSize(3));

        verify(orcidClientMock).getWorks(accessToken, ORCID);
        verify(orcidClientMock).getWorkBulk(accessToken, ORCID, List.of("277904", "277902", "277871"));
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testSearchWithProfileWithoutAccessToken() throws Exception {

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .withOrcidIdentifier(ORCID)
            .build();

        context.restoreAuthSystemState();

        List<ExternalDataObject> externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, -1);
        assertThat(externalObjects, hasSize(3));
        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).getWorks(ACCESS_TOKEN, ORCID);
        verify(orcidClientMock).getWorkBulk(ACCESS_TOKEN, ORCID, List.of("277904", "277902", "277871"));
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testSearchWithoutResults() throws Exception {

        String unknownOrcid = "1111-2222-3333-4444";
        when(orcidClientMock.getWorks(ACCESS_TOKEN, unknownOrcid)).thenReturn(new Works());

        List<ExternalDataObject> externalObjects = dataProvider.searchExternalDataObjects(unknownOrcid, 0, -1);
        assertThat(externalObjects, empty());

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).getWorks(ACCESS_TOKEN, unknownOrcid);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testClientCredentialsTokenCache() throws Exception {

        List<ExternalDataObject> externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, -1);
        assertThat(externalObjects, hasSize(3));

        verify(orcidClientMock).getReadPublicAccessToken();

        externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, -1);
        assertThat(externalObjects, hasSize(3));

        verify(orcidClientMock, times(1)).getReadPublicAccessToken();

        dataProvider.setReadPublicAccessToken(null);

        externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, -1);
        assertThat(externalObjects, hasSize(3));

        verify(orcidClientMock, times(2)).getReadPublicAccessToken();

    }

    @Test
    public void testSearchPagination() throws Exception {

        List<ExternalDataObject> externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, -1);
        assertThat(externalObjects, hasSize(3));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277904"))));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277902"))));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277871"))));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).getWorks(ACCESS_TOKEN, ORCID);
        verify(orcidClientMock).getWorkBulk(ACCESS_TOKEN, ORCID, List.of("277904", "277902", "277871"));

        externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, 5);
        assertThat(externalObjects, hasSize(3));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277904"))));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277902"))));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277871"))));

        verify(orcidClientMock, times(2)).getWorks(ACCESS_TOKEN, ORCID);
        verify(orcidClientMock, times(2)).getWorkBulk(ACCESS_TOKEN, ORCID, List.of("277904", "277902", "277871"));

        externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, 2);
        assertThat(externalObjects, hasSize(2));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277904"))));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277902"))));

        verify(orcidClientMock, times(3)).getWorks(ACCESS_TOKEN, ORCID);
        verify(orcidClientMock).getWorkBulk(ACCESS_TOKEN, ORCID, List.of("277904", "277902"));

        externalObjects = dataProvider.searchExternalDataObjects(ORCID, 1, 1);
        assertThat(externalObjects, hasSize(1));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277902"))));

        verify(orcidClientMock, times(4)).getWorks(ACCESS_TOKEN, ORCID);
        verify(orcidClientMock).getObject(ACCESS_TOKEN, ORCID, "277902", Work.class);

        externalObjects = dataProvider.searchExternalDataObjects(ORCID, 2, 1);
        assertThat(externalObjects, hasSize(1));
        assertThat(externalObjects, has((externalObject -> externalObject.getId().equals(ORCID + "::277871"))));

        verify(orcidClientMock, times(5)).getWorks(ACCESS_TOKEN, ORCID);
        verify(orcidClientMock).getObject(ACCESS_TOKEN, ORCID, "277871", Work.class);

        verifyNoMoreInteractions(orcidClientMock);

    }

    @Test
    public void testGetExternalDataObject() {
        Optional<ExternalDataObject> optional = dataProvider.getExternalDataObject(ORCID + "::277902");
        assertThat(optional.isPresent(), is(true));

        ExternalDataObject externalDataObject = optional.get();
        assertThat(externalDataObject.getDisplayValue(), is("Another cautionary tale."));
        assertThat(externalDataObject.getValue(), is("Another cautionary tale."));
        assertThat(externalDataObject.getId(), is(ORCID + "::277902"));
        assertThat(externalDataObject.getSource(), is("orcidWorks"));

        List<MetadataValueDTO> metadata = externalDataObject.getMetadata();
        assertThat(metadata, hasSize(8));
        assertThat(metadata, has(metadata("dc.date.issued", "2011-05-01")));
        assertThat(metadata, has(metadata("dc.description.abstract", "Short description")));
        assertThat(metadata, has(metadata("dc.relation.ispartof", "Journal title")));
        assertThat(metadata, has(metadata("dc.contributor.author", "Walter White")));
        assertThat(metadata, has(metadata("dc.contributor.author", "John White")));
        assertThat(metadata, has(metadata("dc.contributor.editor", "Jesse Pinkman")));
        assertThat(metadata, has(metadata("dc.title", "Another cautionary tale.")));
        assertThat(metadata, has(metadata("dc.type", "Article")));

        verify(orcidClientMock).getReadPublicAccessToken();
        verify(orcidClientMock).getObject(ACCESS_TOKEN, ORCID, "277902", Work.class);
        verifyNoMoreInteractions(orcidClientMock);
    }

    @Test
    public void testGetExternalDataObjectWithInvalidOrcidId() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> dataProvider.getExternalDataObject("invalid::277902"));

        assertThat(exception.getMessage(), is("The given ORCID ID is not valid: invalid" ));
    }

    @Test
    public void testGetExternalDataObjectWithInvalidId() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> dataProvider.getExternalDataObject("id"));

        assertThat(exception.getMessage(), is("Invalid identifier 'id', expected <orcid-id>::<put-code>"));
    }

    @Test
    public void testSearchWithoutApiKeysConfigured() throws Exception {

        context.turnOffAuthorisationSystem();

        orcidConfiguration.setClientSecret(null);

        ItemBuilder.createItem(context, persons)
            .withTitle("Profile")
            .withOrcidIdentifier(ORCID)
            .build();

        context.restoreAuthSystemState();

        List<ExternalDataObject> externalObjects = dataProvider.searchExternalDataObjects(ORCID, 0, -1);
        assertThat(externalObjects, hasSize(3));

        verify(orcidClientMock).getWorks(ORCID);
        verify(orcidClientMock).getWorkBulk(ORCID, List.of("277904", "277902", "277871"));
        verifyNoMoreInteractions(orcidClientMock);
    }

    private Predicate<MetadataValueDTO> metadata(String metadataField, String value) {
        MetadataFieldName metadataFieldName = new MetadataFieldName(metadataField);
        return metadata(metadataFieldName.schema, metadataFieldName.element, metadataFieldName.qualifier, value);
    }

    private Predicate<MetadataValueDTO> metadata(String schema, String element, String qualifier, String value) {
        return dto -> StringUtils.equals(schema, dto.getSchema())
            && StringUtils.equals(element, dto.getElement())
            && StringUtils.equals(qualifier, dto.getQualifier())
            && StringUtils.equals(value, dto.getValue());
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
