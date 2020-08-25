/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.CrisLayoutBoxBuilder;
import org.dspace.app.rest.builder.CrisLayoutFieldBuilder;
import org.dspace.app.rest.builder.CrisLayoutTabBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.EntityTypeBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class LayoutSecurityIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataFieldService mfss;

    @Test
    public void configurationWithBoxCustomDataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.a@example.com")
                                      .build();
        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.b@example.com")
                                      .build();
        EPerson userC = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.c@example.com")
                                      .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userC)
                                   .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");
        itemService.addMetadata(context, itemA, "cris", "policy", "eperson", null, userA.getName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "cris", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "cris", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "cris", "policy", "group");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        CrisLayoutBox box3 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-three")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .addMetadataSecurityField(policyEperson)
                                                 .addMetadataSecurityField(policyGroup)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withStyle("STYLE")
                              .withBox(box3)
                              .build();

        CrisLayoutBox box4 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-four")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        List<CrisLayoutBox> boxes2tab1 = new LinkedList<>();
        boxes2tab1.add(box1);
        boxes2tab1.add(box2);

        List<CrisLayoutBox> boxes2tab2 = new LinkedList<>();
        boxes2tab2.add(box3);
        boxes2tab2.add(box4);

        CrisLayoutTab tab1 = CrisLayoutTabBuilder.createTab(context, eType, 0)
                                                 .withBoxes(boxes2tab1)
                                                 .build();

        CrisLayoutTab tab2 = CrisLayoutTabBuilder.createTab(context, eType, 0)
                                                 .withBoxes(boxes2tab2)
                                                 .build();

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);
        String tokenUserC = getAuthToken(userC.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").exists())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")));

        getClient(tokenUserB).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        getClient(tokenUserC).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").exists())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")));

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        getClient().perform(get("/api/core/items/" + itemA.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

    @Test
    public void tryToAddInvisibleMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.a@example.com")
                                      .build();
        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.b@example.com")
                                      .build();
        EPerson userC = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.c@example.com")
                                      .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userC)
                                   .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "cris", "policy", "eperson", null, userA.getName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "cris", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "cris", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "cris", "policy", "group");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .addMetadataSecurityField(policyEperson)
                                                 .addMetadataSecurityField(policyGroup)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withStyle("STYLE")
                              .withBox(box2)
                              .build();

        List<CrisLayoutBox> boxes = new LinkedList<>();
        boxes.add(box1);
        boxes.add(box2);

        CrisLayoutTab tab1 = CrisLayoutTabBuilder.createTab(context, eType, 0)
                                                 .withBoxes(boxes)
                                                 .build();

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);

        List<Operation> addAbstractDescription = new ArrayList<Operation>();
        List<Map<String, String>> absValue = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "Test Abstract Description");
        absValue.add(value);
        addAbstractDescription.add(new AddOperation("metadata/dc.description.abstract", absValue));
        String patchBody = getPatchContent(addAbstractDescription);

        getClient(tokenUserB).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenUserA).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").exists())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("Test Abstract Description")));
    }

    @Test
    public void tryToPatchInvisibleMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.a@example.com")
                                      .build();
        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.b@example.com")
                                      .build();
        EPerson userC = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.c@example.com")
                                      .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userC)
                                   .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");
        itemService.addMetadata(context, itemA, "cris", "policy", "eperson", null, userA.getName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "cris", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "cris", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "cris", "policy", "group");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .addMetadataSecurityField(policyEperson)
                                                 .addMetadataSecurityField(policyGroup)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withStyle("STYLE")
                              .withBox(box2)
                              .build();

        List<CrisLayoutBox> boxes = new LinkedList<>();
        boxes.add(box1);
        boxes.add(box2);

        CrisLayoutTab tab1 = CrisLayoutTabBuilder.createTab(context, eType, 0)
                                                 .withBoxes(boxes)
                                                 .build();

        context.restoreAuthSystemState();

        String tokenUserB = getAuthToken(userB.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> removeValueOfDescriptionAbstract = new ArrayList<Operation>();
        removeValueOfDescriptionAbstract.add(new RemoveOperation("metadata/dc.description.abstract/0"));
        String patchBody = getPatchContent(removeValueOfDescriptionAbstract);

        getClient(tokenUserB).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void invisibleMetadataPresentInPublicBox() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.a@example.com")
                                      .build();
        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.b@example.com")
                                      .build();
        EPerson userC = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.c@example.com")
                                      .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userC)
                                   .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");
        itemService.addMetadata(context, itemA, "cris", "policy", "eperson", null, userA.getName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "cris", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "cris", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "cris", "policy", "group");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        CrisLayoutBox box3 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-three")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .addMetadataSecurityField(policyEperson)
                                                 .addMetadataSecurityField(policyGroup)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withStyle("STYLE")
                              .withBox(box3)
                              .build();

        CrisLayoutBox box4 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-four")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withStyle("STYLE")
                              .withBox(box4)
                              .build();

        List<CrisLayoutBox> boxes2tab1 = new LinkedList<>();
        boxes2tab1.add(box1);
        boxes2tab1.add(box2);

        List<CrisLayoutBox> boxes2tab2 = new LinkedList<>();
        boxes2tab2.add(box3);
        boxes2tab2.add(box4);

        CrisLayoutTab tab1 = CrisLayoutTabBuilder.createTab(context, eType, 0)
                                                 .withBoxes(boxes2tab1)
                                                 .build();

        CrisLayoutTab tab2 = CrisLayoutTabBuilder.createTab(context, eType, 0)
                                                 .withBoxes(boxes2tab2)
                                                 .build();

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);
        String tokenUserC = getAuthToken(userC.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").exists())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")));

        getClient(tokenUserB).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").exists())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")));

        getClient(tokenUserC).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").exists())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")));

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").exists())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")));

        getClient().perform(get("/api/core/items/" + itemA.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata['dc.description.abstract']").exists())
                   .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                   is ("A secured abstract")));
    }

    @Test
    public void tryToPutInvisibleMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.a@example.com")
                                      .build();
        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.b@example.com")
                                      .build();
        EPerson userC = EPersonBuilder.createEPerson(context)
                                      .withEmail("user.c@example.com")
                                      .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userC)
                                   .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withRelationshipType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");
        itemService.addMetadata(context, itemA, "cris", "policy", "eperson", null, userA.getName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "cris", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "cris", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "cris", "policy", "group");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .addMetadataSecurityField(policyEperson)
                                                 .addMetadataSecurityField(policyGroup)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withStyle("STYLE")
                              .withBox(box2)
                              .build();

        List<CrisLayoutBox> boxes = new LinkedList<>();
        boxes.add(box1);
        boxes.add(box2);

        CrisLayoutTab tab1 = CrisLayoutTabBuilder.createTab(context, eType, 0)
                                                 .withBoxes(boxes)
                                                 .build();

        context.restoreAuthSystemState();

        String tokenUserB = getAuthToken(userB.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
    }
}
