/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;


import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.IteratorUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.HarvestedCollectionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.model.OAIHarvesterResponseDTO;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.harvest.service.HarvestedItemService;
import org.dspace.harvest.service.OAIHarvesterClient;
import org.dspace.harvest.util.NamespaceUtils;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link OAIHarvester}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_URL = "https://www.test-harvest.it";

    private static final String OAI_PMH_DIR_PATH = "./target/testing/dspace/assetstore/oai-pmh/";


    private OAIHarvester harvester = HarvestServiceFactory.getInstance().getOAIHarvester();

    private HarvestedCollectionService harvestedCollectionService = HarvestServiceFactory.getInstance()
        .getHarvestedCollectionService();

    private HarvestedItemService harvestedItemService = HarvestServiceFactory.getInstance().getHarvestedItemService();

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private SAXBuilder builder = new SAXBuilder();


    private Community community;

    private Collection collection;

    private OAIHarvesterClient oaiHarvesterClient;

    private OAIHarvesterClient mockClient;


    @Before
    public void beforeTests() throws Exception {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

        oaiHarvesterClient = harvester.getOaiHarvesterClient();
        mockClient = mock(OAIHarvesterClient.class);
        harvester.setOaiHarvesterClient(mockClient);

        String metadataURI = NamespaceUtils.getDMDNamespace("cerif").getURI();

        when(mockClient.resolveNamespaceToPrefix(BASE_URL, metadataURI)).thenReturn("oai_cerif_openaire");
        when(mockClient.identify(BASE_URL)).thenReturn(buildResponse("test-identify.xml"));
    }

    @After
    public void afterTests() {
        harvester.setOaiHarvesterClient(oaiHarvesterClient);
    }

    @Test
    public void testRunHarvest() throws Exception {

        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("many-publications.xml"));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow);

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, NamespaceUtils.getDMDNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, hasSize(3));

        harvestRow = harvestedCollectionService.find(context, collection);
        assertThat(harvestRow.getHarvestStatus(), equalTo(HarvestedCollection.STATUS_READY));
        assertThat(harvestRow.getHarvestStartTime(), notNullValue());
        assertThat(harvestRow.getLastHarvestDate(), notNullValue());
        assertThat(harvestRow.getHarvestMessage(), equalTo("Imported 3 records with success"));

        Item item = getItemViaHarvestedItem("oai:test-harvest:Publications/c3ae30ae-ddc4-4c25-b0b8-c87a3f850bca");
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("The International Journal of Digital Curation"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"),
            equalTo("test-harvest::c3ae30ae-ddc4-4c25-b0b8-c87a3f850bca"));

        item = getItemViaHarvestedItem("oai:test-harvest:Publications/123456789/6");
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Metadata and Semantics Research"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::123456789/6"));

        item = getItemViaHarvestedItem("oai:test-harvest:Publications/123456789/7");
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("TEST"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::123456789/7"));

    }

    @Test
    public void testRunHarvestWithOneImportFailure() throws Exception {

        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("many-publications-with-one-corrupted.xml"));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow);

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, NamespaceUtils.getDMDNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, hasSize(2));

        harvestRow = harvestedCollectionService.find(context, collection);
        assertThat(harvestRow.getHarvestStatus(), equalTo(HarvestedCollection.STATUS_RETRY));
        assertThat(harvestRow.getHarvestStartTime(), notNullValue());
        assertThat(harvestRow.getLastHarvestDate(), nullValue());
        assertThat(harvestRow.getHarvestMessage(),
            equalTo("Imported 2 records with success - Record import failures: 1"));

        Item item = getItemViaHarvestedItem("oai:test-harvest:Publications/c3ae30ae-ddc4-4c25-b0b8-c87a3f850bca");
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("The International Journal of Digital Curation"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"),
            equalTo("test-harvest::c3ae30ae-ddc4-4c25-b0b8-c87a3f850bca"));

        item = getItemViaHarvestedItem("oai:test-harvest:Publications/123456789/7");
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("TEST"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::123456789/7"));

        assertThat(harvestedItemService.findByOAIId(context, "oai:test-harvest:Publications/123456789/6", collection),
            nullValue());

    }

    @Test
    public void testRunHarvestWithResumptionToken() throws Exception {

        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponseWithResumptionToken("publications-with-resumption-token.xml", "token"));
        when(mockClient.listRecords(BASE_URL, "token")).thenReturn(buildResponse("single-publication.xml"));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow);

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, NamespaceUtils.getDMDNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verify(mockClient).listRecords(BASE_URL, "token");
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, hasSize(3));

        harvestRow = harvestedCollectionService.find(context, collection);
        assertThat(harvestRow.getHarvestStatus(), equalTo(HarvestedCollection.STATUS_READY));
        assertThat(harvestRow.getHarvestStartTime(), notNullValue());
        assertThat(harvestRow.getLastHarvestDate(), notNullValue());
        assertThat(harvestRow.getHarvestMessage(), equalTo("Imported 3 records with success"));

        Item item = getItemViaHarvestedItem("oai:test-harvest:Publications/1");
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("First Publication"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::1"));

        item = getItemViaHarvestedItem("oai:test-harvest:Publications/2");
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Second Publication"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::2"));

        item = getItemViaHarvestedItem("oai:test-harvest:Publications/3");
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Test Publication"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::3"));
    }

    @Test
    public void testRunHarvestWithNoRecordsMatch() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponseWithErrors("no-records-match.xml", Set.of("noRecordsMatch")));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow);

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, NamespaceUtils.getDMDNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, emptyCollectionOf(Item.class));

        harvestRow = harvestedCollectionService.find(context, collection);
        assertThat(harvestRow.getHarvestStatus(), equalTo(HarvestedCollection.STATUS_READY));
        assertThat(harvestRow.getHarvestStartTime(), notNullValue());
        assertThat(harvestRow.getLastHarvestDate(), notNullValue());
        assertThat(harvestRow.getHarvestMessage(), equalTo("noRecordsMatch: OAI server did not contain any updates"));

    }

    @Test
    public void testRunHarvestWithErrors() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponseWithErrors("response-with-errors.xml", Set.of("errorCode1, errorCode2")));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow);

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, NamespaceUtils.getDMDNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, emptyCollectionOf(Item.class));

        harvestRow = harvestedCollectionService.find(context, collection);
        assertThat(harvestRow.getHarvestStatus(), equalTo(HarvestedCollection.STATUS_UNKNOWN_ERROR));
        assertThat(harvestRow.getHarvestStartTime(), notNullValue());
        assertThat(harvestRow.getLastHarvestDate(), nullValue());
        assertThat(harvestRow.getHarvestMessage(),
            equalTo("OAI server response contains the following error codes: [errorCode1, errorCode2]"));

    }

    private Item getItemViaHarvestedItem(String oaiId) throws SQLException {

        HarvestedItem harvestedItem = harvestedItemService.findByOAIId(context, oaiId, collection);
        assertThat(harvestedItem, notNullValue());
        assertThat(harvestedItem.getHarvestDate(), notNullValue());

        Item item = harvestedItem.getItem();
        assertThat(item, notNullValue());

        return item;
    }

    private String getFirstMetadataValue(Item item, String metadataField) {
        return itemService.getMetadata(item, metadataField);
    }

    private OAIHarvesterResponseDTO buildResponse(String documentPath) {
        return buildResponse(documentPath, null, Collections.emptySet());
    }

    private OAIHarvesterResponseDTO buildResponseWithErrors(String documentPath, Set<String> errors) {
        return buildResponse(documentPath, null, errors);
    }

    private OAIHarvesterResponseDTO buildResponseWithResumptionToken(String documentPath, String resumptionToken) {
        return buildResponse(documentPath, resumptionToken, Collections.emptySet());
    }

    private OAIHarvesterResponseDTO buildResponse(String documentPath, String resumptionToken, Set<String> errors) {
        Document document = readDocument(OAI_PMH_DIR_PATH, documentPath);
        return new OAIHarvesterResponseDTO(document, resumptionToken, errors);
    }

    private Document readDocument(String dir, String name) {
        try (InputStream inputStream = new FileInputStream(new File(dir, name))) {
            return builder.build(inputStream);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
