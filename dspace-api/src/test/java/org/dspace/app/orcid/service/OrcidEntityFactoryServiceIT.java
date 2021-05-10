/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import static org.apache.commons.lang.StringUtils.endsWith;
import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.orcid.jaxb.model.common.ContributorRole.AUTHOR;
import static org.orcid.jaxb.model.common.ContributorRole.EDITOR;
import static org.orcid.jaxb.model.common.FundingContributorRole.CO_LEAD;
import static org.orcid.jaxb.model.common.FundingContributorRole.LEAD;
import static org.orcid.jaxb.model.common.SequenceType.ADDITIONAL;
import static org.orcid.jaxb.model.common.SequenceType.FIRST;

import java.util.List;
import java.util.function.Predicate;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.common.ContributorRole;
import org.orcid.jaxb.model.common.FundingContributorRole;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.common.SequenceType;
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

    private Collection persons;

    private Collection orgUnits;

    private Collection publications;

    private Collection projects;

    @Before
    public void setup() {

        entityFactoryService = OrcidServiceFactory.getInstance().getOrcidEntityFactoryService();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withTitle("Parent community")
            .build();

        persons = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("Person")
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

        Item author = ItemBuilder.createItem(context, persons)
            .withTitle("Jesse Pinkman")
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withPersonEmail("test@test.it")
            .build();

        Item publication = ItemBuilder.createItem(context, publications)
            .withTitle("Test publication")
            .withAuthor("Walter White")
            .withAuthor("Jesse Pinkman", author.getID().toString())
            .withEditor("Editor")
            .withIssueDate("2021-04-30")
            .withDescriptionAbstract("Publication description")
            .withLanguage("en_US")
            .withType("Controlled Vocabulary for Resource Type Genres::text::book")
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
        assertThat(work.getWorkCitation(), notNullValue());
        assertThat(work.getWorkCitation().getCitation(), containsString("Test publication"));
        assertThat(work.getWorkTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle(), notNullValue());
        assertThat(work.getWorkTitle().getTitle().getContent(), is("Test publication"));
        assertThat(work.getWorkContributors(), notNullValue());
        assertThat(work.getUrl(), matches(urlEndsWith(publication.getHandle())));

        List<Contributor> contributors = work.getWorkContributors().getContributor();
        assertThat(contributors, hasSize(3));
        assertThat(contributors, hasItem(matches(contributor("Walter White", AUTHOR, FIRST))));
        assertThat(contributors, hasItem(matches(contributor("Editor", EDITOR, FIRST))));
        assertThat(contributors, hasItem(matches(contributor("Jesse Pinkman", AUTHOR, ADDITIONAL,
            "0000-1111-2222-3333", "test@test.it"))));

        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(3));
        assertThat(externalIds, hasItem(matches(externalId("doi", "doi-id"))));
        assertThat(externalIds, hasItem(matches(externalId("eid", "scopus-id"))));
        assertThat(externalIds, hasItem(matches(externalId("handle", publication.getHandle()))));

    }

    @Test
    public void testFundingCreation() {
        context.turnOffAuthorisationSystem();

        Item investigator = ItemBuilder.createItem(context, persons)
            .withTitle("Jesse Pinkman")
            .withPersonEmail("test@test.it")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, orgUnits)
            .withTitle("4Science")
            .withOrgUnitCountry("IT")
            .withOrgUnitLocality("Milan")
            .withOrgUnitCrossrefIdentifier("12345")
            .build();

        Item project = ItemBuilder.createItem(context, projects)
            .withTitle("Test project")
            .withProjectStartDate("2001-03")
            .withProjectEndDate("2010-03-25")
            .withProjectCoordinator("4Science", orgUnit.getID().toString())
            .withProjectInvestigator("Walter White")
            .withProjectInvestigator("Jesse Pinkman", investigator.getID().toString())
            .withProjectCoinvestigators("Mario Rossi")
            .withInternalId("888-666-444")
            .withUrlIdentifier("www.test.com")
            .withDescriptionAbstract("This is a project to test orcid mapping")
            .build();

        context.restoreAuthSystemState();

        Activity activity = entityFactoryService.createOrcidObject(context, project);
        assertThat(activity, instanceOf(Funding.class));

        Funding funding = (Funding) activity;
        assertThat(funding.getTitle(), notNullValue());
        assertThat(funding.getTitle().getTitle(), notNullValue());
        assertThat(funding.getTitle().getTitle().getContent(), is("Test project"));
        assertThat(funding.getStartDate(), matches(date("2001", "03", "01")));
        assertThat(funding.getEndDate(), matches(date("2010", "03", "25")));
        assertThat(funding.getDescription(), is("This is a project to test orcid mapping"));
        assertThat(funding.getUrl(), matches(urlEndsWith(project.getHandle())));

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
        assertThat(contributors, hasSize(3));
        assertThat(contributors, hasItem(matches(fundingContributor("Walter White", LEAD))));
        assertThat(contributors, hasItem(matches(fundingContributor("Jesse Pinkman", LEAD, "test@test.it"))));
        assertThat(contributors, hasItem(matches(fundingContributor("Mario Rossi", CO_LEAD))));

        assertThat(funding.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = funding.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(2));
        assertThat(externalIds, hasItem(matches(externalId("other-id", "888-666-444"))));
        assertThat(externalIds, hasItem(matches(externalId("uri", "www.test.com"))));
    }

    private Predicate<ExternalID> externalId(String type, String value) {
        return externalId -> externalId.getRelationship() == Relationship.SELF
            && type.equals(externalId.getType())
            && value.equals(externalId.getValue());
    }

    private Predicate<Contributor> contributor(String name, ContributorRole role, SequenceType sequence) {
        return contributor -> contributor.getCreditName().getContent().equals(name)
            && contributor.getContributorAttributes().getContributorRole() == role
            && contributor.getContributorAttributes().getContributorSequence() == sequence;
    }

    private Predicate<FundingContributor> fundingContributor(String name, FundingContributorRole role) {
        return contributor -> contributor.getCreditName().getContent().equals(name)
            && contributor.getContributorAttributes().getContributorRole() == role;
    }

    private Predicate<FundingContributor> fundingContributor(String name, FundingContributorRole role, String email) {
        return fundingContributor(name, role)
            .and(fundingContributor -> sameEmail(fundingContributor.getContributorEmail(), email))
            .and(fundingContributor -> fundingContributor.getContributorOrcid() == null);
    }

    private Predicate<Contributor> contributor(String orcid, String email) {
        return contributor -> sameEmail(contributor.getContributorEmail(), email)
            && sameOrcid(contributor.getContributorOrcid(), orcid);
    }

    private boolean sameEmail(ContributorEmail contributorEmail, String email) {
        return contributorEmail != null && email.equals(contributorEmail.getValue());
    }

    private boolean sameOrcid(ContributorOrcid contributorOrcid, String orcid) {
        return contributorOrcid != null
            && orcid.equals(contributorOrcid.getPath())
            && "https://sandbox.orcid.org".equals(contributorOrcid.getHost())
            && ("https://sandbox.orcid.org/" + orcid).equals(contributorOrcid.getUri());
    }

    private Predicate<Contributor> contributor(String name, ContributorRole role, SequenceType sequence,
        String orcid, String email) {
        return contributor(name, role, sequence).and(contributor(orcid, email));
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
