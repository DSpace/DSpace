/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.util.List.of;
import static org.dspace.app.matcher.ResourcePolicyMatcher.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.edit.EditItem;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Test suite for the EditItem endpoint
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class EditItemRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

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

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        // using "FIRST" mode
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + itemA.getID() + ":FIRST"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                         hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25")),
                         hasJsonPath("$['dc.title'][0].value", is("Title item A"))
                         )))
                 .andExpect(jsonPath("$.sections.titleAndIssuedDate['dc.contributor.author']").doesNotExist());

        // using "SECOND" mode for same item
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":SECOND"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.sections.onlyTitle", Matchers.allOf(
                         hasJsonPath("$['dc.title'][0].value", is("Title item A"))
                         )))
                 .andExpect(jsonPath("$.sections.onlyTitle['dc.date.issued']").doesNotExist())
                 .andExpect(jsonPath("$.sections.onlyTitle['dc.contributor.author']").doesNotExist());

        // using special case "NONE" (not reported in edititem-service.xml configuration) mode for same item
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":none"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._links.modes.href",
                                     is(REST_SERVER_URL +
                                             "core/edititems/" + editItem.getID() + ":none/modes")))
                             .andExpect(jsonPath("$.sections.onlyTitle['dc.title']").doesNotExist())
                             .andExpect(jsonPath("$.sections.onlyTitle['dc.date.issued']").doesNotExist())
                             .andExpect(jsonPath("$.sections.onlyTitle['dc.contributor.author']").doesNotExist());
    }

    @Test
    public void findOneModeConfigurationNotExistTest() throws Exception {
        context.turnOffAuthorisationSystem();

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

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":NotExist"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findOneItemNotExistTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + UUID.randomUUID() + ":FIRST"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void patchAddMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withIssueDate("2015-06-25")
                                .withAuthor("Mykhaylo, Boychuk").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, String>> titelValues = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        titelValues.add(value);
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.title", titelValues));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                            .content(patchBody)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                    hasJsonPath("$['dc.title'][0].value", is("New Title")),
                                    hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                    )));

        // verify that the patch changes have been persisted
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("New Title")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void patchAddMetadataUpdateExistValueTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title of item A")
                                .withIssueDate("2015-06-25").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> addTitle = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        values.add(value);
        addTitle.add(new AddOperation("/sections/titleAndIssuedDate/dc.title", values));

        String patchBody = getPatchContent(addTitle);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("New Title")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));

        // verify that the patch changes have been persisted
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("New Title")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void patchAddMultipleMetadataValuesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title of item A")
                                .withIssueDate("2015-06-25").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value1 = new HashMap<String, String>();
        value1.put("value", "First Subject");
        Map<String, String> value2 = new HashMap<String, String>();
        value2.put("value", "Second Subject");
        values.add(value1);
        values.add(value2);

        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.subject", values));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("Title of item A")),
                                     hasJsonPath("$['dc.subject'][0].value", is("First Subject")),
                                     hasJsonPath("$['dc.subject'][1].value", is("Second Subject")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));

        // verify that the patch changes have been persisted
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("Title of item A")),
                                     hasJsonPath("$['dc.subject'][0].value", is("First Subject")),
                                     hasJsonPath("$['dc.subject'][1].value", is("Second Subject")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void notRepeatableFiledValidationErrorsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title of item A")
                                .withIssueDate("2015-06-25").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value1 = new HashMap<String, String>();
        value1.put("value", "First Title");
        Map<String, String> value2 = new HashMap<String, String>();
        value2.put("value", "Second Title");
        values.add(value1);
        values.add(value2);

        operations.add(new AddOperation("/sections/onlyTitle/dc.title", values));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":SECOND")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        // verify that the patch changes haven't been persisted
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":SECOND"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.onlyTitle['dc.title'][0].value", is("Title of item A")))
                             .andExpect(jsonPath("$.sections.onlyTitle['dc.title'][1].value").doesNotExist());
    }

    @Test
    public void patchAddMetadataOnSectionNotExistentTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title of item A")
                                .withIssueDate("2015-06-25").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> addTitle = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        values.add(value);
        addTitle.add(new AddOperation("/sections/not-existing-section/dc.title", values));

        String patchBody = getPatchContent(addTitle);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("Title of item A")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void patchAddMetadataWrongPathOfMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title item A")
                                .withIssueDate("2015-06-25").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> addTitle = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        values.add(value);
        addTitle.add(new AddOperation("/sections/titleAndIssuedDate/dc.not.existing", values));

        String patchBody = getPatchContent(addTitle);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("Title item A")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void patchAddMetadataIssuedDateOnModeSectionThatNotContainMetadataIssuedDateTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title of item A")
                                .withIssueDate("2015-06-25").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> newDate = new ArrayList<Operation>();
        List<Map<String, String>> values = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "2020-03-17");
        values.add(value);
        newDate.add(new AddOperation("/sections/onlyTitle/dc.date.issued", values));

        String patchBody = getPatchContent(newDate);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":SECOND")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("Title of item A")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void patchDeleteMetadataThatNotExistTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title item A")
                                .withIssueDate("2015-06-25").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        operations.add(new RemoveOperation("/sections/titleAndIssuedDate/dc.not.existing/0"));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("Title item A")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void patchDeleteMetadataIssuedDateOnModeSectionThatNotContainMetadataIssuedDateTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title of item A")
                                .withIssueDate("2015-06-25").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        operations.add(new RemoveOperation("/sections/onlyTitle/dc.date.issued/0"));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":SECOND")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("Title of item A")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void patchDeleteAllMetadataOnModeSectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("Title of item A")
                                .withIssueDate("2015-06-25").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        operations.add(new RemoveOperation("/sections/onlyTitle"));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":SECOND")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity());

        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":SECOND"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.onlyTitle", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is(itemA.getName()))
                                     )));
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

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

        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenEperson).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                               .andExpect(status().isForbidden());

    }

    @Test
    public void findOneUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

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

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void findOneOwnerOnlyForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

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
                                .withTitle("Title item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "dspace", "object", "owner", null, userA.getID().toString());

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenEperson = getAuthToken(eperson.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-OWNER"))
                             .andExpect(status().isForbidden());

        getClient(tokenEperson).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-OWNER"))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void findOneOwnerOnlyUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

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
                                .withTitle("Title item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "dspace", "object", "owner", null, userA.getID().toString());

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-OWNER"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void testFindOneWithModeWithManySecurities() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson author = EPersonBuilder.createEPerson(context)
            .withEmail("author@example.com")
            .withPassword(password)
            .build();

        EPerson user = EPersonBuilder.createEPerson(context)
            .withEmail("user@example.com")
            .withPassword(password)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Author")
            .withDspaceObjectOwner(author)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Title item")
            .withIssueDate("2015-06-25")
            .withAuthor("Author", person.getID().toString())
            .withDspaceObjectOwner(eperson)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/edititems/" + item.getID() + ":MODE-WITH-MANY-SECURITIES"))
            .andExpect(status().isUnauthorized());

        getClient(getAuthToken(user.getEmail(), password))
            .perform(get("/api/core/edititems/" + item.getID() + ":MODE-WITH-MANY-SECURITIES"))
            .andExpect(status().isForbidden());

        getClient(getAuthToken(author.getEmail(), password))
            .perform(get("/api/core/edititems/" + item.getID() + ":MODE-WITH-MANY-SECURITIES"))
            .andExpect(status().isOk());

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(get("/api/core/edititems/" + item.getID() + ":MODE-WITH-MANY-SECURITIES"))
            .andExpect(status().isOk());

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/core/edititems/" + item.getID() + ":MODE-WITH-MANY-SECURITIES"))
            .andExpect(status().isOk());

    }

    @Test
    public void findOneOwnerOnlyTest() throws Exception {
        context.turnOffAuthorisationSystem();

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
                                .withTitle("Title item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .withDspaceObjectOwner(userA)
                                .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);

        getClient(tokenUserA).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-OWNER"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                         hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25")),
                         hasJsonPath("$['dc.title'][0].value", is("Title item A"))
                         )))
                 .andExpect(jsonPath("$.sections.titleAndIssuedDate['dc.contributor.author']").doesNotExist());
    }

    @Test
    public void findOneCustomSecurityModeTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password).build();

        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Simone", "Proni")
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
                                .withTitle("Title item A")
                                .withIssueDate("2015-06-25")
                                .withAuthor("Smith, Maria")
                                .build();

        itemService.addMetadata(context, itemA, "cris", "policy", "eperson", null, userA.getFullName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "cris", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenUserB = getAuthToken(userB.getEmail(), password);

        getClient(tokenUserA).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-CUSTOM"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                         hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25")),
                         hasJsonPath("$['dc.title'][0].value", is("Title item A"))
                         )))
                 .andExpect(jsonPath("$.sections.titleAndIssuedDate['dc.contributor.author']").doesNotExist());

        getClient(tokenUserB).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-CUSTOM"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                         hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25")),
                         hasJsonPath("$['dc.title'][0].value", is("Title item A"))
                         )))
                 .andExpect(jsonPath("$.sections.titleAndIssuedDate['dc.contributor.author']").doesNotExist());
    }

    @Test
    public void findOneAuthorCustomSecurityModeTest() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson firstUser = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("First", "User")
            .withEmail("user1@example.com")
            .withPassword(password)
            .build();

        EPerson secondUser = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Second", "User")
            .withEmail("user2@example.com")
            .withPassword(password)
            .build();

        EPerson thirdUser = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Third", "User")
            .withEmail("user3@example.com")
            .withPassword(password)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item author = ItemBuilder.createItem(context, collection)
            .withDspaceObjectOwner(firstUser)
            .build();

        Item editor = ItemBuilder.createItem(context, collection)
            .withDspaceObjectOwner(secondUser)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Item title")
            .withIssueDate("2015-06-25")
            .withAuthor("First user", author.getID().toString())
            .withEditor("Second user", editor.getID().toString())
            .build();

        context.restoreAuthSystemState();

        getClient(getAuthToken(firstUser.getEmail(), password))
            .perform(get("/api/core/edititems/" + item.getID() + ":AUTHOR-CUSTOM"))
            .andExpect(status().isOk());

        getClient(getAuthToken(secondUser.getEmail(), password))
            .perform(get("/api/core/edititems/" + item.getID() + ":AUTHOR-CUSTOM"))
            .andExpect(status().isForbidden());

        getClient(getAuthToken(thirdUser.getEmail(), password))
            .perform(get("/api/core/edititems/" + item.getID() + ":AUTHOR-CUSTOM"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void patchAddMetadataUsingSecurityConfigurationOwnerTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withIssueDate("2015-06-25")
                                .withAuthor("Mykhaylo, Boychuk")
                                .withDspaceObjectOwner(eperson)
                                .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenOwner = getAuthToken(eperson.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, String>> titelValues = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        titelValues.add(value);
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.title", titelValues));

        String patchBody = getPatchContent(operations);
        getClient(tokenOwner).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST-OWNER")
                            .content(patchBody)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                    hasJsonPath("$['dc.title'][0].value", is("New Title")),
                                    hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                    )));

        // verify that the patch changes have been persisted
        getClient(tokenOwner).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-OWNER"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("New Title")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void patchAddMetadataUsingSecurityConfigurationOwnerForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withIssueDate("2015-06-25")
                                .withAuthor("Mykhaylo, Boychuk")
                                .withDspaceObjectOwner(eperson)
                                .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenOwner = getAuthToken(eperson.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, String>> titelValues = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        titelValues.add(value);
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.title", titelValues));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST-OWNER")
                            .content(patchBody)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isForbidden());

        getClient(tokenOwner).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-OWNER"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )))
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate['dc.title']").doesNotExist());
    }

    @Test
    public void patchAddMetadataUsingSecurityConfigurationCustomTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password).build();

        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Simone", "Proni")
                                      .withEmail("user.b@example.com")
                                      .withPassword(password).build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userB).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withIssueDate("2015-06-25")
                                .withAuthor("Mykhaylo, Boychuk").build();

        itemService.addMetadata(context, itemA, "cris", "policy", "eperson", null, userA.getFullName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "cris", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, String>> titelValues = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        titelValues.add(value);
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.title", titelValues));

        String patchBody = getPatchContent(operations);
        getClient(tokenUserA).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST-CUSTOM")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                    hasJsonPath("$['dc.title'][0].value", is("New Title")),
                                    hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                    )));

        getClient(tokenUserA).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-CUSTOM"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("New Title")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )));
    }

    @Test
    public void patchAddMetadataUsingSecurityConfigurationCustomForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson userA = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Mykhaylo", "Boychuk")
                                      .withEmail("user.a@example.com")
                                      .withPassword(password).build();

        EPerson userB = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("Simone", "Proni")
                                      .withEmail("user.b@example.com")
                                      .withPassword(password).build();

        Group groupA = GroupBuilder.createGroup(context)
                                   .withName("Group A")
                                   .addMember(userB).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withIssueDate("2015-06-25")
                                .withAuthor("Mykhaylo, Boychuk").build();

        itemService.addMetadata(context, itemA, "cris", "policy", "eperson", null, userA.getFullName(),
                                userA.getID().toString(), 600);
        itemService.addMetadata(context, itemA, "cris", "policy", "group", null, groupA.getName(),
                                groupA.getID().toString(), 600);

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenUserA = getAuthToken(userA.getEmail(), password);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Map<String, String>> titelValues = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Title");
        titelValues.add(value);
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.title", titelValues));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST-CUSTOM")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isForbidden());

        getClient(tokenUserA).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST-CUSTOM"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2015-06-25"))
                                     )))
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate['dc.title']").doesNotExist());
    }

    @Test
    public void patchRemoveMandatoryMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("My Title")
                                .withIssueDate("2021-11-11")
                                .withAuthor("Mykhaylo, Boychuk")
                                .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        List<Operation> operations2 = new ArrayList<Operation>();

        List<Map<String, String>> abstractValues = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Abstract");
        abstractValues.add(value);

        operations.add(new RemoveOperation("/sections/titleAndIssuedDate/dc.title/0"));
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.description.abstract", abstractValues));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity())
                             .andExpect(jsonPath("$.[0].message", is("error.validation.required")))
                             .andExpect(jsonPath("$.[0].paths[0]", is("/sections/titleAndIssuedDate/dc.title")));

        // verify that the patch changes have not been persisted
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is(itemA.getName())),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2021-11-11"))
                                     )))
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate['dc.description.abstract']")
                             .doesNotExist());

        operations2.add(new AddOperation("/sections/titleAndIssuedDate/dc.description.abstract", abstractValues));
        operations2.add(new RemoveOperation("/sections/titleAndIssuedDate/dc.title/0"));

        String patchBody2 = getPatchContent(operations2);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody2)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isUnprocessableEntity())
                             .andExpect(jsonPath("$.[0].message", is("error.validation.required")))
                             .andExpect(jsonPath("$.[0].paths[0]", is("/sections/titleAndIssuedDate/dc.title")));

        // verify that the patch changes have not been persisted
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is(itemA.getName())),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2021-11-11"))
                                     )))
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate['dc.description.abstract']")
                             .doesNotExist());
    }

    @Test
    public void patchRemoveAndAddMandatoryMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1").build();

        Item itemA = ItemBuilder.createItem(context, col1)
                                .withTitle("My Title")
                                .withIssueDate("2021-11-11")
                                .withAuthor("Mykhaylo, Boychuk").build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();

        List<Map<String, String>> abstractValues = new ArrayList<Map<String, String>>();
        Map<String, String> value = new HashMap<String, String>();
        value.put("value", "New Abstract");
        abstractValues.add(value);

        List<Map<String, String>> titleValues = new ArrayList<Map<String, String>>();
        Map<String, String> titleValue = new HashMap<String, String>();
        titleValue.put("value", "New TITLE");
        titleValues.add(titleValue);

        operations.add(new RemoveOperation("/sections/titleAndIssuedDate/dc.title/0"));
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.description.abstract", abstractValues));
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.title", titleValues));

        String patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("New TITLE")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2021-11-11")),
                                     hasJsonPath("$['dc.description.abstract'][0].value", is("New Abstract"))
                                     )));

        // verify that the patch changes have been persisted
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("New TITLE")),
                                     hasJsonPath("$['dc.date.issued'][0].value", is("2021-11-11")),
                                     hasJsonPath("$['dc.description.abstract'][0].value", is("New Abstract"))
                                     )));
    }

    @Test
    public void patchReplaceAllMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("item.enable-virtual-metadata", false);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withSubmissionDefinition("modeA")
                                           .withName("Collection 1")
                                           .build();

        Item item = ItemBuilder.createItem(context, col1)
                               .withTitle("My Title")
                               .withIssueDate("2023-03-23")
                               .withAuthor("Wayne, Bruce")
                               .build();

        EditItem editItem = new EditItem(context, item);
        context.restoreAuthSystemState();

        List<Operation> operations = new ArrayList<Operation>();

        List<Map.Entry<String, String>> subjectValues = new ArrayList<>();

        Entry<String, String> subject1 = Map.entry("value", "Subject 1");
        Entry<String, String> subject2 = Map.entry("value", "Subject 2");
        Entry<String, String> subject3 = Map.entry("value", "Subject 3");

        subjectValues.add(subject1);
        subjectValues.add(subject2);
        subjectValues.add(subject3);

        List<Map.Entry<String, String>> titleValues = new ArrayList<>();

        titleValues.add(Map.entry("value", "TITLE 1"));

        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.subject", subjectValues));
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.title", titleValues));

        String patchBody = getPatchContent(operations);
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("TITLE 1")),
                                     hasJsonPath("$['dc.subject'][0].value", is("Subject 1")),
                                     hasJsonPath("$['dc.subject'][1].value", is("Subject 2")),
                                     hasJsonPath("$['dc.subject'][2].value", is("Subject 3"))
                                     )));

        // verify that the patch changes have been persisted
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItem.getID() + ":FIRST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.title'][0].value", is("TITLE 1")),
                                     hasJsonPath("$['dc.subject'][0].value", is("Subject 1")),
                                     hasJsonPath("$['dc.subject'][1].value", is("Subject 2")),
                                     hasJsonPath("$['dc.subject'][2].value", is("Subject 3"))
                                     )));

        operations.clear();

        operations.add(new ReplaceOperation("/sections/titleAndIssuedDate/dc.subject/0", subject2));
        operations.add(new ReplaceOperation("/sections/titleAndIssuedDate/dc.subject/1", subject3));
        operations.add(new ReplaceOperation("/sections/titleAndIssuedDate/dc.subject/2", subject1));

        patchBody = getPatchContent(operations);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                             .content(patchBody)
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.sections.titleAndIssuedDate", Matchers.allOf(
                                     hasJsonPath("$['dc.subject'][0].value", is("Subject 2")),
                                     hasJsonPath("$['dc.subject'][1].value", is("Subject 3")),
                                     hasJsonPath("$['dc.subject'][2].value", is("Subject 1"))
                                     )));
    }

    @Test
    public void testPatchWithValidationErrors() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, collection)
            .withTitle("Intial title")
            .withIssueDate("2023-02-04")
            .withFulltext("bitstream.txt", "source", InputStream.nullInputStream())
            .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> operations = new ArrayList<Operation>();
        operations.add(new RemoveOperation("/sections/titleAndIssuedDate/dc.date.issued"));
        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.title", of(Map.of("value", "My Title"))));

        // we should be unable to change the title removing the date
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$", contains(allOf(
                hasJsonPath("message", is("error.validation.required")),
                hasJsonPath("paths", contains("/sections/titleAndIssuedDate/dc.date.issued"))))));

        operations.add(new AddOperation("/sections/titleAndIssuedDate/dc.date.issued", of(Map.of("value", "2022"))));
        // this should succeed now as we are also providing a new date
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void testPatchItemWithExistingValidationErrors() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Publication")
                .withSubmissionDefinition("traditional-cris")
                .withName("Collection 1")
                .build();

        // the item below miss the mandatory dc.title and dc.date.issued
        Item itemA = ItemBuilder.createItem(context, collection)
                .withAuthor("Wayne, Bruce")
                .grantLicense()
                .build();
        // this one miss the mandatory dc.date.issued
        Item itemB = ItemBuilder.createItem(context, collection)
                .withTitle("At least the title...")
                .withAuthor("Wayne, Bruce")
                .grantLicense()
                .build();

        EditItem editItemA = new EditItem(context, itemA);
        EditItem editItemB = new EditItem(context, itemB);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Map<String, String>> titleValues = new ArrayList<>();
        Map<String, String> titleMap = new HashMap<>();
        List<Map<String, String>> publisherValues = new ArrayList<>();
        Map<String, String> publisherMap = new HashMap<>();
        titleMap.put("value", "A title");
        titleValues.add(titleMap);
        publisherMap.put("value", "A publisher");
        publisherValues.add(publisherMap);
        List<Operation> listOpA = new ArrayList<>();
        listOpA.add(new AddOperation("/sections/traditionalpageone-cris/dc.title", titleValues));
        listOpA.add(new AddOperation("/sections/traditionalpageone-cris/dc.publisher", publisherValues));

        List<Operation> listOpB = new ArrayList<>();
        listOpB.add(new RemoveOperation("/sections/traditionalpageone-cris/dc.title"));
        listOpB.add(new AddOperation("/sections/traditionalpageone-cris/dc.publisher", publisherValues));

        String patchBodyA = getPatchContent(listOpA);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItemA.getID() + ":MODE1")
                        .content(patchBodyA)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.traditionalpageone-cris['dc.title'][0].value",
                        is("A title")))
                .andExpect(jsonPath("$.sections.traditionalpageone-cris['dc.publisher'][0].value",
                        is("A publisher")))
                .andExpect(jsonPath("$.errors[0].message", is("error.validation.required")))
                .andExpect(jsonPath("$.errors[0].paths[0]", is("/sections/traditionalpageone-cris/dc.date.issued")));

        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItemA.getID() + ":MODE1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.traditionalpageone-cris['dc.title'][0].value", is("A title")))
            .andExpect(jsonPath("$.sections.traditionalpageone-cris['dc.publisher'][0].value", is("A publisher")))
            .andExpect(jsonPath("$.errors[0].message", is("error.validation.required")))
            .andExpect(jsonPath("$.errors[0].paths[0]", is("/sections/traditionalpageone-cris/dc.date.issued")));

        // the list of operation B makes the item worst, so we expect a 422 response and the json should
        // describe the errors according to the new state of the item attempted to be generated by the requested
        String patchBodyB = getPatchContent(listOpB);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItemB.getID() + ":MODE1")
                        .content(patchBodyB)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.[0].message", is("error.validation.required")))
                .andExpect(jsonPath("$.[0].paths[0]", is("/sections/traditionalpageone-cris/dc.title")))
                .andExpect(jsonPath("$.[0].paths[1]", is("/sections/traditionalpageone-cris/dc.date.issued")));
        // as the request has been rejected, the state should not be persisted
        getClient(tokenAdmin).perform(get("/api/core/edititems/" + editItemB.getID() + ":MODE1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.traditionalpageone-cris['dc.title'][0].value",
                    is("At least the title...")))
            .andExpect(jsonPath("$.sections.traditionalpageone-cris['dc.publisher']").doesNotExist())
            .andExpect(jsonPath("$.errors[0].message", is("error.validation.required")))
            .andExpect(jsonPath("$.errors[0].paths[0]", is("/sections/traditionalpageone-cris/dc.date.issued")));

    }

    @Test
    public void testUpload() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, collection)
            .withTitle("My Item")
            .withIssueDate("2022")
            .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        InputStream bibtex = getClass().getResourceAsStream("bibtex-test.bib");
        final MockMultipartFile bibtexFile = new MockMultipartFile("file", "/local/path/bibtex-test.bib",
            "application/x-bibtex", bibtex);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(multipart("/api/core/edititems/" + editItem.getID() + ":MODE1")
            .file(bibtexFile))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source'][0].value",
                is("/local/path/bibtex-test.bib")))
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value", is("bibtex-test.bib")));
    }

    @Test
    public void testUploadWithOwner() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        EPerson user = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("First", "User")
            .withEmail("user1@example.com")
            .withPassword(password)
            .build();

        Item itemA = ItemBuilder.createItem(context, collection)
            .withTitle("My Item")
            .withIssueDate("2022")
            .withDspaceObjectOwner(user)
            .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        InputStream bibtex = getClass().getResourceAsStream("bibtex-test.bib");
        final MockMultipartFile bibtexFile = new MockMultipartFile("file", "/local/path/bibtex-test.bib",
            "application/x-bibtex", bibtex);

        String userToken = getAuthToken(user.getEmail(), password);
        getClient(userToken).perform(multipart("/api/core/edititems/" + editItem.getID() + ":TRADITIONAL-OWNER")
            .file(bibtexFile))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.source'][0].value",
                is("/local/path/bibtex-test.bib")))
            .andExpect(jsonPath("$.sections.upload.files[0].metadata['dc.title'][0].value", is("bibtex-test.bib")));
    }

    @Test
    public void testUploadWithoutUploadableStepDefined() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, collection)
            .withTitle("My Item")
            .withIssueDate("2022")
            .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        InputStream bibtex = getClass().getResourceAsStream("bibtex-test.bib");
        final MockMultipartFile bibtexFile = new MockMultipartFile("file", "/local/path/bibtex-test.bib",
            "application/x-bibtex", bibtex);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(multipart("/api/core/edititems/" + editItem.getID() + ":FIRST")
            .file(bibtexFile))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void testForbiddenUpload() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, collection)
            .withTitle("My Item")
            .withIssueDate("2022")
            .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        InputStream bibtex = getClass().getResourceAsStream("bibtex-test.bib");
        final MockMultipartFile bibtexFile = new MockMultipartFile("file", "/local/path/bibtex-test.bib",
            "application/x-bibtex", bibtex);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(multipart("/api/core/edititems/" + editItem.getID() + ":FIRST-OWNER")
            .file(bibtexFile))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testFindModesById() throws Exception {

        context.turnOffAuthorisationSystem();

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

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/core/edititems/search/findModesById")
                                          .param("uuid" , editItem.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded").exists())
                             .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)))
                             .andExpect(jsonPath("$._embedded.edititemmodes[0].id" , is("MODE1")));
    }

    @Test
    public void testFindModesForNotExistingId() throws Exception {

        String id = "bef23ba3-9aeb-4f7b-b153-77b0f1fc3612";

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/core/edititems/search/findModesById")
                                          .param("uuid" , id))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded").doesNotExist())
                             .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void testItemResourcePoliciesUpdateWithoutAppendMode() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withIssueDate("2015-06-25")
            .grantLicense()
            .withFulltext("test.pdf", "source", InputStream.nullInputStream())
            .build();

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Group adminGroup = groupService.findByName(context, Group.ADMIN);

        context.restoreAuthSystemState();

        assertThat(authorizeService.getPolicies(context, itemA),
            contains(matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        Map<String, String> accessCondition = Map.of("name", "administrator");
        Operation addOperation = new AddOperation("/sections/defaultAC/accessConditions", List.of(accessCondition));

        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + itemA.getID() + ":MODE1")
            .content(getPatchContent(List.of(addOperation)))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        assertThat(authorizeService.getPolicies(context, itemA),
            contains(matches(Constants.READ, adminGroup, ResourcePolicy.TYPE_CUSTOM)));

        Operation removeOperation = new RemoveOperation("/sections/defaultAC/accessConditions/0");

        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + itemA.getID() + ":MODE1")
            .content(getPatchContent(List.of(removeOperation)))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        assertThat(authorizeService.getPolicies(context, itemA),
            contains(matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)));

    }

    @Test
    public void testItemResourcePoliciesUpdateWithAppendMode() throws Exception {

        configurationService.setProperty("core.authorization.installitem.inheritance-read.append-mode", "true");

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withIssueDate("2015-06-25")
            .grantLicense()
            .withFulltext("test.pdf", "source", InputStream.nullInputStream())
            .build();

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Group adminGroup = groupService.findByName(context, Group.ADMIN);

        context.restoreAuthSystemState();

        assertThat(authorizeService.getPolicies(context, itemA),
            contains(matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        Map<String, String> accessCondition = Map.of("name", "administrator");
        Operation addOperation = new AddOperation("/sections/defaultAC/accessConditions", List.of(accessCondition));

        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + itemA.getID() + ":MODE1")
            .content(getPatchContent(List.of(addOperation)))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        assertThat(authorizeService.getPolicies(context, itemA), containsInAnyOrder(
            matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED),
            matches(Constants.READ, adminGroup, ResourcePolicy.TYPE_CUSTOM)));

        Operation removeOperation = new RemoveOperation("/sections/defaultAC/accessConditions/0");

        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + itemA.getID() + ":MODE1")
            .content(getPatchContent(List.of(removeOperation)))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        assertThat(authorizeService.getPolicies(context, itemA),
            contains(matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)));

    }

    @Test
    public void testBitstreamResourcePoliciesUpdateWithoutAppendMode() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withIssueDate("2015-06-25")
            .grantLicense()
            .withFulltext("test.pdf", "source", InputStream.nullInputStream())
            .build();

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Group adminGroup = groupService.findByName(context, Group.ADMIN);

        context.restoreAuthSystemState();

        Bitstream bitstream = getBitstream(itemA, "test.pdf");
        assertThat(bitstream, notNullValue());

        assertThat(authorizeService.getPolicies(context, bitstream),
            contains(matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        Map<String, String> value = Map.of("name", "administrator");
        Operation addOperation = new AddOperation("/sections/upload/files/0/accessConditions/-", value);

        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + itemA.getID() + ":MODE1")
            .content(getPatchContent(List.of(addOperation)))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        assertThat(authorizeService.getPolicies(context, bitstream),
            contains(matches(Constants.READ, adminGroup, ResourcePolicy.TYPE_CUSTOM)));

        Operation removeOperation = new RemoveOperation("/sections/upload/files/0/accessConditions/0");

        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + itemA.getID() + ":MODE1")
            .content(getPatchContent(List.of(removeOperation)))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        assertThat(authorizeService.getPolicies(context, bitstream),
            contains(matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)));

    }

    @Test
    public void testBitstreamResourcePoliciesUpdateWithAppendMode() throws Exception {

        configurationService.setProperty("core.authorization.installitem.inheritance-read.append-mode", "true");

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .build();

        Item itemA = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withIssueDate("2015-06-25")
            .grantLicense()
            .withFulltext("test.pdf", "source", InputStream.nullInputStream())
            .build();

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Group adminGroup = groupService.findByName(context, Group.ADMIN);

        context.restoreAuthSystemState();

        Bitstream bitstream = getBitstream(itemA, "test.pdf");
        assertThat(bitstream, notNullValue());

        assertThat(authorizeService.getPolicies(context, bitstream),
            contains(matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)));

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        Map<String, String> value = Map.of("name", "administrator");
        Operation addOperation = new AddOperation("/sections/upload/files/0/accessConditions/-", value);

        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + itemA.getID() + ":MODE1")
            .content(getPatchContent(List.of(addOperation)))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        assertThat(authorizeService.getPolicies(context, bitstream), containsInAnyOrder(
            matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED),
            matches(Constants.READ, adminGroup, ResourcePolicy.TYPE_CUSTOM)));

        Operation removeOperation = new RemoveOperation("/sections/upload/files/0/accessConditions/0");

        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + itemA.getID() + ":MODE1")
            .content(getPatchContent(List.of(removeOperation)))
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk());

        assertThat(authorizeService.getPolicies(context, bitstream),
            contains(matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)));

    }

    @Test
    public void testEditItemModeConfigurationWithEntityTypeAndSubmission() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1")
            .withSubmissionDefinition("modeC")
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Title item A")
            .withIssueDate("2015-06-25")
            .withAuthor("Smith, Maria")
            .withDspaceObjectOwner(admin)
            .build();

        EditItem editItem = new EditItem(context, item);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        getClient(tokenAdmin).perform(get("/api/core/edititems/search/findModesById")
            .param("uuid", editItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded").exists())
            .andExpect(jsonPath("$.page.totalElements", is(2)))
            .andExpect(jsonPath("$._embedded.edititemmodes[0].id", is("MODE-A")))
            .andExpect(jsonPath("$._embedded.edititemmodes[1].id", is("MODE-B")));

    }

    @Test
    public void testEditWithHiddenSections() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .withEntityType("Publication")
            .build();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Item")
            .build();
        context.restoreAuthSystemState();
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/core/edititems/" + item.getID() + ":MODE-TEST-HIDDEN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections.test-outside-workflow-hidden").exists())
            .andExpect(jsonPath("$.sections.test-outside-submission-hidden").doesNotExist())
            .andExpect(jsonPath("$.sections.test-never-hidden").exists())
            .andExpect(jsonPath("$.sections.test-always-hidden").doesNotExist());
    }

    @Test
    public void testValidationWithHiddenSteps() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .withEntityType("Publication")
            .build();
        Item item = ItemBuilder.createItem(context, collection)
            .build();
        context.restoreAuthSystemState();
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/core/edititems/" + item.getID() + ":MODE-TEST-HIDDEN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].message", is("error.validation.required")))
            .andExpect(jsonPath("$.errors[0].paths", contains("/sections/test-outside-workflow-hidden/dc.title")));
    }

    @Test
    public void testPatchWithValidationErrors3213() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Funding")
                .withSubmissionDefinition("modeA")
                .withName("Collection 1")
                .build();

        Collection orgunitCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withEntityType("OrgUnit")
                .withSubmissionDefinition("orgunit")
                .build();

        Item itemA = ItemBuilder.createItem(context, collection)
                .withTitle("Title item")
                .withIssueDate("2015-06-25")
                .withAuthor("Wayne, Bruce")
                .grantLicense()
                .build();

        Item orgUnit = ItemBuilder.createItem(context, orgunitCollection)
                .withTitle("OrgUnit")
                .withIssueDate("1957")
                .grantLicense()
                .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> list = new ArrayList<>();
        List<Map<String, String>> funderValues = new ArrayList<>();
        Map<String, String> funderValuesMap = new HashMap<>();
        List<Map<String, String>> currencyValues = new ArrayList<>();
        Map<String, String> currencyMap = new HashMap<>();
        List<Map<String, String>> amountValues = new ArrayList<>();
        Map<String, String> amountMap = new HashMap<>();
        funderValuesMap.put("value", "OrgUnit");
        funderValuesMap.put("authority", orgUnit.getID().toString());
        funderValues.add(funderValuesMap);
        currencyMap.put("value", "Euro");
        currencyValues.add(currencyMap);
        amountMap.put("value", "12312");
        amountValues.add(amountMap);
        list.add(new AddOperation("/sections/funding/oairecerif.funder", funderValues));
        list.add(new AddOperation("/sections/funding/oairecerif.amount.currency", currencyValues));
        list.add(new AddOperation("/sections/funding/oairecerif.amount", amountValues));

        String patchBody = getPatchContent(list);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.funding['oairecerif.funder'][0].value",
                        is("OrgUnit")))
                .andExpect(jsonPath("$.sections.funding['oairecerif.funder'][0].authority",
                        is(orgUnit.getID().toString())))
                .andExpect(jsonPath("$.sections.funding['oairecerif.amount.currency'][0].value",
                        is("Euro")))
                .andExpect(jsonPath("$.sections.funding['oairecerif.amount'][0].value",
                        is("12312")));
    }

    @Test
    @Ignore("see DSC-1787")
    public void testPatchWithValidationErrors32() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Funding")
                .withSubmissionDefinition("modeA")
                .withName("Collection 1")
                .build();

        Item itemA = ItemBuilder.createItem(context, collection)
                .withIssueDate("2015-06-25")
                .withAuthor("Wayne, Bruce")
                .grantLicense()
                .build();

        EditItem editItem = new EditItem(context, itemA);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);

        List<Operation> list = new ArrayList<>();
        List<Map<String, String>> currencyValues = new ArrayList<>();
        Map<String, String> currencyMap = new HashMap<>();
        List<Map<String, String>> amountValues = new ArrayList<>();
        Map<String, String> amountMap = new HashMap<>();
        currencyMap.put("value", "Euro");
        currencyValues.add(currencyMap);
        amountMap.put("value", "12312");
        amountValues.add(amountMap);
        list.add(new AddOperation("/sections/funding/oairecerif.amount.currency", currencyValues));
        list.add(new AddOperation("/sections/funding/oairecerif.amount", amountValues));

        String patchBody = getPatchContent(list);
        getClient(tokenAdmin).perform(patch("/api/core/edititems/" + editItem.getID() + ":FIRST")
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.[0].message", is("error.validation.required")))
                .andExpect(jsonPath("$.[0].paths[0]", is("/sections/funding/dc.title")));
    }

    private Bitstream getBitstream(Item item, String name) throws SQLException {
        return bitstreamService.getBitstreamByName(item, "ORIGINAL", name);
    }

}
