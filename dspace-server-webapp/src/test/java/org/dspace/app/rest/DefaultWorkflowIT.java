/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Integration tests to verify workflows behavior.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DefaultWorkflowIT extends AbstractControllerIntegrationTest {

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private ClaimedTaskService claimedTaskService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ItemService itemService;

    @Value("classpath:org/dspace/app/rest/simple-article.pdf")
    private Resource simpleArticle;

    private EntityType publicationType;

    private AtomicReference<Integer> workspaceItemIdRef = new AtomicReference<Integer>();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationType, publicationType,
            "isCorrectionOfItem", "isCorrectedByItem", 0, 1, 0, 1);

        context.setCurrentUser(eperson);

        context.restoreAuthSystemState();
    }

    @Override
    public void destroy() throws Exception {
        if (workspaceItemIdRef.get() != null) {
            WorkspaceItemBuilder.deleteWorkspaceItem(workspaceItemIdRef.get());
        }
        poolTaskService.findAll(context).forEach(this::deletePoolTask);
        super.destroy();
    }

    @Test
    public void itemCorrectionApprovedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withRelationshipType("Publication")
            .withWorkflowGroup(2, eperson)
            .withSubmitterGroup(eperson)
            .build();

        Item itemToBeCorrected = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", simpleArticle.getInputStream())
            .withIssueDate("2016-10-17")
            .withType("Controlled Vocabulary for Resource Type Genres::sound")
            .grantLicense()
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/submission/workspaceitems")
            .param("owningCollection", collection.getID().toString())
            .param("relationship", "isCorrectionOfItem")
            .param("item", itemToBeCorrected.getID().toString())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andDo(result -> workspaceItemIdRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        Integer workspaceItemId = workspaceItemIdRef.get();

        List<Relationship> relationships = relationshipService.findByItem(context, itemToBeCorrected);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThat(relationship.getRelationshipType().getLeftwardType(), equalTo("isCorrectionOfItem"));
        assertThat(relationship.getRightItem(), equalTo(itemToBeCorrected));

        Item correctionItem = relationship.getLeftItem();
        assertThat(workspaceItemService.findByItem(context, correctionItem).getID(), equalTo(workspaceItemId));

        MetadataValueRest value = new MetadataValueRest("Test item correction");
        List<Operation> operations = asList(new ReplaceOperation("/sections/publication/dc.title/0", value));

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + workspaceItemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getPatchContent(operations)))
            .andExpect(status().isOk());

        getClient(authToken).perform(post("/api/workflow/workflowitems")
            .content("/api/submission/workspaceitems/" + workspaceItemId).contentType(textUriContentType))
            .andExpect(status().isCreated());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("submit_approve", "submit_approve");
        claimTaskAndPerformAction(eperson, params);

        itemToBeCorrected = context.reloadEntity(itemToBeCorrected);
        assertThat(itemService.getName(itemToBeCorrected), equalTo("Test item correction"));

        correctionItem = context.reloadEntity(correctionItem);
        assertThat(correctionItem, nullValue());

    }

    @Test
    public void itemCorrectionRejectedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withRelationshipType("Publication")
            .withWorkflowGroup(2, eperson)
            .withSubmitterGroup(eperson)
            .build();

        Item itemToBeCorrected = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", simpleArticle.getInputStream())
            .withIssueDate("2016-10-17")
            .withType("Controlled Vocabulary for Resource Type Genres::sound")
            .grantLicense()
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/submission/workspaceitems")
            .param("owningCollection", collection.getID().toString())
            .param("relationship", "isCorrectionOfItem")
            .param("item", itemToBeCorrected.getID().toString())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andDo(result -> workspaceItemIdRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        Integer workspaceItemId = workspaceItemIdRef.get();

        List<Relationship> relationships = relationshipService.findByItem(context, itemToBeCorrected);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThat(relationship.getRelationshipType().getLeftwardType(), equalTo("isCorrectionOfItem"));
        assertThat(relationship.getRightItem(), equalTo(itemToBeCorrected));

        Item correctionItem = relationship.getLeftItem();
        assertThat(workspaceItemService.findByItem(context, correctionItem).getID(), equalTo(workspaceItemId));

        MetadataValueRest value = new MetadataValueRest("Test item correction");
        List<Operation> operations = asList(new ReplaceOperation("/sections/publication/dc.title/0", value));

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + workspaceItemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getPatchContent(operations)))
            .andExpect(status().isOk());

        getClient(authToken).perform(post("/api/workflow/workflowitems")
            .content("/api/submission/workspaceitems/" + workspaceItemId).contentType(textUriContentType))
            .andExpect(status().isCreated());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("submit_reject", "submit_reject");
        params.add("reason", "wrong title");
        claimTaskAndPerformAction(eperson, params);

        itemToBeCorrected = context.reloadEntity(itemToBeCorrected);
        assertThat(itemService.getName(itemToBeCorrected), equalTo("Test item"));

        assertThat(context.reloadEntity(correctionItem), notNullValue());
        assertThat(workspaceItemService.findByItem(context, correctionItem), notNullValue());

    }

    @Test
    public void itemCorrectionWithoutReviewersTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withRelationshipType("Publication")
            .withSubmitterGroup(eperson)
            .build();

        Item itemToBeCorrected = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", simpleArticle.getInputStream())
            .withIssueDate("2016-10-17")
            .withType("Controlled Vocabulary for Resource Type Genres::sound")
            .grantLicense()
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/submission/workspaceitems")
            .param("owningCollection", collection.getID().toString())
            .param("relationship", "isCorrectionOfItem")
            .param("item", itemToBeCorrected.getID().toString())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andDo(result -> workspaceItemIdRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        Integer workspaceItemId = workspaceItemIdRef.get();

        List<Relationship> relationships = relationshipService.findByItem(context, itemToBeCorrected);
        assertThat(relationships, hasSize(1));

        Relationship relationship = relationships.get(0);
        assertThat(relationship.getRelationshipType().getLeftwardType(), equalTo("isCorrectionOfItem"));
        assertThat(relationship.getRightItem(), equalTo(itemToBeCorrected));

        Item correctionItem = relationship.getLeftItem();
        assertThat(workspaceItemService.findByItem(context, correctionItem).getID(), equalTo(workspaceItemId));

        MetadataValueRest value = new MetadataValueRest("Test item correction");
        List<Operation> operations = asList(new ReplaceOperation("/sections/publication/dc.title/0", value));

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + workspaceItemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getPatchContent(operations)))
            .andExpect(status().isOk());

        getClient(authToken).perform(post("/api/workflow/workflowitems")
            .content("/api/submission/workspaceitems/" + workspaceItemId).contentType(textUriContentType))
            .andExpect(status().isCreated());

        itemToBeCorrected = context.reloadEntity(itemToBeCorrected);
        assertThat(itemService.getName(itemToBeCorrected), equalTo("Test item correction"));

        assertThat(context.reloadEntity(correctionItem), nullValue());
        assertThat(workspaceItemService.findByItem(context, correctionItem), nullValue());
    }

    private void claimTaskAndPerformAction(EPerson user, MultiValueMap<String, String> params) throws Exception {

        List<PoolTask> poolTasks = poolTaskService.findByEperson(context, user);
        assertThat(poolTasks, hasSize(1));

        performActionOnPoolTaskViaRest(user, poolTasks.get(0));

        List<ClaimedTask> claimedTasks = claimedTaskService.findByEperson(context, user);
        assertThat(claimedTasks, hasSize(1));

        performActionOnClaimedTaskViaRest(user, claimedTasks.get(0), params);

    }

    private void performActionOnPoolTaskViaRest(EPerson user, PoolTask task) throws Exception {
        getClient(getAuthToken(user.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/pooltasks/{id}", task.getID())
                .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isNoContent());
    }

    private void performActionOnClaimedTaskViaRest(EPerson user, ClaimedTask task, MultiValueMap<String, String> params)
        throws Exception {
        getClient(getAuthToken(user.getEmail(), password))
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/claimedtasks/{id}", task.getID())
                .params(params)
                .contentType("application/x-www-form-urlencoded"))
            .andExpect(status().isNoContent());
    }

    private void deletePoolTask(PoolTask poolTask) {
        try {
            poolTaskService.delete(context, poolTask);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }
}
