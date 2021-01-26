/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.orcid;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.CrisLayoutBoxMatcher;
import org.dspace.app.rest.matcher.CrisLayoutTabMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.CrisLayoutTabBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test class verify the REST Services for the Layout Tabs and Boxes
 * functionalities for the ORCID custom boxes
 * 
 *
 */
public class ORCIDTabIT extends AbstractControllerIntegrationTest {

    @Autowired
    private MetadataSchemaService mdss;

    @Autowired
    private MetadataFieldService mfss;

    /**
     * Test for endpoint /api/layout/tabs/<ID> using the ORCID tab default definition
     * @throws Exception
     */
    @Test
    public void ORCIDDefaulLayoutTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection personCol = CollectionBuilder.createCollection(context, parentCommunity)
                         .withRelationshipType(eTypePer.getLabel())
                         .withName("Collection 1")
                         .build();

        MetadataSchema schema = mdss.find(context, "person");
        MetadataField firstName = mfss.findByElement(context, schema, "givenName", null);
        MetadataField lastName = mfss.findByElement(context, schema, "familyName", null);
        MetadataField birthDate = mfss.findByElement(context, schema, "birthDate", null);
        MetadataField orcidMetadata = mfss.findByElement(context, schema, "identifier", "orcid");

        // Create tabs for Person Entity
       CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
           .withShortname("box1")
           .withHeader("Box person 1")
           .withSecurity(LayoutSecurity.PUBLIC)
           .build();
       CrisLayoutFieldBuilder.createMetadataField(context, lastName, 0, 1)
           .withLabel("LAST NAME")
           .withRendering("TEXT")
           .withBox(boxOne)
           .build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
            .withShortName("tab1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("tab person 1")
            .addBox(boxOne)
            .build();

        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("box2")
            .withHeader("Box person 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, firstName, 0, 1)
            .withLabel("FIRST NAME")
            .withRendering("TEXT")
            .withBox(boxTwo)
            .build();

        CrisLayoutBox boxThree = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("box3")
            .withHeader("Box person 3")
            .withSecurity(LayoutSecurity.ADMINISTRATOR)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, birthDate, 0, 1)
            .withLabel("Birthdate")
            .withRendering("TEXT")
            .withBox(boxThree)
            .build();

        CrisLayoutTab tab2 = CrisLayoutTabBuilder.createTab(context, eTypePer, 1)
            .withShortName("tab2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Tab person 2")
            .addBox(boxTwo)
            .addBox(boxThree)
            .build();

        CrisLayoutBox boxFour = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
                .withShortname("box4")
                .withHeader("Person identifiers")
                .withSecurity(LayoutSecurity.PUBLIC)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, orcidMetadata, 0, 1)
              .withLabel("ORCID")
              .withRendering("TEXT")
              .withBox(boxFour)
              .build();
        CrisLayoutTab tab3 = CrisLayoutTabBuilder.createTab(context, eTypePer, 2)
            .withShortName("tab3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Tab person 3")
            .addBox(boxFour)
            .build();

        // Create the orcid boxes and tab
        CrisLayoutBox orcidsyncqueue = CrisLayoutBoxBuilder
                .createBuilder(context, eTypePer, "ORCID_SYNC_QUEUE", false, false)
                .withShortname("orcidsyncqueue")
                .withHeader("ORCID Registry Queue")
                .withSecurity(LayoutSecurity.OWNER_ONLY)
                .build();
        CrisLayoutBox orcidsyncsettings = CrisLayoutBoxBuilder
                .createBuilder(context, eTypePer, "ORCID_SYNC_SETTINGS", false, false)
                .withShortname("orcidsyncsettings")
                .withHeader("ORCID Synchronization settings")
                .withSecurity(LayoutSecurity.OWNER_ONLY)
                .build();
        CrisLayoutBox orcidauthorizations = CrisLayoutBoxBuilder
                .createBuilder(context, eTypePer, "ORCID_AUTHORIZATIONS", false, false)
                .withShortname("orcidauthorizations")
                .withHeader("ORCID Authorizations")
                .withSecurity(LayoutSecurity.OWNER_ONLY)
                .build();
        CrisLayoutTab orcidTab = CrisLayoutTabBuilder.createTab(context, eTypePer, 3)
                .withShortName("orcid")
                .withSecurity(LayoutSecurity.OWNER_ONLY)
                .withHeader("ORCID")
                .addBox(orcidsyncqueue)
                .addBox(orcidsyncsettings)
                .addBox(orcidauthorizations)
                .build();

