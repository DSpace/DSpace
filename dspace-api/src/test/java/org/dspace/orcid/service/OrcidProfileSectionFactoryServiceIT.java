/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service;

import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.dspace.orcid.model.OrcidProfileSectionType.COUNTRY;
import static org.dspace.orcid.model.OrcidProfileSectionType.EXTERNAL_IDS;
import static org.dspace.orcid.model.OrcidProfileSectionType.KEYWORDS;
import static org.dspace.orcid.model.OrcidProfileSectionType.OTHER_NAMES;
import static org.dspace.orcid.model.OrcidProfileSectionType.RESEARCHER_URLS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.orcid.factory.OrcidServiceFactory;
import org.dspace.orcid.model.OrcidProfileSectionType;
import org.dspace.orcid.model.factory.OrcidProfileSectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.v3.release.record.Address;
import org.orcid.jaxb.model.v3.release.record.Keyword;
import org.orcid.jaxb.model.v3.release.record.OtherName;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifier;
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

        context.restoreAuthSystemState();
    }

    @Test
    public void testAddressCreation() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test profile")
            .withPersonCountry("IT")
            .build();
        context.restoreAuthSystemState();

        List<MetadataValue> values = List.of(getMetadata(item, "person.country", 0));

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
            .withUriIdentifier("www.test.com")
            .build();
        context.restoreAuthSystemState();

        List<MetadataValue> values = List.of(getMetadata(item, "dc.identifier.uri", 0));

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

        List<MetadataValue> values = List.of(getMetadata(item, "person.name.variant", 0));
        Object orcidObject = profileSectionFactoryService.createOrcidObject(context, values, OTHER_NAMES);
        assertThat(orcidObject, instanceOf(OtherName.class));
        assertThat((OtherName) orcidObject, matches(hasValue("Variant name")));

        values = List.of(getMetadata(item, "person.name.translated", 0));
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
