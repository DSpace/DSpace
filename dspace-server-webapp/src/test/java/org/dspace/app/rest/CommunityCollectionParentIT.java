/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

public class CommunityCollectionParentIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CommunityService communityService;

    Community communityA;
    Community communityB;
    Community communityAA;
    Community communityAB;

    Collection col1;
    Collection col2;
    Collection col3;

    Item itemX;
    Item itemY;
    Item itemZ;


    @Before
    public void setup() throws SQLException, AuthorizeException {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        communityA = CommunityBuilder.createCommunity(context)
                                               .withName("Parent CommunityA")
                                               .build();
        communityB = CommunityBuilder.createCommunity(context)
                                               .withName("Parent CommunityB")
                                               .build();
        communityAA = CommunityBuilder.createSubCommunity(context, communityA)
                                                .withName("Sub Community")
                                                .build();
        communityAB = CommunityBuilder.createSubCommunity(context, communityA)
                                                .withName("Sub Community Two")
                                                .build();
        col1 = CollectionBuilder.createCollection(context, communityAA).withName("Collection 1").build();
        col2 = CollectionBuilder.createCollection(context, communityAA).withName("Collection 2").build();
        col3 = CollectionBuilder.createCollection(context, communityAB).withName("Collection 3").build();
        communityService.addCollection(context, communityAB, col2);


        itemX = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item 1")
                                .withIssueDate("2017-10-17")
                                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        itemY = ItemBuilder.createItem(context, col1)
                                .withTitle("Public item 1")
                                .withIssueDate("2017-10-17")
                                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();
        collectionService.addItem(context, col2, itemY);
        itemZ = ItemBuilder.createItem(context, col2)
                                .withTitle("Public item 1")
                                .withIssueDate("2017-10-17")
                                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                .withSubject("ExtraEntry")
                                .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void itemXOwningCollectionTest() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(get("/api/core/items/" + itemX.getID() + "/owningCollection")).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String collectionUuidString =  String.valueOf(map.get("uuid"));
        String collectionName = String.valueOf(map.get("name"));

        mvcResult = getClient(token).perform(get("/api/core/collections/" + col1.getID())).andReturn();
        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String actualCollectionName = String.valueOf(map.get("name"));

        assertThat(collectionName, equalTo(actualCollectionName));
        assertThat(collectionUuidString, equalTo(String.valueOf(col1.getID())));
        assertThat(collectionUuidString, not(String.valueOf(col2.getID())));

    }

    @Test
    public void itemYOwningCollectionTest() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(get("/api/core/items/" + itemY.getID() + "/owningCollection")).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String collectionUuidString =  String.valueOf(map.get("uuid"));
        String collectionName = String.valueOf(map.get("name"));

        mvcResult = getClient(token).perform(get("/api/core/collections/" + col1.getID())).andReturn();
        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String actualCollectionName = String.valueOf(map.get("name"));

        assertThat(collectionName, equalTo(actualCollectionName));
        assertThat(collectionUuidString, equalTo(String.valueOf(col1.getID())));
        assertThat(collectionUuidString, not(String.valueOf(col2.getID())));

    }

    @Test
    public void itemZOwningCollectionTest() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(get("/api/core/items/" + itemZ.getID() + "/owningCollection")).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String collectionUuidString =  String.valueOf(map.get("uuid"));
        String collectionName = String.valueOf(map.get("name"));

        mvcResult = getClient(token).perform(get("/api/core/collections/" + col2.getID())).andReturn();
        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String actualCollectionName = String.valueOf(map.get("name"));

        assertThat(collectionName, equalTo(actualCollectionName));
        assertThat(collectionUuidString, equalTo(String.valueOf(col2.getID())));
        assertThat(collectionUuidString, not(String.valueOf(col1.getID())));

    }

    @Test
    public void col1ParentCommunityTest() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(get("/api/core/collections/" + col1.getID() + "/parentCommunity")).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String communityUuidString =  String.valueOf(map.get("uuid"));
        String communityName = String.valueOf(map.get("name"));

        mvcResult = getClient(token).perform(get("/api/core/communities/" + communityAA.getID())).andReturn();
        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String actualCommunityName = String.valueOf(map.get("name"));

        assertThat(communityName, equalTo(actualCommunityName));
        assertThat(communityUuidString, equalTo(String.valueOf(communityAA.getID())));
        assertThat(communityUuidString, not(String.valueOf(communityA.getID())));
        assertThat(communityUuidString, not(String.valueOf(communityAB.getID())));

    }

    @Test
    public void col2ParentCommunityTest() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(get("/api/core/collections/" + col2.getID() + "/parentCommunity")).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String communityUuidString =  String.valueOf(map.get("uuid"));
        String communityName = String.valueOf(map.get("name"));

        mvcResult = getClient(token).perform(get("/api/core/communities/" + communityAA.getID())).andReturn();
        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String actualCommunityName = String.valueOf(map.get("name"));

        assertThat(communityName, equalTo(actualCommunityName));
        assertThat(communityUuidString, equalTo(String.valueOf(communityAA.getID())));
        assertThat(communityUuidString, not(String.valueOf(communityA.getID())));
        assertThat(communityUuidString, not(String.valueOf(communityAB.getID())));

    }

    @Test
    public void col3ParentCommunityTest() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(get("/api/core/collections/" + col3.getID() + "/parentCommunity")).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String communityUuidString =  String.valueOf(map.get("uuid"));
        String communityName = String.valueOf(map.get("name"));

        mvcResult = getClient(token).perform(get("/api/core/communities/" + communityAB.getID())).andReturn();
        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String actualCommunityName = String.valueOf(map.get("name"));

        assertThat(communityName, equalTo(actualCommunityName));
        assertThat(communityUuidString, equalTo(String.valueOf(communityAB.getID())));
        assertThat(communityUuidString, not(String.valueOf(communityA.getID())));
        assertThat(communityUuidString, not(String.valueOf(communityAA.getID())));

    }

    @Test
    public void comAAParentCommunityTest() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(get("/api/core/communities/" + communityAA.getID() + "/parentCommunity")).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String communityUuidString =  String.valueOf(map.get("uuid"));
        String communityName = String.valueOf(map.get("name"));

        mvcResult = getClient(token).perform(get("/api/core/communities/" + communityA.getID())).andReturn();
        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String actualCommunityName = String.valueOf(map.get("name"));

        assertThat(communityName, equalTo(actualCommunityName));
        assertThat(communityUuidString, equalTo(String.valueOf(communityA.getID())));
        assertThat(communityUuidString, not(String.valueOf(communityB.getID())));

    }


    @Test
    public void comABParentCommunityTest() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(get("/api/core/communities/" + communityAB.getID() + "/parentCommunity")).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String communityUuidString =  String.valueOf(map.get("uuid"));
        String communityName = String.valueOf(map.get("name"));

        mvcResult = getClient(token).perform(get("/api/core/communities/" + communityA.getID())).andReturn();
        content = mvcResult.getResponse().getContentAsString();
        map = mapper.readValue(content, Map.class);
        String actualCommunityName = String.valueOf(map.get("name"));

        assertThat(communityName, equalTo(actualCommunityName));
        assertThat(communityUuidString, equalTo(String.valueOf(communityA.getID())));
        assertThat(communityUuidString, not(String.valueOf(communityB.getID())));

    }

    @Test
    public void comAParentCommunityTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + communityA.getID() + "/parentCommunity"))
                                                        .andExpect(status().isNoContent());


    }

}