        // create some people
        // 1. linked to an user account
        // Create new person item
        Item epersonProfile = ItemBuilder.createItem(context, personCol)
            .withPersonIdentifierFirstName("John")
            .withPersonIdentifierLastName("Smith")
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withBirthDate("1999-11-22")
            .withRelationshipType(eTypePer.getLabel())
            .withCrisOwner(eperson.getFullName(), eperson.getID().toString())
            .build();

        // 2. linked to the admin account
        Item adminProfile = ItemBuilder.createItem(context, personCol)
                .withPersonIdentifierFirstName("Andrea")
                .withPersonIdentifierLastName("Bollini")
                .withOrcidIdentifier("0000-0002-9029-1854")
                .withBirthDate("1980-08-04")
                .withRelationshipType(eTypePer.getLabel())
                .withCrisOwner(admin.getFullName(), admin.getID().toString())
                .build();

        // 3. not linked to anyone
        Item anotherProfile = ItemBuilder.createItem(context, personCol)
                .withPersonIdentifierFirstName("Mario")
                .withPersonIdentifierLastName("Rossi")
                .withOrcidIdentifier("4444-3333-2222-1111")
                .withBirthDate("1944-04-17")
                .withRelationshipType(eTypePer.getLabel())
                .build();

        // 4. linked to another user but without orcid
        EPerson anotherEPerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Another", "EPerson")
                .withEmail("another@example.com")
                .withPassword(password)
                .build();
        Item anotherEPersonProfile = ItemBuilder.createItem(context, personCol)
                .withPersonIdentifierFirstName("Carlo")
                .withPersonIdentifierLastName("Verdi")
                .withBirthDate("1966-01-21")
                .withRelationshipType(eTypePer.getLabel())
                .withCrisOwner(anotherEPerson.getFullName(), anotherEPerson.getID().toString())
                .build();
        context.restoreAuthSystemState();

        // Get the orcid tab by id from REST service and check its response
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);
        String anotherEpersonToken = getAuthToken(anotherEPerson.getEmail(), password);
        getClient().perform(get("/api/layout/tabs/" + orcidTab.getID()).param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(orcidTab))));

        getClient(epersonToken).perform(get("/api/layout/tabs/" + orcidTab.getID()).param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(orcidTab))));

        getClient(adminToken).perform(get("/api/layout/tabs/" + orcidTab.getID()).param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(orcidTab))));

        // Test with anonymous user
        getClient().perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", epersonProfile.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.tabs", Matchers.contains(
                    CrisLayoutTabMatcher.matchTab(tab),
                    CrisLayoutTabMatcher.matchTab(tab2),
                    CrisLayoutTabMatcher.matchTab(tab3)
                    )))
//            there is not embedding of boxes for now
//            .andExpect(jsonPath("$._embedded.tabs[1]._embedded.boxes", Matchers.contains(
//                    CrisLayoutBoxMatcher.matchBox(boxTwo))))
            ;

        // Test with another user
        getClient(anotherEpersonToken).perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", epersonProfile.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.tabs", Matchers.contains(
                    CrisLayoutTabMatcher.matchTab(tab),
                    CrisLayoutTabMatcher.matchTab(tab2),
                    CrisLayoutTabMatcher.matchTab(tab3)
                    )))
