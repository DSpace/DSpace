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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.CrisLayoutBoxBuilder;
import org.dspace.app.rest.builder.CrisLayoutFieldBuilder;
import org.dspace.app.rest.builder.CrisLayoutTabBuilder;
import org.dspace.app.rest.builder.EntityTypeBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.CrisLayoutBoxMatcher;
import org.dspace.app.rest.matcher.CrisLayoutFieldMatcher;
import org.dspace.app.rest.model.CrisLayoutBoxRest;
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
        CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
            .withShortname("Shortname 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
            .withShortname("Shortname 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
            .withShortname("Shortname 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        // Create new EntityType Person
        EntityType eTypePerson = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        CrisLayoutBoxBuilder.createBuilder(context, eTypePerson, false, 0, false)
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
     * Test for endpoint GET /api/layout/boxes/<:id>/fields
     * It returns all the fields included in the box.
     * @throws Exception
     */
    @Test
    public void getBoxFields() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create an empty box
        CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
                .withShortname("Box one shortname")
                .build();
        // Create fields for next box
        MetadataSchema dc = mdss.find(context, "dc");
        MetadataField mfOne = mfss.findByElement(context, dc, "description", "abstract");
        CrisLayoutField fieldOne = CrisLayoutFieldBuilder.createField(context, mfOne, 0, 0)
                .withBundle("bundle 1")
                .withLabel("label 1")
                .withRendering("rendering 1")
                .withStyle("style 1")
                .withType("type 1")
                .build();
        MetadataField mfTwo = mfss.findByElement(context, dc, "description", "provenance");
        CrisLayoutField fieldTwo = CrisLayoutFieldBuilder.createField(context, mfTwo, 0, 1)
                .withBundle("bundle 2")
                .withLabel("label 2")
                .withRendering("rendering 2")
                .withStyle("style 2")
                .withType("type 2")
                .build();
        MetadataField mfThree = mfss.findByElement(context, dc, "description", "sponsorship");
        CrisLayoutField fieldThree = CrisLayoutFieldBuilder.createField(context, mfThree, 0, 2)
                .withBundle("bundle 3")
                .withLabel("label 3")
                .withRendering("rendering 3")
                .withStyle("style 3")
                .withType("type 3")
                .build();
        MetadataField mfFour = mfss.findByElement(context, dc, "description", "statementofresponsibility");
        CrisLayoutField fieldFour = CrisLayoutFieldBuilder.createField(context, mfFour, 0, 3)
                .withBundle("bundle 4")
                .withLabel("label 4")
                .withRendering("rendering 4")
                .withStyle("style 4")
                .withType("type 4")
                .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 1, false)
                .withShortname("Box two shortname")
                .addField(fieldOne)
                .addField(fieldTwo)
                .addField(fieldThree)
                .addField(fieldFour)
                .build();
        // Create another box with a field
        MetadataField mfFive = mfss.findByElement(context, dc, "description", "tableofcontents");
        CrisLayoutField fieldFive = CrisLayoutFieldBuilder.createField(context, mfFive, 0, 4)
                .withBundle("bundle 5")
                .withLabel("label 5")
                .withRendering("rendering 5")
                .withStyle("style 5")
                .withType("type 5")
                .build();
        CrisLayoutBoxBuilder.createBuilder(context, eType, false, 2, false)
                .addField(fieldFive)
                .withShortname("Box three shortname")
                .build();
        context.restoreAuthSystemState();
        // Test WS invocation
        getClient().perform(get("/api/layout/boxes/" + boxTwo.getID() + "/fields"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(4)))
                .andExpect(jsonPath("$._embedded.fields[0]", Matchers.is(
                        CrisLayoutFieldMatcher.matchField(fieldOne))))
                .andExpect(jsonPath("$._embedded.fields[1]", Matchers.is(
                        CrisLayoutFieldMatcher.matchField(fieldTwo))))
                .andExpect(jsonPath("$._embedded.fields[2]", Matchers.is(
                        CrisLayoutFieldMatcher.matchField(fieldThree))))
                .andExpect(jsonPath("$._embedded.fields[3]", Matchers.is(
                        CrisLayoutFieldMatcher.matchField(fieldFour))));
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
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, 0, false)
            .withShortname("Shortname 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        // Create boxes with content
        CrisLayoutField fieldFirstName = CrisLayoutFieldBuilder.createField(context, firstName, 0, 0)
            .withBundle("BUNDLE")
            .withLabel("ISSUE NUMBER")
            .withRendering("TEXT")
            .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, 1, false)
            .withShortname("Shortname 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .addField(fieldFirstName)
            .build();
        CrisLayoutField fieldLastName = CrisLayoutFieldBuilder.createField(context, lastName, 0, 0)
            .withBundle("BUNDLE")
            .withLabel("ISSUE NUMBER")
            .withRendering("TEXT")
            .build();
        CrisLayoutBox boxThree = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, 2, false)
            .withShortname("Shortname 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .addField(fieldLastName)
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
        CrisLayoutBox boxFour = CrisLayoutBoxBuilder.createBuilder(context, eType, false, 0, false)
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
        CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, 0, false)
            .withShortname("Shortname 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePub, false, 0, false)
            .withShortname("Shortname 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eTypePub, false, 1, false)
            .withShortname("Shortname 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("Header")
            .withStyle("Style")
            .build();
        CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, 0, false)
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
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, 0, true)
            .withShortname("Box one shortname")
            .addMetadataSecurityField(sponsorship)
            .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eType, true, 0, true)
            .withShortname("Box two shortname")
            .addMetadataSecurityField(isbn)
            .addMetadataSecurityField(uri)
            .addMetadataSecurityField(abs)
            .addMetadataSecurityField(provenance)
            .build();
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, 0, true)
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

    /**
     * Test for endpoint /api/layout/boxes/<BOX_ID>/configuration.
     * It returns configuration entity with more information specific for the box
     * @throws Exception
     */
    @Test
    public void getConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField abs = mfss.findByElement(context, schema, "description", "abstract");
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        MetadataField extent = mfss.findByElement(context, schema, "format", "extent");
        // Create boxes
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, 0, true)
                .withShortname("Box shortname 1")
                .build();
        CrisLayoutField fieldIsbn = CrisLayoutFieldBuilder.createField(context, isbn, 0, 0)
                .withBundle("BUNDLE ISBN")
                .withLabel("LABEL ISBN")
                .withRendering("RENDERIGN ISBN")
                .withStyle("STYLE")
                .withType("TYPE")
                .build();
        CrisLayoutField fieldUri = CrisLayoutFieldBuilder.createField(context, uri, 0, 1)
                .withBundle("BUNDLE URI")
                .withLabel("LABEL URI")
                .withRendering("RENDERIGN URI")
                .withStyle("STYLE")
                .withType("TYPE")
                .build();
        CrisLayoutField fieldAbs = CrisLayoutFieldBuilder.createField(context, abs, 1, 0)
                .withBundle("BUNDLE ABS")
                .withLabel("LABEL ABS")
                .withRendering("RENDERIGN ABS")
                .withStyle("STYLE")
                .withType("TYPE")
                .build();
        CrisLayoutField fieldProvenance = CrisLayoutFieldBuilder.createField(context, provenance, 1, 1)
                .withBundle("BUNDLE PROVENANCE")
                .withLabel("LABEL PROVENANCE")
                .withRendering("RENDERIGN PROVENANCE")
                .withStyle("STYLE")
                .withType("TYPE")
                .build();
        CrisLayoutField fieldSponsorship = CrisLayoutFieldBuilder.createField(context, provenance, 1, 2)
                .withBundle("BUNDLE SPRONSORSHIP")
                .withLabel("LABEL SPRONSORSHIP")
                .withRendering("RENDERIGN SPRONSORSHIP")
                .withStyle("STYLE")
                .withType("TYPE")
                .build();
        CrisLayoutField fieldExtent = CrisLayoutFieldBuilder.createField(context, extent, 2, 0)
                .withBundle("BUNDLE EXTENT")
                .withLabel("LABEL EXTENT")
                .withRendering("RENDERIGN EXTENT")
                .withStyle("STYLE")
                .withType("TYPE")
                .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, 0, true)
                .withShortname("Box shortname 2")
                .addField(fieldIsbn)
                .addField(fieldUri)
                .addField(fieldAbs)
                .addField(fieldProvenance)
                .addField(fieldSponsorship)
                .addField(fieldExtent)
                .addMetadataSecurityField(isbn)
                .addMetadataSecurityField(uri)
                .addMetadataSecurityField(abs)
                .addMetadataSecurityField(provenance)
                .addMetadataSecurityField(sponsorship)
                .addMetadataSecurityField(extent)
                .build();
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, 0, true)
                .withShortname("Box shortname 3")
                .build();
        context.restoreAuthSystemState();
        // Test WS endpoint
        getClient().perform(get("/api/layout/boxes/" + box.getID() + "/configuration"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", Matchers.is(box.getShortname())))
            .andExpect(jsonPath("$.rows.length()", Matchers.is(3)))
            .andExpect(jsonPath("$.rows[0].fields.length()", Matchers.is(2)))
            .andExpect(jsonPath("$.rows[1].fields.length()", Matchers.is(3)))
            .andExpect(jsonPath("$.rows[2].fields.length()", Matchers.is(1)));
    }

    /**
     * Test for endpoint POST /api/layout/boxes, Its create a new boxes
     * This endpoint is reserved to system administrators
     * @throws Exception
     */
    @Test
    public void createBox() throws Exception {
        context.turnOffAuthorisationSystem();
        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            // Create entity type
            EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
            context.restoreAuthSystemState();

            CrisLayoutBoxRest rest = new CrisLayoutBoxRest();
            rest.setEntityType(eTypePer.getLabel());
            rest.setBoxType("box-type");
            rest.setClear(false);
            rest.setCollapsed(false);
            rest.setHeader("box-header");
            rest.setMinor(false);
            rest.setPriority(0);
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
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, 0, true)
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
}
