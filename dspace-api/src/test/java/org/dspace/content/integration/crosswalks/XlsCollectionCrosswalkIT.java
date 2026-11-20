/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static java.util.Arrays.asList;
import static org.dspace.app.bulkedit.BulkImport.BITSTREAMS_SHEET_HEADERS;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.builder.BitstreamBuilder.createBitstream;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.util.WorkbookUtils.getRowValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.bulkimport.service.BulkImportWorkbookBuilderImpl;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.app.util.DCInputsReader;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Constants;
import org.dspace.core.CrisConstants;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

/**
 * Integration tests for the {@link XlsCollectionCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XlsCollectionCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private XlsCollectionCrosswalk xlsCollectionCrosswalk;

    private BulkImportWorkbookBuilderImpl workbookBuilder;

    private ConfigurationService configurationService;

    private Community community;

    private Group anonymousGroup;

    private Group adminGroup;

    private static final String BITSTREAM_URL_FORMAT = "%s/api/core/bitstreams/%s/content";

    @Before
    public void setup() throws SQLException, AuthorizeException {

        StreamDisseminationCrosswalkMapper crosswalkMapper = new DSpace()
            .getSingletonService(StreamDisseminationCrosswalkMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        xlsCollectionCrosswalk = (XlsCollectionCrosswalk) crosswalkMapper.getByType("collection-xls");

        workbookBuilder = new DSpace().getServiceManager()
            .getServicesByType(BulkImportWorkbookBuilderImpl.class).get(0);

        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
        adminGroup = groupService.findByName(context, Group.ADMIN);
        context.restoreAuthSystemState();

        // CRIS enables authority control on these fields (modules/authority.cfg);
        // main leaves them disabled, so the CRIS test fixtures need them enabled here.
        // Only fields that actually exist in this registry are configured.
        MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        MetadataAuthorityService metadataAuthorityService =
            ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
        String[] authorityControlledFields = {
            "dc.contributor.author",
            "oairecerif.author.affiliation",
            "dc.contributor.editor",
            "oairecerif.editor.affiliation",
            "dc.relation.product",
            "dc.relation.publication",
            "dc.relation.journal",
            "dc.relation.project",
            "dc.publisher",
            "dc.relation.funding",
            "dc.description.sponsorship",
            "crispj.coordinator",
            "crispj.organization",
            "crispj.partnerou",
            "crispj.investigator",
            "crispj.coinvestigators",
            "dc.relation.equipment",
            "person.affiliation.name",
            "oairecerif.person.affiliation",
            "crisrp.qualification",
            "crisrp.education",
            "organization.parentOrganization",
            "crisou.director",
            "oairecerif.funder",
            "oairecerif.fundingParent",
            "crisfund.investigators",
            "crisfund.coinvestigators",
            "crisfund.leadorganizations",
            "crisfund.leadcoorganizations",
            "cris.policy.eperson",
            "cris.policy.group",
            "dspace.object.owner",
            "dc.identifier.issn",
            "dc.type",
            "cris.virtual.department",
            "oairecerif.identifier.url",
        };
        for (String field : authorityControlledFields) {
            String[] parts = field.split("\\.");
            String qualifier = parts.length > 2 ? parts[2] : null;
            if (metadataFieldService.findByElement(context, parts[0], parts[1], qualifier) != null) {
                configurationService.setProperty("authority.controlled." + field, true);
            }
        }
        metadataAuthorityService.clearCache();

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBulkImportOfCollectionDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withAuthor("Test, User")
            .withAuthor("White, Walter")
            .withAcronym("TEST")
            .withIssueDate("2020-01-01")
            .withEntityType("Publication")
            .makeUnDiscoverable()
            .withFulltext("test.pdf", "Test", InputStream.nullInputStream())
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-01-01")
            .withSecuredMetadata("dc", "relation", "publication", null, "First Publication",
                firstItem.getID().toString(), 600, 2)
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Third Publication")
            .withType("Article")
            .withAuthor("White, Walter")
            .build();

        context.turnOffAuthorisationSystem();

        File tempWorkbookFile = File.createTempFile("test-workbook", "xls");

        try (FileOutputStream fos = new FileOutputStream(tempWorkbookFile)) {
            xlsCollectionCrosswalk.disseminate(context, collection, fos);
        }

        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(),
            "-f", tempWorkbookFile.getAbsolutePath(), "-e", admin.getEmail()};

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        assertThat(handler.getInfoMessages(), containsInAnyOrder(
            is("Start reading all the metadata group rows"),
            is("Found 0 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 1 bitstreams to process"),
            is("Found 3 items to process"),
            containsString("Sheet 'bitstream-metadata' - Row 2 - Bitstream updated successfully"),
            containsString("Row 2 - Item updated successfully - ID: "),
            containsString("Row 3 - Item updated successfully - ID: "),
            containsString("Row 4 - Item updated successfully - ID: ")));

    }

    @Test
    public void testCollectionDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withHandle("123456789/001")
            .withTitle("Test Publication")
            .withAlternativeTitle("Alternative publication title")
            .withRelationPublication("Published in publication")
            .withRelationDoi("doi:10.3972/test")
            .withRelationIsbn("ISBN-01")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withLanguage("en")
            .withPublisher("Publication publisher")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withRelationProject("Test Project", "d9471fee-34fa-4a39-9658-443c4bb47b22")
            .withRelationGrantno(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withRelationFunding("Test Funding")
            .withRelationConference("The best Conference")
            .withRelationProduct("DataSet")
            .withDescription("Description")
            .withDescriptionAbstract("Description Abstract")
            .withIsPartOf("Journal")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withHandle("123456789/002")
            .withTitle("Second Publication")
            .withIsbnIdentifier("ISBN-002")
            .withIssnIdentifier("ISSN-002")
            .withIssnIdentifier("ISSN-003")
            .withIsiIdentifier("ISI-002")
            .withScopusIdentifier("SCOPUS-002")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project")
            .withRelationGrantno("01")
            .withRelationConference("Conference1")
            .withRelationConference("Conference2")
            .withRelationProduct("DataSet")
            .withDescription("Publication Description")
            .withCitationIdentifier("CIT-01")
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        // On DSpace main the 'publication' submission form exposes no metadata groups, so the author metadata
        // is flattened into the main 'items' sheet (no nested group sheets); only items + bitstream-metadata.
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        String firstItemId = firstItem.getID().toString();
        String secondItemId = secondItem.getID().toString();

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "id", "discoverable", "dc.contributor.author", "dc.title",
            "dc.title.alternative", "dc.date.issued", "dc.publisher", "dc.identifier.citation",
            "dc.relation.ispartofseries", "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi",
            "dc.identifier.adsbibcode", "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn",
            "dc.identifier.other", "dc.identifier.ismn", "dc.identifier.govdoc", "dc.identifier.uri",
            "dc.identifier.isbn", "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract",
            "dc.description.sponsorship", "dc.description" };
        String[] mainSheetFirstRow = { firstItemId, "Y", "John Smith||Walter White", "Test Publication",
            "Alternative publication title", "2020-01-01", "Publication publisher", "", "",
            "doi:111.111/publication", "99999999", "111-222-333", "", "", "", "2049-3630", "", "", "",
            "http://localhost:4000/handle/123456789/001", "978-3-16-148410-0",
            "Controlled Vocabulary for Resource Type Genres::text::review", "en", "test||export",
            "Description Abstract", "", "Description" };
        String[] mainSheetSecondRow = { secondItemId, "Y", "Jesse Pinkman", "Second Publication", "", "2020-01-01",
            "", "CIT-01", "", "", "SCOPUS-002", "ISI-002", "", "", "", "ISSN-002||ISSN-003", "", "", "",
            "http://localhost:4000/handle/123456789/002", "ISBN-002",
            "Controlled Vocabulary for Resource Type Genres::text::review", "", "export", "", "",
            "Publication Description" };

        asserThatSheetHas(mainSheet, "items", 3, mainSheetHeader, Arrays.asList(mainSheetFirstRow, mainSheetSecondRow));
    }

    @Test
    public void testCollectionDisseminateWithEmptyCollection() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withAdminGroup(eperson)
            .build();
        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "id", "discoverable", "dc.contributor.author", "dc.title", "dc.title.alternative",
            "dc.date.issued", "dc.publisher", "dc.identifier.citation", "dc.relation.ispartofseries",
            "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi", "dc.identifier.adsbibcode",
            "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn", "dc.identifier.other",
            "dc.identifier.ismn", "dc.identifier.govdoc", "dc.identifier.uri", "dc.identifier.isbn",
            "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract", "dc.description.sponsorship",
            "dc.description" };
        assertThat(mainSheet.getPhysicalNumberOfRows(), equalTo(1));
        assertThat(getRowValues(mainSheet.getRow(0), mainSheetHeader.length), contains(mainSheetHeader));
    }

    @Test
    public void testCollectionDisseminateWithMockSubmissionFormConfiguration() throws Exception {

        try {
            DCInputsReader reader = mock(DCInputsReader.class);

            context.turnOffAuthorisationSystem();

            Collection collection = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            List<String> publicationMetadataFields = asList("dc.title", "dc.date.issued", "dc.subject");
            List<String> publicationMetadataFieldGroups = List.of("dc.contributor.author");
            List<String> authorGroup = asList("dc.contributor.author", "oairecerif.author.affiliation");

            when(reader.getLanguagesForMetadata(collection, "dc.title", false)).thenReturn(Arrays.asList("en", "it"));
            when(reader.getSubmissionFormMetadata(collection)).thenReturn(publicationMetadataFields);
            when(reader.getSubmissionFormMetadataGroups(collection)).thenReturn(publicationMetadataFieldGroups);
            when(reader.getAllNestedMetadataByGroupName(collection, "dc.contributor.author")).thenReturn(authorGroup);

            workbookBuilder.setReader(reader);

            Item firstPublication = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("First publication")
                .withTitleForLanguage("Prima pubblicazione", "it")
                .withTitleForLanguage("Primera publicacion", "es")
                .withIssueDate("2020-01-01")
                .withAuthor("Walter White", "0ecd5452-aae2-4c18-9aec-0471bdcbadbc")
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withAuthor("Jesse Pinkman")
                .withAuthorAffiliation("Company")
                .build();

            Item secondPublication = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitleForLanguage("Seconda pubblicazione", "it")
                .withTitleForLanguage("Second publication", "en")
                .withIssueDate("2019-01-01")
                .makeUnDiscoverable()
                .build();

            Item thirdPublication = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("Third publication")
                .withTitleForLanguage("Terza pubblicazione", "it")
                .withIssueDate("2018-01-01")
                .withAuthor("Carl Johnson")
                .makeUnDiscoverable()
                .build();

            Item fourthPublication = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitleForLanguage("Pubblicazione", "it")
                .withTitle("Fourth publication")
                .withIssueDate("2017-01-01")
                .withAuthor("Carl Johnson")
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withAuthor("Red White")
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withSubject("test")
                .withSubject("export")
                .build();

            context.restoreAuthSystemState();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xlsCollectionCrosswalk.disseminate(context, collection, baos);

            Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));

            assertThat(workbook.getNumberOfSheets(), equalTo(3));

            String firstId = firstPublication.getID().toString();
            String secondId = secondPublication.getID().toString();
            String thirdId = thirdPublication.getID().toString();
            String fourthId = fourthPublication.getID().toString();

            Sheet mainSheet = workbook.getSheetAt(0);
            String[] header = { "id", "discoverable", "dc.title", "dc.date.issued",
                "dc.subject", "dc.title[it]", "dc.title[en]" };
            String[] firstRow = { firstId, "Y", "First publication", "2020-01-01", "", "Prima pubblicazione", "" };
            String[] secondRow = { secondId, "N", "", "2019-01-01", "", "Seconda pubblicazione", "Second publication" };
            String[] thirdRow = { thirdId, "N", "Third publication", "2018-01-01", "", "Terza pubblicazione", "" };
            String[] fourthRow = { fourthId, "Y", "Fourth publication", "2017-01-01",
                "test||export", "Pubblicazione", "" };

            asserThatSheetHas(mainSheet, "items", 5, header, asList(firstRow, secondRow, thirdRow, fourthRow));

            Sheet authorSheet = workbook.getSheetAt(1);
            String[] authorSheetHeader = { "parent-id", "dc.contributor.author", "oairecerif.author.affiliation" };
            String[] authorSheetFirstRow = { firstId, "Walter White$$0ecd5452-aae2-4c18-9aec-0471bdcbadbc$$600", "" };
            String[] authorSheetSecondRow = { firstId, "Jesse Pinkman", "Company" };
            String[] authorSheetThirdRow = { thirdId, "Carl Johnson", "" };
            String[] authorSheetFourthRow = { fourthId, "Carl Johnson", "" };
            String[] authorSheetFifthRow = { fourthId, "Red White", "" };

            asserThatSheetHas(authorSheet, "dc.contributor.author", 6, authorSheetHeader, asList(authorSheetFirstRow,
                authorSheetSecondRow, authorSheetThirdRow, authorSheetFourthRow, authorSheetFifthRow));

        } finally {
            this.workbookBuilder.setReader(new DCInputsReader());
        }

    }

    @Test
    public void testManyItemsDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withEntityType("Publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withHandle("123456789/001")
            .withTitle("Test Publication")
            .withAlternativeTitle("Alternative publication title")
            .withRelationPublication("Published in publication")
            .withRelationDoi("doi:10.3972/test")
            .withRelationIsbn("ISBN-01")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withLanguage("en")
            .withPublisher("Publication publisher")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withRelationProject("Test Project", "d9471fee-34fa-4a39-9658-443c4bb47b22")
            .withRelationGrantno(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withRelationFunding("Test Funding")
            .withRelationConference("The best Conference")
            .withRelationProduct("DataSet")
            .withDescription("Description")
            .withDescriptionAbstract("Description Abstract")
            .withIsPartOf("Journal")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withHandle("123456789/002")
            .withTitle("Second Publication")
            .withIsbnIdentifier("ISBN-002")
            .withIssnIdentifier("ISSN-002")
            .withIssnIdentifier("ISSN-003")
            .withIsiIdentifier("ISI-002")
            .withScopusIdentifier("SCOPUS-002")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project")
            .withRelationGrantno("01")
            .withRelationConference("Conference1")
            .withRelationConference("Conference2")
            .withRelationProduct("DataSet")
            .withDescription("Publication Description")
            .withCitationIdentifier("CIT-01")
            .build();

        WorkspaceItem thirdItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Third Publication")
            .withDoiIdentifier("doi:222.111/publication")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2022-01-01")
            .withAuthor("Luca Giamminonni")
            .withAuthorAffilitation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        context.restoreAuthSystemState();

        Iterator<Item> itemIterator = IteratorUtils.arrayIterator(firstItem, secondItem, thirdItem.getItem());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, itemIterator, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        // On DSpace main the 'publication' submission form exposes no metadata groups, so author metadata is
        // flattened into the main 'items' sheet; only items + bitstream-metadata sheets are produced.
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        String firstItemId = firstItem.getID().toString();
        String secondItemId = secondItem.getID().toString();
        String thirdItemId = thirdItem.getItem().getID().toString();

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "id", "discoverable", "dc.contributor.author", "dc.title",
            "dc.title.alternative", "dc.date.issued", "dc.publisher", "dc.identifier.citation",
            "dc.relation.ispartofseries", "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi",
            "dc.identifier.adsbibcode", "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn",
            "dc.identifier.other", "dc.identifier.ismn", "dc.identifier.govdoc", "dc.identifier.uri",
            "dc.identifier.isbn", "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract",
            "dc.description.sponsorship", "dc.description" };
        String[] mainSheetFirstRow = { firstItemId, "Y", "John Smith||Walter White", "Test Publication",
            "Alternative publication title", "2020-01-01", "Publication publisher", "", "",
            "doi:111.111/publication", "99999999", "111-222-333", "", "", "", "2049-3630", "", "", "",
            "http://localhost:4000/handle/123456789/001", "978-3-16-148410-0",
            "Controlled Vocabulary for Resource Type Genres::text::review", "en", "test||export",
            "Description Abstract", "", "Description" };
        String[] mainSheetSecondRow = { secondItemId, "Y", "Jesse Pinkman", "Second Publication", "", "2020-01-01",
            "", "CIT-01", "", "", "SCOPUS-002", "ISI-002", "", "", "", "ISSN-002||ISSN-003", "", "", "",
            "http://localhost:4000/handle/123456789/002", "ISBN-002",
            "Controlled Vocabulary for Resource Type Genres::text::review", "", "export", "", "",
            "Publication Description" };
        String[] mainSheetThirdRow = { thirdItemId, "Y", "Luca Giamminonni", "Third Publication", "", "2022-01-01",
            "", "", "", "doi:222.111/publication", "", "", "", "", "", "", "", "", "", "", "",
            "Controlled Vocabulary for Resource Type Genres::text::review", "", "test||export", "", "" };

        asserThatSheetHas(mainSheet, "items", 4, mainSheetHeader,
            asList(mainSheetFirstRow, mainSheetSecondRow, mainSheetThirdRow));
    }

    @Test
    public void testManyItemsDisseminateWithWrongObjectTypes() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withEntityType("Publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withHandle("123456789/001")
            .withTitle("Test Publication")
            .build();

        context.restoreAuthSystemState();

        Iterator<? extends DSpaceObject> iterator = IteratorUtils.arrayIterator(firstItem, admin);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        IllegalArgumentException argumentException = Assert.assertThrows(IllegalArgumentException.class,
            () -> xlsCollectionCrosswalk.disseminate(context, iterator, baos));

        assertThat(argumentException.getMessage(), is("The xsl export supports only items. "
            + "Found object with type " + Constants.EPERSON + " and id " + admin.getID()));
    }

    @Test
    public void testManyItemsDisseminateWithDifferentCollections() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection collection1 = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withEntityType("Publication")
            .withAdminGroup(eperson)
            .build();
        Collection collection2 = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withEntityType("Publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection1)
            .withHandle("123456789/001")
            .withTitle("Test Publication")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection2)
            .withHandle("123456789/002")
            .withTitle("Second Publication")
            .build();

        context.restoreAuthSystemState();

        Iterator<Item> itemIterator = IteratorUtils.arrayIterator(firstItem, secondItem);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        XlsCollectionCrosswalk spy = spy(xlsCollectionCrosswalk);

        spy.disseminate(context, itemIterator, baos);

        verify(spy, times(1))
            .logMessage(ArgumentMatchers.eq(Level.WARNING), ArgumentMatchers.contains(secondItem.getID().toString()));
    }

    @Test
    public void testDisseminateWithUnexpectedLanguageOnMetadataValues() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withHandle("123456789/001")
            .withTitleForLanguage("Test Publication", "de")
            .withAuthorForLanguage("John Smith", "de")
            .withAuthorAffiliationForLanguage("4Science", "de")
            .withDescriptionAbstract("Description Abstract")
            .withDoiIdentifierForLanguage("XXX", "de")
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        // On main the 'publication' submission form exposes no metadata groups, so the author/editor/project
        // values are flattened into the main 'items' sheet and no nested-group sheets are produced.
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        String itemId = item.getID().toString();

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "id", "discoverable", "dc.contributor.author", "dc.title",
            "dc.title.alternative", "dc.date.issued", "dc.publisher", "dc.identifier.citation",
            "dc.relation.ispartofseries", "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi",
            "dc.identifier.adsbibcode", "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn",
            "dc.identifier.other", "dc.identifier.ismn", "dc.identifier.govdoc", "dc.identifier.uri",
            "dc.identifier.isbn", "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract",
            "dc.description.sponsorship", "dc.description" };

        // The de-language title/author/doi are dropped; only the handle uri and abstract survive.
        String[] mainSheetRow = { itemId, "Y", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "http://localhost:4000/handle/123456789/001", "", "", "", "", "Description Abstract", "", "" };

        List<String[]> rows = new ArrayList<String[]>();
        rows.add(mainSheetRow);
        asserThatSheetHas(mainSheet, "items", 2, mainSheetHeader, rows);
    }

    @Test
    public void testCollectionDisseminateWithBitstreamSheet() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withDescription("Description")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withDescription("Publication Description")
            .build();

        Bundle firstBundle = BundleBuilder.createBundle(context, firstItem)
            .withName("TEST-BUNDLE")
            .build();

        Bitstream firstBitstream = createBitstream(context, firstBundle, getBitstreamSample("First bitstream"))
            .withName("test.txt")
            .withDescription("desc 1")
            .withMetadata("dc", "date", null, "2023-02-23")
            .withMetadata("dc", "contributor", null, "Unknown author")
            .build();

        Bitstream secondBitstream = createBitstream(context, firstBundle, getBitstreamSample("Second bitstream"))
            .withName("test2.txt")
            .withDescription("desc 2")
            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymousGroup)
            .withDspaceObject(secondBitstream)
            .withAction(Constants.READ)
            .withName("openaccess")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        Bundle secondBundle = BundleBuilder.createBundle(context, firstItem)
            .withName("TEST-BUNDLE-2")
            .build();

        Bitstream thirdBitstream = createBitstream(context, secondBundle, getBitstreamSample("Third bitstream"))
            .withName("test3.txt")
            .withDescription("desc 3")
            .build();

        Bundle thirdBundle = BundleBuilder.createBundle(context, secondItem)
            .withName("TEST-BUNDLE")
            .build();

        Bitstream fourthBitstream = createBitstream(context, thirdBundle, getBitstreamSample("Fourth bitstream"))
            .withName("test4.txt")
            .withDescription("desc 4")
            .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        String firstItemId = firstItem.getID().toString();
        String secondItemId = secondItem.getID().toString();

        String firstUrl = getBitstreamLocationUrl(firstBitstream);
        String secondUrl = getBitstreamLocationUrl(secondBitstream);
        String thirdUrl = getBitstreamLocationUrl(thirdBitstream);
        String fourthUrl = getBitstreamLocationUrl(fourthBitstream);

        String[] bitstreamHeaders = ArrayUtils.addAll(BITSTREAMS_SHEET_HEADERS, "dc.title", "dc.description");
        String[] firstRow = { firstItemId, firstUrl, "TEST-BUNDLE", "1", "", "N", "test.txt", "desc 1" };
        String[] secondRow = { firstItemId, secondUrl, "TEST-BUNDLE", "2", "openaccess", "N", "test2.txt", "desc 2" };
        String[] thirdRow = { firstItemId, thirdUrl, "TEST-BUNDLE-2", "1", "", "N", "test3.txt", "desc 3" };
        String[] fourthRow = { secondItemId, fourthUrl, "TEST-BUNDLE", "1", "", "N", "test4.txt", "desc 4" };

        List<String[]> rows = List.of(firstRow, secondRow, thirdRow, fourthRow);

        asserThatSheetHas(workbook.getSheetAt(1), "bitstream-metadata", 5, bitstreamHeaders, rows);
    }

    @Test
    public void testCollectionDisseminateWithBitstreamSheetOneBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withDescription("Description")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withDescription("Publication Description")
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, firstItem)
            .withName("TEST-BUNDLE")
            .build();

        Bitstream bitstream = createBitstream(context, bundle, getBitstreamSample("First bitstream sample"))
            .withName("test.txt")
            .withDescription("test description 1")
            .withMetadata("dc", "date", null, "2023-02-23")
            .withMetadata("dc", "contributor", null, "Unknown author")
            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, adminGroup)
            .withDspaceObject(bitstream)
            .withAction(Constants.READ)
            .withDescription("Test policy")
            .withName("administrator")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, adminGroup)
            .withDspaceObject(bitstream)
            .withAction(Constants.WRITE)
            .withName("administrator")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymousGroup)
            .withDspaceObject(bitstream)
            .withAction(Constants.READ)
            .withName("openaccess")
            .withPolicyType(ResourcePolicy.TYPE_INHERITED)
            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymousGroup)
            .withDspaceObject(bitstream)
            .withAction(Constants.READ)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymousGroup)
            .withDspaceObject(bitstream)
            .withAction(Constants.READ)
            .withName("embargo")
                             .withStartDate(LocalDate.of(2025, 3, 25))
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymousGroup)
            .withDspaceObject(bitstream)
            .withAction(Constants.READ)
            .withName("lease")
            .withDescription("Test")
                             .withEndDate(LocalDate.of(2025, 3, 25))
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        String firstItemId = firstItem.getID().toString();
        String bitstreamLocation = getBitstreamLocationUrl(bitstream);
        String expectedPolicies = "administrator$$Test policy||embargo$$2025-03-25||lease$$2025-03-25$$Test";

        String[] bitstreamHeaders = ArrayUtils.addAll(BITSTREAMS_SHEET_HEADERS, "dc.title", "dc.description");
        String[] firstRow = { firstItemId, bitstreamLocation, "TEST-BUNDLE", "1", expectedPolicies, "N",
            "test.txt", "test description 1" };

        List<String[]> rowList = new ArrayList<>();
        rowList.add(firstRow);

        asserThatSheetHas(workbook.getSheetAt(1), "bitstream-metadata", 2, bitstreamHeaders, rowList);
    }

    @Test
    public void testCollectionDisseminateWithBitstreamSheetEmptyBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withDescription("Description")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withDescription("Publication Description")
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        String[] bitstreamHeaders = ArrayUtils.addAll(BITSTREAMS_SHEET_HEADERS);
        asserThatSheetHas(workbook.getSheetAt(1), "bitstream-metadata", 1, bitstreamHeaders, List.of());
    }

    @Test
    public void testCollectionDisseminateWithSecurityLevel() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withHandle("123456789/001")
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withSecuredMetadata("dc", "type", null, "Article", 1)
            .withSecuredMetadata("dc", "relation", "publication", null, "First Publication", "authority1", 600, 2)
            .withMetadata("dc", "relation", "publication", "Second Publication")
            .withSecuredMetadata("dc", "relation", "publication", "Third Publication", 0)
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        // On main the 'publication' submission form exposes no metadata groups, so no nested-group sheets are
        // produced; the dc.relation.publication column is not part of main's flat form so that value is not exported.
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        String itemId = item.getID().toString();

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "id", "discoverable", "dc.contributor.author", "dc.title",
            "dc.title.alternative", "dc.date.issued", "dc.publisher", "dc.identifier.citation",
            "dc.relation.ispartofseries", "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi",
            "dc.identifier.adsbibcode", "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn",
            "dc.identifier.other", "dc.identifier.ismn", "dc.identifier.govdoc", "dc.identifier.uri",
            "dc.identifier.isbn", "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract",
            "dc.description.sponsorship", "dc.description" };

        String[] mainSheetRow = { itemId, "Y", "", "Test Publication", "", "", "", "", "", "", "", "", "", "", "", "",
            "", "", "", "http://localhost:4000/handle/123456789/001", "", "Article$$sl-1", "", "", "", "", "" };

        List<String[]> rows = new ArrayList<String[]>();
        rows.add(mainSheetRow);
        asserThatSheetHas(mainSheet, "items", 2, mainSheetHeader, rows);

    }

    @Test
    public void testCollectionDisseminateWithBitstreamSheetWithMetadataLanguages() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("Test Publication")
                .withDescription("Description")
                .build();

        Bundle bundle = BundleBuilder.createBundle(context, firstItem)
                .withName("TEST-BUNDLE")
                .build();

        // add metadata dc.description[en]
        Bitstream bitstream = createBitstream(context, bundle, getBitstreamSample("First bitstream sample"))
                .withName("test.txt")
                .withMetadata("dc", "description", null, "en", "test description 1")
                .withMetadata("dc", "date", null, "2023-02-23")
                .withMetadata("dc", "contributor", null, "Unknown author")
                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, adminGroup)
                .withDspaceObject(bitstream)
                .withAction(Constants.READ)
                .withDescription("Test policy")
                .withName("administrator")
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, adminGroup)
                .withDspaceObject(bitstream)
                .withAction(Constants.WRITE)
                .withName("administrator")
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymousGroup)
                .withDspaceObject(bitstream)
                .withAction(Constants.READ)
                .withName("openaccess")
                .withPolicyType(ResourcePolicy.TYPE_INHERITED)
                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymousGroup)
                .withDspaceObject(bitstream)
                .withAction(Constants.READ)
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymousGroup)
                .withDspaceObject(bitstream)
                .withAction(Constants.READ)
                .withName("embargo")
                             .withStartDate(LocalDate.of(2025, 3, 25))
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, anonymousGroup)
                .withDspaceObject(bitstream)
                .withAction(Constants.READ)
                .withName("lease")
                .withDescription("Test")
                             .withEndDate(LocalDate.of(2025, 3, 25))
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(2));

        String firstItemId = firstItem.getID().toString();
        String bitstreamLocation = getBitstreamLocationUrl(bitstream);
        String expectedPolicies = "administrator$$Test policy||embargo$$2025-03-25||lease$$2025-03-25$$Test";

        String[] bitstreamHeaders = ArrayUtils.addAll(BITSTREAMS_SHEET_HEADERS, "dc.title", "dc.description");
        String[] firstRow = { firstItemId, bitstreamLocation, "TEST-BUNDLE", "1", expectedPolicies, "N",
                "test.txt"};

        List<String[]> rowList = new ArrayList<>();
        rowList.add(firstRow);

        asserThatSheetHas(workbook.getSheetAt(1), "bitstream-metadata", 2, bitstreamHeaders, rowList);
    }

    private InputStream getBitstreamSample(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    @SuppressWarnings("unchecked")
    private void asserThatSheetHas(Sheet sheet, String name, int rowsNumber, String[] header, List<String[]> rows) {
        assertThat(sheet.getSheetName(), equalTo(name));
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(rowsNumber));
        assertThat(getRowValues(sheet.getRow(0), header.length), contains(header));

        int rowCount = 1;
        Matcher<List<String>>[] rowMatchers = rows.stream()
            .map(Matchers::contains)
            .toArray(Matcher[]::new);

        for (String[] row : rows) {
            assertThat(getRowValues(sheet.getRow(rowCount++), row.length), anyOf(rowMatchers));
        }
    }

    private String getBitstreamLocationUrl(Bitstream bitstream) {
        String dspaceServerUrl = configurationService.getProperty("dspace.server.url");
        return String.format(BITSTREAM_URL_FORMAT, dspaceServerUrl, bitstream.getID().toString());
    }

}
