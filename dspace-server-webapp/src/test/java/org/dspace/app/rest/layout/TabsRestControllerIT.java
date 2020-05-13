/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.layout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.log4j.Logger;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.CrisLayoutBoxBuilder;
import org.dspace.app.rest.builder.CrisLayoutTabBuilder;
import org.dspace.app.rest.builder.EntityTypeBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.CrisLayoutTabMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutTab;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test class verify the REST Services for the Layout Tabs functionality (endpoint /api/layout/tabs)
 * 
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
public class TabsRestControllerIT extends AbstractControllerIntegrationTest {

    private static final Logger log = Logger.getLogger(TabsRestControllerIT.class);

    @Autowired
    private MetadataSchemaService mdss;

    @Autowired
    private MetadataFieldService mfss;

    /**
     * Test for endopint /api/layout/tabs/<ID>. It returns a determinate tab
     * identified by its ID
     * @throws Exception
     */
    @Test
    public void getSingleTab() throws Exception {
        try {
            context.turnOffAuthorisationSystem();
            // Create new EntityType Person
            EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
            // Create new Tab for Person Entity
            CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                    .withShortName("New Tab shortname")
                    .withSecurity(4)
                    .withHeader("New Tab header")
                    .build();
            context.restoreAuthSystemState();
            // Get created tab by id from REST service and check its response
            getClient().perform(get("/api/layout/tabs/" + tab.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        CrisLayoutTabMatcher.matchTab(tab))));
        } catch ( Exception e ) {
            log.error("getSingleTab ERROR! e:", e);
            // Throw a new exception with method information
            throw new Exception("Error in getSingleTab(), e: ", e);
        }
    }

    /**
     * Test for endpoin /api/layout/tabs/<ID_TAB>/boxes. It returns all the boxes
     * included in the tab. This endpoin is reseved for the admin user
     * @throws Exception
     */
    @Test
    public void getTabBoxes() throws Exception {
        try {
            context.turnOffAuthorisationSystem();
            // Create new EntityType Person
            EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
            // Create new Boxes
            CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
                    .withHeader("First New Box Header")
                    .withSecurity(0)
                    .withShortname("Shortname for new first box")
                    .withStyle("STYLE")
                    .build();
            CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
                    .withHeader("Second New Box Header")
                    .withSecurity(0)
                    .withShortname("Shortname for new second box")
                    .withStyle("STYLE")
                    .build();
            // Create new Tab for Person Entity
            CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                    .withShortName("New Tab shortname")
                    .withSecurity(4)
                    .withHeader("New Tab header")
                    .addBox(boxOne)
                    .addBox(boxTwo)
                    .build();
            context.restoreAuthSystemState();
            // Test without authentication
            getClient().perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
                .andExpect(status().is4xxClientError()); // 401 Unauthorized;
            // Test with non admin user
            String token = getAuthToken(eperson.getEmail(), password);
            getClient(token).perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
                .andExpect(status().is4xxClientError()); // 403 - user haven't sufficient permission
            // Test with admin
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.boxes", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));
        } catch (Exception e) {
            log.error("getTabWithBoxes ERROR! e:", e);
            // Throw a new exception with method information
            throw new Exception("Error in getTabWithBoxes(), e: ", e);
        }
    }

    /**
     * Test for endpoint /api/layout/tabs/<ID_TAB>/securitymetadata.
     * It returns all the metadatafields that defined the security.
     * This endpoin is reseved for the admin user
     * @throws Exception
     */
    @Test
    public void getTabMetadatasecurity() throws Exception {
        try {
            context.turnOffAuthorisationSystem();
            MetadataSchema schema = mdss.find(context, "dc");
            MetadataField fieldOne = mfss.findByElement(context, schema, "identifier", "isbn");
            MetadataField fieldTwo = mfss.findByElement(context, schema, "identifier", "uri");
            EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                    .withShortName("New Tab shortname")
                    .withSecurity(4)
                    .withHeader("New Tab header")
                    .addMetadatasecurity(fieldOne)
                    .addMetadatasecurity(fieldTwo)
                    .build();
            context.restoreAuthSystemState();
            // Test without authentication
            getClient().perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().is4xxClientError()); // 401 Unauthorized;
            // Test with non admin user
            String token = getAuthToken(eperson.getEmail(), password);
            getClient(token).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().is4xxClientError()); // 403 - user haven't sufficient permission
            // Test with admin user
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));
        } catch (Exception e) {
            log.error("getTabMetadatasecurity ERROR! e:", e);
            // Throw a new exception with method information
            throw new Exception("Error in getTabMetadatasecurity(), e: ", e);
        }
    }

    /**
     * Test for endpoin /api/layout/tabs/search/findByItem?uuid=
     * @throws Exception
     */
    @Test
    public void findByItem() throws Exception {
        try {
            context.turnOffAuthorisationSystem();
            Community community = CommunityBuilder.createCommunity(context)
                    .withName("Test Community")
                    .withTitle("Title test community")
                    .build();
            Collection collection = CollectionBuilder.createCollection(context, community)
                    .withName("Test Collection")
                    .build();
            EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication")
                    .build();
            Item item = ItemBuilder.createItem(context, collection)
                    .withAuthor("Danilo Di Nuzzo")
                    .withTitle("Test Content")
                    .withRelationshipType(eType.getLabel())
                    .build();
            CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                    .withShortName("New Tab For Publication")
                    .withSecurity(4)
                    .withHeader("New Tab header")
                    .build();
            context.restoreAuthSystemState();
            getClient().perform(get("/api/layout/tabs/search/findByItem").param("uuid", item.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.tabs[0]", Matchers.is(
                        CrisLayoutTabMatcher.matchTab(tab))));
        } catch (Exception e) {
            log.error("findByItem ERROR! e:", e);
            throw new Exception("Error in findByItem(), e: ", e);
        }
    }

    /**
     * Test for endpoin /api/layout/tabs/search/findByEntityType?type=<:string>. It returns all the tabs
     * that are available for the items of the specified type. This endpoint is reserved to system administrators
     * and this test invoke WS with admin user
     * @throws Exception
     */
    @Test
    public void findByEntityTypeAdmin() throws Exception {
        try {
            context.turnOffAuthorisationSystem();
            // Create new EntityType Person
            EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
            // Create new Tab for Person Entity
            CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                    .withShortName("New Tab shortname")
                    .withSecurity(4)
                    .withHeader("New Tab header")
                    .build();
            context.restoreAuthSystemState();
            // Test without authentication
            getClient().perform(get("/api/layout/tabs/search/findByEntityType")
                    .param("type", tab.getEntity().getLabel()))
                    .andExpect(status().is4xxClientError()); // 401 Unauthorized;
            // Test with a non admin user
            String token = getAuthToken(eperson.getEmail(), password);
            // Get created tab by id from REST service and check its response
            getClient(token).perform(get("/api/layout/tabs/search/findByEntityType")
                    .param("type", tab.getEntity().getLabel()))
                    .andExpect(status().is4xxClientError()); // 403 - user haven't sufficient permission
            // Get auth token of an admin user
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            // Get created tab by id from REST service and check its response
            getClient(tokenAdmin).perform(get("/api/layout/tabs/search/findByEntityType")
                    .param("type", eType.getLabel()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$._embedded.tabs[0]", Matchers.is(
                        CrisLayoutTabMatcher.matchTab(tab))));
        } catch ( Exception e ) {
            log.error("findByEntityType ERROR! e:", e);
            // Throw a new exception with method information
            throw new Exception("Error in findByEntityType(), e: ", e);
        }
    }

}
