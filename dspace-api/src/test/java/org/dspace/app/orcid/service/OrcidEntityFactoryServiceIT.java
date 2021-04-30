/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

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
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.common.SequenceType;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.common.PublicationDate;
import org.orcid.jaxb.model.v3.release.record.Activity;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
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

        List<Contributor> contributors = work.getWorkContributors().getContributor();
        assertThat(contributors, hasSize(2));
        assertThat(contributors, hasItem(matches(contributor("Walter White", AUTHOR, FIRST))));
        assertThat(contributors, hasItem(matches(contributor("Jesse Pinkman", AUTHOR, ADDITIONAL,
            "0000-1111-2222-3333", "test@test.it"))));

        assertThat(work.getExternalIdentifiers(), notNullValue());

        List<ExternalID> externalIds = work.getExternalIdentifiers().getExternalIdentifier();
        assertThat(externalIds, hasSize(3));
        assertThat(externalIds, hasItem(matches(externalId("doi", "doi-id"))));
        assertThat(externalIds, hasItem(matches(externalId("eid", "scopus-id"))));
        assertThat(externalIds, hasItem(matches(externalId("handle", publication.getHandle()))));

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

    private Predicate<Contributor> contributor(String orcid, String email) {
        return contributor -> contributor.getContributorEmail() != null
            && email.equals(contributor.getContributorEmail().getValue())
            && contributor.getContributorOrcid() != null
            && orcid.equals(contributor.getContributorOrcid().getPath())
            && "https://sandbox.orcid.org".equals(contributor.getContributorOrcid().getHost())
            && ("https://sandbox.orcid.org/" + orcid).equals(contributor.getContributorOrcid().getUri());
    }

    private Predicate<Contributor> contributor(String name, ContributorRole role, SequenceType sequence, String orcid,
        String email) {
        return contributor(name, role, sequence).and(contributor(orcid, email));
    }

    private Predicate<PublicationDate> date(String year, String month, String days) {
        return date -> date != null
            && year.equals(date.getYear().getValue())
            && month.equals(date.getMonth().getValue())
            && days.equals(date.getDay().getValue());
    }
}