//          there is not embedding of boxes for now
//            .andExpect(jsonPath("$._embedded.tabs[1]._embedded.boxes", Matchers.contains(
//                    CrisLayoutBoxMatcher.matchBox(boxTwo))))
            ;

        // Test with the admin user
        getClient(adminToken).perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", epersonProfile.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.tabs", Matchers.contains(
                    CrisLayoutTabMatcher.matchTab(tab),
                    CrisLayoutTabMatcher.matchTab(tab2),
                    CrisLayoutTabMatcher.matchTab(tab3)
                    // nor the admin see the orcid tab
                    )))
//          there is not embedding of boxes for now
//            .andExpect(jsonPath("$._embedded.tabs[1]._embedded.boxes", Matchers.contains(
//                    CrisLayoutBoxMatcher.matchBox(boxTwo),
//                    // the admin see box three
//                    CrisLayoutBoxMatcher.matchBox(boxThree))))
            ;

        // Test with the eperson user
        getClient(epersonToken).perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", epersonProfile.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)))
            .andExpect(jsonPath("$._embedded.tabs", Matchers.contains(
                    CrisLayoutTabMatcher.matchTab(tab),
                    CrisLayoutTabMatcher.matchTab(tab2),
                    CrisLayoutTabMatcher.matchTab(tab3),
                    // eperson see it own orcid tab
                    CrisLayoutTabMatcher.matchTab(orcidTab)
                    )))
//          there is not embedding of boxes for now
//            .andExpect(jsonPath("$._embedded.tabs[1]._embedded.boxes", Matchers.contains(
//                    // eperson doesn't see box three
//                    CrisLayoutBoxMatcher.matchBox(boxTwo))))
//            .andExpect(jsonPath("$._embedded.tabs[3]._embedded.boxes", Matchers.contains(
//                    CrisLayoutBoxMatcher.matchBox(orcidauthorizations),
//                    CrisLayoutBoxMatcher.matchBox(orcidsyncsettings),
//                    CrisLayoutBoxMatcher.matchBox(orcidsyncqueue))))
            ;
        // check the boxes
        getClient(epersonToken).perform(get("/api/layout/boxes/search/findByItem")
                .param("uuid", epersonProfile.getID().toString())
                .param("tab", orcidTab.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.boxes", Matchers.containsInAnyOrder(
                    CrisLayoutBoxMatcher.matchBox(orcidauthorizations),
                    CrisLayoutBoxMatcher.matchBox(orcidsyncsettings),
                    CrisLayoutBoxMatcher.matchBox(orcidsyncqueue))));

        getClient(adminToken).perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", adminProfile.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4))) // only two tabs have contents to show
            .andExpect(jsonPath("$._embedded.tabs", Matchers.contains(
                    CrisLayoutTabMatcher.matchTab(tab),
                    CrisLayoutTabMatcher.matchTab(tab2),
                    CrisLayoutTabMatcher.matchTab(tab3),
                    CrisLayoutTabMatcher.matchTab(orcidTab)
                    )))
//          there is not embedding of boxes for now
//            .andExpect(jsonPath("$._embedded.tabs[1]._embedded.boxes", Matchers.contains(
//                    CrisLayoutBoxMatcher.matchBox(boxTwo),
//                    // the admin see box three
//                    CrisLayoutBoxMatcher.matchBox(boxThree))))
//            .andExpect(jsonPath("$._embedded.tabs[3]._embedded.boxes", Matchers.containsInAnyOrder(
//                    CrisLayoutBoxMatcher.matchBox(orcidauthorizations),
//                    CrisLayoutBoxMatcher.matchBox(orcidsyncsettings),
//                    CrisLayoutBoxMatcher.matchBox(orcidsyncqueue))))
            ;
        // check the boxes
        getClient(adminToken).perform(get("/api/layout/boxes/search/findByItem")
                .param("uuid", adminProfile.getID().toString())
                .param("tab", orcidTab.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.boxes", Matchers.containsInAnyOrder(
                    CrisLayoutBoxMatcher.matchBox(orcidauthorizations),
                    CrisLayoutBoxMatcher.matchBox(orcidsyncsettings),
                    CrisLayoutBoxMatcher.matchBox(orcidsyncqueue))));

        // Test with the admin user over the not linked profile
        getClient(adminToken).perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", anotherProfile.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.tabs", Matchers.contains(
                    CrisLayoutTabMatcher.matchTab(tab),
                    CrisLayoutTabMatcher.matchTab(tab2),
                    CrisLayoutTabMatcher.matchTab(tab3)
                    // nor the admin see the orcid tab
                    )))
