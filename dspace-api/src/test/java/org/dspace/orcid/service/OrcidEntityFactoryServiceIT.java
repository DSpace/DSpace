/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service;

import static org.apache.commons.lang.StringUtils.endsWith;
import static org.dspace.app.matcher.LambdaMatcher.has;
import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.orcid.jaxb.model.common.ContributorRole.AUTHOR;
import static org.orcid.jaxb.model.common.ContributorRole.EDITOR;
import static org.orcid.jaxb.model.common.FundingContributorRole.LEAD;
import static org.orcid.jaxb.model.common.SequenceType.ADDITIONAL;
import static org.orcid.jaxb.model.common.SequenceType.FIRST;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.orcid.factory.OrcidServiceFactory;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.common.ContributorRole;
import org.orcid.jaxb.model.common.FundingContributorRole;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.common.SequenceType;
import org.orcid.jaxb.model.common.WorkType;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.common.ContributorEmail;
import org.orcid.jaxb.model.v3.release.common.ContributorOrcid;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.Activity;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.FundingContributor;
import org.orcid.jaxb.model.v3.release.record.FundingContributors;
import org.orcid.jaxb.model.v3.release.record.Work;

/**
 * Integration tests for {@link OrcidEntityFactoryService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidEntityFactoryServiceIT extends AbstractIntegrationTestWithDatabase {

    private OrcidEntityFactoryService entityFactoryService;

    private Collection orgUnits;

    private Collection patents;

    private Collection publications;

    private Collection products;

    private Collection fundings;

    private Collection persons;

    private Collection projects;

    @Before
    public void setup() {

        entityFactoryService = OrcidServiceFactory.getInstance().getOrcidEntityFactoryService();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withTitle("Parent community")
            .build();

        orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("OrgUnit")
            .build();

        patents = CollectionBuilder.createCollection(context, parentCommunity, "123456789/test-patent")
            .withName("Collection")
            .withEntityType("Patent")
            .build();

        publications = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("Publication")
            .build();

        products = CollectionBuilder.createCollection(context, parentCommunity, "123456789/test-product")
            .withName("Collection")
            .withEntityType("Product")
            .build();

        projects = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("Project")
            .build();

        fundings = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Funding")
                .build();

        persons = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Person")
                .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testWorkCreation() {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publications)
            .withTitle("Test publication")
            .withAuthor("Walter White")
            .withAuthor("Jesse Pinkman")
            .withEditor("Editor")
            .withIssueDate("2021-04-30")
            .withDescriptionAbstract("Publication description")
            .withLanguage("en_US")
            .withType("Book")
            .withIsPartOf("Journal")
            .withDoiIdentifier("doi-id")
            .withScopusIdentifier("scopus-id")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, publication);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;
        assertThat(work.getJournalTitle(), notNullValue());
        assertThat(work.getJournalTitle().getContent(), is("Journal"));
        assertThat(work.getLanguageCode(), is("en"));
        assertThat(work.getPublicationDate(), matches(date("2021", "04", "30")));
        assertThat(work.getShortDescription(), is("Publication description"));
        assertThat(work.getPutCode(), nullValue());
        assertThat(work.getWorkType(), is(WorkType.BOOK));
        assertThat(work.getWorkTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle().getContent(), is("Test publication"));
        assertThat(work.getWorkContributors(), notNullValue());
        assertThat(work.getUrl(), matches(urlEndsWith(publication.getHandle())));

        List<Contributor> contributors = work.getWorkContributors().getContributor();
        assertThat(contributors, hasSize(3));
        //tornano 3 ma sono null
        assertThat(contributors, has(contributor("Walter White", AUTHOR, FIRST)));
        assertThat(contributors, has(contributor("Editor", EDITOR, FIRST)));
        assertThat(contributors, has(contributor("Jesse Pinkman", AUTHOR, ADDITIONAL)));

        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(3));
        assertThat(externalIds, has(selfExternalId("doi", "doi-id")));
        assertThat(externalIds, has(selfExternalId("eid", "scopus-id")));
        assertThat(externalIds, has(selfExternalId("handle", publication.getHandle())));

    }

    @Test
    public void testProductWorkCreation() {

        context.turnOffAuthorisationSystem();


        Item product = ItemBuilder.createItem(context, products)
            .withTitle("Test dataset")
            .withEditor("Editor")
            .withIssueDate("2021-04-30")
            .withDescriptionAbstract("Product description")
            .withLanguage("en_US")
            .withType("http://purl.org/coar/resource_type/c_ddb1")
            .withIsPartOf("Collection of Products")
            .withDoiIdentifier("doi-id")
            .withScopusIdentifier("scopus-id")
            .build();

        Item person = ItemBuilder.createItem(context, persons)
                .withTitle("Walter White")
                .withIssueDate("2017-10-17")
                .withAuthor("Walter, White")
                .withPersonIdentifierLastName("Walter")
                .withPersonIdentifierFirstName("White")
                .build();
        Item person2 = ItemBuilder.createItem(context, persons)
                .withTitle("Jesse Pinkman")
                .withIssueDate("2017-10-17")
                .withAuthor("Jesse, Pinkman")
                .withPersonIdentifierLastName("Jesse")
                .withPersonIdentifierFirstName("Pinkman")
                .build();

        EntityType productType = EntityTypeBuilder.createEntityTypeBuilder(context, "Product").build();
        EntityType personType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        RelationshipType isCreatorOfProduct = createRelationshipTypeBuilder(context, productType, personType,
                "isCreatorOfProduct", "isProductOfCreator", 0, null, 0, null).build();

        RelationshipBuilder.createRelationshipBuilder(context, product, person, isCreatorOfProduct).build();
        RelationshipBuilder.createRelationshipBuilder(context, product, person2, isCreatorOfProduct).build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, product);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;
        assertThat(work.getJournalTitle(), notNullValue());
        assertThat(work.getJournalTitle().getContent(), is("Collection of Products"));
        assertThat(work.getLanguageCode(), is("en"));
        assertThat(work.getPublicationDate(), matches(date("2021", "04", "30")));
        assertThat(work.getShortDescription(), is("Product description"));
        assertThat(work.getPutCode(), nullValue());
        // assertThat(work.getWorkCitation(), notNullValue());
        // assertThat(work.getWorkCitation().getCitation(), containsString("Test product"));
        assertThat(work.getWorkType(), is(WorkType.DATA_SET));
        assertThat(work.getWorkTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle().getContent(), is("Test dataset"));
        assertThat(work.getWorkContributors(), notNullValue());
        assertThat(work.getUrl(), matches(urlEndsWith(product.getHandle())));

        List<Contributor> contributors = work.getWorkContributors().getContributor();
        assertThat(contributors, hasSize(2));
        assertThat(contributors, has(contributor("Walter, White", AUTHOR, FIRST)));
        // assertThat(contributors, has(contributor("Editor", EDITOR, FIRST)));
        assertThat(contributors, has(contributor("Jesse, Pinkman", AUTHOR, ADDITIONAL)));

        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(3));
        assertThat(externalIds, has(selfExternalId("doi", "doi-id")));
        assertThat(externalIds, has(selfExternalId("eid", "scopus-id")));
        assertThat(externalIds, has(selfExternalId("handle", product.getHandle())));

    }

    @Test
    public void testPatentWorkCreation() {

        context.turnOffAuthorisationSystem();

        Item patent = ItemBuilder.createItem(context, patents)
            .withTitle("Test patent")
            .withIssueDate("2021-04-30")
            .withDescriptionAbstract("Patent description")
            .withPublisher("Patent registration office")
            .withLanguage("en_US")
            .withPatentNo("2021.01.0111")
            .withType("http://purl.org/coar/resource_type/c_15cd")
            .build();

        Item person = ItemBuilder.createItem(context, persons)
                .withTitle("Walter White")
                .withIssueDate("2017-10-17")
                .withAuthor("Walter, White")
                .withPersonIdentifierLastName("Walter")
                .withPersonIdentifierFirstName("White")
                .build();
        Item person2 = ItemBuilder.createItem(context, persons)
                .withTitle("Jesse Pinkman")
                .withIssueDate("2017-10-17")
                .withAuthor("Jesse, Pinkman")
                .withPersonIdentifierLastName("Jesse")
                .withPersonIdentifierFirstName("Pinkman")
                .build();

        EntityType patentType = EntityTypeBuilder.createEntityTypeBuilder(context, "Patent").build();
        EntityType personType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        RelationshipType isInventorOfPatent = createRelationshipTypeBuilder(context, patentType, personType,
                "isInventorOfPatent", "isPatentOfInventor", 0, null, 0, null).build();

        RelationshipBuilder.createRelationshipBuilder(context, patent, person, isInventorOfPatent).build();
        RelationshipBuilder.createRelationshipBuilder(context, patent, person2, isInventorOfPatent).build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, patent);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;
        assertThat(work.getJournalTitle(), notNullValue());
        assertThat(work.getJournalTitle().getContent(), is("Patent registration office"));
        assertThat(work.getLanguageCode(), is("en"));
        assertThat(work.getPublicationDate(), matches(date("2021", "04", "30")));
        assertThat(work.getShortDescription(), is("Patent description"));
        assertThat(work.getPutCode(), nullValue());
        assertThat(work.getWorkType(), is(WorkType.PATENT));
        assertThat(work.getWorkTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle().getContent(), is("Test patent"));
        assertThat(work.getWorkContributors(), notNullValue());
        assertThat(work.getUrl(), matches(urlEndsWith(patent.getHandle())));

        List<Contributor> contributors = work.getWorkContributors().getContributor();
        assertThat(contributors, hasSize(2));
        assertThat(contributors, has(contributor("Walter, White", AUTHOR, FIRST)));
        // assertThat(contributors, has(contributor("Editor", EDITOR, FIRST)));
        assertThat(contributors, has(contributor("Jesse, Pinkman", AUTHOR, ADDITIONAL)));

        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(2));
        assertThat(externalIds, has(selfExternalId("handle", patent.getHandle())));
        assertThat(externalIds, has(selfExternalId("pat", "2021.01.0111")));

    }

    @Test
    public void testWorkWithFundingWithoutGrantNumberCreation() {
        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publications)
            .withTitle("Test publication")
            .withAuthor("Walter White")
            .withIssueDate("2021-04-30")
            .withType("Controlled Vocabulary for Resource Type Genres::text::book")
            .withRelationFunding("Test funding")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, publication);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(1));
        assertThat(externalIds, has(selfExternalId("handle", publication.getHandle())));
    }

    @Test
    public void testProductWorkWithFundingWithoutGrantNumberCreation() {
        context.turnOffAuthorisationSystem();

        Item product = ItemBuilder.createItem(context, products)
            .withTitle("Test product")
            .withAuthor("Walter White")
            .withIssueDate("2021-04-30")
            .withType("http://purl.org/coar/resource_type/c_e9a0")
            .withRelationFunding("Test funding")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, product);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(1));
        assertThat(externalIds, has(selfExternalId("handle", product.getHandle())));
    }

    @Test
    public void testPatentWorkWithFundingWithoutGrantNumberCreation() {
        context.turnOffAuthorisationSystem();

        Item patent = ItemBuilder.createItem(context, patents)
            .withTitle("Test patent")
            .withAuthor("Walter White")
            .withIssueDate("2021-04-30")
            .withType("http://purl.org/coar/resource_type/c_15cd")
            .withRelationFunding("Test funding")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, patent);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(1));
        assertThat(externalIds, has(selfExternalId("handle", patent.getHandle())));
    }

    @Test
    public void testWorkWithFundingWithGrantNumberPlaceholderCreation() {
        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publications)
            .withTitle("Test publication")
            .withAuthor("Walter White")
            .withIssueDate("2021-04-30")
            .withType("Controlled Vocabulary for Resource Type Genres::text::book")
            .withRelationFunding("Test funding")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, publication);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(1));
        assertThat(externalIds, has(selfExternalId("handle", publication.getHandle())));
    }

    @Test
    public void testProductWorkWithFundingWithGrantNumberPlaceholderCreation() {
        context.turnOffAuthorisationSystem();

        Item product = ItemBuilder.createItem(context, products)
            .withTitle("Test dataset")
            .withAuthor("Walter White")
            .withIssueDate("2021-04-30")
            .withType("http://purl.org/coar/resource_type/c_7ad9")
            .withRelationFunding("Test funding")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, product);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(1));
        assertThat(externalIds, has(selfExternalId("handle", product.getHandle())));
    }

    @Test
    public void testPatentWorkWithFundingWithGrantNumberPlaceholderCreation() {
        context.turnOffAuthorisationSystem();

        Item patent = ItemBuilder.createItem(context, patents)
            .withTitle("Test patent")
            .withAuthor("Walter White")
            .withIssueDate("2021-04-30")
            .withType("http://purl.org/coar/resource_type/c_15cd")
            .withRelationFunding("Test funding")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, patent);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(1));
        assertThat(externalIds, has(selfExternalId("handle", patent.getHandle())));
    }

    @Test
    public void testEmptyWorkWithUnknownTypeCreation() {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publications)
            .withType("TYPE")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, publication);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;
        assertThat(work.getJournalTitle(), nullValue());
        assertThat(work.getLanguageCode(), nullValue());
        assertThat(work.getPublicationDate(), nullValue());
        assertThat(work.getShortDescription(), nullValue());
        assertThat(work.getPutCode(), nullValue());
        assertThat(work.getWorkType(), is(WorkType.OTHER));
        assertThat(work.getWorkTitle(), nullValue());
        assertThat(work.getWorkContributors(), notNullValue());
        assertThat(work.getWorkContributors().getContributor(), empty());
        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(1));
        assertThat(externalIds, has(selfExternalId("handle", publication.getHandle())));
    }

    @Test
    public void testEmptyProductWorkWithUnknownTypeCreation() {

        context.turnOffAuthorisationSystem();

        Item product = ItemBuilder.createItem(context, products)
            .withType("http://purl.org/coar/resource_type/")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, product);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;
        assertThat(work.getJournalTitle(), nullValue());
        assertThat(work.getLanguageCode(), nullValue());
        assertThat(work.getPublicationDate(), nullValue());
        assertThat(work.getShortDescription(), nullValue());
        assertThat(work.getPutCode(), nullValue());
        // assertThat(work.getWorkCitation(), notNullValue());
        assertThat(work.getWorkType(), is(WorkType.DATA_SET));
        assertThat(work.getWorkTitle(), nullValue());
        assertThat(work.getWorkContributors(), notNullValue());
        assertThat(work.getWorkContributors().getContributor(), empty());
        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(1));
        assertThat(externalIds, has(selfExternalId("handle", product.getHandle())));
    }

    @Test
    public void testEmptyPatentWorkWithUnknownTypeCreation() {

        context.turnOffAuthorisationSystem();

        Item patent = ItemBuilder.createItem(context, patents)
            .withType("http://purl.org/coar/resource_type/")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, patent);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;
        assertThat(work.getJournalTitle(), nullValue());
        assertThat(work.getLanguageCode(), nullValue());
        assertThat(work.getPublicationDate(), nullValue());
        assertThat(work.getShortDescription(), nullValue());
        assertThat(work.getPutCode(), nullValue());
        // assertThat(work.getWorkCitation(), notNullValue());
        assertThat(work.getWorkType(), is(WorkType.PATENT));
        assertThat(work.getWorkTitle(), nullValue());
        assertThat(work.getWorkContributors(), notNullValue());
        assertThat(work.getWorkContributors().getContributor(), empty());
        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(1));
        assertThat(externalIds, has(selfExternalId("handle", patent.getHandle())));
    }


    @Test
    public void testFundingCreation() {
        context.turnOffAuthorisationSystem();

        Item orgUnit = ItemBuilder.createItem(context, orgUnits)
            .withOrgUnitLegalName("4Science")
            .withOrgUnitCountry("IT")
            .withOrgUnitLocality("Milan")
            .withOrgUnitCrossrefIdentifier("12345")
            .build();

        Item projectItem = ItemBuilder.createItem(context, projects)
            .withTitle("Test funding")
            .withProjectStartDate("2001-03")
            .withProjectEndDate("2010-03-25")
            .withProjectInvestigator("Walter White")
            .withProjectInvestigator("Jesse Pinkman")
            .withProjectAmount("123")
            .withProjectAmountCurrency("EUR")
            .withOtherIdentifier("888-666-444")
            .withIdentifier("000-111-333")
            .withDescription("This is a funding to test orcid mapping")
            .build();

        EntityType projectType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType orgUnitType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();

        RelationshipType isAuthorOfPublication = createRelationshipTypeBuilder(context, orgUnitType, projectType,
            "isOrgUnitOfProject", "isProjectOfOrgUnit", 0, null, 0, null).build();

        RelationshipBuilder.createRelationshipBuilder(context, orgUnit, projectItem, isAuthorOfPublication).build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, projectItem);
        assertThat(activity, instanceOf(Funding.class));

        Funding funding = (Funding) activity;
        assertThat(funding.getTitle(), notNullValue());
        assertThat(funding.getTitle().getTitle(), notNullValue());
        assertThat(funding.getTitle().getTitle().getContent(), is("Test funding"));
        assertThat(funding.getStartDate(), matches(date("2001", "03", "01")));
        assertThat(funding.getEndDate(), matches(date("2010", "03", "25")));
        assertThat(funding.getDescription(), is("This is a funding to test orcid mapping"));
        assertThat(funding.getUrl(), matches(urlEndsWith(projectItem.getHandle())));
        assertThat(funding.getAmount(), notNullValue());
        assertThat(funding.getAmount().getContent(), is("123"));
        assertThat(funding.getAmount().getCurrencyCode(), is("EUR"));

        Organization organization = funding.getOrganization();
        assertThat(organization, notNullValue());
        assertThat(organization.getName(), is("4Science"));
        assertThat(organization.getAddress(), notNullValue());
        assertThat(organization.getAddress().getCountry(), is(Iso3166Country.IT));
        assertThat(organization.getAddress().getCity(), is("Milan"));
        assertThat(organization.getDisambiguatedOrganization(), notNullValue());
        assertThat(organization.getDisambiguatedOrganization().getDisambiguatedOrganizationIdentifier(), is("12345"));
        assertThat(organization.getDisambiguatedOrganization().getDisambiguationSource(), is("FUNDREF"));

        FundingContributors fundingContributors = funding.getContributors();
        assertThat(fundingContributors, notNullValue());

        List<FundingContributor> contributors = fundingContributors.getContributor();
        assertThat(contributors, hasSize(2));
        assertThat(contributors, has(fundingContributor("Walter White", LEAD)));
        assertThat(contributors, has(fundingContributor("Jesse Pinkman", LEAD)));

        assertThat(funding.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = funding.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(2));
        assertThat(externalIds, has(selfExternalId("other-id", "888-666-444")));
        assertThat(externalIds, has(selfExternalId("grant_number", "000-111-333")));
    }

    private Predicate<ExternalID> selfExternalId(String type, String value) {
        return externalId(type, value, Relationship.SELF);
    }

    private Predicate<ExternalID> fundedByExternalId(String type, String value) {
        return externalId(type, value, Relationship.FUNDED_BY)
                .and(externalId -> externalId.getUrl() == null);
    }

    private Predicate<ExternalID> fundedByExternalId(String type, String value, String url) {
        return externalId(type, value, Relationship.FUNDED_BY)
                .and(externalId -> externalId.getUrl() != null && Objects.equals(url, externalId.getUrl().getValue()));
    }

    private Predicate<ExternalID> externalId(String type, String value, Relationship relationship) {
        return externalId -> externalId.getRelationship() == relationship
            && type.equals(externalId.getType())
            && value.equals(externalId.getValue());
    }

    private Predicate<Contributor> contributor(String name, ContributorRole role, SequenceType sequence) {
        return contributor -> contributor.getCreditName().getContent().equals(name)
            && role.value().equals(contributor.getContributorAttributes().getContributorRole())
            && contributor.getContributorAttributes().getContributorSequence() == sequence;
    }


    private boolean sameOrcid(ContributorOrcid contributorOrcid, String orcid) {
        return contributorOrcid != null
                && orcid.equals(contributorOrcid.getPath())
                && "https://sandbox.orcid.org".equals(contributorOrcid.getHost())
                && ("https://sandbox.orcid.org/" + orcid).equals(contributorOrcid.getUri());
    }

    private boolean sameEmail(ContributorEmail contributorEmail, String email) {
        return contributorEmail != null && email.equals(contributorEmail.getValue());
    }

    private Predicate<FundingContributor> fundingContributor(String name, FundingContributorRole role) {
        return contributor -> contributor.getCreditName().getContent().equals(name)
            && role.value().equals(contributor.getContributorAttributes().getContributorRole());
    }

    private Predicate<? super FuzzyDate> date(String year, String month, String days) {
        return date -> date != null
            && year.equals(date.getYear().getValue())
            && month.equals(date.getMonth().getValue())
            && days.equals(date.getDay().getValue());
    }

    private Predicate<Url> urlEndsWith(String handle) {
        return url -> url != null && url.getValue() != null && endsWith(url.getValue(), handle);
    }
}
