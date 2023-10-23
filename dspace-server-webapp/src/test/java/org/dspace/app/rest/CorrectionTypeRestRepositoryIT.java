/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.repository.CorrectionTypeRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Test suite for {@link CorrectionTypeRestRepository}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 *
 */
public class CorrectionTypeRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuthorizeService authorizeService;

    @Test
    public void findAllTest() throws Exception {
        getClient().perform(get("/api/config/correctiontypes"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(4)))
                   .andExpect(jsonPath("$._embedded.correctiontypes", containsInAnyOrder(
                       allOf(
                           hasJsonPath("$.id", equalTo("addpersonalpath")),
                           hasJsonPath("$.topic", equalTo("/DSPACEUSERS/RELATIONADD/dc.relation.personalpath")),
                           hasJsonPath("$.discoveryConfiguration", equalTo("RELATION.PersonPath.Items")),
                           hasJsonPath("$.creationForm", equalTo("manageRelation"))
                       ),
                       allOf(
                           hasJsonPath("$.id", equalTo("removepersonalpath")),
                           hasJsonPath("$.topic", equalTo("/DSPACEUSERS/RELATIONREMOVE/dc.relation.personalpath")),
                           hasJsonPath("$.discoveryConfiguration", equalTo("RELATION.PersonPath.Items")),
                           hasJsonPath("$.creationForm", equalTo("manageRelation"))
                       ),
                       allOf(hasJsonPath(
                           "$.id", equalTo("addpersonalarchive")),
                           hasJsonPath("$.topic", equalTo("/DSPACEUSERS/RELATIONADD/dc.relation.personalarchive")),
                           hasJsonPath("$.discoveryConfiguration", equalTo("RELATION.PersonArchive.Items")),
                           hasJsonPath("$.creationForm", equalTo("manageRelation"))
                       ),
                       allOf(
                           hasJsonPath("$.id", equalTo("removepersonalarchive")),
                           hasJsonPath("$.topic", equalTo("/DSPACEUSERS/RELATIONREMOVE/dc.relation.personalarchive")),
                           hasJsonPath("$.discoveryConfiguration", equalTo("RELATION.PersonArchive.Items")),
                           hasJsonPath("$.creationForm", equalTo("manageRelation"))
                       )
                   )));
    }

    @Test
    public void findOneTest() throws Exception {
        getClient().perform(get("/api/config/correctiontypes/test"))
                   .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findByItemWithoutUUIDParameterTest() throws Exception {
        getClient().perform(get("/api/config/correctiontypes/search/findByItem/"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findByItemNotFoundTest() throws Exception {
        getClient().perform(get("/api/config/correctiontypes/search/findByItem/")
                       .param("uuid", UUID.randomUUID().toString()))
                   .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findByItemUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        Item privateItem = ItemBuilder.createItem(context, collection).build();
        authorizeService.removeAllPolicies(context, privateItem);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/config/correctiontypes/search/findByItem/")
                       .param("uuid", privateItem.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByNotArchivedItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        item.setArchived(false);
        itemService.update(context, item);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/config/correctiontypes/search/findByItem/")
                       .param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByWithdrawnItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        Item item = ItemBuilder.createItem(context, collection).withdrawn().build();
        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/config/correctiontypes/search/findByItem/")
                .param("uuid", item.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByNotDiscoverableItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        item.setDiscoverable(false);
        itemService.update(context, item);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/config/correctiontypes/search/findByItem/")
                       .param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByPersonalArchiveItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        Item itemOne = ItemBuilder.createItem(context, collection).withEntityType("PersonalArchive").build();
        Item itemTwo = ItemBuilder.createItem(context, collection).withEntityType("PersonalPath").build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/config/correctiontypes/search/findByItem/")
                       .param("uuid", itemOne.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));

        getClient().perform(get("/api/config/correctiontypes/search/findByItem/")
                       .param("uuid", itemTwo.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/config/correctiontypes/search/findByItem/")
                       .param("uuid", item.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(4)))
                   .andExpect(jsonPath("$._embedded.correctiontypes", containsInAnyOrder(
                       allOf(
                           hasJsonPath("$.id", equalTo("addpersonalpath")),
                           hasJsonPath("$.topic", equalTo("/DSPACEUSERS/RELATIONADD/dc.relation.personalpath")),
                           hasJsonPath("$.discoveryConfiguration", equalTo("RELATION.PersonPath.Items")),
                           hasJsonPath("$.creationForm", equalTo("manageRelation"))
                       ),
                       allOf(
                           hasJsonPath("$.id", equalTo("removepersonalpath")),
                           hasJsonPath("$.topic", equalTo("/DSPACEUSERS/RELATIONREMOVE/dc.relation.personalpath")),
                           hasJsonPath("$.discoveryConfiguration", equalTo("RELATION.PersonPath.Items")),
                           hasJsonPath("$.creationForm", equalTo("manageRelation"))
                       ),
                       allOf(hasJsonPath(
                               "$.id", equalTo("addpersonalarchive")),
                           hasJsonPath("$.topic", equalTo("/DSPACEUSERS/RELATIONADD/dc.relation.personalarchive")),
                           hasJsonPath("$.discoveryConfiguration", equalTo("RELATION.PersonArchive.Items")),
                           hasJsonPath("$.creationForm", equalTo("manageRelation"))
                       ),
                       allOf(
                           hasJsonPath("$.id", equalTo("removepersonalarchive")),
                           hasJsonPath("$.topic", equalTo("/DSPACEUSERS/RELATIONREMOVE/dc.relation.personalarchive")),
                           hasJsonPath("$.discoveryConfiguration", equalTo("RELATION.PersonArchive.Items")),
                           hasJsonPath("$.creationForm", equalTo("manageRelation"))
                       )
                   )));
    }

    @Test
    public void findByTopicWithoutTopicParameterTest() throws Exception {
        getClient().perform(get("/api/config/correctiontypes/search/findByTopic/"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findByWrongTopicTest() throws Exception {
        getClient().perform(get("/api/config/correctiontypes/search/findByTopic/")
                       .param("topic", "wrongValue"))
                   .andExpect(status().isNoContent());
    }

    @Test
    public void findByTopicTest() throws Exception {
        getClient().perform(get("/api/config/correctiontypes/search/findByTopic/")
                       .param("topic", "/DSPACEUSERS/RELATIONADD/dc.relation.personalpath"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.id", equalTo("addpersonalpath")))
                   .andExpect(jsonPath("$.topic", equalTo("/DSPACEUSERS/RELATIONADD/dc.relation.personalpath")))
                   .andExpect(jsonPath("$.discoveryConfiguration", equalTo("RELATION.PersonPath.Items")))
                   .andExpect(jsonPath("$.creationForm", equalTo("manageRelation")));
    }

}