//          there is not embedding of boxes for now
//            .andExpect(jsonPath("$._embedded.tabs[1]._embedded.boxes", Matchers.containsInAnyOrder(
//                    CrisLayoutBoxMatcher.matchBox(boxTwo),
//                    // the admin see box three
//                    CrisLayoutBoxMatcher.matchBox(boxThree))))
            ;
        // Test with the eperson user over the not linked profile
        getClient(epersonToken).perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", anotherProfile.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.tabs", Matchers.contains(
                    CrisLayoutTabMatcher.matchTab(tab),
                    CrisLayoutTabMatcher.matchTab(tab2),
                    CrisLayoutTabMatcher.matchTab(tab3)
                    // nor the admin see the orcid tab
                    )))
//          there is not embedding of boxes for now
//            .andExpect(jsonPath("$._embedded.tabs[1]._embedded.boxes", Matchers.containsInAnyOrder(
//                    CrisLayoutBoxMatcher.matchBox(boxTwo),
//                    // the admin see box three
//                    CrisLayoutBoxMatcher.matchBox(boxThree))))
            ;
        // Test with the admin user over the linked profile without orcid
        getClient(adminToken).perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", anotherEPersonProfile.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)))
            .andExpect(jsonPath("$._embedded.tabs", Matchers.contains(
                    CrisLayoutTabMatcher.matchTab(tab),
                    CrisLayoutTabMatcher.matchTab(tab2)
                    // the tab3 is empty
                    // nor the admin see the orcid tab
                    )))
//          there is not embedding of boxes for now
//            .andExpect(jsonPath("$._embedded.tabs[1]._embedded.boxes", Matchers.contains(
//                    CrisLayoutBoxMatcher.matchBox(boxTwo),
//                    // the admin see box three
//                    CrisLayoutBoxMatcher.matchBox(boxThree))))
            ;
        // test with the another eperson owner on a profile without orcid
        getClient(anotherEpersonToken).perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", anotherEPersonProfile.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.tabs", Matchers.contains(
                    CrisLayoutTabMatcher.matchTab(tab),
                    CrisLayoutTabMatcher.matchTab(tab2),
                    // the tab3 is empty
                    // eperson see it own orcid tab
                    CrisLayoutTabMatcher.matchTab(orcidTab)
                    )))
//          there is not embedding of boxes for now
//            .andExpect(jsonPath("$._embedded.tabs[1]._embedded.boxes", Matchers.contains(
//                    // eperson doesn't see box three
//                    CrisLayoutBoxMatcher.matchBox(boxTwo))))
//            .andExpect(jsonPath("$._embedded.tabs[3]._embedded.boxes", Matchers.containsInAnyOrder(
//                    CrisLayoutBoxMatcher.matchBox(orcidauthorizations)
//                    // there are no settings or queue boxes as the orcid is missing
//                    )))
            ;
        // check the boxes
        getClient(anotherEpersonToken).perform(get("/api/layout/boxes/search/findByItem")
                .param("uuid", anotherEPersonProfile.getID().toString())
                .param("tab", orcidTab.getID().toString())
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.boxes", Matchers.contains(
                    CrisLayoutBoxMatcher.matchBox(orcidauthorizations)
                    )));
    }
}
