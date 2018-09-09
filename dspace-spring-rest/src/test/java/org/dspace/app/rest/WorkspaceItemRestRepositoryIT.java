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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.WorkspaceItemBuilder;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.WorkspaceItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test suite for the WorkspaceItem endpoint
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class WorkspaceItemRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    /**
     * All the workspaceitem should be returned regardless of the collection where they were created
     * 
     * @throws Exception
     */
    public void findAllTest() throws Exception {
        context.setCurrentUser(admin);

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();


        //2. Three workspace items in two different collections
        WorkspaceItem workspaceItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                                      .withTitle("Workspace Item 1")
                                      .withIssueDate("2017-10-17")
                                      .build();

        WorkspaceItem workspaceItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 2")
                                      .withIssueDate("2016-02-13")
                                      .build();

        WorkspaceItem workspaceItem3 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 3")
                                      .withIssueDate("2016-02-13")
                                      .build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/submission/workspaceitems"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.workspaceitems", Matchers.containsInAnyOrder(
                        WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workspace Item 1",
                                "2017-10-17"),
                        WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2, "Workspace Item 2",
                                "2016-02-13"),
                        WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem3, "Workspace Item 3",
                                "2016-02-13"))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/submission/workspaceitems")))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    /**
     * The workspaceitem endpoint must provide proper pagination
     * 
     * @throws Exception
     */
    public void findAllWithPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();


        //2. Three workspace items in two different collections
        WorkspaceItem workspaceItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                                      .withTitle("Workspace Item 1")
                                      .withIssueDate("2017-10-17")
                                      .build();

        WorkspaceItem workspaceItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 2")
                                      .withIssueDate("2016-02-13")
                                      .build();

        WorkspaceItem workspaceItem3 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
                                      .withTitle("Workspace Item 3")
                                      .withIssueDate("2016-02-13")
                                      .build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/submission/workspaceitems").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workspaceitems",
                        Matchers.containsInAnyOrder(
                                WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workspace Item 1",
                                        "2017-10-17"),
                                WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2, "Workspace Item 2",
                                        "2016-02-13"))))
                .andExpect(jsonPath("$._embedded.workspaceitems",
                        Matchers.not(Matchers.contains(WorkspaceItemMatcher
                                .matchItemWithTitleAndDateIssued(workspaceItem3, "Workspace Item 3", "2016-02-13")))));

        getClient(token).perform(get("/api/submission/workspaceitems").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workspaceitems",
                        Matchers.contains(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem3,
                                "Workspace Item 3", "2016-02-13"))))
                .andExpect(jsonPath("$._embedded.workspaceitems",
                        Matchers.not(Matchers.contains(
                                WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem1, "Workspace Item 1",
                                        "2017-10-17"),
                                WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(workspaceItem2, "Workspace Item 2",
                                        "2016-02-13")))))
                .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    /**
     * The workspaceitem resource endpoint must expose the proper structure
     * 
     * @throws Exception
     */
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. a workspace item
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssuedAndSubject(witem,
                                "Workspace Item 1", "2017-10-17", "ExtraEntry"))));
    }

    @Test
    /**
     * The workspaceitem resource endpoint must expose the proper structure
     * 
     * @throws Exception
     */
    public void findOneRelsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        //2. a workspace item
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID() + "/collection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers
                        .is(CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle()))));

        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID() + "/item")).andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(ItemMatcher.matchItemWithTitleAndDateIssued(witem.getItem(),
                        "Workspace Item 1", "2017-10-17"))));

        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID() + "/submissionDefinition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(hasJsonPath("$.id", is("traditional")))));

    }

    @Test
    /**
     * Check the response code for unexistent workspaceitem
     * 
     * @throws Exception
     */
    public void findOneWrongUUIDTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/submission/workspaceitems/" + UUID.randomUUID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Removing a workspaceitem should result in delete of all the underline resources (item and bitstreams)
     * 
     * @throws Exception
     */
    public void deleteOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. a workspace item
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Workspace Item 1")
                .withIssueDate("2017-10-17")
                .build();

        Item item = witem.getItem();

        //Add a bitstream to the item
        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder
                    .createBitstream(context, item, is)
                    .withName("Bitstream1")
                    .withMimeType("text/plain").build();
        }

        String token = getAuthToken(admin.getEmail(), password);

        //Delete the workspaceitem
        getClient(token).perform(delete("/api/submission/workspaceitems/" + witem.getID()))
                    .andExpect(status().is(204));

        //Trying to get deleted item should fail with 404
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                   .andExpect(status().is(404));

        //Trying to get deleted workspaceitem's item should fail with 404
        getClient().perform(get("/api/core/items/" + item.getID()))
                   .andExpect(status().is(404));

        //Trying to get deleted workspaceitem's bitstream should fail with 404
        getClient().perform(get("/api/core/biststreams/" + bitstream.getID()))
                   .andExpect(status().is(404));
    }

}
