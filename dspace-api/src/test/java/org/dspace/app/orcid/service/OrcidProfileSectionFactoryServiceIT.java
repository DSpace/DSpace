/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.AFFILIATION;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.COUNTRY;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.EDUCATION;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.EXTERNAL_IDS;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.KEYWORDS;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.OTHER_NAMES;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.QUALIFICATION;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.RESEARCHER_URLS;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Every.everyItem;

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
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.record.Address;
import org.orcid.jaxb.model.v3.release.record.Education;
import org.orcid.jaxb.model.v3.release.record.Employment;
import org.orcid.jaxb.model.v3.release.record.Keyword;
import org.orcid.jaxb.model.v3.release.record.OtherName;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifier;
import org.orcid.jaxb.model.v3.release.record.Qualification;
import org.orcid.jaxb.model.v3.release.record.ResearcherUrl;

/**
 * Integration tests for {@link OrcidProfileSectionFactoryService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidProfileSectionFactoryServiceIT extends AbstractIntegrationTestWithDatabase {

    private OrcidProfileSectionFactoryService profileSectionFactoryService;

    private Collection collection;

    private Collection orgUnits;

    @Before
    public void setup() {

        profileSectionFactoryService = OrcidServiceFactory.getInstance().getOrcidProfileSectionFactoryService();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withTitle("Parent community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("Person")
            .build();

        orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("OrgUnit")
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testManyEmploymentsCreation() {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withPersonAffiliation("4Science")
            .withPersonAffiliationStartDate("2020-02")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Researcher")
            .withPersonAffiliation("Organization")
            .withPersonAffiliationStartDate("2021-02")
            .withPersonAffiliationEndDate("2021-03-31")
            .withPersonAffiliationRole(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        context.restoreAuthSystemState();

        List<Object> orcidObjects = profileSectionFactoryService.createOrcidObjects(context, item, AFFILIATION);
        assertThat(orcidObjects, hasSize(2));

        Object firstOrcidObject = orcidObjects.get(0);
        assertThat(firstOrcidObject, instanceOf(Employment.class));
        Employment qualification = (Employment) firstOrcidObject;
        assertThat(qualification.getStartDate(), notNullValue());
        assertThat(qualification.getStartDate().getYear().getValue(), is("2020"));
        assertThat(qualification.getStartDate().getMonth().getValue(), is("02"));
        assertThat(qualification.getStartDate().getDay().getValue(), is("01"));
        assertThat(qualification.getEndDate(), nullValue());
        assertThat(qualification.getRoleTitle(), is("Researcher"));
        assertThat(qualification.getOrganization(), notNullValue());
        assertThat(qualification.getOrganization().getName(), is("4Science"));
        assertThat(qualification.getOrganization().getAddress(), nullValue());
        assertThat(qualification.getOrganization().getDisambiguatedOrganization(), nullValue());

        Object secondOrcidObject = orcidObjects.get(1);
        assertThat(secondOrcidObject, instanceOf(Employment.class));
        Employment secondEmployment = (Employment) secondOrcidObject;
        assertThat(secondEmployment.getStartDate(), notNullValue());
        assertThat(secondEmployment.getStartDate().getYear().getValue(), is("2021"));
        assertThat(secondEmployment.getStartDate().getMonth().getValue(), is("02"));
        assertThat(secondEmployment.getStartDate().getDay().getValue(), is("01"));
        assertThat(secondEmployment.getEndDate(), notNullValue());
        assertThat(secondEmployment.getEndDate().getYear().getValue(), is("2021"));
        assertThat(secondEmployment.getEndDate().getMonth().getValue(), is("03"));
        assertThat(secondEmployment.getEndDate().getDay().getValue(), is("31"));
        assertThat(secondEmployment.getRoleTitle(), nullValue());
        assertThat(secondEmployment.getOrganization(), notNullValue());
        assertThat(secondEmployment.getOrganization().getName(), is("Organization"));
        assertThat(secondEmployment.getOrganization().getAddress(), nullValue());
        assertThat(secondEmployment.getOrganization().getDisambiguatedOrganization(), nullValue());

    }

    @Test
    public void testFullEmploymentCreation() {

        context.turnOffAuthorisationSystem();

        Item orgUnit = ItemBuilder.createItem(context, orgUnits)
            .withTitle("4Science")
            .withOrgUnitCountry("IT")
            .withOrgUnitLocality("Milan")
            .withOrgUnitRinggoldIdentifier("12345")
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withPersonAffiliation("4Science", orgUnit.getID().toString())
            .withPersonAffiliationStartDate("2020-02")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Researcher")
            .build();

        context.restoreAuthSystemState();

        List<Object> orcidObjects = profileSectionFactoryService.createOrcidObjects(context, item, AFFILIATION);
        assertThat(orcidObjects, hasSize(1));

        Object firstOrcidObject = orcidObjects.get(0);
        assertThat(firstOrcidObject, instanceOf(Employment.class));
        Employment qualification = (Employment) firstOrcidObject;
        assertThat(qualification.getStartDate(), notNullValue());
        assertThat(qualification.getStartDate().getYear().getValue(), is("2020"));
        assertThat(qualification.getStartDate().getMonth().getValue(), is("02"));
        assertThat(qualification.getStartDate().getDay().getValue(), is("01"));
        assertThat(qualification.getEndDate(), nullValue());
        assertThat(qualification.getRoleTitle(), is("Researcher"));

        Organization organization = qualification.getOrganization();
        assertThat(organization, notNullValue());
        assertThat(organization.getName(), is("4Science"));
        assertThat(organization.getAddress(), notNullValue());
        assertThat(organization.getAddress().getCountry(), is(Iso3166Country.IT));
        assertThat(organization.getAddress().getCity(), is("Milan"));
        assertThat(organization.getDisambiguatedOrganization(), notNullValue());
        assertThat(organization.getDisambiguatedOrganization().getDisambiguatedOrganizationIdentifier(), is("12345"));
        assertThat(organization.getDisambiguatedOrganization().getDisambiguationSource(), is("RINGGOLD"));

    }

    @Test
    public void testQualificationCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withPersonQualification("Qualification")
            .withPersonQualificationStartDate("2015-03-01")
            .withPersonQualificationEndDate("2016-03-01")
            .build();
        context.restoreAuthSystemState();

        List<Object> orcidObjects = profileSectionFactoryService.createOrcidObjects(context, item, QUALIFICATION);
        assertThat(orcidObjects, hasSize(1));

        Object orcidObject = orcidObjects.get(0);
        assertThat(orcidObject, instanceOf(Qualification.class));
        Qualification qualification = (Qualification) orcidObject;
        assertThat(qualification.getStartDate(), notNullValue());
        assertThat(qualification.getStartDate().getYear().getValue(), is("2015"));
        assertThat(qualification.getStartDate().getMonth().getValue(), is("03"));
        assertThat(qualification.getStartDate().getDay().getValue(), is("01"));
        assertThat(qualification.getEndDate(), notNullValue());
        assertThat(qualification.getEndDate().getYear().getValue(), is("2016"));
        assertThat(qualification.getEndDate().getMonth().getValue(), is("03"));
        assertThat(qualification.getEndDate().getDay().getValue(), is("01"));
        assertThat(qualification.getRoleTitle(), nullValue());
        assertThat(qualification.getOrganization(), notNullValue());
        assertThat(qualification.getOrganization().getName(), is("Qualification"));
        assertThat(qualification.getOrganization().getAddress(), nullValue());
        assertThat(qualification.getOrganization().getDisambiguatedOrganization(), nullValue());
    }

    @Test
    public void testEducationCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withPersonEducation("High school")
            .withPersonEducationStartDate("2015-03-01")
            .withPersonEducationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();
        context.restoreAuthSystemState();

        List<Object> orcidObjects = profileSectionFactoryService.createOrcidObjects(context, item, EDUCATION);
        assertThat(orcidObjects, hasSize(1));

        Object orcidObject = orcidObjects.get(0);
        assertThat(orcidObject, instanceOf(Education.class));
        Education education = (Education) orcidObject;
        assertThat(education.getStartDate(), notNullValue());
        assertThat(education.getStartDate().getYear().getValue(), is("2015"));
        assertThat(education.getStartDate().getMonth().getValue(), is("03"));
        assertThat(education.getStartDate().getDay().getValue(), is("01"));
        assertThat(education.getEndDate(), nullValue());
        assertThat(education.getRoleTitle(), nullValue());
        assertThat(education.getOrganization(), notNullValue());
        assertThat(education.getOrganization().getName(), is("High school"));
        assertThat(education.getOrganization().getAddress(), nullValue());
        assertThat(education.getOrganization().getDisambiguatedOrganization(), nullValue());
    }

    @Test
    public void testAddressCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withPersonCountry("IT")
            .build();
        context.restoreAuthSystemState();

        List<Object> orcidObjects = profileSectionFactoryService.createOrcidObjects(context, item, COUNTRY);
        assertThat(orcidObjects, hasSize(1));

        Object orcidObject = orcidObjects.get(0);
        assertThat(orcidObject, instanceOf(Address.class));
        Address address = (Address) orcidObject;
        assertThat(address.getCountry(), notNullValue());
        assertThat(address.getCountry().getValue(), is(Iso3166Country.IT));

    }

    @Test
    public void testExternalIdentifiersCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withScopusAuthorIdentifier("SCOPUS-123456")
            .withResearcherIdentifier("R-ID-01")
            .build();
        context.restoreAuthSystemState();

        List<Object> orcidObjects = profileSectionFactoryService.createOrcidObjects(context, item, EXTERNAL_IDS);
        assertThat(orcidObjects, hasSize(2));

        assertThat(orcidObjects, everyItem(instanceOf(PersonExternalIdentifier.class)));
        assertThat(orcidObjects, hasItem(matches(hasTypeAndValue("SCOPUS", "SCOPUS-123456"))));
        assertThat(orcidObjects, hasItem(matches(hasTypeAndValue("RID", "R-ID-01"))));
    }

    @Test
    public void testResearcherUrlsCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withUrlIdentifier("www.test.com")
            .withUrlIdentifier("www.test.it")
            .build();
        context.restoreAuthSystemState();

        List<Object> orcidObjects = profileSectionFactoryService.createOrcidObjects(context, item, RESEARCHER_URLS);
        assertThat(orcidObjects, hasSize(2));

        assertThat(orcidObjects, everyItem(instanceOf(ResearcherUrl.class)));
        assertThat(orcidObjects, hasItem(matches(hasUrl("www.test.com"))));
        assertThat(orcidObjects, hasItem(matches(hasUrl("www.test.it"))));
    }

    @Test
    public void testKeywordsCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withSubject("First subject")
            .withSubject("Second subject")
            .withSubject("Third subject")
            .build();
        context.restoreAuthSystemState();

        List<Object> orcidObjects = profileSectionFactoryService.createOrcidObjects(context, item, KEYWORDS);
        assertThat(orcidObjects, hasSize(3));

        assertThat(orcidObjects, everyItem(instanceOf(Keyword.class)));
        assertThat(orcidObjects, hasItem(matches(hasContent("First subject"))));
        assertThat(orcidObjects, hasItem(matches(hasContent("Second subject"))));
        assertThat(orcidObjects, hasItem(matches(hasContent("Third subject"))));
    }

    @Test
    public void testOtherNamesCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withVariantName("Variant name")
            .withVernacularName("Vernacular name")
            .withUrlIdentifier("www.test.it")
            .build();
        context.restoreAuthSystemState();

        List<Object> orcidObjects = profileSectionFactoryService.createOrcidObjects(context, item, OTHER_NAMES);
        assertThat(orcidObjects, hasSize(2));

        assertThat(orcidObjects, everyItem(instanceOf(OtherName.class)));
        assertThat(orcidObjects, hasItem(matches(hasValue("Variant name"))));
        assertThat(orcidObjects, hasItem(matches(hasValue("Vernacular name"))));
    }

    private Predicate<PersonExternalIdentifier> hasTypeAndValue(String type, String value) {
        return identifier -> value.equals(identifier.getValue())
            && type.equals(identifier.getType())
            && identifier.getRelationship() == Relationship.SELF
            && identifier.getUrl() != null && value.equals(identifier.getUrl().getValue());
    }

    private Predicate<ResearcherUrl> hasUrl(String url) {
        return researcherUrl -> researcherUrl.getUrl() != null && url.equals(researcherUrl.getUrl().getValue());
    }

    private Predicate<Keyword> hasContent(String value) {
        return keyword -> value.equals(keyword.getContent());
    }

    private Predicate<OtherName> hasValue(String value) {
        return name -> value.equals(name.getContent());
    }
}
