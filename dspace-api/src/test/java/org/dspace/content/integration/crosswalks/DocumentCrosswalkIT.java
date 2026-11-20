/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.DeleteOnCloseFileInputStream;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link DocumentCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DocumentCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_OUTPUT_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private static ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
        .getConfigurationService();

    private Community community;

    private Collection collection;

    private String[] configuredFontFamilies;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

        configuredFontFamilies = configurationService.getArrayProperty("crosswalk.fop.font-family");

        // required to read correctly the pdf content using PDFTextStripper
        configurationService.setProperty("crosswalk.fop.font-family", "Times");

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
    public void testPdfCrosswalkPersonDisseminateWithoutImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkPersonCvDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-cv-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> {
                assertThat(content, containsString("John Smith"));
                assertThat(content, containsString("School"));
                assertThat(content, containsString("First Qualification"));
                assertThat(content, containsString("First Publication"));
            });
        }

    }

    @Test
    public void testRtfCrosswalkPersonDisseminateWithoutImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-rtf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatRtfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithJpegImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, personItem)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithPngImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, personItem)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.png"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
        }

    }

    @Test
    public void testRtfCrosswalkPersonDisseminateWithJpegImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, personItem)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-rtf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatRtfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
            assertThatRtfHasJpegImage(out);
        }

    }

    @Test
    public void testRtfCrosswalkPersonDisseminateWithPngImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = buildPersonItem();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, personItem)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.png"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-rtf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatRtfHasContent(out, content -> assertThatPersonDocumentHasContent(content));
            assertThatRtfHasJpegImage(out);
        }

    }

    @Test
    public void testPdfCrosswalkPublicationDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withTitle("Test Project")
            .withInternalId("111-222-333")
            .withAcronym("TP")
            .withOairecerifProjectStartDate("2020-01-01")
            .withOairecerifProjectEndDate("2020-04-01")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test Funding")
            .withType("Internal Funding")
            .withFunder("Test Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Another Test Funding")
            .withType("Contract")
            .withFunder("Another Test Funder")
            .withAcronym("ATF-01")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withAlternativeTitle("Alternative publication title")
            .withRelationPublication("Published in publication")
            .withRelationDoi("doi:10.3972/test")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withLanguage("en")
            .withPublisher("Publication publisher")
            .withVolume("V.01")
            .withIssue("Issue")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project", project.getID().toString())
            .withRelationFunding("Another Test Funding", funding.getID().toString())
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "publication-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, publication, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatPublicationDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkProjectDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item coordinator = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Coordinator OrgUnit")
            .withAcronym("COU")
            .build();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withOpenaireId("44-55-66")
            .withUrlIdentifier("www.project.test")
            .withUrlIdentifier("www.test.project")
            .withOairecerifProjectStartDate("2020-01-01")
            .withOairecerifProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Coordinator OrgUnit", coordinator.getID().toString())
            .withProjectPartner("Partner OrgUnit")
            .withProjectPartner("Another Partner OrgUnit")
            .withProjectOrganization("First Member OrgUnit")
            .withProjectOrganization("Second Member OrgUnit")
            .withProjectOrganization("Third Member OrgUnit")
            .withCrispjProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test funding")
            .withType("Award")
            .withFunder("OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Another Test funding")
            .withType("Award")
            .withFunder("Another OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "project-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, project, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatProjectDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkOrgUnitDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item parent = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("POU")
            .withTitle("Parent OrgUnit")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit", parent.getID().toString())
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Walter White")
            .withPersonMainAffiliationName("Test OrgUnit", orgUnit.getID().toString())
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliationName("Test OrgUnit", orgUnit.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "orgUnit-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, orgUnit, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatOrgUnitDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkFundingDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "funding-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, funding, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> assertThatFundingDocumentHasContent(content));
        }

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithEmptyPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Test user")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> {
                assertThat(content, equalTo("Test user\n"));
            });
        }

    }

    @Test
    public void testPdfCrosswalkPersonWithPublicationsGroupedByType() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Test user")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Publication 1")
            .withType("article")
            .withIssueDate("2023")
            .withAuthor("Test User", personItem.getID().toString())
            .withAuthor("Walter White")
            .withHandle("123456789/111111")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Publication 2")
            .withType("book")
            .withIssueDate("2019-03-01")
            .withAuthor("Test User", personItem.getID().toString())
            .withHandle("123456789/222222")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Publication 3")
            .withType("chapter")
            .withIssueDate("2020-06-01")
            .withAuthor("John Smith")
            .withAuthor("Test User", personItem.getID().toString())
            .withHandle("123456789/333333")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Publication 4")
            .withType("article")
            .withIssueDate("2022-02-01")
            .withAuthor("Walter White")
            .withAuthor("Test User", personItem.getID().toString())
            .withHandle("123456789/444444")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Publication 5")
            .withType("book")
            .withIssueDate("2022-05-06")
            .withAuthor("Test User", personItem.getID().toString())
            .withHandle("123456789/555555")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Publication 6")
            .withIssueDate("2023-03-01")
            .withAuthor("Test User", personItem.getID().toString())
            .withAuthor("Another User")
            .withHandle("123456789/666666")
            .build();

        context.restoreAuthSystemState();

        DocumentCrosswalk documentCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("pdfCrosswalkPersonGroupPublicationsByType", DocumentCrosswalk.class);
        assertThat(documentCrosswalk, notNullValue());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            documentCrosswalk.disseminate(context, personItem, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasContent(out, content -> {
                String[] rows = Arrays.stream(content.split("\n"))
                                      .map(String::trim)
                                      .filter(s -> !s.isEmpty())
                                      .toArray(String[]::new);

                String[] expectedRows = {
                    "Publications",
                    "Article",
                    "Test User, & Walter White. (2023). Publication 1. http://localhost:4000/handle/123456789/111111",
                    "Walter White, & Test User. (2022). Publication 4. http://localhost:4000/handle/123456789/444444",
                    "Book",
                    "Test User. (2022). Publication 5. http://localhost:4000/handle/123456789/555555",
                    "Test User. (2019). Publication 2. http://localhost:4000/handle/123456789/222222",
                    "Chapter",
                    "John Smith, & Test User. (2020). Publication 3. http://localhost:4000/handle/123456789/333333",
                    "Other",
                    "Test User, & Another User. (2023, March 1). Publication 6. " +
                        "http://localhost:4000/handle/123456789/666666"
                };

                assertThat("Number of rows", rows.length, is(expectedRows.length));
                for (int i = 0; i < expectedRows.length; i++) {
                    assertThat("Row " + i, rows[i], is(expectedRows[i]));
                }
            });
        }

    }

    @Test
    public void testPdfCrosswalkPublicationDisseminateWithNotEnglishCharacters() throws Exception {

        configurationService.setProperty("crosswalk.fop.font-family", configuredFontFamilies);

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Političeskaja religija v Svjaščennoe")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "publication-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, publication, out);
            assertThat(out.toString(), not(isEmptyString()));
            String content = getPdfContent(out);
            assertThat(content, containsString("Političeskaja religija v Svjaščennoe"));
        }

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithNotEnglishCharacters() throws Exception {

        configurationService.setProperty("crosswalk.fop.font-family", configuredFontFamilies);

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Političeskaja religija v Svjaščennoe")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, person, out);
            assertThat(out.toString(), not(isEmptyString()));
            String content = getPdfContent(out);
            assertThat(content, containsString("Političeskaja religija v Svjaščennoe"));
        }

    }

    @Test
    public void testPdfCrosswalkOrgUnitDisseminateWithNotEnglishCharacters() throws Exception {

        configurationService.setProperty("crosswalk.fop.font-family", configuredFontFamilies);

        context.turnOffAuthorisationSystem();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Političeskaja religija v Svjaščennoe")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "orgUnit-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, orgUnit, out);
            assertThat(out.toString(), not(isEmptyString()));
            String content = getPdfContent(out);
            assertThat(content, containsString("Političeskaja religija v Svjaščennoe"));
        }

    }

    @Test
    public void testPdfCrosswalkProjectDisseminateWithNotEnglishCharacters() throws Exception {

        configurationService.setProperty("crosswalk.fop.font-family", configuredFontFamilies);

        context.turnOffAuthorisationSystem();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withTitle("Političeskaja religija v Svjaščennoe")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "project-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, project, out);
            assertThat(out.toString(), not(isEmptyString()));
            String content = getPdfContent(out);
            assertThat(content, containsString("Političeskaja religija v Svjaščennoe"));
        }

    }

    @Test
    public void testPdfCrosswalkFundingDisseminateWithNotEnglishCharacters() throws Exception {

        configurationService.setProperty("crosswalk.fop.font-family", configuredFontFamilies);

        context.turnOffAuthorisationSystem();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Političeskaja religija v Svjaščennoe")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "funding-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, funding, out);
            assertThat(out.toString(), not(isEmptyString()));
            String content = getPdfContent(out);
            assertThat(content, containsString("Političeskaja religija v Svjaščennoe"));
        }

    }

    private Item buildPersonItem() {
        Item item = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withFullName("John Smith")
            .withCrisrpVariantName("J.S.")
            .withCrisrpVariantName("Smith John")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withWorkingGroup("First work group")
            .withWorkingGroup("Second work group")
            .withPersonalSiteUrl("www.test.com")
            .withPersonalSiteTitle("Test")
            .withPersonalSiteUrl("www.john-smith.com")
            .withPersonalSiteTitle(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonalSiteUrl("www.site.com")
            .withPersonalSiteTitle("Site")
            .withPersonEmail("test@test.com")
            .withSubject("Science")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withScopusAuthorIdentifier("111-222-333")
            .withScopusAuthorIdentifier("444-555-666")
            .withPersonAffiliation("University")
            .withPersonAffiliationStartDate("2020-01-02")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Researcher")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2015-01-01")
            .withPersonAffiliationEndDate("2020-01-01")
            .withPersonAffiliationRole("Developer")
            .withDescriptionAbstract(getBiography())
            .withCrisrpCountry("England")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .withPersonQualification("First Qualification")
            .withPersonQualificationStartDate("2015-01-01")
            .withPersonQualificationEndDate("2016-01-01")
            .withPersonQualification("Second Qualification")
            .withPersonQualificationStartDate("2016-01-02")
            .withPersonQualificationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();
        return item;
    }

    private String getBiography() {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut "
            + "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris "
            + "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit "
            + "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in "
            + "culpa qui officia deserunt mollit anim id est laborum.Lorem ipsum dolor sit amet, consectetur "
            + "adipiscing elit, sed do eiusmod tempor incididunt ut "
            + "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris "
            + "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit "
            + "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in "
            + "culpa qui officia deserunt mollit anim id est laborum.";
    }

    private void assertThatRtfHasContent(ByteArrayOutputStream out, Consumer<String> assertConsumer)
        throws IOException, BadLocationException {
        RTFEditorKit rtfParser = new RTFEditorKit();
        Document document = rtfParser.createDefaultDocument();
        rtfParser.read(new ByteArrayInputStream(out.toByteArray()), document, 0);
        String content = document.getText(0, document.getLength());
        assertConsumer.accept(content);
    }

    private void assertThatRtfHasJpegImage(ByteArrayOutputStream out) {
        assertThat(out.toString(), containsString("\\jpegblip"));
    }

    private void assertThatPdfHasContent(ByteArrayOutputStream out, Consumer<String> assertConsumer) {
        assertConsumer.accept(getPdfContent(out));
    }

    private String getPdfContent(ByteArrayOutputStream out) {
        try {
            PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(createTempFile(out.toByteArray())));
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileInputStream createTempFile(byte[] content) throws IOException {
        File tempFile = File.createTempFile("test-document-" + UUID.randomUUID(), "temp");
        tempFile.deleteOnExit();
        FileUtils.writeByteArrayToFile(tempFile, content);
        return new DeleteOnCloseFileInputStream(tempFile);
    }

    private void assertThatPersonDocumentHasContent(String content) {
        assertThat(content, containsString("John Smith"));
        assertThat(content, containsString("Researcher at University"));

        assertThat(content, containsString("Birth Date: 1992-06-26"));
        assertThat(content, containsString("Gender: M"));
        assertThat(content, containsString("Country: England"));
        assertThat(content, containsString("Email: test@test.com"));
        assertThat(content, containsString("ORCID: 0000-0002-9079-5932"));
        assertThat(content, containsString("Scopus Author IDs: 111-222-333, 444-555-666"));
        assertThat(content, containsString("Lorem ipsum dolor sit amet"));

        assertThat(content, containsString("Affiliations"));
        assertThat(content, containsString("Researcher at University from 2020-01-02"));
        assertThat(content, containsString("Developer at Company from 2015-01-01 to 2020-01-01"));

        assertThat(content, containsString("Education"));
        assertThat(content, containsString("Student at School from 2000-01-01 to 2005-01-01"));

        assertThat(content, containsString("Qualifications"));
        assertThat(content, containsString("First Qualification from 2015-01-01 to 2016-01-01"));
        assertThat(content, containsString("Second Qualification from 2016-01-02"));

        assertThat(content, containsString("Publications"));
        assertThat(content, containsString("John Smith and Walter White (2020-01-01). First Publication"));
        assertThat(content, containsString("John Smith (2020-04-01). Second Publication"));

        assertThat(content, containsString("Other informations"));
        assertThat(content, containsString("Working groups: First work group, Second work group"));
        assertThat(content, containsString("Interests: Science"));
        assertThat(content, containsString("Knows languages: English, Italian"));
        assertThat(content, containsString("Personal sites: www.test.com ( Test ) , www.john-smith.com , "
            + "www.site.com ( Site )"));
    }

    private void assertThatPublicationDocumentHasContent(String content) {
        assertThat(content, containsString("Test Publication"));

        assertThat(content, containsString("Publication basic information"));
        assertThat(content, containsString("Other titles: Alternative publication title"));
        assertThat(content, containsString("Publication date: 2020-01-01"));
        assertThat(content, containsString("DOI: doi:111.111/publication"));
        assertThat(content, containsString("ISBN: 978-3-16-148410-0"));
        assertThat(content, containsString("ISI number: 111-222-333"));
        assertThat(content, containsString("SCP number: 99999999"));
        assertThat(content, containsString("Authors: John Smith and Walter White ( Company )"));
        assertThat(content, containsString("Editors: Editor ( Editor Affiliation )"));
        assertThat(content, containsString("Keywords: test, export"));
        assertThat(content, containsString("Type: http://purl.org/coar/resource_type/c_efa0"));

        assertThat(content, containsString("Publication bibliographic details"));
        assertThat(content, containsString("Published in: Published in publication"));
        assertThat(content, containsString("ISSN: 2049-3630"));
        assertThat(content, containsString("Volume: V.01"));
        assertThat(content, containsString("Issue: Issue"));

        assertThat(content, containsString("Projects"));
        assertThat(content, containsString("Test Project ( TP ) - from 2020-01-01 to 2020-04-01"));

        assertThat(content, containsString("Fundings"));
        assertThat(content, containsString("Another Test Funding ( ATF-01 ) - Funder: Another Test Funder"));
    }

    private void assertThatProjectDocumentHasContent(String content) {
        assertThat(content, containsString("Test Project"));
        assertThat(content, containsString("This is a project to test the export"));

        assertThat(content, containsString("Basic informations"));
        assertThat(content, containsString("Project Acronym: TP"));
        assertThat(content, containsString("OpenAIRE id(s): 11-22-33, 44-55-66"));
        assertThat(content, containsString("URL(s): www.project.test, www.test.project"));
        assertThat(content, containsString("Start date: 2020-01-01"));
        assertThat(content, containsString("End date: 2020-12-31"));
        assertThat(content, containsString("Status: OPEN"));

        assertThat(content, containsString("Consortium"));
        assertThat(content, containsString("Consortium Coordinator(s): Coordinator OrgUnit"));
        assertThat(content, containsString("Partner Organization(s): Partner OrgUnit, Another Partner OrgUnit"));
        assertThat(content, containsString("Participant Organization(s): First Member OrgUnit, "
            + "Second Member OrgUnit, Third Member OrgUnit"));

        assertThat(content, containsString("Team"));
        assertThat(content, containsString("Project Coordinator: Investigator"));
        assertThat(content, containsString("Co-Investigator(s): First coinvestigator, Second coinvestigator"));

        assertThat(content, containsString("Other informations"));
        assertThat(content, containsString("Uses equipment(s): Test equipment"));
        assertThat(content, containsString("Keyword(s): project, test"));
        assertThat(content, containsString("OA Mandate: true"));
        assertThat(content, containsString("OA Policy URL: oamandate-url"));

    }

    private void assertThatOrgUnitDocumentHasContent(String content) {
        assertThat(content, containsString("Test OrgUnit"));

        assertThat(content, containsString("Basic informations"));
        assertThat(content, containsString("Acronym: TOU"));
        assertThat(content, containsString("Type: https://w3id.org/cerif/vocab/OrganisationTypes"
            + "#StrategicResearchInsitute"));
        assertThat(content, containsString("Parent Organization: Parent OrgUnit"));
        assertThat(content, containsString("Identifier(s): ID-01, ID-02"));
        assertThat(content, containsString("URL(s): www.orgUnit.com, www.orgUnit.it"));
        assertThat(content, anyOf(
            containsString("People: Walter White, Jesse Pinkman"),
            containsString("People: Jesse Pinkman, Walter White")));
    }

    private void assertThatFundingDocumentHasContent(String content) {
        assertThat(content, containsString("Test Funding"));
        assertThat(content, containsString("Funding to test export"));

        assertThat(content, containsString("Basic informations"));
        assertThat(content, containsString("Acronym: T-FU"));
        assertThat(content, containsString("Type: https://www.openaire.eu/cerif-profile/vocab/"
            + "OpenAIRE_Funding_Types#Gift"));
        assertThat(content, containsString("Funding Code: ID-01"));
        assertThat(content, containsString("Grant Number: 0001"));
        assertThat(content, containsString("Amount: 30.000,00 (EUR)"));
        assertThat(content, containsString("Funder: OrgUnit Funder"));
        assertThat(content, containsString("Duration: from 2015-01-01 to 2020-01-01"));
        assertThat(content, containsString("OA Mandate: true"));
        assertThat(content, containsString("OA Policy URL: www.mandate.url"));
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }
}
