/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.authority.service.AuthorityValueService.AUTHORITY_CLEANUP_BUSINESS_MODE;
import static org.dspace.authority.service.AuthorityValueService.AUTHORITY_CLEANUP_CLEAN_ALL_MODE;
import static org.dspace.authority.service.AuthorityValueService.AUTHORITY_CLEANUP_PROPERTY_PREFIX;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class CleanUpAuthorityRelationMetadataIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;


    @Before
    public void setupAuthorityConfiguration() throws Exception {
        // Common authority configuration for all tests
        choiceAuthorityService.getChoiceAuthoritiesNames();

        // Configure plugin and authority settings
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" }
        );

        // Configure choice plugins and authority control
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        configurationService.setProperty("authority.controlled.person.affiliation.name", "true");

        // Clear caches again after authority configuration
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();
    }

    @Test
    public void checkBusinessModeWhileDeletionPersonItemTest() throws Exception {

        configurationService.setProperty("item-deletion.authority-cleanup.enabled", true);
        // configure BUSINESS_MODE for dc.contributor.author metadata
        configurationService.setProperty(AUTHORITY_CLEANUP_PROPERTY_PREFIX + "dc.contributor.author",
                                         AUTHORITY_CLEANUP_BUSINESS_MODE);

        // Clear caches after test-specific configuration
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection of Publications")
                                                   .build();

        Collection persons = CollectionBuilder.createCollection(context, parentCommunity)
                                              .withName("Collection of Persons")
                                              .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection of OrgUnits")
                                               .build();

        Item orgUnitItem = ItemBuilder.createItem(context, orgUnits)
                                      .withTitle("4Science")
                                      .withEntityType("OrgUnit")
                                      .build();

        Item personItem = ItemBuilder.createItem(context, persons)
                                     .withTitle("Boychuk, Mykhaylo")
                                     .withOrcidIdentifier("0001-002-0003-0001")
                                     .withScopusAuthorIdentifier("12345678001")
                                     .withAffiliation("4Science", orgUnitItem.getID().toString())
                                     .withEntityType("Person")
                                     .build();

        Item personItem2 = ItemBuilder.createItem(context, persons)
                                      .withTitle("Giamminonni, Luca")
                                      .withOrcidIdentifier("0000-0002-8310-6788")
                                      .withScopusAuthorIdentifier("12345678002")
                                      .withAffiliation("4Science", orgUnitItem.getID().toString())
                                      .withEntityType("Person")
                                      .build();

        Item publicationItem = ItemBuilder.createItem(context, publications)
                                          .withTitle("New functionalities of the DSpace7")
                                          .withAuthor("Boychuk, Mykhaylo", personItem.getID().toString())
                                          .withAuthor("Giamminonni, Luca", personItem2.getID().toString())
                                          .withIssueDate("2023-02-14")
                                          .withSubject("ExtraEntry")
                                          .withEntityType("Publication")
                                          .build();

        context.restoreAuthSystemState();



        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", Matchers.allOf(
              hasJsonPath("$.name", is(personItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].value", is(orgUnitItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].authority", is(orgUnitItem.getID().toString())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].confidence", is(600))
              )));

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", Matchers.allOf(
              // check Boychuk, Mykhaylo
              hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem.getName())),
              hasJsonPath("$.metadata['dc.contributor.author'][0].authority", is(personItem.getID().toString())),
              hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(600)),
              // Giamminonni, Luca
              hasJsonPath("$.metadata['dc.contributor.author'][1].value", is(personItem2.getName())),
              hasJsonPath("$.metadata['dc.contributor.author'][1].authority", is(personItem2.getID().toString())),
              hasJsonPath("$.metadata['dc.contributor.author'][1].confidence", is(600))
              )));

        getClient(tokenAdmin).perform(delete("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
             // check Boychuk, Mykhaylo
             hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][0].authority",
                      is("will be referenced::ORCID::0001-002-0003-0001")),
             hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(-1)),
             // check Giamminonni, Luca
             hasJsonPath("$.metadata['dc.contributor.author'][1].value", is(personItem2.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].authority", is(personItem2.getID().toString())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].confidence", is(600))
             )));
    }

    @Test
    public void checkBusinessModeAndPersonItemWithoutAnyBusinessIdentifierTest() throws Exception {
        configurationService.setProperty("authority.controlled.cris.virtual.department", "true");
        configurationService.setProperty("item-deletion.authority-cleanup.enabled", true);
        // configure BUSINESS_MODE for dc.contributor.author metadata
        configurationService.setProperty(AUTHORITY_CLEANUP_PROPERTY_PREFIX + "dc.contributor.author",
                                         AUTHORITY_CLEANUP_BUSINESS_MODE);

        // Clear caches after test-specific configuration
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection of Publications")
                                                   .build();

        Collection persons = CollectionBuilder.createCollection(context, parentCommunity)
                                              .withName("Collection of Persons")
                                              .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection of OrgUnits")
                                               .build();

        Item orgUnitItem = ItemBuilder.createItem(context, orgUnits)
                                      .withTitle("4Science")
                                      .withEntityType("OrgUnit")
                                      .build();

        Item personItem = ItemBuilder.createItem(context, persons)
                                     .withTitle("Boychuk, Mykhaylo")
                                     .withAffiliation("4Science", orgUnitItem.getID().toString())
                                     .withEntityType("Person")
                                     .build();

        Item personItem2 = ItemBuilder.createItem(context, persons)
                                      .withTitle("Giamminonni, Luca")
                                      .withOrcidIdentifier("0000-0002-8310-6788")
                                      .withScopusAuthorIdentifier("12345678002")
                                      .withAffiliation("4Science", orgUnitItem.getID().toString())
                                      .withEntityType("Person")
                                      .build();

        Item publicationItem = ItemBuilder.createItem(context, publications)
                                          .withTitle("New functionalities of the DSpace7")
                                          .withAuthor("Boychuk, Mykhaylo", personItem.getID().toString())
                                          .withAuthor("Giamminonni, Luca", personItem2.getID().toString())
                                          .withIssueDate("2023-02-14")
                                          .withSubject("ExtraEntry")
                                          .withEntityType("Publication")
                                          .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", Matchers.allOf(
              hasJsonPath("$.name", is(personItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].value", is(orgUnitItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].authority", is(orgUnitItem.getID().toString())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].confidence", is(600))
              )));

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", Matchers.allOf(
              // check Boychuk, Mykhaylo
              hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem.getName())),
              hasJsonPath("$.metadata['dc.contributor.author'][0].authority", is(personItem.getID().toString())),
              hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(600)),
              // Giamminonni, Luca
              hasJsonPath("$.metadata['dc.contributor.author'][1].value", is(personItem2.getName())),
              hasJsonPath("$.metadata['dc.contributor.author'][1].authority", is(personItem2.getID().toString())),
              hasJsonPath("$.metadata['dc.contributor.author'][1].confidence", is(600))
              )));

        getClient(tokenAdmin).perform(delete("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
             // check Boychuk, Mykhaylo
             hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][0].authority", nullValue()),
             hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(-1)),
             // check Giamminonni, Luca
             hasJsonPath("$.metadata['dc.contributor.author'][1].value", is(personItem2.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].authority", is(personItem2.getID().toString())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].confidence", is(600))
             )));
    }

    @Test
    public void checkCleanAllModeWhileDeletionPersonItemTest() throws Exception {

        configurationService.setProperty("item-deletion.authority-cleanup.enabled", true);
        // configure CLEAN_ALL_MODE for dc.contributor.author metadata
        configurationService.setProperty(AUTHORITY_CLEANUP_PROPERTY_PREFIX + "dc.contributor.author",
                                         AUTHORITY_CLEANUP_CLEAN_ALL_MODE);

        // Clear caches after test-specific configuration
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection of Publications")
                                                   .build();

        Collection persons = CollectionBuilder.createCollection(context, parentCommunity)
                                              .withName("Collection of Persons")
                                              .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection of OrgUnits")
                                               .build();

        Item orgUnitItem = ItemBuilder.createItem(context, orgUnits)
                                      .withTitle("4Science")
                                      .withEntityType("OrgUnit")
                                      .build();

        Item personItem = ItemBuilder.createItem(context, persons)
                                     .withTitle("Boychuk, Mykhaylo")
                                     .withOrcidIdentifier("0001-002-0003-0001")
                                     .withScopusAuthorIdentifier("12345678001")
                                     .withAffiliation("4Science", orgUnitItem.getID().toString())
                                     .withEntityType("Person")
                                     .build();

        Item personItem2 = ItemBuilder.createItem(context, persons)
                                      .withTitle("Giamminonni, Luca")
                                      .withOrcidIdentifier("0000-0002-8310-6788")
                                      .withScopusAuthorIdentifier("12345678002")
                                      .withAffiliation("4Science", orgUnitItem.getID().toString())
                                      .withEntityType("Person")
                                      .build();

        Item publicationItem = ItemBuilder.createItem(context, publications)
                                          .withTitle("New functionalities of the DSpace7")
                                          .withAuthor("Boychuk, Mykhaylo", personItem.getID().toString())
                                          .withAuthor("Giamminonni, Luca", personItem2.getID().toString())
                                          .withIssueDate("2023-02-14")
                                          .withSubject("ExtraEntry")
                                          .withEntityType("Publication")
                                          .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", Matchers.allOf(
              hasJsonPath("$.name", is(personItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].value", is(orgUnitItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].authority", is(orgUnitItem.getID().toString())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].confidence", is(600))
              )));

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
             // check Boychuk, Mykhaylo
             hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][0].authority", is(personItem.getID().toString())),
             hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(600)),
             // check Giamminonni, Luca
             hasJsonPath("$.metadata['dc.contributor.author'][1].value", is(personItem2.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].authority", is(personItem2.getID().toString())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].confidence", is(600))
             )));

        getClient(tokenAdmin).perform(delete("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
             // Giamminonni, Luca shift to the 1 position
             hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem2.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][0].authority", is(personItem2.getID().toString())),
             hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(600))
             )))
             // and Boychuk, Mykhaylo was deleted
            .andExpect(jsonPath("$.metadata['dc.contributor.author'][1].value").doesNotExist())
            .andExpect(jsonPath("$.metadata['dc.contributor.author'][1].authority").doesNotExist())
            .andExpect(jsonPath("$.metadata['dc.contributor.author'][1].confidence").doesNotExist());
    }

    @Test
    public void checkWithoutAnyModeWhileDeletionPersonItemTest() throws Exception {
        configurationService.setProperty("item-deletion.authority-cleanup.enabled", true);
        // dc.contributor.author metadata with out any mode
        configurationService.setProperty(AUTHORITY_CLEANUP_PROPERTY_PREFIX + "dc.contributor.author", "");
        configurationService.setProperty(AUTHORITY_CLEANUP_PROPERTY_PREFIX + "default",
                                         AUTHORITY_CLEANUP_BUSINESS_MODE);

        // Clear caches after test-specific configuration
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection of Publications")
                                                   .build();

        Collection persons = CollectionBuilder.createCollection(context, parentCommunity)
                                              .withName("Collection of Persons")
                                              .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection of OrgUnits")
                                               .build();

        Item orgUnitItem = ItemBuilder.createItem(context, orgUnits)
                                      .withTitle("4Science")
                                      .withEntityType("OrgUnit")
                                      .build();

        Item personItem = ItemBuilder.createItem(context, persons)
                                     .withTitle("Boychuk, Mykhaylo")
                                     .withOrcidIdentifier("0001-002-0003-0001")
                                     .withScopusAuthorIdentifier("12345678001")
                                     .withAffiliation("4Science", orgUnitItem.getID().toString())
                                     .withEntityType("Person")
                                     .build();

        Item personItem2 = ItemBuilder.createItem(context, persons)
                                      .withTitle("Giamminonni, Luca")
                                      .withOrcidIdentifier("0000-0002-8310-6788")
                                      .withScopusAuthorIdentifier("12345678002")
                                      .withAffiliation("4Science", orgUnitItem.getID().toString())
                                      .withEntityType("Person")
                                      .build();

        Item publicationItem = ItemBuilder.createItem(context, publications)
                                          .withTitle("New functionalities of the DSpace7")
                                          .withAuthor("Boychuk, Mykhaylo", personItem.getID().toString())
                                          .withAuthor("Giamminonni, Luca", personItem2.getID().toString())
                                          .withIssueDate("2023-02-14")
                                          .withSubject("ExtraEntry")
                                          .withEntityType("Publication")
                                          .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", Matchers.allOf(
              hasJsonPath("$.name", is(personItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].value", is(orgUnitItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].authority", is(orgUnitItem.getID().toString())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].confidence", is(600))
              )));

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", Matchers.allOf(
              // check Boychuk, Mykhaylo
              hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem.getName())),
              hasJsonPath("$.metadata['dc.contributor.author'][0].authority", is(personItem.getID().toString())),
              hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(600)),
              // Giamminonni, Luca
              hasJsonPath("$.metadata['dc.contributor.author'][1].value", is(personItem2.getName())),
              hasJsonPath("$.metadata['dc.contributor.author'][1].authority", is(personItem2.getID().toString())),
              hasJsonPath("$.metadata['dc.contributor.author'][1].confidence", is(600))
              )));

        getClient(tokenAdmin).perform(delete("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
             // check Boychuk, Mykhaylo
             hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][0].authority",
                      is("will be referenced::ORCID::0001-002-0003-0001")),
             hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(-1)),
             // check Giamminonni, Luca
             hasJsonPath("$.metadata['dc.contributor.author'][1].value", is(personItem2.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].authority", is(personItem2.getID().toString())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].confidence", is(600))
             )));
    }

    @Test
    public void turnOffCleanupModeTest() throws Exception {
        configurationService.setProperty("item-deletion.authority-cleanup.enabled", false);
        // configure BUSINESS_MODE for dc.contributor.author metadata
        configurationService.setProperty(AUTHORITY_CLEANUP_PROPERTY_PREFIX + "dc.contributor.author",
                                         AUTHORITY_CLEANUP_BUSINESS_MODE);
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection of Publications")
                                                   .build();

        Collection persons = CollectionBuilder.createCollection(context, parentCommunity)
                                              .withName("Collection of Persons")
                                              .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection of OrgUnits")
                                               .build();

        Item orgUnitItem = ItemBuilder.createItem(context, orgUnits)
                                      .withTitle("4Science")
                                      .withEntityType("OrgUnit")
                                      .build();

        Item personItem = ItemBuilder.createItem(context, persons)
                                     .withTitle("Boychuk, Mykhaylo")
                                     .withOrcidIdentifier("0001-002-0003-0001")
                                     .withScopusAuthorIdentifier("12345678001")
                                     .withAffiliation("4Science", orgUnitItem.getID().toString())
                                     .withEntityType("Person")
                                     .build();

        Item personItem2 = ItemBuilder.createItem(context, persons)
                                      .withTitle("Giamminonni, Luca")
                                      .withOrcidIdentifier("0000-0002-8310-6788")
                                      .withScopusAuthorIdentifier("12345678002")
                                      .withAffiliation("4Science", orgUnitItem.getID().toString())
                                      .withEntityType("Person")
                                      .build();

        Item publicationItem = ItemBuilder.createItem(context, publications)
                                          .withTitle("New functionalities of the DSpace7")
                                          .withAuthor("Boychuk, Mykhaylo", personItem.getID().toString())
                                          .withAuthor("Giamminonni, Luca", personItem2.getID().toString())
                                          .withIssueDate("2023-02-14")
                                          .withSubject("ExtraEntry")
                                          .withEntityType("Publication")
                                          .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", Matchers.allOf(
              hasJsonPath("$.name", is(personItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].value", is(orgUnitItem.getName())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].authority", is(orgUnitItem.getID().toString())),
              hasJsonPath("$.metadata['person.affiliation.name'][0].confidence", is(600))
              )));

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", Matchers.allOf(
              // check Boychuk, Mykhaylo
              hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem.getName())),
              hasJsonPath("$.metadata['dc.contributor.author'][0].authority", is(personItem.getID().toString())),
              hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(600)),
              // Giamminonni, Luca
              hasJsonPath("$.metadata['dc.contributor.author'][1].value", is(personItem2.getName())),
              hasJsonPath("$.metadata['dc.contributor.author'][1].authority", is(personItem2.getID().toString())),
              hasJsonPath("$.metadata['dc.contributor.author'][1].confidence", is(600))
              )));

        getClient(tokenAdmin).perform(delete("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
             // check Boychuk, Mykhaylo
             hasJsonPath("$.metadata['dc.contributor.author'][0].value", is(personItem.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][0].authority", is(personItem.getID().toString())),
             hasJsonPath("$.metadata['dc.contributor.author'][0].confidence", is(600)),
             // check Giamminonni, Luca
             hasJsonPath("$.metadata['dc.contributor.author'][1].value", is(personItem2.getName())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].authority", is(personItem2.getID().toString())),
             hasJsonPath("$.metadata['dc.contributor.author'][1].confidence", is(600))
             )));
    }

}
