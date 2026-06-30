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
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.dspace.app.rest.matcher.CrisLayoutBoxMatcher.matchBox;
import static org.dspace.app.rest.matcher.CrisLayoutTabMatcher.matchRest;
import static org.dspace.app.rest.matcher.CrisLayoutTabMatcher.matchTab;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.matcher.CrisLayoutTabMatcher;
import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.CrisLayoutTabBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutCell;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisLayoutRow;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutTabService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

/**
 * This test class verify the REST Services for the Layout Tabs functionality (endpoint /api/layout/tabs)
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutTabRestRepositoryIT extends AbstractControllerIntegrationTest {

    private static final String BASE_TEST_DIR = "./target/testing/dspace/assetstore/layout/";

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataSchemaService mdss;

    @Autowired
    private MetadataFieldService mfss;

    @Autowired
    private CrisLayoutTabService crisLayoutTabService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    protected EntityTypeService entityTypeService;

    @Autowired
    protected RelationshipService relationshipService;

    @Autowired
    protected GroupService groupService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    private final String METADATASECURITY_URL = "http://localhost:8080/api/core/metadatafield/";

    @Value("classpath:org/dspace/app/rest/simple-article.pdf")
    private Resource simpleArticle;

    /**
     * Test for endpoint /api/layout/tabs/<ID_TAB>.
     * @throws Exception
     */
    @Test
    public void testFindOne() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField lastName = mfss.findByElement(context, schema, "familyName", null);
        MetadataField givenName = mfss.findByElement(context, schema, "givenName", null);

        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                                                   .withHeader("First New Box Header")
                                                   .withSecurity(LayoutSecurity.PUBLIC)
                                                   .withShortname("Shortname for new first box")
                                                   .withStyle("STYLE")
                                                   .build();

        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("Second New Box Header")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname for new second box")
                .withStyle("STYLE")
                .withType(CrisLayoutBoxTypes.METADATA.name())
                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, lastName, 0, 1)
                              .withLabel("LAST NAME")
                              .withRendering("TEXT")
                              .withBox(boxTwo)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, givenName, 0, 1)
            .withLabel("GIVEN NAME")
            .withRendering("TEXT")
            .withBox(boxTwo)
            .build();

        CrisLayoutBox boxThree = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("Third New Box Header - priority 0")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("orgUnits")
                .withStyle("STYLE")
                .addMetadataSecurityField(isbn)
                .withType(CrisLayoutBoxTypes.RELATION.name())
                .build();


        CrisLayoutBox boxFive = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("Fifth New Box Header - priority 2")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname 5")
                .withStyle("STYLE")
                .addMetadataSecurityField(isbn)
                .addMetadataSecurityField(uri)
                .build();

        CrisLayoutBox boxSix = CrisLayoutBoxBuilder.createBuilder(context, eType, false, false)
                .withHeader("Sixth New Box Header - priority 2")
                .withSecurity(LayoutSecurity.PUBLIC)
                .withShortname("Shortname 6")
                .withStyle("STYLE")
                .addMetadataSecurityField(uri)
                .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                                                .withShortName("Another New Tab shortname")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .withHeader("New Tab header")
                                                .withLeading(true)
                                                .addBoxIntoNewRow(boxOne)
                                                .addBoxIntoNewRow(boxTwo, "rowTwoStyle", "cellOfRowTwoStyle")
                                                .addBoxIntoLastRow(boxThree, "style")
                                                .addBoxIntoNewRow(boxFive)
                                                .addBoxIntoLastCell(boxSix)
                                                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", is(tab.getID())))
            .andExpect(jsonPath("$.shortname", is("Another New Tab shortname")))
            .andExpect(jsonPath("$.header", is("New Tab header")))
            .andExpect(jsonPath("$.leading", is(true)))
            .andExpect(jsonPath("$.security", is(LayoutSecurity.PUBLIC.getValue())))
            .andExpect(jsonPath("$.rows", hasSize(3)))
            .andExpect(jsonPath("$.rows[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells[0].boxes", contains(matchBox(boxOne))))
            .andExpect(jsonPath("$.rows[1].style", is("rowTwoStyle")))
            .andExpect(jsonPath("$.rows[1].cells", hasSize(2)))
            .andExpect(jsonPath("$.rows[1].cells[0].style", is("cellOfRowTwoStyle")))
            .andExpect(jsonPath("$.rows[1].cells[0].boxes", contains(matchBox(boxTwo))))
            .andExpect(jsonPath("$.rows[1].cells[1].style", is("style")))
            .andExpect(jsonPath("$.rows[1].cells[1].boxes", contains(matchBox(boxThree))))
            .andExpect(jsonPath("$.rows[2].style").doesNotExist())
            .andExpect(jsonPath("$.rows[2].cells", hasSize(1)))
            .andExpect(jsonPath("$.rows[2].cells[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[2].cells[0].boxes", contains(matchBox(boxFive), matchBox(boxSix))));
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

    @Test
    public void addSecurityMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tab
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("New Tab")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        // get metadata field isbn
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        getClient(tokenAdmin).perform(post("/api/layout/tabs/" + tab.getID() + "/securitymetadata")
                            .contentType(org.springframework.http.MediaType.parseMediaType
                                    (org.springframework.data.rest.webmvc.RestMediaTypes
                                         .TEXT_URI_LIST_VALUE))
                            .content(METADATASECURITY_URL + isbn.getID())
                            ).andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             // Expect a not empty collection in $._embedded.securitymetadata because
                             // the previous POST invocation add the ISBN element
                             .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                             .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void addSecurityMetadataUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tab
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("New Tab")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        // get metadata field isbn
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        getClient().perform(post("/api/layout/tabs/" + tab.getID() + "/securitymetadata")
                    .contentType(org.springframework.http.MediaType.parseMediaType
                            (org.springframework.data.rest.webmvc.RestMediaTypes
                                 .TEXT_URI_LIST_VALUE))
                    .content(METADATASECURITY_URL + isbn.getID())
                    ).andExpect(status().isUnauthorized());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void addSecurityMetadataisForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tab
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("New Tab")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        // get metadata field isbn
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(post("/api/layout/tabs/" + tab.getID() + "/securitymetadata")
                                .contentType(org.springframework.http.MediaType.parseMediaType
                                        (org.springframework.data.rest.webmvc.RestMediaTypes
                                             .TEXT_URI_LIST_VALUE))
                                .content(METADATASECURITY_URL + isbn.getID()))
                               .andExpect(status().isForbidden());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.is(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));
    }

    @Test
    public void addSecurityMetadataisNotFoundTabTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // get metadata field isbn
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/layout/tabs/" + Integer.MAX_VALUE + "/securitymetadata")
                .contentType(org.springframework.http.MediaType.parseMediaType
                        (org.springframework.data.rest.webmvc.RestMediaTypes
                             .TEXT_URI_LIST_VALUE))
                .content(METADATASECURITY_URL + isbn.getID()))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void addSecurityMetadataMissingMetadataTest() throws Exception {
       context.turnOffAuthorisationSystem();
       // Create entity type Publication
       EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
       // Create tab
       CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                           .withShortName("New Tab")
                           .withSecurity(LayoutSecurity.PUBLIC)
                           .build();

       context.restoreAuthSystemState();

       String tokenAdmin = getAuthToken(admin.getEmail(), password);
       getClient(tokenAdmin).perform(
               post("/api/layout/tabs/" + tab.getID() + "/securitymetadata")
               .contentType(org.springframework.http.MediaType.parseMediaType
                       (org.springframework.data.rest.webmvc.RestMediaTypes
                            .TEXT_URI_LIST_VALUE)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void removeSecurityMetadata() throws Exception {
        context.turnOffAuthorisationSystem();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField abs = mfss.findByElement(context, schema, "description", "abstract");
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Create tabs
        CrisLayoutTab tabOne = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(uri)
            .build();
        CrisLayoutTab tabTwo = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("New Tab 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(abs)
            .addMetadatasecurity(provenance)
            .addMetadatasecurity(sponsorship)
            .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // try to remove a not existing metadata
        getClient(tokenAdmin)
                .perform(delete("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata/" + Integer.MAX_VALUE))
                .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

        // try to remove a not associated metadata
        getClient(tokenAdmin)
                .perform(delete("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata/" + isbn.getID()))
                .andExpect(status().isNoContent());
        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));

        // remove the only associated metadata
        getClient(tokenAdmin)
                .perform(delete("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata/" + uri.getID()))
                .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tabOne.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)));

        // remove one of the many associated metadata
        getClient(tokenAdmin)
                .perform(delete("/api/layout/tabs/" + tabTwo.getID() + "/securitymetadata/" + abs.getID()))
                .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tabTwo.getID() + "/securitymetadata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.securitymetadata", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)));
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
                               .withEntityType(eTypePer.getLabel())
                               .build();
        MetadataSchema schema = mdss.find(context, "person");
        MetadataField firstName = mfss.findByElement(context, schema, "givenName", null);
        MetadataField lastName = mfss.findByElement(context, schema, "familyName", null);
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");

        // Create tabs for Person Entity
       CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
           .withShortname("Box shortname 1")
           .withSecurity(LayoutSecurity.PUBLIC)
            .withContainer(false)
           .build();
       CrisLayoutFieldBuilder.createMetadataField(context, firstName, 0, 1)
           .withLabel("LAST NAME")
           .withRendering("TEXT")
           .withBox(boxOne)
           .build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
            .withShortName("TabOne For Person - priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(boxOne)
            .build();

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Box shortname 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .build();
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Box shortname 33")
            .withSecurity(LayoutSecurity.ADMINISTRATOR)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, lastName, 0, 1)
            .withLabel("LAST NAME")
            .withRendering("TEXT")
            .withBox(box)
            .build();
        CrisLayoutTab tabTwo = CrisLayoutTabBuilder.createTab(context, eTypePer, 1)
            .withShortName("TabTwo For Person - priority 1")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(box)
            .addBoxIntoNewRow(boxTwo)
            .build();

        // tab without data
        CrisLayoutTabBuilder.createTab(context, eTypePer, 2)
            .withShortName("New Tab For Person 2")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addMetadatasecurity(provenance)
            .addMetadatasecurity(sponsorship)
            .build();

        CrisLayoutBox boxThree = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Box shortname 3")
            .withSecurity(LayoutSecurity.PUBLIC)
            .build();

        CrisLayoutTabBuilder.createTab(context, eTypePer, 2)
              .withShortName("AdministratorTab")
              .withSecurity(LayoutSecurity.ADMINISTRATOR)
              .withHeader("Administrator Tab header")
              .addMetadatasecurity(provenance)
              .addMetadatasecurity(sponsorship)
              .addBoxIntoNewRow(boxThree)
              .build();

        CrisLayoutBox administratorSecuredBox = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
            .withShortname("Box shortname secured")
            .withSecurity(LayoutSecurity.ADMINISTRATOR)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, lastName, 0, 1)
              .withLabel("LAST NAME")
              .withRendering("TEXT")
              .withBox(administratorSecuredBox)
              .build();
        // tab With Only SecuredBox
        CrisLayoutTabBuilder.createTab(context, eTypePer, 1)
           .withShortName("secured box holder - priority 1")
           .withSecurity(LayoutSecurity.PUBLIC)
           .withHeader("secured box holder")
           .addBoxIntoNewRow(administratorSecuredBox)
           .build();

        context.restoreAuthSystemState();
        // Test
        getClient().perform(get("/api/layout/tabs/search/findByItem").param("uuid", item.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(2))) // only two tabs have contents to show
            .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tab), matchTab(tabTwo))))
            .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(boxOne))))
            .andExpect(jsonPath("$._embedded.tabs[1].rows[0].cells[0].boxes", contains(matchBox(box))));
    }

    /**
     * Test for endpoint /api/layout/tabs/search/findByItem?uuid=<ITEM-UUID>
     * The tabs are sorted by priority ascending. This are filtered based on the permission of the
     * current user and available data.
     * The expected result is a list of tabs derived from the item type, where the item type is:
     * <ul>
     *     <li>submissionName.Authority of metadata configured in property {@code dspace.metadata.layout.tab}</li>
     *     <li>If null, submissionName.value of that metadata</li>
     *     <li>if null, Authority of metadata configured in property {@code dspace.metadata.layout.tab}</li>
     *     <li>If null, value of that metadata</li>
     *     <li>if null, submission name of item</li>
     *     <li>If null, value of entity type (metadata {@code dspace.entity.type})</li>
     *     <li>Otherwise, null</li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void findByItemMetadata() throws Exception {
        configurationService.setProperty("authority.controlled.dc.type", "true");
        configurationService.setProperty("dspace.metadata.layout.tab", "dc.type");
        metadataAuthorityService.clearCache();
        context.turnOffAuthorisationSystem();

        // Create new community
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .withTitle("Title test community")
                                              .build();
        // Create new collection
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("Test Collection")
                                                 .withSubmissionDefinition("publication")
                                                 .build();

        Collection collectionTwo = CollectionBuilder.createCollection(context, community)
                                                 .withName("Test Collection two")
                                                 .withSubmissionDefinition("traditional")
                                                 .build();

        Collection collectionThree = CollectionBuilder.createCollection(context, community)
                                                      .withName("Test Collection two")
                                                      .withSubmissionDefinition("patent")
                                                      .build();

        // Create entity Type
        EntityType publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType journalType = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        EntityType patentType = EntityTypeBuilder.createEntityTypeBuilder(context, "Patent").build();
        EntityType eTypePer = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType eTypeCollection = EntityTypeBuilder.createEntityTypeBuilder(context, "Collection").build();
        MetadataSchema schema = mdss.find(context, MetadataSchemaEnum.DC.getName());
        MetadataField title = mfss.findByElement(context, schema, "title", null);

        // Create new items
        // first uses metadata type authority and submission as custom filter
        String authority = "publication-coar-types:c_2f33";
        String metadataValue = "Resource Types::text::book";
        String submissionNameMetadataValue = "traditional." + metadataValue;
        String submissionNameAuthority = "patent." + authority;

        Item itemPublicationAuthority = ItemBuilder.createItem(context, collection)
                                                   .withTitle("TITLE")
                                                   .withType(metadataValue, authority)
                                                   .withEntityType(publicationType.getLabel())
                                                   .build();
        // second uses ametadata type value as custom filter
        Item itemPublicationValue = ItemBuilder.createItem(context, collection)
                                               .withTitle("TITLE 1")
                                               .withType(metadataValue)
                                               .withEntityType(publicationType.getLabel())
                                               .build();
        // third uses entity type value as custom filter
        Item itemPublication = ItemBuilder.createItem(context, collection)
                                          .withTitle("TITLE 2")
                                          .withEntityType(publicationType.getLabel())
                                          .build();
        // fourth uses submission name as custom filter
        Item itemPublicationSubmission = ItemBuilder.createItem(context, collection)
                                                    .withTitle("TITLE 3")
                                                    .withType("type value")
                                                    .withEntityType(publicationType.getLabel())
                                                    .build();
        // fifth uses submissionName.metadataValue as custom filter
        Item itemPublicationSubmissionMetadata = ItemBuilder.createItem(context, collectionTwo)
                                                    .withTitle("TITLE 4")
                                                    .withType(metadataValue)
                                                    .withEntityType(journalType.getLabel())
                                                    .build();

        // sixth uses submissionName.authority as custom filter
        Item itemPublicationSubmissionAuthority = ItemBuilder.createItem(context, collectionThree)
                                                    .withTitle("TITLE 5")
                                                    .withType(metadataValue, authority)
                                                    .withEntityType(patentType.getLabel())
                                                    .build();


        // Create tabs for Publication Entity
        CrisLayoutField field = CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 1)
                                                      .withLabel("TITLE")
                                                      .withRendering("TEXT")
                                                      //.withBox(boxOne)
                                                      .build();
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, publicationType, false, false)
                                                   .withShortname("Box shortname 1")
                                                   .withSecurity(LayoutSecurity.PUBLIC)
                                                   .withContainer(false)
                                                   .addField(field)
                                                   .build();
        CrisLayoutTab tabAuthority = CrisLayoutTabBuilder.createTab(context, publicationType, 0)
                                                         .withShortName("TabOne For Publication - priority 0")
                                                         .withSecurity(LayoutSecurity.PUBLIC)
                                                         .withHeader("New Tab header")
                                                         .withCustomFilter(authority)
                                                         .addBoxIntoNewRow(boxOne)
                                                         .build();

        context.restoreAuthSystemState();
        // Test
        getClient()
            .perform(
                get("/api/layout/tabs/search/findByItem")
                    .param("uuid",itemPublicationAuthority.getID().toString())
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tabAuthority))));

        context.turnOffAuthorisationSystem();

        boxOne = CrisLayoutBoxBuilder.createBuilder(context, publicationType, false, false)
                                     .withShortname("Box shortname 1")
                                     .withSecurity(LayoutSecurity.PUBLIC)
                                     .withContainer(false)
                                     .addField(field)
                                     .build();
        CrisLayoutTab tabPublicationValue = CrisLayoutTabBuilder.createTab(context, publicationType, 0)
                                                                .withShortName("TabOne For Collection - priority 0")
                                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                                .withHeader("New Tab header")
                                                                .withCustomFilter(metadataValue)
                                                                .addBoxIntoNewRow(boxOne)
                                                                .build();

        context.restoreAuthSystemState();

        getClient()
            .perform(
                get("/api/layout/tabs/search/findByItem")
                    .param("uuid",itemPublicationValue.getID().toString())
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
            .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tabPublicationValue))));

        context.turnOffAuthorisationSystem();

        boxOne = CrisLayoutBoxBuilder.createBuilder(context, publicationType, false, false)
                                     .withShortname("Box shortname 1")
                                     .withSecurity(LayoutSecurity.PUBLIC)
                                     .withContainer(false)
                                     .addField(field)
                                     .build();
        CrisLayoutTab tabPublication = CrisLayoutTabBuilder.createTab(context, publicationType, 0)
                                                           .withShortName("TabOne For Person - priority 0")
                                                           .withSecurity(LayoutSecurity.PUBLIC)
                                                           .withHeader("New Tab header")
                                                           .withCustomFilter(null)
                                                           .addBoxIntoNewRow(boxOne)
                                                           .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/search/findByItem").param("uuid", itemPublication.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                   .andExpect(
                       jsonPath(
                           "$._embedded.tabs",
                           contains(
                               matchTab(tabPublication)
                           )
                       )
                   );

        context.turnOffAuthorisationSystem();

        boxOne = CrisLayoutBoxBuilder.createBuilder(context, publicationType, false, false)
                                     .withShortname("Box shortname 1")
                                     .withSecurity(LayoutSecurity.PUBLIC)
                                     .withContainer(false)
                                     .addField(field)
                                     .build();

        CrisLayoutTab tabSubmissionName = CrisLayoutTabBuilder.createTab(context, publicationType, 0)
                                                              .withShortName("TabOne For Submission - priority 0")
                                                              .withSecurity(LayoutSecurity.PUBLIC)
                                                              .withHeader("New Tab header")
                                                              .withCustomFilter("publication")
                                                              .addBoxIntoNewRow(boxOne)
                                                              .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/search/findByItem")
                       .param("uuid", itemPublicationSubmission.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                   .andExpect(
                       jsonPath(
                           "$._embedded.tabs",
                           contains(
                               matchTab(tabSubmissionName)
                           )
                       )
                   );

        context.turnOffAuthorisationSystem();

        boxOne = CrisLayoutBoxBuilder.createBuilder(context, publicationType, false, false)
                                     .withShortname("Box shortname 1")
                                     .withSecurity(LayoutSecurity.PUBLIC)
                                     .withContainer(false)
                                     .addField(field)
                                     .build();

        CrisLayoutTab tabSubmissionNameMetadata =
            CrisLayoutTabBuilder.createTab(context, journalType, 0)
                                .withShortName("TabOne For Submission metadata value - priority 0")
                                .withSecurity(LayoutSecurity.PUBLIC)
                                .withHeader("New Tab header")
                                .withCustomFilter(submissionNameMetadataValue)
                                .addBoxIntoNewRow(boxOne)
                                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/search/findByItem")
                       .param("uuid", itemPublicationSubmissionMetadata.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                   .andExpect(
                       jsonPath(
                           "$._embedded.tabs",
                           contains(
                               matchTab(tabSubmissionNameMetadata)
                           )
                       )
                   );

        context.turnOffAuthorisationSystem();

        boxOne = CrisLayoutBoxBuilder.createBuilder(context, patentType, false, false)
                                     .withShortname("Box shortname 1")
                                     .withSecurity(LayoutSecurity.PUBLIC)
                                     .withContainer(false)
                                     .addField(field)
                                     .build();

        CrisLayoutTab tabSubmissionNameAuthority =
            CrisLayoutTabBuilder.createTab(context, patentType, 0)
                                .withShortName("TabOne For Submission authority - priority 0")
                                .withSecurity(LayoutSecurity.PUBLIC)
                                .withHeader("New Tab header")
                                .withCustomFilter(submissionNameAuthority)
                                .addBoxIntoNewRow(boxOne)
                                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/search/findByItem")
                       .param("uuid", itemPublicationSubmissionAuthority.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                   .andExpect(
                       jsonPath(
                           "$._embedded.tabs",
                           contains(
                               matchTab(tabSubmissionNameAuthority)
                           )
                       )
                   );
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
            EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            context.restoreAuthSystemState();

            CrisLayoutTabRest rest = parseJson("tab.json");

            ObjectMapper mapper = new ObjectMapper();

            // Test without authentication
            getClient().perform(post("/api/layout/tabs")
                .content(mapper.writeValueAsBytes(rest))
                .contentType(contentType))
                .andExpect(status().isUnauthorized());

            // Test with a non admin user
            String token = getAuthToken(eperson.getEmail(), password);
            getClient(token).perform(post("/api/layout/tabs")
                .content(mapper.writeValueAsBytes(rest))
                .contentType(contentType))
                .andExpect(status().isForbidden());

            // Test with admin user
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(post("/api/layout/tabs")
                .content(mapper.writeValueAsBytes(rest))
                .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", Matchers.is(matchRest(rest))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            // Get created tab by id from REST service and check its response
            getClient().perform(get("/api/layout/tabs/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(matchRest(rest))));

            CrisLayoutTab tab = crisLayoutTabService.find(context, idRef.get());
            assertThat(tab, notNullValue());
            assertThat(tab.getEntity().getLabel(), is("Publication"));
            assertThat(tab.getHeader(), is("Publication HEADER"));
            assertThat(tab.getShortName(), is("info"));
            assertThat(tab.getPriority(), is(1));
            assertThat(tab.getSecurity(), is(0));
            assertThat(tab.isLeading(), is(true));

            assertThat(tab.getRows(), hasSize(2));

            CrisLayoutRow firstRow = tab.getRows().get(0);
            assertThat(firstRow.getStyle(), nullValue());
            assertThat(firstRow.getCells(), hasSize(2));

            CrisLayoutCell firstCell = firstRow.getCells().get(0);
            assertThat(firstCell.getStyle(), is("col-md-6"));
            assertThat(firstCell.getBoxes(), hasSize(1));

            CrisLayoutBox firstBox = firstCell.getBoxes().get(0);
            assertThat(firstBox.getShortname(), is("primary"));
            assertThat(firstBox.getHeader(), is("Primary Information"));
            assertThat(firstBox.getEntitytype().getLabel(), is("Publication"));
            assertThat(firstBox.getCollapsed(), is(true));
            assertThat(firstBox.isContainer(), is(false));
            assertThat(firstBox.getStyle(), is("col-md-6"));
            assertThat(firstBox.getSecurity(), is(0));
            assertThat(firstBox.getType(), is("METADATA"));
            assertThat(firstBox.getMetadataSecurityFields(), hasSize(1));
            assertThat(firstBox.getLayoutFields(), hasSize(2));

            CrisLayoutCell secondCell = firstRow.getCells().get(1);
            assertThat(secondCell.getStyle(), is("col-md-6"));
            assertThat(secondCell.getBoxes(), hasSize(1));

            CrisLayoutBox secondBox = secondCell.getBoxes().get(0);
            assertThat(secondBox.getShortname(), is("orgUnits"));
            assertThat(secondBox.getHeader(), is("OrgUnits"));
            assertThat(secondBox.getEntitytype().getLabel(), is("Publication"));
            assertThat(secondBox.getCollapsed(), is(false));
            assertThat(secondBox.isContainer(), is(true));
            assertThat(secondBox.getStyle(), is("col-md-6"));
            assertThat(secondBox.getSecurity(), is(0));
            assertThat(secondBox.getType(), is("RELATION"));
            assertThat(secondBox.getMetadataSecurityFields(), empty());
            assertThat(secondBox.getLayoutFields(), empty());

            CrisLayoutRow secondRow = tab.getRows().get(1);
            assertThat(secondRow.getStyle(), is("bg-light"));
            assertThat(secondRow.getCells(), hasSize(1));

            CrisLayoutCell thirdCell = secondRow.getCells().get(0);
            assertThat(thirdCell.getStyle(), is("col-md-12"));
            assertThat(thirdCell.getBoxes(), hasSize(1));

            CrisLayoutBox thirdBox = thirdCell.getBoxes().get(0);
            assertThat(thirdBox.getShortname(), is("metrics"));
            assertThat(thirdBox.getHeader(), is("Metrics"));
            assertThat(thirdBox.getEntitytype().getLabel(), is("Publication"));
            assertThat(thirdBox.getCollapsed(), is(false));
            assertThat(thirdBox.isContainer(), is(true));
            assertThat(thirdBox.getStyle(), nullValue());
            assertThat(thirdBox.getSecurity(), is(0));
            assertThat(thirdBox.getType(), is("METADATA"));
            assertThat(thirdBox.getMetadataSecurityFields(), empty());
            assertThat(thirdBox.getLayoutFields(), hasSize(1));
            assertThat(thirdBox.getMaxColumns(), is(2));

        } finally {
            if (idRef.get() != null) {
                CrisLayoutTabBuilder.delete(idRef.get());
            }
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

    @Test
    public void patchTabReplaceShortnameTest() throws Exception {

        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("Tab shortname")
                            .withHeader("Tab Header")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String shortname = "New Shortname";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/shortname", shortname);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/tabs/" + tab.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$", Matchers.allOf(
                         hasJsonPath("$.shortname", is(shortname)),
                         hasJsonPath("$.header", is(tab.getHeader()))
                         )));

        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.shortname", is(shortname)),
                hasJsonPath("$.header", is(tab.getHeader())))));

        tab = context.reloadEntity(tab);
        assertThat(tab.getShortName(), is(shortname));
    }

    @Test
    public void patchTabReplacePriorityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("Tab shortname")
                            .withHeader("Tab Header")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        int newPriority = 99;
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/priority", newPriority);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/tabs/" + tab.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isOk())
                 .andExpect(content().contentType(contentType))
                 .andExpect(jsonPath("$", Matchers.allOf(
                         hasJsonPath("$.shortname", is("Tab shortname")),
                         hasJsonPath("$.priority", is(99)),
                         hasJsonPath("$.header", is(tab.getHeader()))
                         )));

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", is("Tab shortname")),
                           hasJsonPath("$.priority", is(99)),
                           hasJsonPath("$.header", is(tab.getHeader()))
                           )));
    }

    @Test
    public void patchTabReplacePriorityBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create new EntityType Person
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("Tab shortname")
                            .withHeader("Tab Header")
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String newPriority = "wrongPriority";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/priority", newPriority);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/layout/tabs/" + tab.getID())
                 .content(patchBody)
                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                 .andExpect(status().isBadRequest());

        getClient(tokenAdmin).perform(get("/api/layout/tabs/" + tab.getID()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.shortname", is("Tab shortname")),
                           hasJsonPath("$.priority", is(0)),
                           hasJsonPath("$.header", is(tab.getHeader()))
                           )));
    }

    @Test
    public void testGetTabWithMetadataBox() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // get metadata field
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataSchema schemaOaire = mdss.find(context, "oairecerif");
        MetadataField isbn = mfss.findByElement(context, schema, "identifier", "isbn");
        MetadataField uri = mfss.findByElement(context, schema, "identifier", "uri");
        MetadataField abs = mfss.findByElement(context, schema, "description", "abstract");
        MetadataField provenance = mfss.findByElement(context, schema, "description", "provenance");
        MetadataField sponsorship = mfss.findByElement(context, schema, "description", "sponsorship");
        MetadataField extent = mfss.findByElement(context, schema, "format", "extent");
        // nested metadata
        MetadataField author = mfss.findByElement(context, schema, "contributor", "author");
        MetadataField affiliation = mfss.findByElement(context, schemaOaire, "author", "affiliation");
        List<MetadataField> nestedMetadata = new ArrayList<>();
        nestedMetadata.add(author);
        nestedMetadata.add(affiliation);
        // Create boxes
        CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("box-shortname-one")
            .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("box-shortname-two")
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, isbn, 0, 0)
            .withLabel("LABEL ISBN")
            .withRendering("RENDERIGN ISBN")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, uri, 0, 1)
            .withLabel("LABEL URI")
            .withRendering("RENDERIGN URI")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, abs, 1, 0)
            .withLabel("LABEL ABS")
            .withRendering("RENDERIGN ABS")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, provenance, 1, 1)
            .withLabel("LABEL PROVENANCE")
            .withRendering("RENDERIGN PROVENANCE")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, sponsorship, 1, 2)
            .withLabel("LABEL SPRONSORSHIP")
            .withRendering("RENDERIGN SPRONSORSHIP")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        CrisLayoutFieldBuilder.createMetadataField(context, extent, 2, 0)
            .withLabel("LABEL EXTENT")
            .withRendering("RENDERIGN EXTENT")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withBox(box)
            .build();
        // nested field
        CrisLayoutFieldBuilder.createMetadataField(context, author, 0, 1)
            .withLabel("Authors")
            .withRendering("table")
            .withRowStyle("row")
            .withLabelStyle("col-6")
            .withValueStyle("col-6")
            .withNestedField(nestedMetadata)
            .withBox(box)
            .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("TabOne For Person - priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(box)
            .build();

        context.restoreAuthSystemState();

        String firstConfigurationCell = "$.rows[0].cells[0].boxes[0].configuration.rows[0].cells[0]";

        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", is(tab.getID())))
            .andExpect(jsonPath("$.rows", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells[0].boxes", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.rows[0].cells[0].fields", hasSize(3)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.rows[1].cells[0].fields", hasSize(3)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.rows[2].cells[0].fields", hasSize(1)))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].metadata", is("dc.contributor.author")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].label", is("Authors")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].rendering", is("table")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].styleLabel", is("col-6")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].styleValue", is("col-6")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].metadataGroup.leading",
                is("dc.contributor.author")))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].metadataGroup.elements", hasSize(2)))
            .andExpect(jsonPath(firstConfigurationCell + ".fields[2].metadataGroup.elements[1].metadata",
                is("oairecerif.author.affiliation")));
    }

    @Test
    public void testGetTabWithRelationBox() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        CrisLayoutBoxBuilder.createBuilder(context, eType, CrisLayoutBoxTypes.RELATION.name(), true, true)
            .withShortname("box-shortname-one")
            .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType,
            CrisLayoutBoxTypes.RELATION.name(), true, true)
            .withShortname("authors")
            .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("TabOne For Person - priority 0")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(box)
            .build();

        CrisLayoutBoxBuilder.createBuilder(context, eType, CrisLayoutBoxTypes.RELATION.name(), true, true)
            .withShortname("box-shortname-three")
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/" + tab.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", is(tab.getID())))
            .andExpect(jsonPath("$.rows", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].style").doesNotExist())
            .andExpect(jsonPath("$.rows[0].cells[0].boxes", hasSize(1)))
            .andExpect(jsonPath("$.rows[0].cells[0].boxes[0].configuration.discovery-configuration",
                is("RELATION.Publication.authors")));
    }

    @Test
    public void findByItemTabsWithCustomSecurityLayoutAnonynousTest() throws Exception {
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] {
                "org.dspace.content.authority.EPersonAuthority = EPersonAuthority",
                "org.dspace.content.authority.GroupAuthority = GroupAuthority"
            });
        configurationService.setProperty("choices.plugin.dspace.policy.eperson", "EPersonAuthority");
        configurationService.setProperty("choices.plugin.dspace.policy.group", "GroupAuthority");
        configurationService.setProperty("authority.controlled.dspace.policy.eperson", "true");
        configurationService.setProperty("authority.controlled.dspace.policy.group", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password)
                                      .build();

        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Volodyner", "Chornenkiy")
                                      .withEmail("user.b@example.com")
                                      .withPassword(password)
                                      .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userB)
                                   .build();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("Test Community")
                                              .withTitle("Title test community")
                                              .build();

        Collection col1 = CollectionBuilder.createCollection(context, community)
                                                 .withName("Test Collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("Title Of Item")
                               .withIssueDate("2015-06-25")
                               .withAuthor("Smith, Maria")
                               .withEntityType("Person")
                               .build();

        itemService.addMetadata(context, item, "dc", "description", "abstract", null, "A secured abstract");
        itemService.addMetadata(context, item, "dspace", "policy", "eperson", null, userA.getFullName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, item, "dspace", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "dspace", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "dspace", "policy", "group");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .addMetadataSecurityField(policyEperson)
                                                 .addMetadataSecurityField(policyGroup)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eType, 0)
                            .withShortName("TabOne For Person - priority 0")
                            .withHeader("New Tab header")
                            .addBoxIntoNewRow(box1)
                            .addBoxIntoNewRow(box2)
                            .withSecurity(LayoutSecurity.PUBLIC)
                            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/search/findByItem")
                           .param("uuid", item.getID().toString()))
                           .andExpect(status().isOk())
                           .andExpect(content().contentType(contentType))
                           .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                           .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tab))))
                           .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", hasSize(1)))
                           .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(box2))))
                           .andExpect(jsonPath("$._embedded.tabs[0].rows[1]").doesNotExist());

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        getClient(tokenUserA).perform(get("/api/layout/tabs/search/findByItem")
                           .param("uuid", item.getID().toString()))
                           .andExpect(status().isOk())
                           .andExpect(content().contentType(contentType))
                           .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                           .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tab))))
                           .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", hasSize(1)))
                           .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(box1))))
                           .andExpect(jsonPath("$._embedded.tabs[0].rows[1].cells[0].boxes", hasSize(1)))
                           .andExpect(jsonPath("$._embedded.tabs[0].rows[1].cells[0].boxes", contains(matchBox(box2))));
    }

    @Test
    public void findThumbnailUsingLayoutTabBoxConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        // Setting up configuration for dc.type = logo with rendering thumbnail
        MetadataField metadataField = mfss.findByElement(context, "dc", "type", null);

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType,
            CrisLayoutBoxTypes.RELATION.name(), true, true)
            .withShortname("description-test")
            .build();
        CrisLayoutField field = CrisLayoutFieldBuilder.createBistreamField(context, metadataField, "ORIGINAL", 0, 0, 0)
            .withRendering("thumbnail")
            .withBox(box)
            .build();
        ((CrisLayoutFieldBitstream)field).setMetadataValue("logo");
        CrisLayoutTabBuilder.createTab(context, eType, 0)
            .withShortName("TabOne")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .addBoxIntoNewRow(box)
            .build();

        Community testCommunity = CommunityBuilder.createCommunity(context).build();
        Collection testCollection = CollectionBuilder.createCollection(context, testCommunity).build();
        Item item = ItemBuilder.createItem(context, testCollection).withEntityType("Publication").build();

        Bundle original = BundleBuilder.createBundle(context, item).withName("ORIGINAL").build();

        org.dspace.content.Bitstream bitstream0 = BitstreamBuilder
            .createBitstream(context, original, InputStream.nullInputStream()).withType("other").build();
        org.dspace.content.Bitstream bitstream1 = BitstreamBuilder
            .createBitstream(context, original, InputStream.nullInputStream()).withType("other").build();
        org.dspace.content.Bitstream bitstream2 = BitstreamBuilder
            .createBitstream(context, original, InputStream.nullInputStream()).withType("Logo").build();

        original.setPrimaryBitstreamID(bitstream0);

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + item.getID() + "/thumbnail"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", is(bitstream2.getID().toString())))
            .andExpect(jsonPath("$.id", not(bitstream0.getID().toString())))
            .andExpect(jsonPath("$.id", not(bitstream1.getID().toString())))
            .andExpect(jsonPath("$.uuid", is(bitstream2.getID().toString())))
            .andExpect(jsonPath("$.uuid", not(bitstream0.getID().toString())))
            .andExpect(jsonPath("$.uuid", not(bitstream1.getID().toString())))
            .andExpect(jsonPath("$.metadata.['dc.type'][0].value", is("Logo")))
            .andExpect(jsonPath("$.bundleName", is("ORIGINAL")))
            .andExpect(jsonPath("$.type", is("bitstream")))
            .andExpect(jsonPath("$.name", is(bitstream2.getName())));

    }

    @Test
    public void excludeThumbnailNegativeMetadataValueMatcherTabBoxConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        // Setting up configuration for dc.type = logo with rendering thumbnail
        MetadataField metadataField =
            mfss.findByElement(context, "dc", "type", null);

        CrisLayoutBox box =
            CrisLayoutBoxBuilder.createBuilder(context, eType, true, false)
                                .withShortname("researcherprofile")
                                .withSecurity(LayoutSecurity.PUBLIC)
                                .build();

        CrisLayoutField field =
            CrisLayoutFieldBuilder.createBistreamField(context, metadataField, "ORIGINAL", 0, 0, 0)
                                  .withRendering("thumbnail")
                                  .withBox(box)
                                  .build();

        // filter out bitstreams with "personal picture" as dc.type
        ((CrisLayoutFieldBitstream)field).setMetadataValue("!personal picture");

        CrisLayoutTab tab =
            CrisLayoutTabBuilder.createTab(context, eType, 0)
                                .withShortName("otherinfo")
                                .withSecurity(LayoutSecurity.PUBLIC)
                                .withHeader("Other")
                                .addBoxIntoNewRow(box)
                                .build();

        Community community = CommunityBuilder.createCommunity(context).build();
        Collection personCollection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, personCollection).withEntityType("Person").build();

        Bundle original = BundleBuilder.createBundle(context, item).withName("ORIGINAL").build();

        org.dspace.content.Bitstream bitstream0 =
            BitstreamBuilder.createBitstream(context, original, InputStream.nullInputStream())
                            .withType("logo")
                            .build();

        original.setPrimaryBitstreamID(bitstream0);

        context.commit();
        context.restoreAuthSystemState();

        item = context.reloadEntity(item);

        getClient().perform(get("/api/layout/tabs/search/findByItem")
                                .param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                   .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tab))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(box))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[1]").doesNotExist());

        context.turnOffAuthorisationSystem();

        original = context.reloadEntity(original);
        org.dspace.content.Bitstream bitstream1 =
            BitstreamBuilder.createBitstream(context, original, InputStream.nullInputStream())
                            .withType("personal picture")
                            .build();
        original.setPrimaryBitstreamID(bitstream1);

        context.commit();
        context.restoreAuthSystemState();

        item = context.reloadEntity(item);

        getClient().perform(get("/api/layout/tabs/search/findByItem")
                                .param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                   .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tab))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(box))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[1]").doesNotExist());

        context.turnOffAuthorisationSystem();

        bitstream0 = context.reloadEntity(bitstream0);

        bitstreamService.delete(context, bitstream0);

        context.commit();
        context.restoreAuthSystemState();

        context.reloadEntity(item);

        getClient().perform(get("/api/layout/tabs/search/findByItem")
                                .param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(0)))
                   .andExpect(jsonPath("$._embedded.tabs").doesNotExist());

    }

    @Test
    public void excludeThumbnailNegativeMetadataValueMatcherTabMultiBoxConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        // Setting up configuration for dc.type = logo with rendering thumbnail
        MetadataField dcType =
            mfss.findByElement(context, "dc", "type", null);
        MetadataField dcTitle =
            mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox thumbnailBox =
            CrisLayoutBoxBuilder.createBuilder(context, eType, true, false)
                                .withShortname("researcherprofile")
                                .withSecurity(LayoutSecurity.PUBLIC)
                                .build();
        CrisLayoutBox titleBox =
            CrisLayoutBoxBuilder.createBuilder(context, eType, true, false)
                                .withShortname("title")
                                .withSecurity(LayoutSecurity.PUBLIC)
                                .build();

        CrisLayoutField thumbnailField =
            CrisLayoutFieldBuilder.createBistreamField(context, dcType, "ORIGINAL", 0, 0, 0)
                                  .withRendering("thumbnail")
                                  .withBox(thumbnailBox)
                                  .build();

        // filter out bitstreams with "personal picture" as dc.type
        ((CrisLayoutFieldBitstream)thumbnailField).setMetadataValue("!personal picture");

        CrisLayoutField titleField =
            CrisLayoutFieldBuilder.createMetadataField(context, dcTitle, 0, 0)
                                  .withRendering("heading")
                                  .withBox(titleBox)
                                  .build();

        CrisLayoutTab tab =
            CrisLayoutTabBuilder.createTab(context, eType, 0)
                                .withShortName("otherinfo")
                                .withSecurity(LayoutSecurity.PUBLIC)
                                .withHeader("Other")
                                .addBoxIntoNewRow(thumbnailBox)
                                .addBoxIntoNewRow(titleBox)
                                .build();

        Community community = CommunityBuilder.createCommunity(context).build();
        Collection personCollection = CollectionBuilder.createCollection(context, community).build();
        Item item =
            ItemBuilder.createItem(context, personCollection)
                       .withEntityType("Person")
                       .withTitle("Custom Person")
                       .build();

        Bundle original =
            BundleBuilder.createBundle(context, item)
                         .withName("ORIGINAL")
                         .build();

        org.dspace.content.Bitstream bitstream0 =
            BitstreamBuilder.createBitstream(context, original, InputStream.nullInputStream())
                            .withType("personal picture")
                            .build();

        original.setPrimaryBitstreamID(bitstream0);

        context.commit();
        context.restoreAuthSystemState();

        item = context.reloadEntity(item);

        getClient().perform(get("/api/layout/tabs/search/findByItem")
                                .param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                   .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tab))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes",
                                       not(contains(matchBox(thumbnailBox), matchBox(titleBox)))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(titleBox))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[1]").doesNotExist());
    }

    @Test
    public void testFindByItemWithAlternativeTabs() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = mdss.find(context, "person");
        MetadataField firstName = mfss.findByElement(context, schema, "givenName", null);
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
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
                               .withEntityType(eTypePer.getLabel())
                               .build();

        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
                                                   .withShortname("Box shortname 1")
                                                   .withSecurity(LayoutSecurity.PUBLIC)
                                                   .withContainer(false)
                                                   .build();

        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
                                                   .withShortname("Box shortname 2")
                                                   .withSecurity(LayoutSecurity.PUBLIC)
                                                   .withContainer(false)
                                                   .build();

        CrisLayoutFieldBuilder.createMetadataField(context, firstName, 0, 1)
                              .withLabel("GIVEN NAME")
                              .withRendering("TEXT")
                              .withBox(boxOne)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, firstName, 0, 1)
                              .withLabel("GIVEN NAME")
                              .withRendering("TEXT")
                              .withBox(boxTwo)
                              .build();

        // add boxOne to tabOne
        CrisLayoutTab tabOne =
            CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
                                .withShortName("TabOne For Person - priority 0")
                                .withSecurity(LayoutSecurity.ADMINISTRATOR)
                                .withHeader("New Tab header")
                                .addBoxIntoNewRow(boxOne, "rowTwoStyle", "cellOfRowTwoStyle")
                                .build();

        // add boxTwo to tabTwo
        CrisLayoutTab tabTwo =
            CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
                                .withShortName("Tab2 For Person - priority 0")
                                .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                .withHeader("New Tab2 header")
                                .addBoxIntoNewRow(boxTwo, "rowTwoStyle2", "cellOfRowTwoStyle2")
                                .addTab2SecurityGroups(adminGroup, tabOne)
                                .build();

        context.restoreAuthSystemState();

        // admin user will see two tabs
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", item.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.page.totalElements", is(2)))
            .andExpect(jsonPath("$._embedded.tabs[0].id", is(tabOne.getID())))
            .andExpect(jsonPath("$._embedded.tabs[0].shortname", is("TabOne For Person - priority 0")))
            .andExpect(jsonPath("$._embedded.tabs[0].header", is("New Tab header")))
            .andExpect(jsonPath("$._embedded.tabs[0].security", is(LayoutSecurity.ADMINISTRATOR.getValue())))
            .andExpect(jsonPath("$._embedded.tabs[0].rows", hasSize(1)))
            .andExpect(jsonPath("$._embedded.tabs[1].id", is(tabTwo.getID())))
            .andExpect(jsonPath("$._embedded.tabs[1].shortname", is("Tab2 For Person - priority 0")))
            .andExpect(jsonPath("$._embedded.tabs[1].header", is("New Tab2 header")))
            .andExpect(jsonPath("$._embedded.tabs[1].security", is(LayoutSecurity.CUSTOM_DATA.getValue())))
            .andExpect(jsonPath("$._embedded.tabs[1].rows", hasSize(1)));

        // anonymous user will see only alternative tab is tabOne
        getClient().perform(get("/api/layout/tabs/search/findByItem")
                       .param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(1)))
                   .andExpect(jsonPath("$._embedded.tabs[0].id", is(tabOne.getID())))
                   .andExpect(jsonPath("$._embedded.tabs[0].shortname", is("TabOne For Person - priority 0")))
                   .andExpect(jsonPath("$._embedded.tabs[0].header", is("New Tab header")))
                   .andExpect(jsonPath("$._embedded.tabs[0].security", is(LayoutSecurity.ADMINISTRATOR.getValue())))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].style", is("rowTwoStyle")))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].style", is("cellOfRowTwoStyle")))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(boxOne))));
    }

    @Test
    public void testFindByItemWithAlternativeBoxes() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataSchema schema = mdss.find(context, "person");
        MetadataField firstName = mfss.findByElement(context, schema, "givenName", null);
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
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
                               .withEntityType(eTypePer.getLabel())
                               .build();

        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
                                                   .withShortname("Box shortname 1")
                                                   .withSecurity(LayoutSecurity.PUBLIC)
                                                   .withContainer(false)
                                                   .build();

        // add boxOne as alternative to boxTwo
        CrisLayoutBox boxTwo = CrisLayoutBoxBuilder.createBuilder(context, eTypePer, false, false)
                                                   .withShortname("Box shortname 2")
                                                   .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                   .withContainer(false)
                                                   .addBox2SecurityGroups(adminGroup, boxOne)
                                                   .build();

        CrisLayoutFieldBuilder.createMetadataField(context, firstName, 0, 1)
                              .withLabel("GIVEN NAME")
                              .withRendering("TEXT")
                              .withBox(boxOne)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, firstName, 0, 1)
                              .withLabel("GIVEN NAME")
                              .withRendering("TEXT")
                              .withBox(boxTwo)
                              .build();

        // add boxTwo to tab
        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, eTypePer, 0)
                                                .withShortName("TabOne For Person - priority 0")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .withHeader("New Tab header")
                                                .withLeading(true)
                                                .addBoxIntoNewRow(boxTwo, "rowTwoStyle", "cellOfRowTwoStyle")
                                                .build();

        context.restoreAuthSystemState();

        // admin user will see boxTwo
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/layout/tabs/search/findByItem")
                .param("uuid", item.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.tabs[0].id", is(tab.getID())))
            .andExpect(jsonPath("$._embedded.tabs[0].shortname", is("TabOne For Person - priority 0")))
            .andExpect(jsonPath("$._embedded.tabs[0].header", is("New Tab header")))
            .andExpect(jsonPath("$._embedded.tabs[0].leading", is(true)))
            .andExpect(jsonPath("$._embedded.tabs[0].security", is(LayoutSecurity.PUBLIC.getValue())))
            .andExpect(jsonPath("$._embedded.tabs[0].rows", hasSize(1)))
            .andExpect(jsonPath("$._embedded.tabs[0].rows[0].style", is("rowTwoStyle")))
            .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells", hasSize(1)))
            .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].style", is("cellOfRowTwoStyle")))
            .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(boxTwo))));

        // anonymous user will see boxOne
        getClient().perform(get("/api/layout/tabs/search/findByItem")
                       .param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$._embedded.tabs[0].id", is(tab.getID())))
                   .andExpect(jsonPath("$._embedded.tabs[0].shortname", is("TabOne For Person - priority 0")))
                   .andExpect(jsonPath("$._embedded.tabs[0].header", is("New Tab header")))
                   .andExpect(jsonPath("$._embedded.tabs[0].leading", is(true)))
                   .andExpect(jsonPath("$._embedded.tabs[0].security", is(LayoutSecurity.PUBLIC.getValue())))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].style", is("rowTwoStyle")))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells", hasSize(1)))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].style", is("cellOfRowTwoStyle")))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(boxOne))));
    }

    @Test
    public void findByItemWithAttachment() throws Exception {
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
        EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication")
                                                 .build();

        // Create new Publication item
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("test item")
                               .withEntityType(entityType.getLabel())
                               .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf",
                                   simpleArticle.getInputStream())
                               .build();
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField title = mfss.findByElement(context, schema, "title", null);

        // Create tabs for Publication Entity
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
                                                   .withShortname("Box shortname 1")
                                                   .withSecurity(LayoutSecurity.PUBLIC)
                                                   .withContainer(false)
                                                   .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 1)
                              .withLabel("TITLE")
                              .withRendering("TEXT")
                              .withBox(boxOne)
                              .build();

        CrisLayoutField attachField = CrisLayoutFieldBuilder.createBistreamField(context, null, "ORIGINAL", 1, 0, 0)
                                                            .withLabel("ADVANCED ATTACHMENT")
                                                            .withRendering("advancedattachment")
                                                            .withBox(boxOne)
                                                            .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, entityType, 0)
                                                .withShortName("TabOne For Publication - priority 0")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .withHeader("New Tab header")
                                                .addBoxIntoNewRow(boxOne)
                                                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/search/findByItem").param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                   .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tab))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(boxOne))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes[0].configuration.rows", hasSize(2)))
                   .andExpect(jsonPath(
                       "$._embedded.tabs[0].rows[0].cells[0].boxes[0].configuration.rows[1].cells[0].fields[0]",
                       allOf(
                       hasJsonPath("$.label", is("ADVANCED ATTACHMENT")),
                       hasJsonPath("$.rendering", is( "advancedattachment")),
                       hasJsonPath("$.fieldType", is("BITSTREAM")),
                       hasJsonPath("$.bitstream.bundle", is( "ORIGINAL")))));

    }

    @Test
    public void findByItemWithoutAttachment() throws Exception {
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
        EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication")
                                                 .build();

        // Create new person item
        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("test item")
                               .withEntityType(entityType.getLabel())
                               .build();
        MetadataSchema schema = mdss.find(context, "dc");
        MetadataField title = mfss.findByElement(context, schema, "title", null);

        // Create tabs for Publication Entity
        CrisLayoutBox boxOne = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
                                                   .withShortname("Box shortname 1")
                                                   .withSecurity(LayoutSecurity.PUBLIC)
                                                   .withContainer(false)
                                                   .build();
        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 1)
                              .withLabel("TITLE")
                              .withRendering("TEXT")
                              .withBox(boxOne)
                              .build();

        CrisLayoutFieldBuilder.createBistreamField(context, null, "ORIGINAL", 1, 0, 1)
                              .withLabel("ADVANCED ATTACHMENT")
                              .withRendering("advancedattachment")
                              .withBox(boxOne)
                              .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, entityType, 0)
                                                .withShortName("TabOne For Publication - priority 0")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .withHeader("New Tab header")
                                                .addBoxIntoNewRow(boxOne)
                                                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/layout/tabs/search/findByItem").param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)))
                   .andExpect(jsonPath("$._embedded.tabs", contains(matchTab(tab))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes", contains(matchBox(boxOne))))
                   .andExpect(jsonPath("$._embedded.tabs[0].rows[0].cells[0].boxes[0].configuration.rows", hasSize(1)))
                   .andExpect(jsonPath(
                       "$._embedded.tabs[0].rows[0].cells[0].boxes[0].configuration.rows[0].cells[0].fields[0]",
                       allOf(
                           hasJsonPath("$.label", not(equalTo("ADVANCED ATTACHMENT"))),
                           hasJsonPath("$.rendering", not(equalTo( "advancedattachment"))),
                           hasJsonPath("$.fieldType", not(equalTo("BITSTREAM"))),
                           hasNoJsonPath("$.bitstream"))));

    }

    private CrisLayoutTabRest parseJson(String name) throws Exception {
        return new ObjectMapper().readValue(getFileInputStream(name), CrisLayoutTabRest.class);
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_TEST_DIR, name));
    }

}