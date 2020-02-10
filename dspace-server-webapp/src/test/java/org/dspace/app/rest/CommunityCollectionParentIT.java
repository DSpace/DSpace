package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Map;

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
                                                .addParentCommunity(context, communityB)
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
    public void itemOwningCollectionTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(get("/api/core/items/" + itemX + "/owningCollection")).andReturn();

        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        String uuidString =  String.valueOf(map.get("uuid"));
    }
}
