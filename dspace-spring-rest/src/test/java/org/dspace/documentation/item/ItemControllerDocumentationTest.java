/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.documentation.item;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Test;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.relaxedLinks;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Documentation test for the {@link org.dspace.app.rest.repository.ItemRestRepository}
 */
public class ItemControllerDocumentationTest extends AbstractControllerIntegrationTest {

    protected String getRestCategory() {
        return RestModel.CORE;
    }


    @Test
    public void findOneItemDocumentation() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = new CollectionBuilder().createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = new CollectionBuilder().createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();



        //When we call the root endpoint
        getClient().perform(get("/api/core/items/" + publicItem1.getID()))
                //The status has to be 200 OK
                .andExpect(status().isOk())

                .andDo(document("item",

                        relaxedLinks(
                                linkWithRel("core:bitstreams").description("Link to the <<bitstreams.adoc#bitstream-list, bitstreams>> of this item"),
                                linkWithRel("core:owningCollection").description("Link to the <<owningCollectionOf, owning collection of>> this item"),
                                linkWithRel("core:templateItemOf").description("Link to the <<templateItemOf, template item of>> this item"),
                                linkWithRel("self").description("Link to <<items.adoc, this>> page"),
                                linkWithRel("curies").description("Curies for documentation")
                        ),

                        relaxedResponseFields(
                                fieldWithPath("_links").description("The links that are displayed on this page namely bitstreams, owningCollection, templateItemOf, self and curies"),
                                fieldWithPath("uuid").description("The UUID of the selected item"),
                                fieldWithPath("name").description("The name of the selected item"),
                                fieldWithPath("handle").description("The handle of the selected item"),
                                fieldWithPath("metadata").description("The metadata of the selected item"),
                                fieldWithPath("inArchive").description("Indication of whether the item is in the archive or not"),
                                fieldWithPath("discoverable").description("Indication of whether the item is discoverable or not"),
                                fieldWithPath("withdrawn").description("Indication of whether the item is withdrawn or not"),
                                fieldWithPath("lastModified").description("The time of the last modification of the selected item"),
                                fieldWithPath("type").description("The type namely item"))
                ))
        ;
    }

    @Test
    public void findAllItemsDocumentation() throws Exception {
        context.turnOffAuthorisationSystem();

//** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = new CollectionBuilder().createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = new CollectionBuilder().createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();




        //When we call the root endpoint
        getClient().perform(get("/api/core/items")
                .param("size", "2"))
                //The status has to be 200 OK
                .andExpect(status().isOk())

                .andDo(document("items",

                        relaxedLinks(
                                linkWithRel("first").description("Link to the first page of items"),
                                linkWithRel("self").description("Link to <<items.adoc#item-list, this>> page"),
                                linkWithRel("next").description("Link to the next page"),
                                linkWithRel("last").description("Link to the last page"),
                                linkWithRel("curies").description("Curies for documentation")
                        ),

                        relaxedResponseFields(
                                fieldWithPath("_embedded").description("The items that are in the repository, displayed with pagination"),
                                fieldWithPath("_links").description("The links that are displayed on this page, mostly with regards to pagination if applicable"),
                                fieldWithPath("page").description("A page object that contains information about the amount of items shown and in total"))
                ))
        ;
    }

}
