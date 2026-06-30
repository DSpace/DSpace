/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.edit.EditItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Constants;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.RegistrationDataService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBox2SecurityGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class LayoutSecurityIT extends AbstractControllerIntegrationTest {
    @Autowired(required = true)
    protected EPersonService ePersonService;
    @Autowired
    private ItemService itemService;

    @Autowired
    private GroupService groupService;
    @Autowired(required = true)
    protected RegistrationDataService registrationDataService;
    @Autowired
    private MetadataFieldService mfss;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    @Test
    public void configurationContainLayoutSecurityAdministratorTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.ADMINISTRATOR)
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

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        // An admin can see the dc.description.abstract metadata
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                             is ("Public item A")));

        // An user who is not admin can not see the dc.description.abstract metadata
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                               is ("Public item A")))
                               .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        // An anonymous user can not see the dc.description.abstract metadata
        getClient().perform(get("/api/core/items/" + itemA.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                   is ("Public item A")))
                   .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

    @Test
    public void configurationContainLayoutSecurityOwnerOnlyTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password)
                                      .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withDspaceObjectOwner(userA)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withDescriptionAbstract("A secured abstract")
                                .build();

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.OWNER_ONLY)
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

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        // The owner of Item can see the dc.description.abstract metadata
        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                             is ("Public item A")));

        // An user who is not owner of the item can not see the dc.description.abstract metadata
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                               is ("Public item A")))
                               .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        // An user who is not owner of the item can not see the dc.description.abstract metadata
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                             is ("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        // An user who is not owner of the item can not see the dc.description.abstract metadata
        getClient().perform(get("/api/core/items/" + itemA.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                   is ("Public item A")))
                   .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

    @Test
    public void configurationContainLayoutSecurityOwnerAndAdministratorTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password)
                                      .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withDspaceObjectOwner(userA)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withDescriptionAbstract("A secured abstract")
                                .build();

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR)
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

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        // The owner of Item can see the dc.description.abstract metadata
        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                             is ("Public item A")));

        // The admin see the dc.description.abstract metadata
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                             is ("Public item A")));

        // An user who is not owner of the item and is not admin, can not see the dc.description.abstract metadata
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                               is ("Public item A")))
                               .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        // An user who is not owner of the item and is not admin, can not see the dc.description.abstract metadata
        getClient().perform(get("/api/core/items/" + itemA.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                   is ("Public item A")))
                   .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

    @Test
    public void configurationContainLayoutSecurityCustomDataTest() throws Exception {
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
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

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
        EPerson userC = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Simone", "Proni")
                                      .withEmail("user.c@example.com")
                                      .withPassword(password)
                                      .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userC)
                                   .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");
        itemService.addMetadata(context, itemA, "dspace", "policy", "eperson", null, userA.getFullName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "dspace", "policy", "group", null, groupA.getName(),
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

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);
        String tokenUserC = getAuthToken(userC.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")));

        getClient(tokenUserB).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        getClient(tokenUserC).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")));

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        getClient().perform(get("/api/core/items/" + itemA.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }


    @Test
    public void customDataTestWithOneGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

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

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, col1)
            .withTitle("Public item A")
            .withIssueDate("2015-06-25")
            .withAuthor("Smith, Maria")
            .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("box-shortname-one")
            .withSecurity(LayoutSecurity.CUSTOM_DATA)
            .build();

        // Create Group with member userA
        Set<CrisLayoutBox2SecurityGroup> box2SecurityGroups = new HashSet<>();
        Group testGroup = GroupBuilder.createGroup(context)
                .withName("testGroup")
                .addMember(userA)
                .build();

        new CrisLayoutBox2SecurityGroup(
            new CrisLayoutBox2SecurityGroup.CrisLayoutBox2SecurityGroupId(box1, testGroup),
            box1, testGroup, null);

        box2SecurityGroups.add(new CrisLayoutBox2SecurityGroup(
            new CrisLayoutBox2SecurityGroup.CrisLayoutBox2SecurityGroupId(box1, testGroup),
            box1, testGroup, null));
        box1.setBox2SecurityGroups(box2SecurityGroups);

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
            .withLabel("LABEL ABS")
            .withRendering("RENDERIGN ABS")
            .withRowStyle("STYLE")
            .withBox(box1)
            .build();

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);

        // userA and box1 are both part of the same Group, abstract is present
        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                is ("A secured abstract")))
            .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")));

        // userB is not part of the same group as box1, abstract is not present
        getClient(tokenUserB).perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
            .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        getClient().perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

    @Test
    public void customDataTestWithMultipleGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

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

        EPerson userC = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Simone", "Proni")
            .withEmail("user.c@example.com")
            .withPassword(password)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, col1)
            .withTitle("Public item A")
            .withIssueDate("2015-06-25")
            .withAuthor("Smith, Maria")
            .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("box-shortname-one")
            .withSecurity(LayoutSecurity.CUSTOM_DATA)
            .build();

        // Create Group with member userA
        Set<CrisLayoutBox2SecurityGroup> boxGroups = new HashSet<>();

        Group testGroup = GroupBuilder.createGroup(context)
            .withName("testGroup")
            .addMember(userA)
            .build();

        Group testGroup1 = GroupBuilder.createGroup(context)
            .withName("testGroup1")
            .addMember(userB)
            .build();

        boxGroups.add(new CrisLayoutBox2SecurityGroup(
            new CrisLayoutBox2SecurityGroup.CrisLayoutBox2SecurityGroupId(box1, testGroup),
            box1, testGroup, null));
        boxGroups.add(new CrisLayoutBox2SecurityGroup(
            new CrisLayoutBox2SecurityGroup.CrisLayoutBox2SecurityGroupId(box1, testGroup1),
            box1, testGroup, null));

        box1.setBox2SecurityGroups(boxGroups);

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
            .withLabel("LABEL ABS")
            .withRendering("RENDERIGN ABS")
            .withRowStyle("STYLE")
            .withBox(box1)
            .build();

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);
        String tokenUserC = getAuthToken(userC.getEmail(), password);

        // userA and box1 share one Group together, abstract is present
        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                is ("A secured abstract")))
            .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")));

        // userB and box1 share one Group together, abstract is present
        getClient(tokenUserB).perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                is ("A secured abstract")))
            .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")));

        // userC and box1 do not share any groups, abstract is not present
        getClient(tokenUserC).perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist())
            .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")));

        getClient().perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

    @Test
    public void configurationContainAllLayoutSecurityAspectTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password)
                                      .build();
        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Volodymyr", "Chornenkiy")
                                      .withEmail("user.b@example.com")
                                      .withPassword(password)
                                      .build();
        EPerson userC = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Simone", "Proni")
                                      .withEmail("user.c@example.com")
                                      .withPassword(password)
                                      .build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userC)
                                   .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withDspaceObjectOwner(userB)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withDescriptionAbstract("A secured abstract")
                                .withPolicyEPerson(userA.getFullName(), userA.getID().toString())
                                .withPolicyGroup(groupA.getName(), groupA.getID().toString())
                                .build();

        MetadataField policyEperson = mfss.findByElement(context, "dspace", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "dspace", "policy", "group");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField issueDate = mfss.findByElement(context, "dc", "date", "issued");
        MetadataField author = mfss.findByElement(context, "dc", "contributor", "author");

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

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
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

        CrisLayoutBox box3 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-three")
                                                 .withSecurity(LayoutSecurity.ADMINISTRATOR)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, issueDate, 0, 0)
                              .withLabel("LABEL IssueDate")
                              .withRendering("RENDERIGN IssueDate")
                              .withRowStyle("STYLE")
                              .withBox(box3)
                              .build();

        CrisLayoutBox box4 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-four")
                                                 .withSecurity(LayoutSecurity.OWNER_ONLY)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, author, 0, 0)
                              .withLabel("LABEL AUTHOR")
                              .withRendering("RENDERIGN AUTHOR")
                              .withRowStyle("STYLE")
                              .withBox(box4)
                              .build();

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);
        String tokenUserC = getAuthToken(userC.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.date.issued']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.contributor.author']").doesNotExist());

        getClient(tokenUserB).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value", is ("Smith, Maria")))
                             .andExpect(jsonPath("$.metadata['dc.date.issued']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        getClient(tokenUserC).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.date.issued']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.contributor.author']").doesNotExist());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.date.issued'].[0].value", is ("2015-06-25")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.contributor.author']").doesNotExist());

        getClient().perform(get("/api/core/items/" + itemA.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
                   .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist())
                   .andExpect(jsonPath("$.metadata['dc.contributor.author']").doesNotExist())
                   .andExpect(jsonPath("$.metadata['dc.data.issued']").doesNotExist());

    }

    @Test
    public void patchAddMetadataContainedInAdministratorBoxTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.ADMINISTRATOR)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        String abstractDescription = "Test abstract description";
        List<Operation> ops = new ArrayList<Operation>();
        ops.add(new AddOperation("/metadata/dc.description.abstract/0", abstractDescription));
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("Test abstract description")))
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                             is ("Public item A")));

        String abstractDescription2 = "New Abstract Description";
        List<Operation> ops2 = new ArrayList<Operation>();
        ops2.add(new AddOperation("/metadata/dc.description.abstract/0", abstractDescription2));
        String patchBody2 = getPatchContent(ops2);

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody2)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("Test abstract description")));
    }

    @Test
    public void adminTryToPatchAddMetadataToBoxesWithDifferentLayoutSecurityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withIssueDate("2015-06-25")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        MetadataField author = mfss.findByElement(context, "dc", "contributor", "author");
        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.OWNER_ONLY)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();

        CrisLayoutBox box3 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-three")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, author, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box3)
                              .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ops.add(new AddOperation("/metadata/dc.title/0", "Test Title"));
        String patchBody = getPatchContent(ops);

        // dc.title is difined on box with OWNER_AND_ADMINISTRATOR layout security
        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                             is ("Test Title")));

        // dc.description.abstract is difined on box with OWNER_ONLY layout security
        List<Operation> ops2 = new ArrayList<Operation>();
        ops2.add(new AddOperation("/metadata/dc.description.abstract/0", "Test Abstract Description"));
        String patchBody2 = getPatchContent(ops2);

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody2)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isUnprocessableEntity());

        // dc.contributor.author is difined on box with CUSTOM_DATA layout security
        List<Operation> ops3 = new ArrayList<Operation>();
        ops3.add(new AddOperation("/metadata/dc.contributor.author/0", "Mykhayl oBoychuk"));
        String patchBody3 = getPatchContent(ops3);

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody3)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.contributor.author']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                             is ("Test Title")));
    }

    @Test
    public void patchRemoveMetadataContainedInAdministratorAndPublicBoxesTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.ADMINISTRATOR)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ops.add(new RemoveOperation("/metadata/dc.description.abstract/0"));
        String patchBody = getPatchContent(ops);

        // the eperson has permession to WRITE on item, but his hasn't access
        // to the boxes with Administrator layout security
        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isUnprocessableEntity());

        List<Operation> removeTitle = new ArrayList<Operation>();
        removeTitle.add(new RemoveOperation("/metadata/dc.title/0"));
        String patchBody2 = getPatchContent(removeTitle);

        // metadata title is difined in Public box, so eperson can remove it
        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody2)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")));

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

    @Test
    public void patchRemoveMetadataContainedInBoxesWithOnlyOwnerLayoutSecurityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withDspaceObjectOwner(eperson)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withDescriptionAbstract("A secured abstract")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.OWNER_ONLY)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ops.add(new RemoveOperation("/metadata/dc.description.abstract/0"));
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Public item A")))
                               .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                               is ("A secured abstract")));

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

    @Test
    public void patchRemoveMetadataContainedInBoxesWithCustomDataLayoutSecurityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password).build();

        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Volodymyr", "Chornenkiy")
                                      .withEmail("user.b@example.com")
                                      .withPassword(password).build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userB).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, groupA)
                            .withDspaceObject(itemA)
                            .withAction(Constants.WRITE)
                            .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");
        itemService.addMetadata(context, itemA, "dspace", "policy", "eperson", null, userA.getFullName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "dspace", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "dspace", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "dspace", "policy", "group");
        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField author = mfss.findByElement(context, "dc", "contributor", "author");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
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
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();
        CrisLayoutFieldBuilder.createMetadataField(context, author, 0, 0)
                              .withLabel("LABEL AUTOR")
                              .withRendering("RENDERIGN AUTOR")
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);

        List<Operation> ops = new ArrayList<Operation>();
        ops.add(new RemoveOperation("/metadata/dc.description.abstract/0"));
        String patchBody = getPatchContent(ops);

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenUserA).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        List<Operation> ops2 = new ArrayList<Operation>();
        ops2.add(new RemoveOperation("/metadata/dc.contributor.author/0"));
        String patchBody2 = getPatchContent(ops2);
        getClient(tokenUserB).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody2)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isOk());

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Public item A")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist())
                             .andExpect(jsonPath("$.metadata['dc.contributor.author']").doesNotExist());

    }

    @Test
    public void patchReplaceMetadataContainedInAdministratorAndPublicBoxesTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1).build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.ADMINISTRATOR)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        List<Operation> replaceAbs = new ArrayList<Operation>();
        replaceAbs.add(new ReplaceOperation("/metadata/dc.description.abstract/0", "New Abstract Description"));
        String patchBody = getPatchContent(replaceAbs);

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isUnprocessableEntity());

        List<Operation> replaceTitle = new ArrayList<Operation>();
        replaceTitle.add(new ReplaceOperation("/metadata/dc.title/0", "New Title"));
        String patchBody2 = getPatchContent(replaceTitle);

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody2)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("New Title")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("A secured abstract")));

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("New Title")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is ("New Abstract Description")));
    }

    @Test
    public void patchReplaceMetadataContainedInBoxesWithCustomDataLayoutSecurityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password).build();

        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Volodymyr", "Chornenkiy")
                                      .withEmail("user.b@example.com")
                                      .withPassword(password).build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userB).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, groupA)
                            .withDspaceObject(itemA)
                            .withAction(Constants.WRITE)
                            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");
        itemService.addMetadata(context, itemA, "dspace", "policy", "eperson", null, userA.getFullName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "dspace", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "dspace", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "dspace", "policy", "group");
        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField author = mfss.findByElement(context, "dc", "contributor", "author");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
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
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        CrisLayoutFieldBuilder.createMetadataField(context, author, 0, 0)
                              .withLabel("LABEL AUTOR")
                              .withRendering("RENDERIGN AUTOR")
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);

        List<Operation> replaceAbs = new ArrayList<Operation>();
        replaceAbs.add(new ReplaceOperation("/metadata/dc.description.abstract/0", "New Abstract Description"));
        String patchBody = getPatchContent(replaceAbs);

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isUnprocessableEntity());

        List<Operation> replaceTitle = new ArrayList<Operation>();
        replaceTitle.add(new ReplaceOperation("/metadata/dc.title/0", "New Title"));
        String patchBody2 = getPatchContent(replaceTitle);
        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody2)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isOk());

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("New Title")))
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value", is("Smith, Maria")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                              is("A secured abstract")));

        getClient(tokenUserA).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        List<Operation> replaceAuthor = new ArrayList<Operation>();
        replaceAuthor.add(new ReplaceOperation("/metadata/dc.contributor.author/0", "New Author"));
        String patchBody3 = getPatchContent(replaceAuthor);

        getClient(tokenUserB).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody3)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("New Title")))
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value", is("New Author")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                              is("New Abstract Description")));
    }

    @Test
    public void patchReplaceMetadataContainedInBoxesWithOnlyOwnerLayoutSecurityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withDspaceObjectOwner(userA)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withDescriptionAbstract("A secured abstract")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1).build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.OWNER_ONLY)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        List<Operation> replaceAbs = new ArrayList<Operation>();
        replaceAbs.add(new ReplaceOperation("/metadata/dc.description.abstract/0", "New Abstract Description"));
        String patchBody = getPatchContent(replaceAbs);

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isUnprocessableEntity());

        List<Operation> replaceTitle = new ArrayList<Operation>();
        replaceTitle.add(new ReplaceOperation("/metadata/dc.title/0", "New Title"));
        String patchBody2 = getPatchContent(replaceTitle);

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody2)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isOk());

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("New Title")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                              is("A secured abstract")));

        getClient(tokenUserA).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("New Title")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                              is("New Abstract Description")));
    }

    @Test
    public void patchMoveMetadataContainedInAdministratorAndPublicBoxesTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withAuthor("Doe, John")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "First Abstract description");
        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "Second Abstract description");


        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField author = mfss.findByElement(context, "dc", "contributor", "author");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.ADMINISTRATOR)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, author, 0, 0)
                              .withLabel("LABEL AUTHOR")
                              .withRendering("RENDERIGN AUTHOR")
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        List<Operation> moveAuthor = new ArrayList<Operation>();
        moveAuthor.add(new MoveOperation("/metadata/dc.contributor.author/0", "/metadata/dc.contributor.author/1"));
        String patchBody = getPatchContent(moveAuthor);

        List<Operation> moveAbs = new ArrayList<Operation>();
        moveAbs.add(new MoveOperation("/metadata/dc.description.abstract/0", "/metadata/dc.description.abstract/1"));
        String patchBody2 = getPatchContent(moveAbs);

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isUnprocessableEntity());

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody2)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                              is("Second Abstract description")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[1].value",
                                              is("First Abstract description")))
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value",
                                              is("Smith, Maria")))
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[1].value",
                                              is("Doe, John")));

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value", is ("Doe, John")))
                            .andExpect(jsonPath("$.metadata['dc.contributor.author'].[1].value", is ("Smith, Maria")))
                            .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                             is("Second Abstract description")))
                            .andExpect(jsonPath("$.metadata['dc.description.abstract'].[1].value",
                                             is("First Abstract description")));
    }

    @Test
    public void patchMoveMetadataContainedInBoxesWithOnlyOwnerLayoutSecurityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withDspaceObjectOwner(eperson)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withAuthor("Doe, John")
                                .withDescriptionAbstract("First Abstract description")
                                .withDescriptionAbstract("Second Abstract description")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField author = mfss.findByElement(context, "dc", "contributor", "author");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutBox box2 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-two")
                                                 .withSecurity(LayoutSecurity.OWNER_ONLY)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, author, 0, 0)
                              .withLabel("LABEL AUTHOR")
                              .withRendering("RENDERIGN AUTHOR")
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        List<Operation> moveAuthor = new ArrayList<Operation>();
        moveAuthor.add(new MoveOperation("/metadata/dc.contributor.author/0", "/metadata/dc.contributor.author/1"));
        String patchBody = getPatchContent(moveAuthor);

        List<Operation> moveAbs = new ArrayList<Operation>();
        moveAbs.add(new MoveOperation("/metadata/dc.description.abstract/0", "/metadata/dc.description.abstract/1"));
        String patchBody2 = getPatchContent(moveAbs);

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody2)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                                is("Second Abstract description")))
                               .andExpect(jsonPath("$.metadata['dc.description.abstract'].[1].value",
                                                is("First Abstract description")))
                               .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value",
                                                is("Smith, Maria")))
                               .andExpect(jsonPath("$.metadata['dc.contributor.author'].[1].value",
                                                is("Doe, John")));

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isOk());

        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value",
                                                is("Doe, John")))
                               .andExpect(jsonPath("$.metadata['dc.contributor.author'].[1].value",
                                                is("Smith, Maria")))
                               .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                                is("Second Abstract description")))
                               .andExpect(jsonPath("$.metadata['dc.description.abstract'].[1].value",
                                                is("First Abstract description")));
    }

    @Test
    public void patchMoveMetadataContainedInBoxesWithCustomDataLayoutSecurityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password).build();

        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Andriy", "Senyk")
                                      .withEmail("user.b@example.com")
                                      .withPassword(password).build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userB).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withAuthor("Doe, John")
                                .build();

        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        ResourcePolicyBuilder.createResourcePolicy(context, null, groupA)
                            .withDspaceObject(itemA)
                            .withAction(Constants.WRITE)
                            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.WRITE)
                             .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "First Abstract description");
        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "Second Abstract description");

        itemService.addMetadata(context, itemA, "dspace", "policy", "eperson", null, userA.getFullName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "dspace", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "dspace", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "dspace", "policy", "group");
        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField author = mfss.findByElement(context, "dc", "contributor", "author");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                .withShortname("box-shortname-one")
                                                .withSecurity(LayoutSecurity.PUBLIC)
                                                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box1)
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
                              .withRowStyle("STYLE")
                              .withBox(box2)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, author, 0, 0)
                              .withLabel("LABEL AUTHOR")
                              .withRendering("RENDERIGN AUTHOR")
                              .withRowStyle("STYLE")
                              .withBox(box2).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        List<Operation> moveAuthor = new ArrayList<Operation>();
        moveAuthor.add(new MoveOperation("/metadata/dc.contributor.author/0", "/metadata/dc.contributor.author/1"));
        String patchBody = getPatchContent(moveAuthor);

        List<Operation> moveAbs = new ArrayList<Operation>();
        moveAbs.add(new MoveOperation("/metadata/dc.description.abstract/0", "/metadata/dc.description.abstract/1"));
        String patchBody2 = getPatchContent(moveAbs);

        getClient(tokenAdmin).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isUnprocessableEntity());

        getClient(tokenEperson).perform(patch("/api/core/items/" + itemA.getID())
                               .content(patchBody2)
                               .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                               .andExpect(status().isOk());

        getClient(tokenUserB).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                              is("Second Abstract description")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[1].value",
                                              is("First Abstract description")))
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value",
                                                is("Smith, Maria")))
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[1].value",
                                                is("Doe, John")));

        getClient(tokenUserA).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value",
                                              is("Doe, John")))
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[1].value",
                                              is("Smith, Maria")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                              is("Second Abstract description")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[1].value",
                                              is("First Abstract description")));

        getClient(tokenUserB).perform(patch("/api/core/items/" + itemA.getID())
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[0].value",
                                              is("Smith, Maria")))
                             .andExpect(jsonPath("$.metadata['dc.contributor.author'].[1].value",
                                              is("Doe, John")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                                              is("Second Abstract description")))
                             .andExpect(jsonPath("$.metadata['dc.description.abstract'].[1].value",
                                              is("First Abstract description")));
    }

    @Test
    public void findOneWorkspaceItemUsingLayoutSecurityCustomDataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                                                  .withTitle("Title Workspace 1")
                                                  .withIssueDate("2017-10-17")
                                                  .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                  .withSubject("ExtraEntry").build();

        MetadataField policyEperson = mfss.findByElement(context, "dspace", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "dspace", "policy", "group");

        MetadataField dateIssued = mfss.findByElement(context, "dc", "date", "issued");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .addMetadataSecurityField(policyEperson)
                                                 .addMetadataSecurityField(policyGroup)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, dateIssued, 0, 0)
                              .withLabel("LABEL DATE")
                              .withRendering("RENDERIGN DATE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
                            get("/api/submission/workspaceitems/" + witem.getID())
                                .param("embed", "item")
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.sections.traditionalpageone", Matchers.allOf(
                                hasJsonPath("$['dc.contributor.author'][0].value", is("Smith, Donald")),
                                hasJsonPath("$['dc.contributor.author'][1].value", is("Doe, John")),
                                hasJsonPath("$['dc.date.issued'][0].value", is("2017-10-17")),
                                hasJsonPath("$['dc.title'][0].value", is("Title Workspace 1"))
                                )))
                        .andExpect(jsonPath("$._embedded.item.metadata", Matchers.allOf(
                                hasJsonPath("$['dc.contributor.author'][0].value", is("Smith, Donald")),
                                hasJsonPath("$['dc.contributor.author'][1].value", is("Doe, John")),
                                hasJsonPath("$['dc.date.issued'][0].value", is("2017-10-17")),
                                hasJsonPath("$['dc.title'][0].value", is("Title Workspace 1")),
                                hasJsonPath("$['dspace.entity.type'][0].value", is("Publication"))
                                )));

        Item item = witem.getItem();
        // dc.date.issued should be hidden
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                       is ("Title Workspace 1")))
                               .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value",
                                       is ("Publication")))
                               .andExpect(jsonPath("$.metadata['dc.date.issued']").doesNotExist());
    }

    @Test
    public void findOneWorkflowItemUsingLayoutSecurityCustomDataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .withWorkflowGroup(1, admin).build();

        XmlWorkflowItem witem = WorkflowItemBuilder.createWorkflowItem(context, col1)
                                                   .withTitle("Workflow Item 1")
                                                   .withIssueDate("2017-10-17")
                                                   .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                                   .withSubject("ExtraEntry").build();

        MetadataField policyEperson = mfss.findByElement(context, "dspace", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "dspace", "policy", "group");

        MetadataField dateIssued = mfss.findByElement(context, "dc", "date", "issued");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .addMetadataSecurityField(policyEperson)
                                                 .addMetadataSecurityField(policyGroup)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, dateIssued, 0, 0)
                              .withLabel("LABEL DATE")
                              .withRendering("RENDERIGN DATE")
                              .withRowStyle("STYLE")
                              .withBox(box1).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                                get("/api/workflow/workflowitems/" + witem.getID())
                                    .param("embed", "item")
                            )
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.sections.traditionalpageone", Matchers.allOf(
                                    hasJsonPath("$['dc.contributor.author'][0].value", is("Smith, Donald")),
                                    hasJsonPath("$['dc.contributor.author'][1].value", is("Doe, John")),
                                    hasJsonPath("$['dc.date.issued'][0].value", is("2017-10-17")),
                                    hasJsonPath("$['dc.title'][0].value", is("Workflow Item 1"))
                                    )))
                            .andExpect(jsonPath("$._embedded.item.metadata", Matchers.allOf(
                                    hasJsonPath("$['dc.contributor.author'][0].value", is("Smith, Donald")),
                                    hasJsonPath("$['dc.contributor.author'][1].value", is("Doe, John")),
                                    hasJsonPath("$['dc.date.issued'][0].value", is("2017-10-17")),
                                    hasJsonPath("$['dc.title'][0].value", is("Workflow Item 1")),
                                    hasJsonPath("$['dspace.entity.type'][0].value", is("Publication"))
                                    )));

        Item item = witem.getItem();
        // dc.date.issued should be hidden
        getClient(authToken).perform(get("/api/core/items/" + item.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.metadata['dc.title'].[0].value",
                                       is ("Workflow Item 1")))
                               .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value",
                                       is ("Publication")))
                               .andExpect(jsonPath("$.metadata['dc.date.issued']").doesNotExist());
    }

    @Test
    public void findOneEditItemUsingLayoutSecurityCustomDataTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        EditItem editItem = new EditItem(context, itemA);

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");

        MetadataField policyEperson = mfss.findByElement(context, "dspace", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "dspace", "policy", "group");

        MetadataField dateIssued = mfss.findByElement(context, "dc", "date", "issued");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.CUSTOM_DATA)
                                                 .addMetadataSecurityField(policyEperson)
                                                 .addMetadataSecurityField(policyGroup)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, dateIssued, 0, 0)
                              .withLabel("LABEL DATE")
                              .withRendering("RENDERIGN DATE")
                              .withRowStyle("STYLE")
                              .withBox(box1).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(
                                 get("/api/core/edititems/" + itemA.getID() + ":MODE1")
                                     .param("embed", "item")
                             )
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.traditionalpageone", Matchers.allOf(
                                     hasJsonPath("$['dc.contributor.author'][0].value", is("Smith, Maria")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25")),
                                     hasJsonPath("$['dc.title'][0].value", is("Title item A"))
                                     )))
                             .andExpect(jsonPath("$._embedded.item.metadata", Matchers.allOf(
                                     hasJsonPath("$['dc.contributor.author'][0].value", is("Smith, Maria")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25")),
                                     hasJsonPath("$['dc.title'][0].value", is("Title item A"))
                                     )));

    }

    @Test
    public void configurationContainLayoutSecurityWithNestedFieldAdministratorTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withPersonEducation("School")
                                .withPersonEducationStartDate("2010-09-15")
                                .withPersonEducationEndDate("2015-06-24")
                                .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField education = mfss.findByElement(context, "crisrp", "education", null);

        MetadataField educationStart = mfss.findByElement(context, "crisrp", "education", "start");
        MetadataField educationEnd = mfss.findByElement(context, "crisrp", "education", "end");

        List<MetadataField> crisMetadataGroup = new ArrayList<MetadataField>();
        crisMetadataGroup.add(educationStart);
        crisMetadataGroup.add(educationEnd);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.ADMINISTRATOR)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, abs, 0, 0)
                              .withLabel("LABEL ABS")
                              .withRendering("RENDERIGN ABS")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, education, 0, 0)
                              .withLabel("LABEL Education")
                              .withRendering("RENDERIGN Education")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .withNestedField(crisMetadataGroup)
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

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        // An admin can see the dc.description.abstract metadata
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value", is ("A secured abstract")))
                 .andExpect(jsonPath("$.metadata['crisrp.education'].[0].value", is ("School")))
                 .andExpect(jsonPath("$.metadata['crisrp.education.start'].[0].value", is ("2010-09-15")))
                 .andExpect(jsonPath("$.metadata['crisrp.education.end'].[0].value", is ("2015-06-24")))
                 .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")));

        // An user who is not admin can not see the dc.description.abstract metadata
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is ("Public item A")))
                               .andExpect(jsonPath("$.metadata['crisrp.education']").doesNotExist())
                               .andExpect(jsonPath("$.metadata['crisrp.education.start']").doesNotExist())
                               .andExpect(jsonPath("$.metadata['crisrp.education.end']").doesNotExist())
                               .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        // An anonymous user can not see the dc.description.abstract metadata
        getClient().perform(get("/api/core/items/" + itemA.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.metadata['dc.title'].[0].value",is ("Public item A")))
                   .andExpect(jsonPath("$.metadata['crisrp.education']").doesNotExist())
                   .andExpect(jsonPath("$.metadata['crisrp.education.start']").doesNotExist())
                   .andExpect(jsonPath("$.metadata['crisrp.education.end']").doesNotExist())
                   .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

    @Test
    public void configurationContainMetadataSecurityThirdLevel() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Publication")
                .withName("Collection 1")
                .build();
        Item itemA = ItemBuilder.createItem(context, col1).build();
        itemService.addSecuredMetadata(context, itemA, "dc", "description", "abstract", null, "Metadata Secured",
            null, 0, 2);
        itemService.addMetadata(context, itemA, "dspace", "object", "owner", null, "Owner of the item",
            admin.getID().toString(), 0, 1);
        MetadataField description = mfss.findByElement(context, "dc", "description", "abstract");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("box-shortname-one")
                .withSecurity(LayoutSecurity.PUBLIC)
                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, description, 0, 0)
                .withLabel("LABEL ABS")
                .withRendering("RENDERIGN ABS")
                .withRowStyle("STYLE")
                .withBox(box1)
                .build();
        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // An admin can see the dc.description.abstract metadata
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is ("Publication")))
                .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value", is ("Metadata Secured")))
                .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].securityLevel", is (2)));

        // An user who is not admin can not see the dc.description.abstract metadata
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is ("Publication")))
                .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
        // An anonymous user can not see the dc.description.abstract metadata
        getClient().perform(get("/api/core/items/" + itemA.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is ("Publication")))
                .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

    }

    @Test
    public void configurationContainMetadataSecurityFirstLevel() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Person")
                .withName("Collection 1")
                .build();
        Item itemA = ItemBuilder.createItem(context, col1).build();
        itemService.addSecuredMetadata(context, itemA, "dc", "description", "provenance", null, "Metadata Secured",
            null, 0, 0);
        itemService.addMetadata(context, itemA, "dspace", "object", "owner", null, "Owner of the item",
            admin.getID().toString(), 0, 1);
        MetadataField description = mfss.findByElement(context, "dc", "description", "provenance");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("box-shortname-one")
                .withSecurity(LayoutSecurity.PUBLIC)
                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, description, 0, 0)
                .withLabel("LABEL ABS")
                .withRendering("RENDERIGN ABS")
                .withRowStyle("STYLE")
                .withBox(box1)
                .build();
        context.restoreAuthSystemState();
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // An admin can see the dc.description.provenance metadata
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID())
                                 .param("projection", "allLanguages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is ("Person")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].value", is ("Metadata Secured")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].securityLevel", is (0)));

        // An user who is not admin can not see the dc.description.provenance metadata
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID())
                                   .param("projection", "allLanguages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is ("Person")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].value", is ("Metadata Secured")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].securityLevel", is (0)));

        // An anonymous user can not see the dc.description.provenance metadata
        getClient().perform(get("/api/core/items/" + itemA.getID())
                       .param("projection", "allLanguages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is ("Person")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].value", is ("Metadata Secured")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].securityLevel", is (0)));

    }

    @Test
    public void configurationContainMetadataSecuritySecondLevel() throws Exception {
        context.turnOffAuthorisationSystem();

        GroupBuilder.createGroup(context)
            .withName("Trusted")
            .addMember(eperson)
            .build();

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Person")
                .withName("Collection 1")
                .build();
        Item itemA = ItemBuilder.createItem(context, col1).build();
        itemService.addSecuredMetadata(context, itemA, "dc", "description", "provenance", null, "Metadata Secured",
            null, 0, 1);
        itemService.addMetadata(context, itemA, "dspace", "object", "owner", null, "Owner of the item",
            admin.getID().toString(), 0, 1);
        MetadataField description = mfss.findByElement(context, "dc", "description", "provenance");

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                .withShortname("box-shortname-one")
                .withSecurity(LayoutSecurity.PUBLIC)
                .build();

        CrisLayoutFieldBuilder.createMetadataField(context, description, 0, 0)
                .withLabel("LABEL ABS")
                .withRendering("RENDERIGN ABS")
                .withRowStyle("STYLE")
                .withBox(box1)
                .build();

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(ePersonService.find(context, eperson.getID()).getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // An admin can see the dc.description.provenance metadata
        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID())
                                 .param("projection", "allLanguages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is ("Person")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].value", is ("Metadata Secured")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].securityLevel", is (1)));

        // An user that belongs to 'Trusted' group can see the dc.description.provenance metadata
        getClient(tokenEperson).perform(get("/api/core/items/" + itemA.getID())
                                   .param("projection", "allLanguages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is ("Person")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].value", is ("Metadata Secured")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].securityLevel", is (1)));

        // An anonymous user can not see the dc.description.provenance metadata
        getClient().perform(get("/api/core/items/" + itemA.getID())
                       .param("projection", "allLanguages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata['dspace.entity.type'].[0].value", is ("Person")))
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].value").doesNotExist())
                .andExpect(jsonPath("$.metadata['dc.description.provenance'].[1].securityLevel").doesNotExist());

    }

    @Test
    public void configurationContainLayoutSecurityCustomDataAndAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

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
        EPerson userC = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Simone", "Proni")
            .withEmail("user.c@example.com")
            .withPassword(password)
            .build();

        Group groupA = GroupBuilder.createGroup(context)
            .withName("Group A")
            .addMember(userC)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, col1)
            .withTitle("Public item A")
            .withIssueDate("2015-06-25")
            .withAuthor("Smith, Maria")
            .build();

        itemService.addMetadata(context, itemA, "dc", "description", "abstract", null, "A secured abstract");
        itemService.addMetadata(context, itemA, "dspace", "policy", "eperson", null, userA.getFullName(),
            userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "dspace", "policy", "group", null, groupA.getName(),
            groupA.getID().toString(), 600);

        MetadataField policyEperson = mfss.findByElement(context, "dspace", "policy", "eperson");
        MetadataField policyGroup = mfss.findByElement(context, "dspace", "policy", "group");

        MetadataField abs = mfss.findByElement(context, "dc", "description", "abstract");
        MetadataField title = mfss.findByElement(context, "dc", "title", null);

        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
            .withShortname("box-shortname-one")
            .withSecurity(LayoutSecurity.CUSTOM_DATA_AND_ADMINISTRATOR)
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

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);
        String tokenUserC = getAuthToken(userC.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenUserA).perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                is("A secured abstract")))
            .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Public item A")));

        getClient(tokenUserB).perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Public item A")))
            .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());

        getClient(tokenUserC).perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                is("A secured abstract")))
            .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Public item A")));

        getClient(tokenAdmin).perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.title'].[0].value", is("Public item A")))
            .andExpect(jsonPath("$.metadata['dc.description.abstract'].[0].value",
                is("A secured abstract")));

        getClient().perform(get("/api/core/items/" + itemA.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata['dc.description.abstract']").doesNotExist());
    }

}
