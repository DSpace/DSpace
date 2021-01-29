/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static java.util.UUID.randomUUID;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.dspace.harvest.util.NamespaceUtils.getMetadataFormatNamespace;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.IteratorUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.HarvestedCollectionBuilder;
import org.dspace.builder.HarvestedItemBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.model.OAIHarvesterOptions;
import org.dspace.harvest.model.OAIHarvesterReport;
import org.dspace.harvest.model.OAIHarvesterReport.ErrorDetails;
import org.dspace.harvest.model.OAIHarvesterResponseDTO;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.harvest.service.HarvestedItemService;
import org.dspace.harvest.service.OAIHarvesterClient;
import org.dspace.harvest.service.OAIHarvesterEmailSender;
import org.dspace.harvest.util.NamespaceUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.UUIDUtils;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Integration tests for {@link OAIHarvester}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_URL = "https://www.test-harvest.it";

    private static final String OAI_PMH_DIR_PATH = "./target/testing/dspace/assetstore/oai-pmh/";
    private static final String VALIDATION_DIR = OAI_PMH_DIR_PATH + "cerif/validation/";
    private static final String CERIF_XSD_NAME = "openaire-cerif-profile.xsd";

    private OAIHarvester harvester = HarvestServiceFactory.getInstance().getOAIHarvester();

    private HarvestedCollectionService harvestedCollectionService = HarvestServiceFactory.getInstance()
        .getHarvestedCollectionService();

    private HarvestedItemService harvestedItemService = HarvestServiceFactory.getInstance().getHarvestedItemService();

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private XmlWorkflowItemService workflowItemService = XmlWorkflowServiceFactory.getInstance()
        .getXmlWorkflowItemService();

    private PoolTaskService poolTaskService = XmlWorkflowServiceFactory.getInstance().getPoolTaskService();

    private SAXBuilder builder = new SAXBuilder();

    private Community community;

    private Collection collection;

    private OAIHarvesterClient oaiHarvesterClient;

    private OAIHarvesterClient mockClient;

    @Before
    public void beforeTests() throws Exception {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community)
            .withRelationshipType("Publication")
            .withAdminGroup(eperson)
            .build();
        context.restoreAuthSystemState();

        oaiHarvesterClient = harvester.getOaiHarvesterClient();
        mockClient = mock(OAIHarvesterClient.class);
        harvester.setOaiHarvesterClient(mockClient);

        String metadataURI = NamespaceUtils.getMetadataFormatNamespace("cerif").getURI();

        when(mockClient.resolveNamespaceToPrefix(BASE_URL, metadataURI)).thenReturn("oai_cerif_openaire");
        when(mockClient.identify(BASE_URL)).thenReturn(buildResponse("test-identify.xml"));

        configurationService.setProperty("oai.harvester.tranformation-dir", OAI_PMH_DIR_PATH + "cerif");
        configurationService.setProperty("oai.harvester.validation-dir", VALIDATION_DIR);
        configurationService.setProperty("oai.harvester.validation.cerif.xsd", CERIF_XSD_NAME);
    }

    @After
    public void afterTests() throws SQLException {
        harvester.setOaiHarvesterClient(oaiHarvesterClient);
        poolTaskService.findAll(context).forEach(this::deletePoolTask);
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

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
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

        Item item = findItemByOaiID("oai:test-harvest:Publications/c3ae30ae-ddc4-4c25-b0b8-c87a3f850bca", collection);
        assertThat(item.isArchived(), equalTo(true));
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("The International Journal of Digital Curation"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"),
            equalTo("test-harvest::c3ae30ae-ddc4-4c25-b0b8-c87a3f850bca"));

        item = findItemByOaiID("oai:test-harvest:Publications/123456789/6", collection);
        assertThat(item.isArchived(), equalTo(true));
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Metadata and Semantics Research"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::123456789/6"));

        item = findItemByOaiID("oai:test-harvest:Publications/123456789/7", collection);
        assertThat(item.isArchived(), equalTo(true));
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

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
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

        Item item = findItemByOaiID("oai:test-harvest:Publications/c3ae30ae-ddc4-4c25-b0b8-c87a3f850bca", collection);
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("The International Journal of Digital Curation"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"),
            equalTo("test-harvest::c3ae30ae-ddc4-4c25-b0b8-c87a3f850bca"));

        item = findItemByOaiID("oai:test-harvest:Publications/123456789/7", collection);
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("TEST"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::123456789/7"));

        assertThat(
            harvestedItemService.findByOAIId(context, "oai:test-harvest:Publications/123456789/6",
                collection),
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

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
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

        Item item = findItemByOaiID("oai:test-harvest:Publications/1", collection);
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("First Publication"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::1"));

        item = findItemByOaiID("oai:test-harvest:Publications/2", collection);
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Second Publication"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::2"));

        item = findItemByOaiID("oai:test-harvest:Publications/3", collection);
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

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
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

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, emptyCollectionOf(Item.class));

        harvestRow = harvestedCollectionService.find(context, collection);
        assertThat(harvestRow.getHarvestStatus(), equalTo(HarvestedCollection.STATUS_RETRY));
        assertThat(harvestRow.getHarvestStartTime(), notNullValue());
        assertThat(harvestRow.getLastHarvestDate(), nullValue());
        assertThat(harvestRow.getHarvestMessage(), equalTo("Not recoverable error occurs: "
            + "OAI server response contains the following error codes: [errorCode1, errorCode2]"));

    }

    @Test
    public void testRunHarvestWithUnexpectedError() throws Exception {

        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenThrow(new RuntimeException("GENERIC ERROR"));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, emptyCollectionOf(Item.class));

        harvestRow = harvestedCollectionService.find(context, collection);
        assertThat(harvestRow.getHarvestStatus(), equalTo(HarvestedCollection.STATUS_RETRY));
        assertThat(harvestRow.getHarvestStartTime(), notNullValue());
        assertThat(harvestRow.getLastHarvestDate(), nullValue());
        assertThat(harvestRow.getHarvestMessage(), equalTo("Not recoverable error occurs: GENERIC ERROR"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestWithUpdate() throws Exception {

        when(mockClient.listRecords(eq(BASE_URL), isNotNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("update-publication.xml"));

        context.turnOffAuthorisationSystem();

        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .withLastHarvested(new Date())
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Old title")
            .withIssueDate("2020-11-29")
            .build();

        HarvestedItemBuilder.create(context, item, "oai:test-harvest:Publications/3").build();

        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNotNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, hasSize(1));

        Item updatedItem = findItemByOaiID("oai:test-harvest:Publications/3", collection);
        assertThat(updatedItem.getID(), equalTo(item.getID()));

        List<MetadataValue> values = updatedItem.getMetadata();
        assertThat(values, hasSize(16));
        assertThat(values, hasItems(with("dc.title", "Test Publication Updated")));
        assertThat(values, hasItems(with("dc.type", "Controlled Vocabulary for Resource Type Genres::text")));
        assertThat(values, hasItems(with("dc.date.issued", "2012-11-30")));
        assertThat(values, hasItems(with("oaire.citation.volume", "500")));
        assertThat(values, hasItems(with("oaire.citation.issue", "200")));
        assertThat(values, hasItems(with("oaire.citation.startPage", "200")));
        assertThat(values, hasItems(with("oaire.citation.endPage", "250")));
        assertThat(values, hasItems(with("dc.identifier.doi", "10.1007/978-3-642-35233-1_18")));
        assertThat(values, hasItems(with("dc.contributor.author", "Manghi, Paolo", null,
            "will be generated::test-harvest::123", 0, 500)));
        assertThat(values, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(values, hasItems(with("cris.sourceId", "test-harvest::3")));
        assertThat(values, hasItems(with("relationship.type", "Publication")));
    }

    @Test
    public void testRunHarvestWithDeletion() throws Exception {

        when(mockClient.listRecords(eq(BASE_URL), isNotNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("delete-publication.xml"));

        context.turnOffAuthorisationSystem();

        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .withLastHarvested(new Date())
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Publication title")
            .withIssueDate("2020-11-29")
            .build();

        HarvestedItemBuilder.create(context, item, "oai:test-harvest:Publications/3").build();

        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNotNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, emptyCollectionOf(Item.class));

        assertThat(harvestedItemService.findByOAIId(context, "oai:test-harvest:Publications/3", collection),
            nullValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestDoesNotUpdateWithoutForcingSynchronization() throws Exception {

        when(mockClient.listRecords(eq(BASE_URL), isNotNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("update-publication.xml"));

        context.turnOffAuthorisationSystem();

        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .withLastHarvested(new Date())
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Publication title")
            .withIssueDate("2020-11-29")
            .build();

        HarvestedItemBuilder.create(context, item, "oai:test-harvest:Publications/3")
            .withHarvestDate(new SimpleDateFormat("yyyy-MM-dd").parse("2101-01-01"))
            .build();

        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNotNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, hasSize(1));

        Item updatedItem = findItemByOaiID("oai:test-harvest:Publications/3", collection);
        assertThat(updatedItem.getID(), equalTo(item.getID()));

        List<MetadataValue> values = updatedItem.getMetadata();
        assertThat(values, hasSize(7));

        assertThat(values, hasItems(with("dc.title", "Publication title")));
        assertThat(values, hasItems(with("dc.date.issued", "2020-11-29")));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestUpdateWithForcingSynchronization() throws Exception {

        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("update-publication.xml"));

        context.turnOffAuthorisationSystem();

        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .withLastHarvested(new Date())
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Publication title")
            .withIssueDate("2020-11-29")
            .build();

        HarvestedItemBuilder.create(context, item, "oai:test-harvest:Publications/3")
            .withHarvestDate(new SimpleDateFormat("yyyy-MM-dd").parse("2101-01-01"))
            .build();

        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getOptionsWithForceSynchronization());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, hasSize(1));

        Item updatedItem = findItemByOaiID("oai:test-harvest:Publications/3", collection);
        assertThat(updatedItem.getID(), equalTo(item.getID()));

        List<MetadataValue> values = updatedItem.getMetadata();
        assertThat(values, hasSize(16));

        assertThat(values, hasItems(with("dc.title", "Test Publication Updated")));
        assertThat(values, hasItems(with("dc.date.issued", "2012-11-30")));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestWithUpdateSearchingByCrisSourceId() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNotNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("update-publication.xml"));

        context.turnOffAuthorisationSystem();

        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .withLastHarvested(new Date())
            .build();

        Item item = ItemBuilder.createItem(context, collection).withCrisSourceId("test-harvest::3").build();

        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, hasSize(1));

        Item updatedItem = findItemByOaiID("oai:test-harvest:Publications/3", collection);
        assertThat(updatedItem.getID(), equalTo(item.getID()));

        List<MetadataValue> values = updatedItem.getMetadata();
        assertThat(values, hasSize(16));
        assertThat(values, hasItems(with("dc.title", "Test Publication Updated")));
        assertThat(values, hasItems(with("dc.type", "Controlled Vocabulary for Resource Type Genres::text")));
        assertThat(values, hasItems(with("dc.date.issued", "2012-11-30")));
        assertThat(values, hasItems(with("oaire.citation.volume", "500")));
        assertThat(values, hasItems(with("oaire.citation.issue", "200")));
        assertThat(values, hasItems(with("oaire.citation.startPage", "200")));
        assertThat(values, hasItems(with("oaire.citation.endPage", "250")));
        assertThat(values, hasItems(with("dc.identifier.doi", "10.1007/978-3-642-35233-1_18")));
        assertThat(values, hasItems(with("dc.contributor.author", "Manghi, Paolo", null,
            "will be generated::test-harvest::123", 0, 500)));
        assertThat(values, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(values, hasItems(with("cris.sourceId", "test-harvest::3")));
        assertThat(values, hasItems(with("relationship.type", "Publication")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestWithCreationAndUpdating() throws Exception {

        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("single-publication.xml"));

        when(mockClient.listRecords(eq(BASE_URL), isNotNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("update-publication.xml"));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        // create the item
        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, hasSize(1));

        Item createdItem = items.get(0);

        List<MetadataValue> values = createdItem.getMetadata();
        assertThat(values, hasSize(17));
        assertThat(values, hasItems(with("dc.title", "Test Publication")));
        assertThat(values, hasItems(with("dc.type", "Controlled Vocabulary for Resource Type Genres::text")));
        assertThat(values, hasItems(with("dc.date.issued", "2012-11-30")));
        assertThat(values, hasItems(with("oaire.citation.volume", "343")));
        assertThat(values, hasItems(with("oaire.citation.issue", "168")));
        assertThat(values, hasItems(with("oaire.citation.startPage", "168")));
        assertThat(values, hasItems(with("oaire.citation.endPage", "180")));
        assertThat(values, hasItems(with("dc.identifier.doi", "10.1007/978-3-642-35233-1_18")));
        assertThat(values, hasItems(with("dc.contributor.author", "Manghi, Paolo", null,
            "will be generated::test-harvest::123", 0, 500)));
        assertThat(values, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(values, hasItems(with("cris.sourceId", "test-harvest::3")));
        assertThat(values, hasItems(with("relationship.type", "Publication")));

        harvestRow = harvestedCollectionService.find(context, collection);
        assertThat(harvestRow.getHarvestStatus(), equalTo(HarvestedCollection.STATUS_READY));
        assertThat(harvestRow.getHarvestStartTime(), notNullValue());
        assertThat(harvestRow.getLastHarvestDate(), notNullValue());
        assertThat(harvestRow.getHarvestMessage(), equalTo("Imported 1 records with success"));

        // update the item
        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, hasSize(1));

        Item updatedItem = items.get(0);
        assertThat(updatedItem.getID(), equalTo(createdItem.getID()));

        values = updatedItem.getMetadata();
        assertThat(values, hasSize(17));
        assertThat(values, hasItems(with("dc.title", "Test Publication Updated")));
        assertThat(values, hasItems(with("dc.type", "Controlled Vocabulary for Resource Type Genres::text")));
        assertThat(values, hasItems(with("dc.date.issued", "2012-11-30")));
        assertThat(values, hasItems(with("oaire.citation.volume", "500")));
        assertThat(values, hasItems(with("oaire.citation.issue", "200")));
        assertThat(values, hasItems(with("oaire.citation.startPage", "200")));
        assertThat(values, hasItems(with("oaire.citation.endPage", "250")));
        assertThat(values, hasItems(with("dc.identifier.doi", "10.1007/978-3-642-35233-1_18")));
        assertThat(values, hasItems(with("dc.contributor.author", "Manghi, Paolo", null,
            "will be generated::test-harvest::123", 0, 500)));
        assertThat(values, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(values, hasItems(with("cris.sourceId", "test-harvest::3")));
        assertThat(values, hasItems(with("relationship.type", "Publication")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestWithPublicationAndThenPerson() throws Exception {
        try {
            context.setDispatcher("cris-default");
            when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
                .thenReturn(buildResponse("single-publication.xml"));

            when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("persons"), eq("oai_cerif_openaire")))
                .thenReturn(buildResponse("single-person.xml"));

            context.turnOffAuthorisationSystem();

            Collection personCollection = createCollection(context, community)
                .withRelationshipType("Person")
                .withAdminGroup(eperson)
                .build();

            HarvestedCollection publicationHarvest = HarvestedCollectionBuilder.create(context, collection)
                .withOaiSource(BASE_URL)
                .withOaiSetId("publications")
                .withMetadataConfigId("cerif")
                .withHarvestType(HarvestedCollection.TYPE_DMD)
                .withHarvestStatus(HarvestedCollection.STATUS_READY)
                .build();

            HarvestedCollection personHarvest = HarvestedCollectionBuilder.create(context, personCollection)
                .withOaiSource(BASE_URL)
                .withOaiSetId("persons")
                .withMetadataConfigId("cerif")
                .withHarvestType(HarvestedCollection.TYPE_DMD)
                .withHarvestStatus(HarvestedCollection.STATUS_READY)
                .build();

            context.restoreAuthSystemState();

            // import the publication and create the person with the CrisConsumer
            harvester.runHarvest(context, publicationHarvest, getDefaultOptions());

            List<Item> publications = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
            assertThat(publications, hasSize(1));

            Item publication = publications.get(0);

            List<MetadataValue> values = publication.getMetadata();
            assertThat(values, hasSize(17));

            assertThat(values, hasItems(with("dc.title", "Test Publication")));
            assertThat(values, hasItems(with("dc.type", "Controlled Vocabulary for Resource Type Genres::text")));
            assertThat(values, hasItems(with("dc.date.issued", "2012-11-30")));
            assertThat(values, hasItems(with("oaire.citation.volume", "343")));
            assertThat(values, hasItems(with("oaire.citation.issue", "168")));
            assertThat(values, hasItems(with("oaire.citation.startPage", "168")));
            assertThat(values, hasItems(with("oaire.citation.endPage", "180")));
            assertThat(values, hasItems(with("dc.identifier.doi", "10.1007/978-3-642-35233-1_18")));
            assertThat(values, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER_PARENT_METADATA_VALUE)));
            assertThat(values, hasItems(with("cris.sourceId", "test-harvest::3")));
            assertThat(values, hasItems(with("relationship.type", "Publication")));

            MetadataValue author = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY).get(0);
            UUID authorAuthority = UUIDUtils.fromString(author.getAuthority());
            assertThat(authorAuthority, notNullValue());

            Item authorPerson = itemService.find(context, authorAuthority);
            assertThat(authorPerson, notNullValue());
            assertThat(authorPerson.getOwningCollection(), equalTo(personCollection));

            values = authorPerson.getMetadata();
            assertThat(values, hasSize(7));
            assertThat(values, hasItems(with("dc.title", "Manghi, Paolo")));
            assertThat(values, hasItems(with("cris.sourceId", "test-harvest::123")));
            assertThat(values, hasItems(with("relationship.type", "Person")));

            // import the author person
            harvester.runHarvest(context, personHarvest, getDefaultOptions());

            Item updatedAuthor = findItemByOaiID("oai:test-harvest:Persons/123", personCollection);
            assertThat(updatedAuthor.getID(), equalTo(authorPerson.getID()));

            values = updatedAuthor.getMetadata();
            assertThat(values, hasSize(11));
            assertThat(values, hasItems(with("dc.title", "Manghi, Paolo")));
            assertThat(values, hasItems(with("cris.sourceId", "test-harvest::123")));
            assertThat(values, hasItems(with("relationship.type", "Person")));
            assertThat(values, hasItems(with("oairecerif.person.gender", "M")));
            assertThat(values, hasItems(with("person.identifier.orcid", "0000-0002-9079-5932")));
            assertThat(values, hasItems(with("person.givenName", "Paolo")));
            assertThat(values, hasItems(with("person.familyName", "Manghi")));

        } finally {
            context.setDispatcher(null);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestWithPersonAndThenPublication() throws Exception {
        try {
            context.setDispatcher("cris-default");
            when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
                .thenReturn(buildResponse("single-publication.xml"));

            when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("persons"), eq("oai_cerif_openaire")))
                .thenReturn(buildResponse("single-person.xml"));

            context.turnOffAuthorisationSystem();

            Collection personCollection = createCollection(context, community)
                .withRelationshipType("Person")
                .withAdminGroup(eperson)
                .build();

            HarvestedCollection publicationHarvest = HarvestedCollectionBuilder.create(context, collection)
                .withOaiSource(BASE_URL)
                .withOaiSetId("publications")
                .withMetadataConfigId("cerif")
                .withHarvestType(HarvestedCollection.TYPE_DMD)
                .withHarvestStatus(HarvestedCollection.STATUS_READY)
                .build();

            HarvestedCollection personHarvest = HarvestedCollectionBuilder.create(context, personCollection)
                .withOaiSource(BASE_URL)
                .withOaiSetId("persons")
                .withMetadataConfigId("cerif")
                .withHarvestType(HarvestedCollection.TYPE_DMD)
                .withHarvestStatus(HarvestedCollection.STATUS_READY)
                .build();

            context.restoreAuthSystemState();

            harvester.runHarvest(context, personHarvest, getDefaultOptions());

            Item person = findItemByOaiID("oai:test-harvest:Persons/123", personCollection);

            List<MetadataValue> values = person.getMetadata();
            assertThat(values, hasSize(12));
            assertThat(values, hasItems(with("dc.title", "Manghi, Paolo")));
            assertThat(values, hasItems(with("cris.sourceId", "test-harvest::123")));
            assertThat(values, hasItems(with("relationship.type", "Person")));
            assertThat(values, hasItems(with("oairecerif.person.gender", "M")));
            assertThat(values, hasItems(with("person.identifier.orcid", "0000-0002-9079-5932")));
            assertThat(values, hasItems(with("person.givenName", "Paolo")));
            assertThat(values, hasItems(with("person.familyName", "Manghi")));

            harvester.runHarvest(context, publicationHarvest, getDefaultOptions());

            Item publication = findItemByOaiID("oai:test-harvest:Publications/3", collection);
            values = publication.getMetadata();
            assertThat(values, hasSize(17));

            assertThat(values, hasItems(with("dc.title", "Test Publication")));
            assertThat(values, hasItems(with("dc.type", "Controlled Vocabulary for Resource Type Genres::text")));
            assertThat(values, hasItems(with("dc.date.issued", "2012-11-30")));
            assertThat(values, hasItems(with("oaire.citation.volume", "343")));
            assertThat(values, hasItems(with("oaire.citation.issue", "168")));
            assertThat(values, hasItems(with("oaire.citation.startPage", "168")));
            assertThat(values, hasItems(with("oaire.citation.endPage", "180")));
            assertThat(values, hasItems(with("dc.identifier.doi", "10.1007/978-3-642-35233-1_18")));
            assertThat(values, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER_PARENT_METADATA_VALUE)));
            assertThat(values, hasItems(with("cris.sourceId", "test-harvest::3")));
            assertThat(values, hasItems(with("relationship.type", "Publication")));

            MetadataValue author = itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY).get(0);
            assertThat(UUIDUtils.fromString(author.getAuthority()), equalTo(person.getID()));

        } finally {
            context.setDispatcher(null);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestWithPreTransformation() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("equipments"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("single-equipment.xml"));

        context.turnOffAuthorisationSystem();

        Collection equipmentCollection = createCollection(context, community)
            .withRelationshipType("Equipment")
            .withAdminGroup(eperson)
            .withHarvestingPreTrasform("preTransformation.xsl")
            .build();

        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, equipmentCollection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("equipments")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("equipments"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        Item item = findItemByOaiID("oai:cris:equipments/f3e39333-5c82-40c2-aa3d-103def9abd97", equipmentCollection);
        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(11));
        assertThat(values, hasItems(with("dc.title", "Microflown Scan&Paint")));
        assertThat(values, hasItems(with("oairecerif.internalid", "test-id")));
        assertThat(values, hasItems(with("cris.sourceId", "test-harvest::f3e39333-5c82-40c2-aa3d-103def9abd97")));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestWithPostTransformation() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("equipments"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("single-equipment.xml"));

        context.turnOffAuthorisationSystem();

        Collection equipmentCollection = createCollection(context, community)
            .withRelationshipType("Equipment")
            .withAdminGroup(eperson)
            .withHarvestingPostTrasform("postTransformation.xsl")
            .build();

        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, equipmentCollection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("equipments")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("equipments"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        Item item = findItemByOaiID("oai:cris:equipments/f3e39333-5c82-40c2-aa3d-103def9abd97", equipmentCollection);
        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(10));
        assertThat(values, hasItems(with("dc.title", "MICROFLOWN SCAN&PAINT")));
        assertThat(values, hasItems(with("cris.sourceId", "test-harvest::f3e39333-5c82-40c2-aa3d-103def9abd97")));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunHarvestWithPreAndPostTransformation() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("equipments"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("single-equipment.xml"));

        context.turnOffAuthorisationSystem();

        Collection equipmentCollection = createCollection(context, community)
            .withRelationshipType("Equipment")
            .withAdminGroup(eperson)
            .withHarvestingPreTrasform("preTransformation.xsl")
            .withHarvestingPostTrasform("postTransformation.xsl")
            .build();

        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, equipmentCollection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("equipments")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getDefaultOptions());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("equipments"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        Item item = findItemByOaiID("oai:cris:equipments/f3e39333-5c82-40c2-aa3d-103def9abd97", equipmentCollection);
        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(11));
        assertThat(values, hasItems(with("dc.title", "MICROFLOWN SCAN&PAINT")));
        assertThat(values, hasItems(with("oairecerif.internalid", "TEST-ID")));
        assertThat(values, hasItems(with("cris.sourceId", "test-harvest::f3e39333-5c82-40c2-aa3d-103def9abd97")));

    }

    @Test
    public void testRunHarvestWithItemValidationFailureForMissingFile() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("single-publication.xml"));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getOptionsWithItemValidationEnabled());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, emptyCollectionOf(Item.class));

        List<WorkspaceItem> workspaceItems = workspaceItemService.findByCollection(context, collection);
        assertThat(workspaceItems, hasSize(1));

        Item item = workspaceItems.get(0).getItem();
        assertThat(item.isArchived(), equalTo(false));
        assertThat(item.getMetadata(), hasSize(13));
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Test Publication"));
        assertThat(harvestedItemService.find(context, item), notNullValue());
    }

    @Test
    public void testRunHarvestWithSubmissionNotEnabled() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("equipments"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("single-equipment.xml"));

        context.turnOffAuthorisationSystem();

        Collection equipmentCollection = CollectionBuilder.createCollection(context, community)
            .withRelationshipType("Equipment")
            .withSubmitterGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, equipmentCollection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("equipments")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getOptionsWithSubmissionNotEnabled());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("equipments"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, equipmentCollection));
        assertThat(items, emptyCollectionOf(Item.class));

        List<XmlWorkflowItem> workflowItems = workflowItemService.findByCollection(context, equipmentCollection);
        assertThat(workflowItems, hasSize(1));

        Item item = workflowItems.get(0).getItem();
        assertThat(item.isArchived(), equalTo(false));
        assertThat(item.getMetadata(), hasSize(7));
        assertThat(harvestedItemService.find(context, item), notNullValue());
    }

    @Test
    public void testRunHarvestWithRecordValidationEnabledAndNoValidationError() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("single-publication.xml"));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getOptionsWithRecordValidationEnabled());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        Item item = findItemByOaiID("oai:test-harvest:Publications/3", collection);
        assertThat(item.isArchived(), equalTo(true));
        assertThat(item.getOwningCollection(), equalTo(collection));
        assertThat(harvestedItemService.find(context, item), notNullValue());
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Test Publication"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::3"));
    }

    @Test
    public void testRunHarvestWithRecordValidationError() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("many-publications-with-one-invalid.xml"));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getOptionsWithRecordValidationEnabled());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
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

        Item item = findItemByOaiID("oai:test-harvest:Publications/123456789/1001", collection);
        assertThat(item.isArchived(), equalTo(true));
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("The International Journal of Digital Curation"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::123456789/1001"));

        item = findItemByOaiID("oai:test-harvest:Publications/123456789/1002", collection);
        assertThat(item.isArchived(), equalTo(true));
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Metadata and Semantics Research"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::123456789/1002"));

        item = findItemByOaiID("oai:test-harvest:Publications/123456789/1003", collection);
        assertThat(item.isArchived(), equalTo(true));
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("TEST"));
        assertThat(getFirstMetadataValue(item, "cris.sourceId"), equalTo("test-harvest::123456789/1003"));

    }

    @Test
    public void testRunHarvestWithItemValidationErrorButNoRecordValidationError() throws Exception {
        when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
            .thenReturn(buildResponse("single-publication.xml"));

        context.turnOffAuthorisationSystem();
        HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
            .withOaiSource(BASE_URL)
            .withOaiSetId("publications")
            .withMetadataConfigId("cerif")
            .withHarvestType(HarvestedCollection.TYPE_DMD)
            .withHarvestStatus(HarvestedCollection.STATUS_READY)
            .build();
        context.restoreAuthSystemState();

        harvester.runHarvest(context, harvestRow, getOptionsWithRecordAndItemValidationEnabled());

        verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
        verify(mockClient).identify(BASE_URL);
        verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));
        verifyNoMoreInteractions(mockClient);

        List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
        assertThat(items, emptyCollectionOf(Item.class));

        List<WorkspaceItem> workspaceItems = workspaceItemService.findByCollection(context, collection);
        assertThat(workspaceItems, hasSize(1));

        Item item = workspaceItems.get(0).getItem();
        assertThat(item.isArchived(), equalTo(false));
        assertThat(item.getMetadata(), hasSize(13));
        assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Test Publication"));
        assertThat(harvestedItemService.find(context, item), notNullValue());
    }

    @Test
    public void testRunHarvestWithNoEmailSentIfNoErrorOccurs() throws Exception {

        OAIHarvesterEmailSender originalEmailSender = harvester.getOaiHarvesterEmailSender();

        try {

            OAIHarvesterEmailSender mockEmailSender = mock(OAIHarvesterEmailSender.class);
            harvester.setOaiHarvesterEmailSender(mockEmailSender);

            when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
                .thenReturn(buildResponse("single-publication.xml"));

            context.turnOffAuthorisationSystem();
            HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
                .withOaiSource(BASE_URL)
                .withOaiSetId("publications")
                .withMetadataConfigId("cerif")
                .withHarvestType(HarvestedCollection.TYPE_DMD)
                .withHarvestStatus(HarvestedCollection.STATUS_READY)
                .build();
            context.restoreAuthSystemState();

            harvester.runHarvest(context, harvestRow, getDefaultOptions());

            verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
            verify(mockClient).identify(BASE_URL);
            verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));

            Item item = findItemByOaiID("oai:test-harvest:Publications/3", collection);
            assertThat(item.isArchived(), equalTo(true));

            verifyNoMoreInteractions(mockClient, mockEmailSender);

        } finally {
            harvester.setOaiHarvesterEmailSender(originalEmailSender);
        }
    }

    @Test
    public void testRunHarvestWithEmailSentIfItemValidationFails() throws Exception {

        OAIHarvesterEmailSender originalEmailSender = harvester.getOaiHarvesterEmailSender();

        try {

            OAIHarvesterEmailSender mockEmailSender = mock(OAIHarvesterEmailSender.class);
            harvester.setOaiHarvesterEmailSender(mockEmailSender);

            when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
                .thenReturn(buildResponse("single-publication.xml"));

            context.turnOffAuthorisationSystem();

            collection = createCollection(context, community)
                .withRelationshipType("Publication")
                .withAdminGroup(eperson)
                .withHarvestingEmail("IDENTIFY")
                .withHarvestingItemValidationEnabled()
                .build();

            HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
                .withOaiSource(BASE_URL)
                .withOaiSetId("publications")
                .withMetadataConfigId("cerif")
                .withHarvestType(HarvestedCollection.TYPE_DMD)
                .withHarvestStatus(HarvestedCollection.STATUS_READY)
                .build();
            context.restoreAuthSystemState();

            harvester.runHarvest(context, harvestRow, getDefaultOptions());

            verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
            verify(mockClient, times(2)).identify(BASE_URL);
            verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));

            List<WorkspaceItem> workspaceItems = workspaceItemService.findByCollection(context, collection);
            assertThat(workspaceItems, hasSize(1));

            Item item = workspaceItems.get(0).getItem();
            assertThat(item.isArchived(), equalTo(false));
            assertThat(item.getMetadata(), hasSize(13));
            assertThat(getFirstMetadataValue(item, "dc.title"), equalTo("Test Publication"));

            ArgumentCaptor<OAIHarvesterReport> captor = ArgumentCaptor.forClass(OAIHarvesterReport.class);
            verify(mockEmailSender).notifyCompletionWithErrors(eq("test@4science.it"), any(), captor.capture());

            OAIHarvesterReport harvesterReport = captor.getValue();
            assertThat(harvesterReport.getSuccessCount(), is(1));
            assertThat(harvesterReport.getFailureCount(), is(0));

            Map<String, ErrorDetails> errors = harvesterReport.getErrors();
            assertThat(errors.size(), is(1));
            assertThat(errors, hasKey("oai:test-harvest:Publications/3"));

            ErrorDetails errorDetails = errors.get("oai:test-harvest:Publications/3");
            assertThat(errorDetails.getAction(), is("created"));
            assertThat(errorDetails.getMessages(), hasSize(1));
            assertThat(errorDetails.getMessages(), contains("error.validation.filerequired - [/sections/upload]"));

            verifyNoMoreInteractions(mockClient, mockEmailSender);

        } finally {
            harvester.setOaiHarvesterEmailSender(originalEmailSender);
        }
    }

    @Test
    public void testRunHarvestWithEmailSentIfRecordValidationFails() throws Exception {

        OAIHarvesterEmailSender originalEmailSender = harvester.getOaiHarvesterEmailSender();

        try {

            OAIHarvesterEmailSender mockEmailSender = mock(OAIHarvesterEmailSender.class);
            harvester.setOaiHarvesterEmailSender(mockEmailSender);

            when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
                .thenReturn(buildResponse("single-publication-with-wrong-order.xml"));

            context.turnOffAuthorisationSystem();

            collection = createCollection(context, community)
                .withRelationshipType("Publication")
                .withAdminGroup(eperson)
                .withHarvestingEmail("dspace@test.it")
                .withHarvestingRecordValidationEnabled()
                .build();

            HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
                .withOaiSource(BASE_URL)
                .withOaiSetId("publications")
                .withMetadataConfigId("cerif")
                .withHarvestType(HarvestedCollection.TYPE_DMD)
                .withHarvestStatus(HarvestedCollection.STATUS_READY)
                .build();
            context.restoreAuthSystemState();

            harvester.runHarvest(context, harvestRow, getDefaultOptions());

            verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
            verify(mockClient).identify(BASE_URL);
            verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));

            Item item = findItemByOaiID("oai:test-harvest:Publications/2000", collection);
            assertThat(item.isArchived(), equalTo(true));

            ArgumentCaptor<OAIHarvesterReport> captor = ArgumentCaptor.forClass(OAIHarvesterReport.class);
            verify(mockEmailSender).notifyCompletionWithErrors(eq("dspace@test.it"), any(), captor.capture());

            OAIHarvesterReport harvesterReport = captor.getValue();
            assertThat(harvesterReport.getSuccessCount(), is(1));
            assertThat(harvesterReport.getFailureCount(), is(0));

            Map<String, ErrorDetails> errors = harvesterReport.getErrors();
            assertThat(errors.size(), is(1));
            assertThat(errors, hasKey("oai:test-harvest:Publications/2000"));

            ErrorDetails errorDetails = errors.get("oai:test-harvest:Publications/2000");
            assertThat(errorDetails.getAction(), is("created"));
            assertThat(errorDetails.getMessages(), hasSize(1));
            assertThat(errorDetails.getMessages(), hasItem(containsString("Invalid content was found starting "
                + "with element '{\"https://www.openaire.eu/cerif-profile/1.1/\":StartPage}'")));

            verifyNoMoreInteractions(mockClient, mockEmailSender);

        } finally {
            harvester.setOaiHarvesterEmailSender(originalEmailSender);
        }
    }

    @Test
    public void testRunHarvestWithEmailSentIfItemAndRecordValidationFails() throws Exception {

        OAIHarvesterEmailSender originalEmailSender = harvester.getOaiHarvesterEmailSender();

        try {

            OAIHarvesterEmailSender mockEmailSender = mock(OAIHarvesterEmailSender.class);
            harvester.setOaiHarvesterEmailSender(mockEmailSender);

            when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
                .thenReturn(buildResponse("many-publications-with-one-invalid.xml"));

            context.turnOffAuthorisationSystem();
            HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
                .withOaiSource(BASE_URL)
                .withOaiSetId("publications")
                .withMetadataConfigId("cerif")
                .withHarvestType(HarvestedCollection.TYPE_DMD)
                .withHarvestStatus(HarvestedCollection.STATUS_READY)
                .build();
            context.restoreAuthSystemState();

            harvester.runHarvest(context, harvestRow, getOptionsWithRecordAndItemValidationEnabled());

            verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
            verify(mockClient).identify(BASE_URL);
            verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));

            assertThat(workspaceItemService.findByCollection(context, collection), hasSize(3));

            ArgumentCaptor<OAIHarvesterReport> captor = ArgumentCaptor.forClass(OAIHarvesterReport.class);
            String defaultEmail = configurationService.getProperty("alert.recipient");
            verify(mockEmailSender).notifyCompletionWithErrors(eq(defaultEmail), any(), captor.capture());

            OAIHarvesterReport harvesterReport = captor.getValue();
            assertThat(harvesterReport.getSuccessCount(), is(3));
            assertThat(harvesterReport.getFailureCount(), is(0));

            Map<String, ErrorDetails> errors = harvesterReport.getErrors();
            assertThat(errors.size(), is(3));
            assertThat(errors, hasKey("oai:test-harvest:Publications/123456789/1001"));
            assertThat(errors, hasKey("oai:test-harvest:Publications/123456789/1002"));
            assertThat(errors, hasKey("oai:test-harvest:Publications/123456789/1003"));

            ErrorDetails errorDetails = errors.get("oai:test-harvest:Publications/123456789/1001");
            assertThat(errorDetails.getAction(), is("created"));
            List<String> messages = errorDetails.getMessages();
            assertThat(messages, hasSize(2));
            assertThat(messages, hasItem("error.validation.filerequired - [/sections/upload]"));
            assertThat(messages, hasItem("error.validation.required - [/sections/publication/dc.date.issued]"));

            errorDetails = errors.get("oai:test-harvest:Publications/123456789/1002");
            assertThat(errorDetails.getAction(), is("created"));
            messages = errorDetails.getMessages();
            assertThat(messages, hasSize(2));
            assertThat(messages, hasItem("error.validation.filerequired - [/sections/upload]"));
            assertThat(errorDetails.getMessages(), hasItem(containsString("Element 'oai_cerif:Publishers' "
                + "cannot have character [children]")));

            errorDetails = errors.get("oai:test-harvest:Publications/123456789/1003");
            assertThat(errorDetails.getAction(), is("created"));
            messages = errorDetails.getMessages();
            assertThat(messages, hasSize(1));
            assertThat(messages, hasItem("error.validation.filerequired - [/sections/upload]"));

            verifyNoMoreInteractions(mockClient, mockEmailSender);

        } finally {
            harvester.setOaiHarvesterEmailSender(originalEmailSender);
        }
    }

    @Test
    public void testRunHarvestWithEmailSentIfUnexpectedErrorOccurs() throws Exception {

        OAIHarvesterEmailSender originalEmailSender = harvester.getOaiHarvesterEmailSender();

        try {

            OAIHarvesterEmailSender mockEmailSender = mock(OAIHarvesterEmailSender.class);
            harvester.setOaiHarvesterEmailSender(mockEmailSender);

            RuntimeException genericErrorException = new RuntimeException("GENERIC ERROR");

            when(mockClient.listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire")))
                .thenThrow(genericErrorException);

            context.turnOffAuthorisationSystem();
            HarvestedCollection harvestRow = HarvestedCollectionBuilder.create(context, collection)
                .withOaiSource(BASE_URL)
                .withOaiSetId("publications")
                .withMetadataConfigId("cerif")
                .withHarvestType(HarvestedCollection.TYPE_DMD)
                .withHarvestStatus(HarvestedCollection.STATUS_READY)
                .build();
            context.restoreAuthSystemState();

            harvester.runHarvest(context, harvestRow, getDefaultOptions());

            List<Item> items = IteratorUtils.toList(itemService.findAllByCollection(context, collection));
            assertThat(items, emptyCollectionOf(Item.class));

            verify(mockClient).resolveNamespaceToPrefix(BASE_URL, getMetadataFormatNamespace("cerif").getURI());
            verify(mockClient).identify(BASE_URL);
            verify(mockClient).listRecords(eq(BASE_URL), isNull(), any(), eq("publications"), eq("oai_cerif_openaire"));

            String defaultEmail = configurationService.getProperty("alert.recipient");
            verify(mockEmailSender).notifyFailure(eq(defaultEmail), any(), eq(genericErrorException));

            verifyNoMoreInteractions(mockClient, mockEmailSender);

        } finally {
            harvester.setOaiHarvesterEmailSender(originalEmailSender);
        }
    }

    private Item findItemByOaiID(String oaiId, Collection collection) throws SQLException {

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

    private OAIHarvesterOptions getDefaultOptions() {
        return new OAIHarvesterOptions(UUID.randomUUID(), null, null, null, true);
    }

    private OAIHarvesterOptions getOptionsWithForceSynchronization() {
        return new OAIHarvesterOptions(randomUUID(), true, null, null, true);
    }

    private OAIHarvesterOptions getOptionsWithItemValidationEnabled() {
        return new OAIHarvesterOptions(randomUUID(), null, null, true, true);
    }

    private OAIHarvesterOptions getOptionsWithRecordValidationEnabled() {
        return new OAIHarvesterOptions(randomUUID(), null, true, null, true);
    }

    private OAIHarvesterOptions getOptionsWithRecordAndItemValidationEnabled() {
        return new OAIHarvesterOptions(randomUUID(), null, true, true, true);
    }

    private OAIHarvesterOptions getOptionsWithSubmissionNotEnabled() {
        return new OAIHarvesterOptions(randomUUID(), null, null, null, false);
    }

    private void deletePoolTask(PoolTask poolTask) {
        try {
            poolTaskService.delete(context, poolTask);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }
}
