/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.layout;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.dspace.app.rest.builder.MetadataFieldBuilder;
import org.dspace.app.rest.builder.MetadataSchemaBuilder;
import org.dspace.app.rest.matcher.CrisLayoutBoxMatcher;
import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.model.MetadataSchemaRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
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
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test class verify the REST Services for the Layout Boxes functionality (endpoint /api/layout/boxes)
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class BoxesRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private MetadataSchemaService mdss;

    @Autowired
    private MetadataFieldService mfss;

    /**
     * Test for endpoint /api/layout/boxes/<:id>. It returns
     * detailed information about a specific box
     * @throws Exception
     */
    @Test
    public void getSingleBox() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
            .withShortname("Shortname 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
            .withShortname("Shortname 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
            .withShortname("Shortname 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        // Create new EntityType Person
        EntityType eTypePerson = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        CrisLayoutBoxBuilder.createBuilder(context, eTypePerson, false, false)
            .withShortname("Shortname 4")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        context.restoreAuthSystemState();
        // invoke service without authentication
        getClient().perform(get("/api/layout/boxes/" + box.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", Matchers.is(
                    CrisLayoutBoxMatcher.matchBox(box))));
    }

    /**
     * Test for endpoint /api/layout/boxes/search/findByItem?uuid=<:item-uuid>&tab=<:id>
     * It returns the boxes that are available for the specified item in the requested tab.
     * The boxes are sorted by priority ascending. This are filtered based on the permission
     * of the current user and available data in the items (empty boxes are not included).
     * @throws Exception
     */
    @Test
    public void findByItem() throws Exception {
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
        EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person")
            .build();
        // Create new person item
        Item item = ItemBuilder.createItem(context, collection)
            .withPersonIdentifierFirstName("Danilo")
            .withPersonIdentifierLastName("Di Nuzzo")
            .withRelationshipType(eTypePer.getLabel())
            .build();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "person");
        MetadataField firstName = mfss.findByElement(context, schema, "givenName", null);
        MetadataField lastName = mfss.findByElement(context, schema, "familyName", null);
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        // Create box without content
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Shortname 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        // Create boxes with content
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Shortname 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, firstName, 0, 0)
            .withLabel("ISSUE NUMBER")
            .withRendering("TEXT")
            .withBox(boxTwo)
            .build();
        CrisLayoutBox boxThree = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Shortname 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, lastName, 0, 0)
            .withLabel("ISSUE NUMBER")
            .withRendering("TEXT")
            .withBox(boxThree)
            .build();
        // Create tab
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
            .withShortName("Tab Shortname 1")
            .withHeader("tab header")
            .withSecurity(LayoutSecurity.PUBLIC)
            .addBox(boxOne)
            .addBox(boxTwo)
            .addBox(boxThree)
            .build();
        // Create box and tab for other entity type
        CrisLayoutBox boxFour = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
            .withShortname("Shortname 4")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("Tab Shortname 2")
            .withHeader("tab header")
            .addMetadatasecurity(provenance)
            .addMetadatasecurity(sponsorship)
            .withSecurity(LayoutSecurity.PUBLIC)
            .addBox(boxFour)
            .build();
        context.restoreAuthSystemState();
        // Test WS invocation
        getClient().perform(get("/api/layout/boxes/search/findByItem")
            .param("uuid", item.getID().toString())
            .param("tab", tab.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2))) // only two boxes have contents to show
            .andExpect(jsonPath("$._embedded.boxes[0]", Matchers.is(
                    CrisLayoutBoxMatcher.matchBox(boxTwo))))
            .andExpect(jsonPath("$._embedded.boxes[1]", Matchers.is(
                    CrisLayoutBoxMatcher.matchBox(boxThree))));
    }

    /**
     * Test for endpoint /api/layout/boxes/search/findByEntityType?type=<:string>
     * It returns the boxes that are available for the items of the specified type.
     * This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void findByEntityType() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type
        EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType eTypePub = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        // Create box without content
        CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Shortname 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePub, false, false)
            .withShortname("Shortname 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eTypePub, false, false)
            .withShortname("Shortname 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Shortname 4")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        context.restoreAuthSystemState();
        // Test without authentication
        getClient().perform(get("/api/layout/boxes/search/findByEntityType")
            .param("type", eTypePub.getLabel()))
            .andExpect(status().isUnauthorized()); // 401 Unauthorized;;
        // Test with non admin user
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/layout/boxes/search/findByEntityType")
            .param("type", eTypePub.getLabel()))
            .andExpect(status().isForbidden()); // 403 - user haven't sufficient permission;
        // Test with admin
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/boxes/search/findByEntityType")
            .param("type", eTypePub.getLabel()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)))
            .andExpect(jsonPath("$._embedded.boxes[0]", Matchers.is(
                    CrisLayoutBoxMatcher.matchBox(boxOne))))
            .andExpect(jsonPath("$._embedded.boxes[1]", Matchers.is(
                    CrisLayoutBoxMatcher.matchBox(boxTwo))));
    }

    /**
     * Test for endpoint /api/layout/boxes/<BOX_ID>/securitymetadata.
     * It returns all the metadatafields that define the security.
     * @throws Exception
     */
    @Test
    public void getBoxMetadatasecurity() throws Exception {
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
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("Box one shortname")
            .addMetadataSecurityField(sponsorship)
            .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("Box two shortname")
            .addMetadataSecurityField(isbn)
            .addMetadataSecurityField(uri)
            .addMetadataSecurityField(abs)
            .addMetadataSecurityField(provenance)
            .build();
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("Box three shortname")
            .addMetadataSecurityField(extent)
            .build();
        context.restoreAuthSystemState();
        // Test WS endpoint
        getClient().perform(get("/api/layout/boxes/" + boxTwo.getID() + "/securitymetadata"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)));
    }

    public void addSecurityMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                   .withShortname("Box shortname")
                                                   .build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        MetadataSchemaRest schema = new MetadataSchemaRest();
        schema.setNamespace("dc");
        MetadataFieldRest isbn = new MetadataFieldRest();
        isbn.setSchema(schema);
        isbn.setElement("identifier");
        isbn.setQualifier("isbn");
        isbn.setScopeNote(" Is a numeric commercial identifier ");

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/boxes/" + box.getID() + "/securitymetadata"))
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));


        getClient(tokenAdmin).perform(post("/api/layout/boxes/" + box.getID() + "/securitymetadata")
                             .content(mapper.writeValueAsBytes(isbn))
                             .contentType(contentType))
                             .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/layout/boxes/" + box.getID() + "/securitymetadata"))
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    public void addSecurityMetadataUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                            .withShortname("Box shortname")
                            .build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        MetadataFieldRest isbn = new MetadataFieldRest();
        MetadataSchemaRest schema = new MetadataSchemaRest();
        try {
            schema.setNamespace("dc");
            isbn.setSchema(schema);
            isbn.setElement("identifier");
            isbn.setQualifier("isbn");
            isbn.setScopeNote(" Is a numeric commercial identifier ");

            getClient().perform(post("/api/layout/boxes/" + box.getID() + "/securitymetadata")
                       .content(mapper.writeValueAsBytes(isbn))
                       .contentType(contentType))
                       .andExpect(status().isUnauthorized());

            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/layout/boxes/" + box.getID() + "/securitymetadata"))
                                 .andExpect(status().isOk())
                                 .andExpect(content().contentType(contentType))
                                 .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
        } finally {
            MetadataSchemaBuilder.deleteMetadataSchema(schema.getId());
            MetadataFieldBuilder.deleteMetadataField(isbn.getId());
        }
    }

    public void addSecurityMetadataForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                            .withShortname("Box shortname")
                            .build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        MetadataFieldRest isbn = new MetadataFieldRest();
        MetadataSchemaRest schema = new MetadataSchemaRest();
        try {
            schema.setNamespace("dc");
            isbn.setSchema(schema);
            isbn.setElement("identifier");
            isbn.setQualifier("isbn");
            isbn.setScopeNote(" Is a numeric commercial identifier ");

            String tokenEperson = getAuthToken(eperson.getEmail(), password);
            getClient(tokenEperson).perform(post("/api/layout/boxes/" + box.getID() + "/securitymetadata")
                                   .content(mapper.writeValueAsBytes(isbn))
                                   .contentType(contentType))
                                   .andExpect(status().isForbidden());

            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/layout/boxes/" + box.getID() + "/securitymetadata"))
                                 .andExpect(status().isOk())
                                 .andExpect(content().contentType(contentType))
                                 .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                                 .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
        } finally {
            MetadataSchemaBuilder.deleteMetadataSchema(schema.getId());
            MetadataFieldBuilder.deleteMetadataField(isbn.getId());
        }
    }

    public void addSecurityMetadataNotFoundBoxTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MetadataFieldRest isbn = new MetadataFieldRest();
        MetadataSchemaRest schema = new MetadataSchemaRest();
        try {
            schema.setNamespace("dc");
            isbn.setSchema(schema);
            isbn.setElement("identifier");
            isbn.setQualifier("isbn");
            isbn.setScopeNote(" Is a numeric commercial identifier ");

            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(post("/api/layout/boxes/" + UUID.randomUUID() + "/securitymetadata")
                                   .content(mapper.writeValueAsBytes(isbn))
                                   .contentType(contentType))
                                   .andExpect(status().isNotFound());
        } finally {
            MetadataSchemaBuilder.deleteMetadataSchema(schema.getId());
            MetadataFieldBuilder.deleteMetadataField(isbn.getId());
        }
    }

    public void addSecurityMetadataUnpTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                            .withShortname("Box shortname")
                            .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/layout/boxes/" + box.getID() + "/securitymetadata"))
                             .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Test for endpoint /api/layout/boxes/<BOX_ID>/configuration.
     * It returns configuration entity with more information specific a metadata box
     * @throws Exception
     */
    @Test
    public void getMetadataConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField abs = mfss.findByElement(context, schema, "description", "abstract");
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField extent = mfss.findByElement(context, schema, "format", "extent");
        // Create boxes
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("Box shortname 1")
                .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("Box shortname 2")
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, isbn, 0, 0)
                .withLabel("LABEL ISBN")
                .withRendering("RENDERIGN ISBN")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, uri, 0, 1)
                .withLabel("LABEL URI")
                .withRendering("RENDERIGN URI")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, abs, 1, 0)
                .withLabel("LABEL ABS")
                .withRendering("RENDERIGN ABS")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, provenance, 1, 1)
                .withLabel("LABEL PROVENANCE")
                .withRendering("RENDERIGN PROVENANCE")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, provenance, 1, 2)
                .withLabel("LABEL SPRONSORSHIP")
                .withRendering("RENDERIGN SPRONSORSHIP")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutFieldBuilder.createMetadataField(context, extent, 2, 0)
                .withLabel("LABEL EXTENT")
                .withRendering("RENDERIGN EXTENT")
                .withStyle("STYLE")
                .withBox(box)
                .build();
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("Box shortname 3")
                .build();
        context.restoreAuthSystemState();
        // Test WS endpoint
        getClient().perform(get("/api/layout/boxes/" + box.getID() + "/configuration"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", Matchers.is(box.getID())))
            .andExpect(jsonPath("$.rows.length()", Matchers.is(3)))
            .andExpect(jsonPath("$.rows[0].fields.length()", Matchers.is(2)))
            .andExpect(jsonPath("$.rows[1].fields.length()", Matchers.is(3)))
            .andExpect(jsonPath("$.rows[2].fields.length()", Matchers.is(1)));
    }

    /**
     * Test for endpoint /api/layout/boxes/<BOX_ID>/configuration.
     * It returns configuration entity with more information specific a relation box
     * @throws Exception
     */
    @Test
    public void getRelationConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        // Create a box
        CrisLayoutBox box = CrisLayoutBoxBuilder
                .createBuilder(context, eType, CrisLayoutBoxTypes.RELATION.name(), true, true)
                .withShortname("shortname1").build();
        context.restoreAuthSystemState();
        // Test WS endpoint
        getClient().perform(get("/api/layout/boxes/" + box.getID() + "/configuration")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType)).andExpect(jsonPath("$.id", Matchers.is(box.getID())))
                .andExpect(jsonPath("$.discovery-configuration", Matchers.is("RELATION.Person.shortname1")));
    }

    /**
     * Test for endpoint POST /api/layout/boxes, Its create a new boxes
     * This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void createBox() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type
        EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            CrisLayoutBoxRest rest = new CrisLayoutBoxRest();
            rest.setEntityType(eTypePer.getLabel());
            rest.setBoxType("box-type");
            rest.setClear(false);
            rest.setCollapsed(false);
            rest.setHeader("box-header");
            rest.setMinor(false);
            rest.setSecurity(0);
            rest.setShortname("shortname-box");
            rest.setStyle("style-box");

            ObjectMapper mapper = new ObjectMapper();
            // Test without authentication
            getClient()
                    .perform(post("/api/layout/boxes")
                            .content(mapper.writeValueAsBytes(rest))
                            .contentType(contentType))
                    .andExpect(status().isUnauthorized());
            // Test with a non admin user
            String token = getAuthToken(eperson.getEmail(), password);
            getClient(token)
                .perform(post("/api/layout/boxes")
                        .content(mapper.writeValueAsBytes(rest))
                        .contentType(contentType))
                .andExpect(status().isForbidden());
            // Test with admin user
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin)
                .perform(post("/api/layout/boxes")
                        .content(mapper.writeValueAsBytes(rest))
                        .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.is(
                        CrisLayoutBoxMatcher.matchRest(rest))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient().perform(get("/api/layout/boxes/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        CrisLayoutBoxMatcher.matchRest(rest))));
        } finally {
            CrisLayoutBoxBuilder.delete(idRef.get());
        }
    }

    /**
     * Test for endpoint DELETE /api/layout/boxes/<:id>, Its delete a box
     * This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void deleteBox() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("Box shortname 1")
                .withSecurity(LayoutSecurity.PUBLIC)
                .build();
        context.restoreAuthSystemState();

        getClient().perform(
                get("/api/layout/boxes/" + box.getID())
        ).andExpect(status().isOk())
        .andExpect(content().contentType(contentType));

        // Delete with anonymous user
        getClient().perform(
                delete("/api/layout/boxes/" + box.getID())
        ).andExpect(status().isUnauthorized());

        // Delete with non admin user
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
                delete("/api/layout/boxes/" + box.getID())
        ).andExpect(status().isForbidden());

        // delete with admin
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(
                delete("/api/layout/boxes/" + box.getID())
        ).andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(
                get("/api/layout/boxes/" + box.getID())
        ).andExpect(status().isNotFound());
    }

    @Test
    @Ignore
    public void patchBoxReplaceShortnameTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                            .withShortname("Box shortname")
                            .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String newShortname = "New Shortname";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/shortname", newShortname);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/boxes/" + box.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$", Matchers.allOf(
                         hasJsonPath("$.shortname", is(newShortname)),
                         hasJsonPath("$.header", is(box.getHeader()))
                         )));

        getClient().perform(get("/api/layout/boxes/" + box.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", is(newShortname)),
                           hasJsonPath("$.header", is(box.getHeader()))
                           )));
    }

    @Test
    @Ignore
    public void patchBoxRemoveShortnameTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                            .withShortname("Box shortname")
                            .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation removeOperation = new RemoveOperation("/shortname");
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/boxes/" + box.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$", Matchers.allOf(
                         hasJsonPath("$.shortname", nullValue()),
                         hasJsonPath("$.header", is(box.getHeader()))
                         )));

        getClient().perform(get("/api/layout/boxes/" + box.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", nullValue()),
                           hasJsonPath("$.header", is(box.getHeader()))
                           )));
    }

    @Test
    @Ignore
    public void patchBoxReplaceShortnameWrongPathTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                            .withShortname("Box shortname")
                            .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String newShortname = "New Shortname";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/wrongPath", newShortname);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/boxes/" + box.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isUnprocessableEntity());

        getClient().perform(get("/api/layout/boxes/" + box.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", is(box.getShortname())),
                           hasJsonPath("$.header", is(box.getHeader()))
                           )));
    }

    @Test
    @Ignore
    public void patchBoxReplaceShortnameNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String newShortname = "New Shortname";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/shortname", newShortname);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/boxes/" + UUID.randomUUID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isNotFound());
    }

    @Test
    @Ignore
    public void patchBoxAddShortnameTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true).build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String shortname = "Tab Shortname";
        List<Operation> ops = new ArrayList<Operation>();
        AddOperation addOperation = new AddOperation("/shortname", shortname);
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);;

        getClient(tokenAdmin).perform(patch("/api/layout/boxes/" + box.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$", Matchers.allOf(
                         hasJsonPath("$.shortname", is(shortname)),
                         hasJsonPath("$.header", is(box.getHeader()))
                         )));

        getClient().perform(get("/api/layout/boxes/" + box.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", is(shortname)),
                           hasJsonPath("$.header", is(box.getHeader()))
                           )));
    }

    @Test
    @Ignore
    public void patchBoxReplaceCollapsedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                            .withShortname("Box shortname")
                            .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        boolean newCollapsed = false;
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/collapsed", newCollapsed);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/boxes/" + box.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$", Matchers.allOf(
                         hasJsonPath("$.shortname", is(box.getShortname())),
                         hasJsonPath("$.collapsed", is(newCollapsed)),
                         hasJsonPath("$.header", is(box.getHeader()))
                         )));

        getClient().perform(get("/api/layout/boxes/" + box.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", is(box.getShortname())),
                           hasJsonPath("$.collapsed", is(newCollapsed)),
                           hasJsonPath("$.header", is(box.getHeader()))
                           )));
    }

    @Test
    @Ignore
    public void patchBoxReplaceCollapsedUnprocessableEntityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create box
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                            .withShortname("Box shortname")
                            .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String newCollapsed = "wrongValue";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/collapsed", newCollapsed);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/boxes/" + box.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isUnprocessableEntity());

        getClient().perform(get("/api/layout/boxes/" + box.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", is(box.getShortname())),
                           hasJsonPath("$.collapsed", is(box.getCollapsed())),
                           hasJsonPath("$.header", is(box.getHeader()))
                           )));
    }
}
