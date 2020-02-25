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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.TemplateItemRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

public class ItemTemplateRestControllerIT extends AbstractControllerIntegrationTest {

    private ObjectMapper mapper;
    private String adminAuthToken;
    private Collection childCollection;
    private TemplateItemRest testTemplateItem;
    private String patchBody;

    @Before
    public void createStructure() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        childCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();
        adminAuthToken = getAuthToken(admin.getEmail(), password);

        mapper = new ObjectMapper();
    }

    private void setupTestTemplate() {
        testTemplateItem = new TemplateItemRest();

        testTemplateItem.setMetadata(new MetadataRest()
                                         .put("dc.description", new MetadataValueRest("dc description content"))
                                         .put("dc.description.abstract",
                                              new MetadataValueRest("dc description abstract content")));

        List<Operation> ops = new ArrayList<>();
        List<Map<String, String>> values = new ArrayList<>();
        Map<String, String> value = new HashMap<>();
        value.put("value", "table of contents");
        values.add(value);
        AddOperation addOperation = new AddOperation("/metadata/dc.description.tableofcontents", values);
        ops.add(addOperation);
        patchBody = getPatchContent(ops);
    }

    private String installTestTemplate() throws Exception {
        MvcResult mvcResult = getClient(adminAuthToken).perform(post(
            getCollectionTemplateItemUrlTemplate(childCollection.getID().toString()))
                                                                    .content(mapper.writeValueAsBytes(testTemplateItem))
                                                                    .contentType(contentType))
                                                       .andExpect(status().isCreated())
                                                       .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        return String.valueOf(map.get("id"));
    }

    @Test
    public void createTemplateItemNotLoggedIn() throws Exception {
        setupTestTemplate();

        getClient().perform(post(
            getCollectionTemplateItemUrlTemplate(childCollection.getID().toString()))
                                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void createTemplateItem() throws Exception {
        setupTestTemplate();
        installTestTemplate();
    }

    @Test
    public void createTemplateItemNoRights() throws Exception {
        setupTestTemplate();

        String userToken = getAuthToken(eperson.getEmail(), password);
        getClient(userToken).perform(post(
            getCollectionTemplateItemUrlTemplate(childCollection.getID().toString()))
                                         .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void createDuplicateTemplateItem() throws Exception {
        setupTestTemplate();

        installTestTemplate();

        getClient(adminAuthToken).perform(post(
            getCollectionTemplateItemUrlTemplate(childCollection.getID().toString()))
                                              .content(mapper.writeValueAsBytes(testTemplateItem))
                                              .contentType(contentType))
                                 .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createTemplateItemForNonexisting() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
            getCollectionTemplateItemUrlTemplate("16a4b65b-3b3f-4ef5-8058-ef6f5a653ef9"))
                                              .content(mapper.writeValueAsBytes(testTemplateItem))
                                              .contentType(contentType))
                                 .andExpect(status().isNotFound());
    }

    @Test
    public void getTemplateItemFromCollection() throws Exception {
        setupTestTemplate();
        String itemUuidString = installTestTemplate();

        getClient(adminAuthToken).perform(get(getCollectionTemplateItemUrlTemplate(childCollection.getID().toString())))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.allOf(
                                     hasJsonPath("$.id", is(itemUuidString)),
                                     hasJsonPath("$.uuid", is(itemUuidString)),
                                     hasJsonPath("$.type", is("itemtemplate")),
                                     hasJsonPath("$.metadata", Matchers.allOf(
                                         MetadataMatcher.matchMetadata("dc.description",
                                                                       "dc description content"),
                                         MetadataMatcher.matchMetadata("dc.description.abstract",
                                                                       "dc description abstract content")))
                                 )));
    }

    @Test
    public void getTemplateItemFromItemId() throws Exception {
        setupTestTemplate();
        String itemUuidString = installTestTemplate();

        getClient(adminAuthToken).perform(get(getTemplateItemUrlTemplate(itemUuidString)))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.allOf(
                                     hasJsonPath("$.id", is(itemUuidString)),
                                     hasJsonPath("$.uuid", is(itemUuidString)),
                                     hasJsonPath("$.type", is("itemtemplate")),
                                     hasJsonPath("$.metadata", Matchers.allOf(
                                         MetadataMatcher.matchMetadata("dc.description",
                                                                       "dc description content"),
                                         MetadataMatcher.matchMetadata("dc.description.abstract",
                                                                       "dc description abstract content")))
                                 )));
    }

    @Test
    public void patchTemplateItemNotLoggedIn() throws Exception {
        setupTestTemplate();

        String itemId = installTestTemplate();

        getClient().perform(patch(getTemplateItemUrlTemplate(itemId))
                                .content(patchBody)
                                .contentType(contentType))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void patchTemplateItem() throws Exception {
        setupTestTemplate();

        String itemId = installTestTemplate();

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate(itemId))
                                              .content(patchBody)
                                              .contentType(contentType))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.allOf(
                                     hasJsonPath("$.type", is("itemtemplate")),
                                     hasJsonPath("$.metadata", Matchers.allOf(
                                         MetadataMatcher.matchMetadata("dc.description",
                                                                       "dc description content"),
                                         MetadataMatcher.matchMetadata("dc.description.abstract",
                                                                       "dc description abstract content"),
                                         MetadataMatcher.matchMetadata("dc.description.tableofcontents",
                                                                       "table of contents")
                                     )))));

        getClient(adminAuthToken).perform(get(getCollectionTemplateItemUrlTemplate(childCollection.getID().toString())))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", Matchers.allOf(
                                     hasJsonPath("$.type", is("itemtemplate")),
                                     hasJsonPath("$.metadata", Matchers.allOf(
                                         MetadataMatcher.matchMetadata("dc.description",
                                                                       "dc description content"),
                                         MetadataMatcher.matchMetadata("dc.description.abstract",
                                                                       "dc description abstract content"),
                                         MetadataMatcher.matchMetadata("dc.description.tableofcontents",
                                                                       "table of contents")
                                     )))));
    }

    @Test
    public void patchIllegalInArchiveTemplateItem() throws Exception {
        setupTestTemplate();

        String itemId = installTestTemplate();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/inArchive", true);
        ops.add(replaceOperation);
        String illegalPatchBody = getPatchContent(ops);

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate(itemId))
                                              .content(illegalPatchBody)
                                              .contentType(contentType))
                                 .andExpect(status().isBadRequest());
    }

    @Test
    public void patchIllegalDiscoverableTemplateItem() throws Exception {
        setupTestTemplate();

        String itemId = installTestTemplate();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", true);
        ops.add(replaceOperation);
        String illegalPatchBody = getPatchContent(ops);

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate(itemId))
                                              .content(illegalPatchBody)
                                              .contentType(contentType))
                                 .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void patchIllegalWithdrawnTemplateItem() throws Exception {
        setupTestTemplate();

        String itemId = installTestTemplate();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", true);
        ops.add(replaceOperation);
        String illegalPatchBody = getPatchContent(ops);

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate(itemId))
                                              .content(illegalPatchBody)
                                              .contentType(contentType))
                                 .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void patchTemplateItemNoRights() throws Exception {
        setupTestTemplate();

        String itemId = installTestTemplate();

        String userToken = getAuthToken(eperson.getEmail(), password);
        getClient(userToken).perform(patch(getTemplateItemUrlTemplate(itemId))
                                         .content(patchBody)
                                         .contentType(contentType))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void patchTemplateItemNonexisting() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate("16a4b65b-3b3f-4ef5-8058-ef6f5a653ef9"))
                                              .content(patchBody)
                                              .contentType(contentType))
                                 .andExpect(status().isNotFound());
    }

    @Test
    public void deleteTemplateItemNotLoggedIn() throws Exception {
        setupTestTemplate();

        String itemId = installTestTemplate();

        getClient().perform(delete(getTemplateItemUrlTemplate(itemId)))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteTemplateItem() throws Exception {
        setupTestTemplate();

        String itemId = installTestTemplate();

        getClient(adminAuthToken).perform(delete(getTemplateItemUrlTemplate(itemId)))
                                 .andExpect(status().isNoContent());
    }

    @Test
    public void deleteTemplateItemNoRights() throws Exception {
        setupTestTemplate();

        String itemId = installTestTemplate();

        String userToken = getAuthToken(eperson.getEmail(), password);
        getClient(userToken).perform(delete(getTemplateItemUrlTemplate(itemId)))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void deleteTemplateItemForNonexisting() throws Exception {
        getClient(adminAuthToken).perform(delete(getTemplateItemUrlTemplate("16a4b65b-3b3f-4ef5-8058-ef6f5a653ef9")))
                                 .andExpect(status().isNotFound());
    }

    private String getCollectionTemplateItemUrlTemplate(String uuid) {
        return "/api/core/collections/" + uuid + "/itemtemplate";
    }

    private String getTemplateItemUrlTemplate(String uuid) {
        return "/api/core/itemtemplates/" + uuid;
    }
}
