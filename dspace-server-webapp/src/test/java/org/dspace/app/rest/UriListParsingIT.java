/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * This test class will test the UriList parsing method in the Utils class
 */
public class UriListParsingIT extends AbstractControllerIntegrationTest {

    @Autowired
    protected Utils utils;

    @Test
    public void mockRequestTextUriParsingTest() throws Exception {

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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();


        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        String uriListString =  "https://localhost:8080/server/api/core/items/" + publicItem1.getID() + "\n" +
            "https://localhost:8080/server/api/core/items/" + publicItem2.getID();
        mockRequest.setContentType("text/uri-list");
        mockRequest.setContent(uriListString.getBytes());
        List<DSpaceObject> dSpaceObjectList = utils.constructDSpaceObjectList(
            ContextUtil.obtainContext(mockRequest), utils.getStringListFromRequest(mockRequest));
        assertThat("DSpaceObject List is of size 2" ,dSpaceObjectList.size(), equalTo(2));
        assertThat("DSpaceObject 1 is an item", dSpaceObjectList.get(0).getType(), equalTo(Constants.ITEM));
        assertThat("DSpaceObject 2 is an item", dSpaceObjectList.get(1).getType(), equalTo(Constants.ITEM));
        assertTrue(dSpaceObjectList.get(0).equals(publicItem1));
        assertTrue(dSpaceObjectList.get(1).equals(publicItem2));


    }
}
