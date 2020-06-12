/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.layout;

import static com.jayway.jsonpath.JsonPath.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.CrisLayoutBoxBuilder;
import org.dspace.app.rest.builder.CrisLayoutFieldBuilder;
import org.dspace.app.rest.builder.CrisLayoutTabBuilder;
import org.dspace.app.rest.builder.EntityTypeBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.converter.CrisLayoutBoxConverter;
import org.dspace.app.rest.matcher.CrisLayoutBoxMatcher;
import org.dspace.app.rest.matcher.CrisLayoutTabMatcher;
import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
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
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test class verify the REST Services for the Layout Tabs functionality (endpoint /api/layout/tabs)
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class TabsRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private MetadataSchemaService mdss;

    @Autowired
    private MetadataFieldService mfss;

    @Autowired
    private CrisLayoutBoxConverter boxConverter;
    /**
     * Test for endopint /api/layout/tabs/<ID>. It returns a determinate tab
     * identified by its ID
     * @throws Exception
     */
    @Test
    public void getSingleTab() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        // Create new Tab for Person Entity
        CrisLayoutTabBuilder.createTab(context, eType, 0)
                .withShortName("New Person Tab")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("New Person Tab header")
                .build();
        // Create new EntityType Publication
        EntityType eTypeTwo = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create new Tab for Publication Entity
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eTypeTwo, 0)
                .withShortName("First Publication Tab")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("First Publication Tab header")
                .build();
        // Create second Tab for Publication Entity
        CrisLayoutTabBuilder.createTab(context, eTypeTwo, 0)
                .withShortName("Second Publication Tab")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("Second Publication Tab header")
                .build();
        context.restoreAuthSystemState();
        // Get created tab by id from REST service and check its response
        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(tab))));
    }

    /**
     * Test for endpoint /api/layout/tabs/<ID_TAB>/boxes. It returns all the boxes
     * included in the tab. The boxes are sorted by priority ascending.
     * This endpoint is reseved for the admin user
     * @throws Exception
     */
    @Test
    public void getTabBoxes() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        // Create new Boxes
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
                .withHeader("First New Box Header")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname for new first box")
                .withStyle("STYLE")
                .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
                .withHeader("Second New Box Header")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname for new second box")
                .withStyle("STYLE")
                .build();
        // Create new Tab for Person Entity with two boxes
        CrisLayoutTabBuilder.createTab(context, eType, 0)
                .withShortName("New Tab shortname")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("New Tab header")
                .addBox(boxOne)
                .addBox(boxTwo)
                .build();
        // Create new Boxes
        CrisLayoutBox boxThree = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
                .withHeader("Third New Box Header - priority 0")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname 3")
                .withStyle("STYLE")
                .build();
        // Create new Boxes
        CrisLayoutBox boxFour = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 1, false)
                .withHeader("Fourth New Box Header - priority 1")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname 4")
                .withStyle("STYLE")
                .build();
        // Create new Boxes
        CrisLayoutBox boxFive = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 2, false)
                .withHeader("Fifth New Box Header - priority 2")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname 5")
                .withStyle("STYLE")
                .build();
        // Create new Tab for Person Entity with three boxes
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                .withShortName("Another New Tab shortname")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("New Tab header")
                .addBox(boxThree)
                .addBox(boxFour)
                .addBox(boxFive)
                .build();
        // Create new Tab for Person Entity without boxes
        CrisLayoutTabBuilder.createTab(context, eType, 0)
                .withShortName("Tab no box")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("New Tab header")
                .build();
        context.restoreAuthSystemState();
        // Test without authentication
        getClient().perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
            .andExpect(status().isUnauthorized()); // 401 Unauthorized;
        // Test with non admin user
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
            .andExpect(status().isForbidden()); // 403 - user haven't sufficient permission
        // Test with admin
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.boxes", Matchers.not(Matchers.empty())))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.boxes[0]", Matchers.is(
                    CrisLayoutBoxMatcher.matchBox(boxThree))))
            .andExpect(jsonPath("$._embedded.boxes[1]", Matchers.is(
                    CrisLayoutBoxMatcher.matchBox(boxFour))))
            .andExpect(jsonPath("$._embedded.boxes[2]", Matchers.is(
                    CrisLayoutBoxMatcher.matchBox(boxFive))));
    }

    /**
     * Test for endpoint /api/layout/tabs/<ID_TAB>/securitymetadata.
     * It returns all the metadatafields that define the security.
     * This endpoint is reseved for the admin user
     * @throws Exception
     */
    @Test
    public void getTabMetadatasecurity() throws Exception {
        context.turnOffAuthorisationSystem();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField abs = mfss.findByElement(context, schema, "description", "abstract");
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        MetadataField extent = mfss.findByElement(context, schema, "format", "extent");
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tabs
        CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(isbn)
            .addMetadatasecurity(uri)
            .build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(abs)
            .addMetadatasecurity(provenance)
            .addMetadatasecurity(sponsorship)
            .build();
        CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(extent)
            .build();
        context.restoreAuthSystemState();
        // Test without authentication
        getClient().perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
            .andExpect(status().isUnauthorized()); // 401 Unauthorized;
        // Test with non admin user
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
            .andExpect(status().isForbidden()); // 403 - user haven't sufficient permission
        // Test with admin user
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
    }

    /**
     * Test for endpoint /api/layout/tabs/search/findByItem?uuid=<ITEM-UUID>
     * The tabs are sorted by priority ascending. This are filtered based on the permission of the
     * current user and available data.
     * @throws Exception
     */
    @Test
    public void findByItem() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new community
        Community community = CommunityBuilder.createCommunity(context)
            .withName("Test Community")
            .withTitle("Title test community")
            .build();
        // Create new collection
        Collection collection = CollectionBuilder.createCollection(context, community)
            .withName("Test Collection")
            .build();
        // Create entity Type
        EntityTypeBuilder.createEntityTypeBuilder(context, "Publication")
            .build();
        EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person")
            .build();
        // Create new person item
        Item item = ItemBuilder.createItem(context, collection)
            .withPersonIdentifierFirstName("Danilo")
            .withPersonIdentifierLastName("Di Nuzzo")
            .withRelationshipType(eTypePer.getLabel())
            .build();
        MetadataSchema schema = mdss.find(context, "person");
        MetadataField firstName = mfss.findByElement(context, schema, "givenName", null);
        MetadataField lastName = mfss.findByElement(context, schema, "familyName", null);
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        // Create tabs for Person Entity
        CrisLayoutField fieldFirstname = CrisLayoutFieldBuilder.createField(context, firstName, 0, 1)
            .withBundle("BUNDLE")
            .withLabel("LAST NAME")
            .withRendering("TEXT")
            .build();
       CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, 0, false)
           .withShortname("Box shortname 1")
           .addField(fieldFirstname)
           .build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
            .withShortName("TabOne For Person - priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBox(boxOne)
            .build();
        CrisLayoutField field = CrisLayoutFieldBuilder.createField(context, lastName, 0, 1)
             .withBundle("BUNDLE")
             .withLabel("LAST NAME")
             .withRendering("TEXT")
             .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, 0, false)
            .withShortname("Box shortname 2")
            .addField(field)
            .build();
        CrisLayoutTab tabTwo = CrisLayoutTabBuilder.createTab(context, eTypePer, 1)
            .withShortName("TabTwo For Person - priority 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBox(box)
            .build();
        // tab without data
        CrisLayoutTabBuilder.createTab(context, eTypePer, 2)
            .withShortName("New Tab For Person 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(provenance)
            .addMetadatasecurity(sponsorship)
            .build();
        context.restoreAuthSystemState();
        // Test
        getClient().perform(get("/api/layout/tabs/search/findByItem").param("uuid", item.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2))) // only two tabs have contents to show
            .andExpect(jsonPath("$._embedded.tabs[0]", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(tab))))
            .andExpect(jsonPath("$._embedded.tabs[1]", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(tabTwo))));
    }

    /**
     * Test for endpoint /api/layout/tabs/search/findByEntityType?type=<:string>. It returns all the tabs
     * that are available for the items of the specified type. This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void findByEntityType() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type
        EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType eTypePub = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        // Create new Tab for Publication Entity
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eTypePub, 0)
            .withShortName("New Tab shortname priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        CrisLayoutTab tabTwo = CrisLayoutTabBuilder.createTab(context, eTypePub, 1)
            .withShortName("New Tab shortname priority 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        CrisLayoutTab tabThree = CrisLayoutTabBuilder.createTab(context, eTypePub, 2)
            .withShortName("New Tab shortname priority 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        // Create tabs for Person
        CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
            .withShortName("First Person Tab")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
            .withShortName("Second Person Tab")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .build();
        context.restoreAuthSystemState();
        // Test without authentication
        getClient().perform(get("/api/layout/tabs/search/findByEntityType")
            .param("type", tab.getEntity().getLabel()))
            .andExpect(status().isUnauthorized()); // 401 Unauthorized;
        // Test with a non admin user
        String token = getAuthToken(eperson.getEmail(), password);
        // Get created tab by id from REST service and check its response
        getClient(token).perform(get("/api/layout/tabs/search/findByEntityType")
            .param("type", tab.getEntity().getLabel()))
            .andExpect(status().isForbidden()); // 403 - user haven't sufficient permission
        // Get auth token of an admin user
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // Get created tab by id from REST service and check its response
        getClient(tokenAdmin).perform(get("/api/layout/tabs/search/findByEntityType")
            .param("type", eTypePub.getLabel()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)))
            .andExpect(jsonPath("$._embedded.tabs[0]", Matchers.is(
                CrisLayoutTabMatcher.matchTab(tab))))
            .andExpect(jsonPath("$._embedded.tabs[1]", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(tabTwo))))
            .andExpect(jsonPath("$._embedded.tabs[2]", Matchers.is(
                    CrisLayoutTabMatcher.matchTab(tabThree))));
    }

    /**
     * Test for endpoint POST /api/layout/tabs, Its create a new tab
     * This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void createTab() throws Exception {
        context.turnOffAuthorisationSystem();
        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            // Create entity type
            EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
            context.restoreAuthSystemState();

            CrisLayoutTabRest rest = new CrisLayoutTabRest();
            rest.setPriority(0);
            rest.setSecurity(0);
            rest.setShortname("short-name");
            rest.setHeader("header");
            rest.setEntityType(eTypePer.getLabel());

            ObjectMapper mapper = new ObjectMapper();
            // Test without authentication
            getClient()
                    .perform(post("/api/layout/tabs")
                            .content(mapper.writeValueAsBytes(rest))
                            .contentType(contentType))
                    .andExpect(status().isUnauthorized());
            // Test with a non admin user
            String token = getAuthToken(eperson.getEmail(), password);
            getClient(token)
                .perform(post("/api/layout/tabs")
                        .content(mapper.writeValueAsBytes(rest))
                        .contentType(contentType))
                .andExpect(status().isForbidden());
            // Test with admin user
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin)
                .perform(post("/api/layout/tabs")
                        .content(mapper.writeValueAsBytes(rest))
                        .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.is(
                    CrisLayoutTabMatcher.matchRest(rest))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            // Get created tab by id from REST service and check its response
            getClient().perform(get("/api/layout/tabs/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        CrisLayoutTabMatcher.matchRest(rest))));
        } finally {
            CrisLayoutTabBuilder.delete(idRef.get());
        }
    }

    /**
     * Test for endpoint DELETE /api/layout/tabs/<:id>, Its delete a tab
     * This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void deleteTab() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create new Tab for Person Entity
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                .withShortName("New Person Tab")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("New Person Tab header")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(
                get("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isOk())
        .andExpect(content().contentType(contentType));

        // Delete with anonymous user
        getClient().perform(
                delete("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isUnauthorized());

        // Delete with non admin user
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
                delete("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isForbidden());

        // delete with admin
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(
                delete("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(
                get("/api/layout/tabs/" + tab.getID())
        ).andExpect(status().isNotFound());
    }

    /**
     * Test for endpoint PATCH /api/layout/tabs/<:id>
     * 
     * Example: <code>
     * curl -X PATCH http://${dspace.server.url}/api/layout/tabs/<:id> -H "
     * Content-Type: application/json" -d '[{ "op": "remove", "path": "
     * /boxes/<:index>"]'
     * </code>
     */
    @Test
    public void removeBox() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        // Create new Boxes
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
                .withHeader("First New Box Header")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname for new first box")
                .withStyle("STYLE")
                .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 1, false)
                .withHeader("Second New Box Header")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname for new second box")
                .withStyle("STYLE")
                .build();
        // Create new Tab for Person Entity with two boxes
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                .withShortName("New Tab shortname")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("New Tab header")
                .addBox(boxOne)
                .addBox(boxTwo)
                .build();
        context.restoreAuthSystemState();

        // get boxes
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.boxes", Matchers.not(Matchers.empty())))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation removeOperation = new RemoveOperation("/boxes/0");
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);

        getClient().perform(patch("/api/layout/tabs/" + tab.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnauthorized());

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(patch("/api/layout/tabs/" + tab.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isForbidden());

        getClient(tokenAdmin).perform(patch("/api/layout/tabs/" + tab.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.boxes", Matchers.not(Matchers.empty())))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    /**
     * Test for endpoint PATCH /api/layout/tabs/<:id>
     * 
     * Example: <code>
     * curl -X PATCH http://${dspace.server.url}/api/layout/tabs/<:id> -H "
     * Content-Type: application/json" -d '[{ "op": "add", "path": "
     * /boxes/", "value": [{ ... box_object ... }]]'
     * </code>
     */
    @Test
    public void addBox() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        // Create new Boxes
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
                .withHeader("First New Box Header")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname for new first box")
                .withStyle("STYLE")
                .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 1, false)
                .withHeader("Second New Box Header")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname for new second box")
                .withStyle("STYLE")
                .build();
        // Create new Tab for Person Entity with two boxes
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                .withShortName("New Tab shortname")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withHeader("New Tab header")
                .build();
        context.restoreAuthSystemState();

        // No boxes at this time
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.boxes", Matchers.is(Matchers.empty())));

        List<Operation> ops = new ArrayList<Operation>();
        List<CrisLayoutBoxRest> boxes = new ArrayList<>();
        CrisLayoutBoxRest restBoxOne = boxConverter.convert(boxOne, null);
        CrisLayoutBoxRest restBoxTwo = boxConverter.convert(boxTwo, null);
        boxes.add(restBoxOne);
        boxes.add(restBoxTwo);
        AddOperation addOperation = new AddOperation("/boxes", boxes);
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);

        getClient().perform(patch("/api/layout/tabs/" + tab.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(patch("/api/layout/tabs/" + tab.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        getClient(tokenAdmin).perform(patch("/api/layout/tabs/" + tab.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // After patch the tab should be two box associated
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/boxes"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.boxes", Matchers.not(Matchers.empty())))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));
    }
}
