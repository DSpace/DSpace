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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.orcid.jaxb.model.common.ContributorRole.AUTHOR;
import static org.orcid.jaxb.model.common.ContributorRole.EDITOR;
import static org.orcid.jaxb.model.common.FundingContributorRole.LEAD;
import static org.orcid.jaxb.model.common.SequenceType.ADDITIONAL;
import static org.orcid.jaxb.model.common.SequenceType.FIRST;

import java.util.List;
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
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.common.ContributorRole;
import org.orcid.jaxb.model.common.FundingContributorRole;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.common.SequenceType;
import org.orcid.jaxb.model.common.WorkType;
import org.orcid.jaxb.model.v3.release.common.Contributor;
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

    private Collection publications;

    private Collection projects;

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final String isbn = "978-0-439-02348-1";
    private static final String issn = "1234-1234X";

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

        publications = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("Publication")
            .build();

        projects = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("Project")
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
            .withISBN(isbn)
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
        assertThat(contributors, has(contributor("Walter White", AUTHOR, FIRST)));
        assertThat(contributors, has(contributor("Editor", EDITOR, FIRST)));
        assertThat(contributors, has(contributor("Jesse Pinkman", AUTHOR, ADDITIONAL)));

        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(4));
        assertThat(externalIds, has(selfExternalId("doi", "doi-id")));
        assertThat(externalIds, has(selfExternalId("eid", "scopus-id")));
        assertThat(externalIds, has(selfExternalId("handle", publication.getHandle())));
        // Book type should have SELF rel for ISBN
        assertThat(externalIds, has(selfExternalId("isbn", isbn)));

    }

    @Test
    public void testJournalArticleAndISSN() {
        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publications)
            .withTitle("Test publication")
            .withAuthor("Walter White")
            .withAuthor("Jesse Pinkman")
            .withEditor("Editor")
            .withIssueDate("2021-04-30")
            .withDescriptionAbstract("Publication description")
            .withLanguage("en_US")
            .withType("Article")
            .withIsPartOf("Journal")
            .withISSN(issn)
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
        assertThat(work.getWorkType(), is(WorkType.JOURNAL_ARTICLE));
        assertThat(work.getWorkTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle().getContent(), is("Test publication"));
        assertThat(work.getWorkContributors(), notNullValue());
        assertThat(work.getUrl(), matches(urlEndsWith(publication.getHandle())));

        List<Contributor> contributors = work.getWorkContributors().getContributor();
        assertThat(contributors, hasSize(3));
        assertThat(contributors, has(contributor("Walter White", AUTHOR, FIRST)));
        assertThat(contributors, has(contributor("Editor", EDITOR, FIRST)));
        assertThat(contributors, has(contributor("Jesse Pinkman", AUTHOR, ADDITIONAL)));

        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(4));
        assertThat(externalIds, has(selfExternalId("doi", "doi-id")));
        assertThat(externalIds, has(selfExternalId("eid", "scopus-id")));
        assertThat(externalIds, has(selfExternalId("handle", publication.getHandle())));
        // journal-article should have PART_OF rel for ISSN
        assertThat(externalIds, has(externalId("issn", issn, Relationship.PART_OF)));
    }

    @Test
    public void testJournalWithISSN() {
        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publications)
            .withTitle("Test journal")
            .withEditor("Editor")
            .withType("Journal")
            .withISSN(issn)
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, publication);
        assertThat(activity, instanceOf(Work.class));

        Work work = (Work) activity;
        assertThat(work.getWorkType(), is(WorkType.JOURNAL_ISSUE));
        assertThat(work.getWorkTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle().getContent(), is("Test journal"));
        assertThat(work.getUrl(), matches(urlEndsWith(publication.getHandle())));

        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(2));
        // journal-issue should have SELF rel for ISSN
        assertThat(externalIds, has(selfExternalId("issn", issn)));
        assertThat(externalIds, has(selfExternalId("handle", publication.getHandle())));
    }

    @Test
    public void testWorkCreationWithShortenedValues() {

        context.turnOffAuthorisationSystem();

        String desc = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor." +
            " Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus." +
            " Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim." +
            " Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut,imperdiet" +
            " a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras" +
            " dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor" +
            " eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a,tellus." +
            " Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi" +
            " vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus" +
            " eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum. Nam quam" +
            " nunc, blandit vel, luctus pulvinar, hendrerit id, lorem. Maecenas nec odio et ante tincidunt tempus." +
            " Donec vitae sapien ut libero venenatis faucibus. Nullam quis ante. Etiam sit amet orci eget eros" +
            " faucibus tincidunt. Duis leo. Sed fringilla mauris sit amet nibh. Donec sodales sagittis magna. Sed" +
            " consequat, leo eget bibendum sodales, augue velit cursus nunc, quis gravida magna mi a libero. Fusce" +
            " vulputate eleifend sapien. Vestibulum purus quam, scelerisque ut, mollis sed, nonummy id, metus. Nullam" +
            " accumsan lorem in dui. Cras ultricies mi eu turpis hendrerit fringilla. Vestibulum ante ipsum primis in" +
            " faucibus orci luctus et ultrices posuere cubilia Curae; In ac dui quis mi consectetuer lacinia. Nam" +
            " pretium turpis et arcu. Duis arcu tortor, suscipit eget, imperdiet nec, imperdiet iaculis, ipsum. Sed" +
            " aliquam ultrices mauris. Integer ante arcu, accumsan a, consectetuer eget, posuere ut, mauris." +
            " Praesent adipiscing. Phasellus ullamcorper ipsum rutrum nunc. Nunc nonummy metus. Vestibulum volutpat" +
            " pretium libero. Cras id dui. Aenean ut eros et nisl sagittis vestibulum. Nullam nulla eros, ultricies" +
            " sit amet, nonummy id, imperdiet feugiat, pede. Sed lectus. Donec mollis hendrerit risus. Phasellus nec" +
            " sem in justo pellentesque facilisis. Etiam imperdiet imperdiet orci. Nunc nec neque. Phasellus leo" +
            " dolor, tempus non, auctor et, hendrerit quis, nisi. Curabitur ligula sapien, tincidunt non, euismod" +
            " vitae, posuere imperdiet, leo. Maecenas malesuada. Praesent congue erat at massa. Sed cursus turpis" +
            " vitae tortor. Donec posuere vulputate arcu. Phasellus accumsan cursus velit. Vestibulum ante ipsum" +
            " primis in faucibus orci luctus et ultrices posuere cubilia Curae; Sed aliquam, nisi quis porttitor" +
            " congue, elit erat euismod orci, ac placerat dolor lectus quis orci. Phasellus consectetuer vestibulum" +
            " elit. Aenean tellus metus, bibendum sed, posuere ac, mattis non, nunc. Vestibulum fringilla pede sit" +
            " amet augue. In turpis. Pellentesque posuere. Praesent turpis. Aenean posuere, tortor sed cursus" +
            " feugiat, nunc augue blandit nunc, eu sollicitudin urna dolor sagittis lacus. Donec elit libero, sodales" +
            " nec, volutpat a, suscipit non, turpis. Nullam sagittis. Suspendisse pulvinar, augue ac venenatis" +
            " condimentum, sem libero volutpat nibh, nec pellentesque velit pede quis nunc. Vestibulum ante ipsum" +
            " primis in faucibus orci luctus et ultrices posuere cubilia Curae; Fusce id purus. Ut varius tincidunt" +
            " libero. Phasellus dolor. Maecenas vestibulum mollis diam. Pellentesque ut neque. Pellentesque habitant" +
            " morbi tristique senectus et netus et malesuada fames ac turpis egestas. In dui magna, posuere eget," +
            " vestibulum et, tempor auctor, justo. In ac felis quis tortor malesuada pretium. Pellentesque auctor" +
            " neque nec urna. Proin sapien ipsum, porta a, auctor quis, euismod ut, mi. Aenean viverra rhoncus pede." +
            " Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Ut non" +
            " enim eleifend felis pretium feugiat. Vivamus quis mi. Phasellus a est. Phasellus";

        assertThat(desc.length(), greaterThan(4000));
        String longAuthor = "Walter White William Wisdom Wilfired Winter Willa Winnie Wendy Waylon Winona " +
            "Willow Walker Wade Waverly Whitney Whitley Watson Wanda Wren Warren Wesley Wynter";
        assertThat(longAuthor.length(), greaterThan(150));
        Item publication = ItemBuilder.createItem(context, publications)
            .withTitle("Test publication")
            .withAuthor(longAuthor)
            .withAuthor("Jesse Pinkman")
            .withEditor("Editor")
            .withIssueDate("2021-04-30")
            .withDescriptionAbstract(desc)
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
        assertThat(work.getShortDescription(), startsWith(desc.substring(0, 3996)));
        assertThat(work.getShortDescription().length(), Matchers.lessThan(desc.length()));
        assertThat(work.getShortDescription().length(), Matchers.lessThan(4001));
        assertThat(work.getPutCode(), nullValue());
        assertThat(work.getWorkType(), is(WorkType.BOOK));
        assertThat(work.getWorkTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle().getContent(), is("Test publication"));
        assertThat(work.getWorkContributors(), notNullValue());
        assertThat(work.getUrl(), matches(urlEndsWith(publication.getHandle())));

        List<Contributor> contributors = work.getWorkContributors().getContributor();
        assertThat(contributors, hasSize(3));
        assertThat(contributors,
            has(shortenedContributor(longAuthor.substring(0, 150 - 4), AUTHOR, FIRST, 151)));
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
    public void testEmptyWorkWithUnknownTypeCreation() {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publications)
            .withType("TYPE")
            .withISSN(issn)
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
        assertThat(externalIds, hasSize(2));
        assertThat(externalIds, has(selfExternalId("handle", publication.getHandle())));
        assertThat(externalIds, has(externalId("issn", issn, Relationship.PART_OF)));
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

    private Predicate<Contributor> shortenedContributor(String name, ContributorRole role, SequenceType sequence,
                                                        int maxlength) {
        return contributor -> contributor.getCreditName().getContent().startsWith(name)
            && contributor.getCreditName().getContent().length() <= maxlength
            && role.value().equals(contributor.getContributorAttributes().getContributorRole())
            && contributor.getContributorAttributes().getContributorSequence() == sequence;
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
