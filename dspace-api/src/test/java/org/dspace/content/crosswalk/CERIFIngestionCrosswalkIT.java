/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CERIFIngestionCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CERIFIngestionCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String CROSSWALK_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private static final String OAI_PMH_DIR_PATH = "./target/testing/dspace/assetstore/oai-pmh/cerif/";

    private static final String METADATA_PLACEHOLDER = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

    private Community community;

    private Collection collection;

    private CERIFIngestionCrosswalk crosswalk;

    private SAXBuilder builder = new SAXBuilder();

    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    @Before
    public void setup() throws Exception {

        crosswalk = (CERIFIngestionCrosswalk) pluginService.getNamedPlugin(IngestionCrosswalk.class, "cerif");
        assertThat("A CERIF ingestion crosswalk should be configured", crosswalk, notNullValue());
        crosswalk.setIdPrefix("repository-id::");

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPublicationIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Publication").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-publication.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(22));

        assertThat(values, hasItems(with("dc.type",
            "Controlled Vocabulary for Resource Type Genres::text::conference object::conference proceedings"
                + "::conference paper")));

        assertThat(values, hasItems(with("dc.title", "Metadata and Semantics Research")));

        assertThat(values, hasItems(with("dc.title.alternative",
            "6th Research Conference, MTSR 2012, Cádiz, Spain, November 28-30, 2012. Proceedings")));

        assertThat(values, hasItems(with("dc.relation.ispartof",
            "The International Journal of Digital Curation")));

        assertThat(values, hasItems(with("dc.date.issued", "2020-03-30")));
        assertThat(values, hasItems(with("oaire.citation.startPage", "10")));
        assertThat(values, hasItems(with("oaire.citation.endPage", "20")));
        assertThat(values, hasItems(with("dc.identifier.doi", "10.1007/978-3-642-35233-1")));
        assertThat(values, hasItems(with("dc.identifier.isbn", "9783642352324")));
        assertThat(values, hasItems(with("dc.publisher", "Springer, Berlin, Heidelberg")));
        assertThat(values, hasItems(with("dc.subject", "cultural heritage")));
        assertThat(values, hasItems(with("dc.subject", "digital libraries", 1)));
        assertThat(values, hasItems(with("dc.subject", "learning objects", 2)));
        assertThat(values, hasItems(with("dc.subject", "linked open data", 3)));
        assertThat(values, hasItems(with("dc.subject", "scholarly publications", 4)));

        assertThat(values, hasItems(with("dc.relation.project", "2nd-Generation Open Access Infrastructure", null,
            "will be generated::repository-id::e9ed438e-c7f7-4a18-95e5-3f635ea65fee", 0, 500)));

        assertThat(values, hasItems(with("dc.relation.conference",
            "6th Research Conference on Metadata and Semantics Research")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAnotherPublicationIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Publication").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-publication-2.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(32));

        assertThat(values, hasItems(with("dc.type",
            "Controlled Vocabulary for Resource Type Genres::text::periodical::journal::contribution to journal"
                + "::journal article")));

        assertThat(values, hasItems(with("dc.title", "Durability of thermally modified sapwood and heartwood of "
            + "Scots pine and Norway spruce in the modified double layer test")));

        assertThat(values, hasItems(with("dc.language.iso", "English")));
        assertThat(values, hasItems(with("dc.relation.publication", "Wood Material Science and Engineering")));
        assertThat(values, hasItems(with("dc.date.issued", "2017-05-27")));
        assertThat(values, hasItems(with("oaire.citation.volume", "12")));
        assertThat(values, hasItems(with("oaire.citation.issue", "3")));
        assertThat(values, hasItems(with("oaire.citation.startPage", "129")));
        assertThat(values, hasItems(with("oaire.citation.endPage", "139")));
        assertThat(values, hasItems(with("dc.identifier.doi", "10.1080/17480272.2015.1061596")));
        assertThat(values, hasItems(with("dc.identifier.issn", "1748-0272", 0)));
        assertThat(values, hasItems(with("dc.identifier.issn", "1748-0280", 1)));
        assertThat(values, hasItems(with("dc.identifier.scopus", "84941368060")));
        assertThat(values, hasItems(with("dc.subject", "biological durability")));
        assertThat(values, hasItems(with("dc.subject", "decay resistance", 1)));
        assertThat(values, hasItems(with("dc.subject", "ground contact", 2)));
        assertThat(values, hasItems(with("dc.subject", "heartwood", 3)));
        assertThat(values, hasItems(with("dc.subject", "Norway spruce", 4)));
        assertThat(values, hasItems(with("dc.subject", "sapwood", 5)));
        assertThat(values, hasItems(with("dc.subject", "Scots pine", 6)));
        assertThat(values, hasItems(with("dc.subject", "thermal modification", 7)));
        assertThat(values, hasItems(with("dc.publisher", "Taylor & Francis")));
        assertThat(values, hasItems(with("dc.description.abstract", "In the present study, durability of untreated and "
            + "thermally modified sapwood and heartwood of Scots pine and Norway spruce was examined.")));

        assertThat(values, hasItems(with("dc.contributor.author", "Metsä-Kortelainen, Sini", null,
            "will be generated::repository-id::0d9dfbba-44a4-480d-a3f5-7690adfd7d10", 0, 500)));

        assertThat(values, hasItems(with("oairecerif.author.affiliation", "BA2405 Advanced manufacturing technologies",
            null, "will be generated::repository-id::e8d60e26-7264-4925-92d0-a781657f9d3a", 0, 500)));

        assertThat(values, hasItems(with("dc.contributor.author", "Viitanen, Hannu", null,
            "will be generated::repository-id::ea98bc38-dcba-41f0-a08f-d6b73fd6c431", 1, 500)));

        assertThat(values, hasItems(with("oairecerif.author.affiliation", "VTT (former employee or external)",
            null, "will be generated::repository-id::6f450daa-5b6d-4d18-926d-e1bbe3314fb9", 1, 500)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExportPublicationIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Publication").build();
        context.restoreAuthSystemState();

        Document document = readDocument(CROSSWALK_DIR_PATH, "publication.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(32));

        assertThat(values, hasItems(
            with("dc.type", "Controlled Vocabulary for Resource Type Genres::text::review")));

        assertThat(values, hasItems(with("dc.language.iso", "en")));
        assertThat(values, hasItems(with("dc.title", "Test Publication")));
        assertThat(values, hasItems(with("dc.title.alternative", "Alternative publication title")));
        assertThat(values, hasItems(with("dc.relation.publication", "Published in publication")));
        assertThat(values, hasItems(with("dc.relation.doi", "doi:10.3972/test")));
        assertThat(values, hasItems(with("dc.publisher", "Publication publisher")));
        assertThat(values, hasItems(with("dc.date.issued", "2020-01-01")));
        assertThat(values, hasItems(with("oaire.citation.volume", "V.01")));
        assertThat(values, hasItems(with("oaire.citation.issue", "Issue")));
        assertThat(values, hasItems(with("dc.identifier.doi", "doi:111.111/publication")));
        assertThat(values, hasItems(with("dc.identifier.isbn", "978-3-16-148410-0")));
        assertThat(values, hasItems(with("dc.identifier.issn", "2049-3630")));
        assertThat(values, hasItems(with("dc.identifier.isi", "111-222-333")));
        assertThat(values, hasItems(with("dc.identifier.scopus", "99999999")));
        assertThat(values, hasItems(with("dc.subject", "test")));
        assertThat(values, hasItems(with("dc.subject", "export", 1)));

        assertThat(values, hasItems(with("dc.contributor.author", "John Smith")));
        assertThat(values, hasItems(with("oairecerif.author.affiliation", METADATA_PLACEHOLDER)));

        assertThat(values, hasItems(with("dc.contributor.author", "Walter White", null,
            "will be generated::repository-id::6c36b2b0-b2cf-41a5-8241-11d0ea56ed97", 1, 500)));

        assertThat(values, hasItems(with("oairecerif.author.affiliation", "Company", 1)));

        assertThat(values, hasItems(with("dc.contributor.editor", "Editor", null,
            "will be generated::repository-id::25887329-a648-46f9-a2ac-99319b8e9766", 0, 500)));

        assertThat(values, hasItems(with("oairecerif.editor.affiliation", "Editor Affiliation")));

        assertThat(values, hasItems(with("dc.relation.project", "Test Project", null,
            "will be generated::repository-id::mock-id", 0, 500)));

        assertThat(values, hasItems(with("dc.relation.funding", "Another Test Funding", null,
            "will be generated::repository-id::mock-id", 0, 500)));

        assertThat(values, hasItems(with("dc.relation.conference", "The best Conference")));
        assertThat(values, hasItems(with("dc.relation.dataset", "DataSet")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExportPersonIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Person").build();
        context.restoreAuthSystemState();

        Document document = readDocument(CROSSWALK_DIR_PATH, "person-cerif.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();

        assertThat(values, hasSize(25));
        assertThat(values, hasItems(with("dc.title", "Smith, John")));
        assertThat(values, hasItems(with("person.givenName", "John")));
        assertThat(values, hasItems(with("person.familyName", "Smith")));
        assertThat(values, hasItems(with("crisrp.name.variant", "J.S.")));
        assertThat(values, hasItems(with("crisrp.name.variant", "Smith John", 1)));
        assertThat(values, hasItems(with("oairecerif.person.gender", "M")));
        assertThat(values, hasItems(with("person.identifier.orcid", "0000-0002-9079-5932")));
        assertThat(values, hasItems(with("person.identifier.rid", "R-01")));
        assertThat(values, hasItems(with("person.identifier.rid", "R-02", 1)));
        assertThat(values, hasItems(with("person.identifier.scopus-author-id", "SA-01")));
        assertThat(values, hasItems(with("person.email", "test@test.com")));
        assertThat(values, hasItems(with("oairecerif.person.affiliation", "Company")));
        assertThat(values, hasItems(with("oairecerif.affiliation.startDate", "2018-01-01")));
        assertThat(values, hasItems(with("oairecerif.affiliation.endDate", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(values, hasItems(with("oairecerif.affiliation.role", "Developer")));
        assertThat(values, hasItems(with("oairecerif.person.affiliation", "Another Company", 1)));
        assertThat(values, hasItems(with("oairecerif.affiliation.startDate", "2017-01-01", 1)));
        assertThat(values, hasItems(with("oairecerif.affiliation.endDate", "2017-12-31", 1)));
        assertThat(values, hasItems(with("oairecerif.affiliation.role", "Developer", 1)));
        assertThat(values, hasItems(with("person.affiliation.name", "University")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPersonIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Person").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-person.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();

        assertThat(values, hasSize(8));
        assertThat(values, hasItems(with("dc.title", "Li-Shiuan, Peh")));
        assertThat(values, hasItems(with("person.givenName", "Peh")));
        assertThat(values, hasItems(with("person.familyName", "Li-Shiuan")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProjectIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Project").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-project.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(9));

        assertThat(values, hasItems(with("dc.title", "GlobalSeaRoutes")));
        assertThat(values, hasItems(with("oairecerif.project.startDate", "2013-08-01")));
        assertThat(values, hasItems(with("oairecerif.project.endDate", "2016-07-31")));

        assertThat(values, hasItems(with("dc.description.abstract",
            "GlobalSeaRoutes (GSR) is a relational geospatial database")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExportProjectIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Project").build();
        context.restoreAuthSystemState();

        Document document = readDocument(CROSSWALK_DIR_PATH, "project.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(24));

        assertThat(values, hasItems(with("dc.title", "Test Project")));
        assertThat(values, hasItems(with("oairecerif.acronym", "TP")));
        assertThat(values, hasItems(with("crispj.openaireid", "11-22-33")));
        assertThat(values, hasItems(with("oairecerif.identifier.url", "www.project.test")));
        assertThat(values, hasItems(with("oairecerif.project.startDate", "2020-01-01")));
        assertThat(values, hasItems(with("oairecerif.project.endDate", "2020-12-31")));
        assertThat(values, hasItems(with("oairecerif.project.status", "OPEN")));

        assertThat(values, hasItems(with("crispj.coordinator", "Coordinator OrgUnit", null,
            "will be generated::repository-id::mock-id", 0, 500)));

        assertThat(values, hasItems(with("crispj.partnerou", "Partner OrgUnit")));
        assertThat(values, hasItems(with("crispj.organization", "Member OrgUnit")));
        assertThat(values, hasItems(with("crispj.investigator", "Investigator")));
        assertThat(values, hasItems(with("crispj.coinvestigators", "First coinvestigator", 0)));
        assertThat(values, hasItems(with("crispj.coinvestigators", "Second coinvestigator", 1)));
        assertThat(values, hasItems(with("dc.relation.equipment", "Test equipment")));
        assertThat(values, hasItems(with("dc.subject", "project")));
        assertThat(values, hasItems(with("dc.subject", "test", 1)));
        assertThat(values, hasItems(with("dc.description.abstract", "This is a project to test the export")));
        assertThat(values, hasItems(with("oairecerif.oamandate", "true")));
        assertThat(values, hasItems(with("oairecerif.oamandate.url", "oamandate-url")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOrgUnitIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("OrgUnit").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-orgUnit.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(6));
        assertThat(values, hasItems(with("dc.title", "Institute of Applied Biosciences (INAB)")));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExportOrgUnitIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("OrgUnit").build();
        context.restoreAuthSystemState();

        Document document = readDocument(CROSSWALK_DIR_PATH, "orgUnit.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(14));
        assertThat(values, hasItems(with("dc.title", "Test OrgUnit")));
        assertThat(values, hasItems(with("organization.legalName", "Test OrgUnit LegalName")));
        assertThat(values, hasItems(with("oairecerif.acronym", "TOU")));
        assertThat(values, hasItems(with("dc.type", "Strategic Research Insitute")));

        assertThat(values, hasItems(with("organization.parentOrganization", "Parent OrgUnit", null,
            "will be generated::repository-id::mock-id", 0, 500)));

        assertThat(values, hasItems(with("organization.identifier", "ID-01", 0)));
        assertThat(values, hasItems(with("organization.identifier", "ID-02", 1)));
        assertThat(values, hasItems(with("oairecerif.identifier.url", "www.orgUnit.com", 0)));
        assertThat(values, hasItems(with("oairecerif.identifier.url", "www.orgUnit.it", 1)));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEquipmentIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Equipment").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-equipment.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(8));
        assertThat(values, hasItems(with("dc.title", "Microflown Scan&Paint")));
        assertThat(values, hasItems(with("dc.description", "A unique tool for acoustic trouble shooting "
            + "and sound source localization")));
        assertThat(values, hasItems(with("crisequipment.ownerou", "BA2E09 Machinery noise")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExportEquipmentIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Equipment").build();
        context.restoreAuthSystemState();

        Document document = readDocument(CROSSWALK_DIR_PATH, "equipment.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(11));
        assertThat(values, hasItems(with("dc.title", "Test Equipment")));
        assertThat(values, hasItems(with("oairecerif.acronym", "T-EQ")));
        assertThat(values, hasItems(with("oairecerif.internalid", "ID-01")));
        assertThat(values, hasItems(with("dc.description", "This is an equipment to test the export functionality")));
        assertThat(values, hasItems(with("crisequipment.ownerou", "Test OrgUnit")));
        assertThat(values, hasItems(with("crisequipment.ownerrp", "Walter White")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExportFundingIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Funding").build();
        context.restoreAuthSystemState();

        Document document = readDocument(CROSSWALK_DIR_PATH, "funding.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(18));
        assertThat(values, hasItems(with("dc.type", "Gift")));
        assertThat(values, hasItems(with("dc.title", "Test Funding")));
        assertThat(values, hasItems(with("oairecerif.acronym", "T-FU")));
        assertThat(values, hasItems(with("oairecerif.internalid", "ID-01")));
        assertThat(values, hasItems(with("oairecerif.funding.identifier", "0001")));
        assertThat(values, hasItems(with("oairecerif.amount", "30.000,00")));
        assertThat(values, hasItems(with("oairecerif.amount.currency", "EUR")));
        assertThat(values, hasItems(with("dc.description", "Funding to test export")));
        assertThat(values, hasItems(with("oairecerif.funder", "OrgUnit Funder")));
        assertThat(values, hasItems(with("oairecerif.funding.startDate", "2015-01-01")));
        assertThat(values, hasItems(with("oairecerif.funding.endDate", "2020-01-01")));
        assertThat(values, hasItems(with("oairecerif.oamandate", "true")));
        assertThat(values, hasItems(with("oairecerif.oamandate.url", "www.mandate.url")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIngestWithPreTransformation() throws Exception {

        crosswalk.setPreTransformXsl(OAI_PMH_DIR_PATH + "preTransformation.xsl");

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Equipment").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-equipment.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(9));
        assertThat(values, hasItems(with("dc.title", "Microflown Scan&Paint")));
        assertThat(values, hasItems(with("dc.description", "A unique tool for acoustic trouble shooting "
            + "and sound source localization")));
        assertThat(values, hasItems(with("crisequipment.ownerou", "BA2E09 Machinery noise")));
        assertThat(values, hasItems(with("oairecerif.internalid", "test-id")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIngestWithPostTransformation() throws Exception {

        crosswalk.setPostTransformXsl(OAI_PMH_DIR_PATH + "postTransformation.xsl");

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Equipment").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-equipment.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(8));
        assertThat(values, hasItems(with("dc.title", "MICROFLOWN SCAN&PAINT")));
        assertThat(values, hasItems(with("dc.description", "A UNIQUE TOOL FOR ACOUSTIC TROUBLE SHOOTING "
            + "AND SOUND SOURCE LOCALIZATION")));
        assertThat(values, hasItems(with("crisequipment.ownerou", "BA2E09 MACHINERY NOISE")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIngestWithPreAndPostTransformation() throws Exception {

        crosswalk.setPreTransformXsl(OAI_PMH_DIR_PATH + "preTransformation.xsl");
        crosswalk.setPostTransformXsl(OAI_PMH_DIR_PATH + "postTransformation.xsl");

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withRelationshipType("Equipment").build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-equipment.xml");
        crosswalk.ingest(context, item, document.getRootElement(), false);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasSize(9));
        assertThat(values, hasItems(with("dc.title", "MICROFLOWN SCAN&PAINT")));
        assertThat(values, hasItems(with("dc.description", "A UNIQUE TOOL FOR ACOUSTIC TROUBLE SHOOTING "
            + "AND SOUND SOURCE LOCALIZATION")));
        assertThat(values, hasItems(with("crisequipment.ownerou", "BA2E09 MACHINERY NOISE")));
        assertThat(values, hasItems(with("oairecerif.internalid", "TEST-ID")));
    }

    private Document readDocument(String dir, String name) throws Exception {
        try (InputStream inputStream = new FileInputStream(new File(dir, name))) {
            return builder.build(inputStream);
        }
    }
}
