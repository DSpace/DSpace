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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.orcid.model.factory.OrcidProfileSectionFactory;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
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

    private ItemService itemService;

    private Collection collection;

    private Collection orgUnits;

    @Before
    public void setup() {

        profileSectionFactoryService = OrcidServiceFactory.getInstance().getOrcidProfileSectionFactoryService();
        itemService = ContentServiceFactory.getInstance().getItemService();

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

        List<MetadataValue> values = new ArrayList<>();
        values.add(getMetadata(item, "oairecerif.person.affiliation", 0));
        values.add(getMetadata(item, "oairecerif.affiliation.startDate", 0));
        values.add(getMetadata(item, "oairecerif.affiliation.endDate", 0));
        values.add(getMetadata(item, "oairecerif.affiliation.role", 0));

        Object firstOrcidObject = profileSectionFactoryService.createOrcidObject(context, values, AFFILIATION);

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

        values = new ArrayList<>();
        values.add(getMetadata(item, "oairecerif.person.affiliation", 1));
        values.add(getMetadata(item, "oairecerif.affiliation.startDate", 1));
        values.add(getMetadata(item, "oairecerif.affiliation.endDate", 1));
        values.add(getMetadata(item, "oairecerif.affiliation.role", 1));

        Object secondOrcidObject = profileSectionFactoryService.createOrcidObject(context, values, AFFILIATION);
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
    public void testManyEmploymentsMetadataSignatureGeneration() {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withPersonAffiliation("4Science")
            .withPersonAffiliationStartDate("2020-02")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Researcher")
            .withPersonAffiliation("Organization", "a1ce40bc-448c-47a6-ba1a-c695bc88f3b7")
            .withPersonAffiliationStartDate("2021-02")
            .withPersonAffiliationEndDate("2021-03-31")
            .withPersonAffiliationRole(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        OrcidProfileSectionFactory affiliationFactory = getFactory(item, AFFILIATION);

        List<String> signatures = affiliationFactory.getMetadataSignatures(context, item);
        assertThat(signatures, hasSize(2));

        String firstDescription = affiliationFactory.getDescription(context, item, signatures.get(0));
        assertThat(firstDescription, is("Researcher at 4Science ( from 2020-02 to present )"));

        String secondDescription = affiliationFactory.getDescription(context, item, signatures.get(1));
        assertThat(secondDescription, is("Organization ( from 2021-02 to 2021-03-31 )"));
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

        List<MetadataValue> values = new ArrayList<>();
        values.add(getMetadata(item, "oairecerif.person.affiliation", 0));
        values.add(getMetadata(item, "oairecerif.affiliation.startDate", 0));
        values.add(getMetadata(item, "oairecerif.affiliation.endDate", 0));
        values.add(getMetadata(item, "oairecerif.affiliation.role", 0));

        Object firstOrcidObject = profileSectionFactoryService.createOrcidObject(context, values, AFFILIATION);
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

        List<MetadataValue> values = new ArrayList<>();
        values.add(getMetadata(item, "crisrp.qualification", 0));
        values.add(getMetadata(item, "crisrp.qualification.start", 0));
        values.add(getMetadata(item, "crisrp.qualification.end", 0));

        Object orcidObject = profileSectionFactoryService.createOrcidObject(context, values, QUALIFICATION);
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

        List<MetadataValue> values = new ArrayList<>();
        values.add(getMetadata(item, "crisrp.education", 0));
        values.add(getMetadata(item, "crisrp.education.start", 0));
        values.add(getMetadata(item, "crisrp.education.end", 0));

        Object orcidObject = profileSectionFactoryService.createOrcidObject(context, values, EDUCATION);
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

        List<MetadataValue> values = List.of(getMetadata(item, "crisrp.country", 0));

        Object orcidObject = profileSectionFactoryService.createOrcidObject(context, values, COUNTRY);
        assertThat(orcidObject, instanceOf(Address.class));
        Address address = (Address) orcidObject;
        assertThat(address.getCountry(), notNullValue());
        assertThat(address.getCountry().getValue(), is(Iso3166Country.IT));

    }

    @Test
    public void testAddressMetadataSignatureGeneration() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withPersonCountry("IT")
            .build();
        context.restoreAuthSystemState();

        OrcidProfileSectionFactory countryFactory = getFactory(item, COUNTRY);

        List<String> signatures = countryFactory.getMetadataSignatures(context, item);
        assertThat(signatures, hasSize(1));
        assertThat(countryFactory.getDescription(context, item, signatures.get(0)), is("IT"));
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

        List<MetadataValue> values = List.of(getMetadata(item, "person.identifier.scopus-author-id", 0));

        Object firstOrcidObject = profileSectionFactoryService.createOrcidObject(context, values, EXTERNAL_IDS);
        assertThat(firstOrcidObject, instanceOf(PersonExternalIdentifier.class));
        assertThat((PersonExternalIdentifier) firstOrcidObject, matches(hasTypeAndValue("SCOPUS", "SCOPUS-123456")));

        values = List.of(getMetadata(item, "person.identifier.rid", 0));

        Object secondOrcidObject = profileSectionFactoryService.createOrcidObject(context, values, EXTERNAL_IDS);
        assertThat(secondOrcidObject, instanceOf(PersonExternalIdentifier.class));
        assertThat((PersonExternalIdentifier) secondOrcidObject, matches(hasTypeAndValue("RID", "R-ID-01")));
    }

    @Test
    public void testExternalIdentifiersGeneration() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withScopusAuthorIdentifier("SCOPUS-123456")
            .withResearcherIdentifier("R-ID-01")
            .build();
        context.restoreAuthSystemState();

        OrcidProfileSectionFactory externalIdsFactory = getFactory(item, EXTERNAL_IDS);
        List<String> signatures = externalIdsFactory.getMetadataSignatures(context, item);
        assertThat(signatures, hasSize(2));

        List<String> descriptions = signatures.stream()
            .map(signature -> externalIdsFactory.getDescription(context, item, signature))
            .collect(Collectors.toList());

        assertThat(descriptions, containsInAnyOrder("SCOPUS-123456", "R-ID-01"));
    }

    @Test
    public void testResearcherUrlsCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withUrlIdentifier("www.test.com")
            .build();
        context.restoreAuthSystemState();

        List<MetadataValue> values = List.of(getMetadata(item, "oairecerif.identifier.url", 0));

        Object orcidObject = profileSectionFactoryService.createOrcidObject(context, values, RESEARCHER_URLS);
        assertThat(orcidObject, instanceOf(ResearcherUrl.class));
        assertThat((ResearcherUrl) orcidObject, matches(hasUrl("www.test.com")));
    }

    @Test
    public void testKeywordsCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withSubject("Subject")
            .build();
        context.restoreAuthSystemState();

        List<MetadataValue> values = List.of(getMetadata(item, "dc.subject", 0));
        Object orcidObject = profileSectionFactoryService.createOrcidObject(context, values, KEYWORDS);
        assertThat(orcidObject, instanceOf(Keyword.class));
        assertThat((Keyword) orcidObject, matches(hasContent("Subject")));
    }

    @Test
    public void testOtherNamesCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withVariantName("Variant name")
            .withVernacularName("Vernacular name")
            .build();
        context.restoreAuthSystemState();

        List<MetadataValue> values = List.of(getMetadata(item, "crisrp.name.variant", 0));
        Object orcidObject = profileSectionFactoryService.createOrcidObject(context, values, OTHER_NAMES);
        assertThat(orcidObject, instanceOf(OtherName.class));
        assertThat((OtherName) orcidObject, matches(hasValue("Variant name")));

        values = List.of(getMetadata(item, "crisrp.name.translated", 0));
        orcidObject = profileSectionFactoryService.createOrcidObject(context, values, OTHER_NAMES);
        assertThat(orcidObject, instanceOf(OtherName.class));
        assertThat((OtherName) orcidObject, matches(hasValue("Vernacular name")));
    }

    private MetadataValue getMetadata(Item item, String metadataField, int place) {
        List<MetadataValue> values = itemService.getMetadataByMetadataString(item, metadataField);
        assertThat(values.size(), greaterThan(place));
        return values.get(place);
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

    private OrcidProfileSectionFactory getFactory(Item item, OrcidProfileSectionType sectionType) {
        return profileSectionFactoryService.findBySectionType(sectionType)
            .orElseThrow(() -> new IllegalStateException("No profile section factory of type " + sectionType));
    }
}
