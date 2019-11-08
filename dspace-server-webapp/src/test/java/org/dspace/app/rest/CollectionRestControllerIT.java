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
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

public class CollectionRestControllerIT extends AbstractControllerIntegrationTest {

    private ObjectMapper mapper;
    private String adminAuthToken;
    private Collection childCollection;
    private ItemRest testTemplateItem;
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
        testTemplateItem = new ItemRest();
        testTemplateItem.setInArchive(false);
        testTemplateItem.setDiscoverable(false);
        testTemplateItem.setWithdrawn(false);

        testTemplateItem.setMetadata(new MetadataRest()
                .put("dc.description", new MetadataValueRest("dc description content"))
                .put("dc.description.abstract", new MetadataValueRest("dc description abstract content")));

        List<Operation> ops = new ArrayList<>();
        List<Map<String, String>> values = new ArrayList<>();
        Map<String, String> value = new HashMap<>();
        value.put("value", "table of contents");
        values.add(value);
        AddOperation addOperation = new AddOperation("/metadata/dc.description.tableofcontents", values);
        ops.add(addOperation);
        patchBody = getPatchContent(ops);
    }

    @Test
    public void createTemplateItemNotLoggedIn() throws Exception {
        setupTestTemplate();

        getClient().perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createTemplateItem() throws Exception {
        setupTestTemplate();

        MvcResult mvcResult = getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                .andExpect(status().isCreated())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String itemUuidString = String.valueOf(map.get("uuid"));

        getClient(adminAuthToken).perform(get(getTemplateItemUrlTemplate(childCollection.getID().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.id", is(itemUuidString)),
                        hasJsonPath("$.uuid", is(itemUuidString)),
                        hasJsonPath("$.type", is("item")),
                        hasJsonPath("$.inArchive", is(false)),
                        hasJsonPath("$.discoverable", is(false)),
                        hasJsonPath("$.withdrawn", is(false)),
                        hasJsonPath("$.metadata", Matchers.allOf(
                                MetadataMatcher.matchMetadata("dc.description",
                                        "dc description content"),
                                MetadataMatcher.matchMetadata("dc.description.abstract",
                                        "dc description abstract content")
                        )))));
    }

    @Test
    public void createIllegal1TemplateItem() throws Exception {
        setupTestTemplate();
        testTemplateItem.setInArchive(true);

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createIllegal2TemplateItem() throws Exception {
        setupTestTemplate();
        testTemplateItem.setDiscoverable(true);

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createIllegal3TemplateItem() throws Exception {
        setupTestTemplate();
        testTemplateItem.setWithdrawn(true);

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createTemplateItemNoRights() throws Exception {
        setupTestTemplate();

        String userToken = getAuthToken(eperson.getEmail(), password);
        getClient(userToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createDuplicateTemplateItem() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                .andExpect(status().isCreated());

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createTemplateItemForNonexisting() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate("16a4b65b-3b3f-4ef5-8058-ef6f5a653ef9"))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType))
                .andExpect(status().isNotFound());
    }

    @Test
    public void patchTemplateItemNotLoggedIn() throws Exception {
        setupTestTemplate();

        getClient().perform(patch(getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(patchBody)
                .contentType(contentType))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void patchTemplateItem() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType));

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(patchBody)
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.type", is("item")),
                        hasJsonPath("$.inArchive", is(false)),
                        hasJsonPath("$.discoverable", is(false)),
                        hasJsonPath("$.withdrawn", is(false)),
                        hasJsonPath("$.metadata", Matchers.allOf(
                                MetadataMatcher.matchMetadata("dc.description",
                                        "dc description content"),
                                MetadataMatcher.matchMetadata("dc.description.abstract",
                                        "dc description abstract content"),
                                MetadataMatcher.matchMetadata("dc.description.tableofcontents",
                                        "table of contents")
                        )))));

        getClient(adminAuthToken).perform(get(getTemplateItemUrlTemplate(childCollection.getID().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.type", is("item")),
                        hasJsonPath("$.inArchive", is(false)),
                        hasJsonPath("$.discoverable", is(false)),
                        hasJsonPath("$.withdrawn", is(false)),
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
    public void patchIllegal1TemplateItem() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType));

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/inArchive", true);
        ops.add(replaceOperation);
        String illegalPatchBody = getPatchContent(ops);

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(illegalPatchBody)
                .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void patchIllegal2TemplateItem() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType));

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/discoverable", true);
        ops.add(replaceOperation);
        String illegalPatchBody = getPatchContent(ops);

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(illegalPatchBody)
                .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void patchIllegal3TemplateItem() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType));

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/withdrawn", true);
        ops.add(replaceOperation);
        String illegalPatchBody = getPatchContent(ops);

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(illegalPatchBody)
                .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void patchTemplateItemNoRights() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType));

        String userToken = getAuthToken(eperson.getEmail(), password);
        getClient(userToken).perform(patch(getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(patchBody)
                .contentType(contentType));
        // TODO: this should be forbidden
        //        .andExpect(status().isForbidden());
    }

    @Test
    public void patchMissingTemplateItem() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(patchBody)
                .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void patchTemplateItemForNonexisting() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(patch(getTemplateItemUrlTemplate("16a4b65b-3b3f-4ef5-8058-ef6f5a653ef9"))
                .content(patchBody)
                .contentType(contentType))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteTemplateItemNotLoggedIn() throws Exception {
        getClient().perform(delete(getTemplateItemUrlTemplate(childCollection.getID().toString())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteTemplateItem() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType));

        getClient(adminAuthToken).perform(delete(getTemplateItemUrlTemplate(childCollection.getID().toString())))
                .andExpect(status().isNoContent());
    }

    @Test
    public void deleteTemplateItemNoRights() throws Exception {
        setupTestTemplate();

        getClient(adminAuthToken).perform(post(
                getTemplateItemUrlTemplate(childCollection.getID().toString()))
                .content(mapper.writeValueAsBytes(testTemplateItem)).contentType(contentType));

        String userToken = getAuthToken(eperson.getEmail(), password);
        getClient(userToken).perform(delete(getTemplateItemUrlTemplate(childCollection.getID().toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteMissingTemplateItem() throws Exception {
        getClient(adminAuthToken).perform(delete(getTemplateItemUrlTemplate(childCollection.getID().toString())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void deleteTemplateItemForNonexisting() throws Exception {
        getClient(adminAuthToken).perform(delete(getTemplateItemUrlTemplate("16a4b65b-3b3f-4ef5-8058-ef6f5a653ef9")))
                .andExpect(status().isNotFound());
    }

    private String getTemplateItemUrlTemplate(String uuid) {
        return "/api/core/collections/" + uuid + "/itemtemplate";
    }
}
