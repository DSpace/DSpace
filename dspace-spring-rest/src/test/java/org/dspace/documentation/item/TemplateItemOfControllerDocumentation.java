/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.documentation.item;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.relaxedLinks;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractDocumentationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;


/**
 * Documentation test for the Template Item Of link in items
 */
public class TemplateItemOfControllerDocumentation extends AbstractDocumentationTest {

    protected String getRestCategory() {
        return RestModel.CORE;
    }


    @Test
    public void findOneItemTemplateItemOf() throws Exception {
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

        CollectionService collectionService = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("contentServiceFactory", ContentServiceFactory.class).getCollectionService();
        collectionService.createTemplateItem(context, col1);

        Item templateItem = col1.getTemplateItem();
        //When we call the root endpoint
        getClient(this.getRestDocumentation()).perform(get("/api/core/items/" + templateItem.getID() + "/templateItemOf"))
                //The status has to be 200 OK
                .andExpect(status().isOk())

                .andDo(document("templateitemof",

                        relaxedLinks(
                                linkWithRel("c:logo").description("Link to the logo of this collection"),
                                linkWithRel("self").description("Link to <<templateItemOf.adoc#collection,this>> page"),
                                linkWithRel("curies").description("Curies for documentation")
                        ),

                        relaxedResponseFields(
                                fieldWithPath("_links").description("The links that are displayed on this page namely logo, self and curies"),
                                fieldWithPath("uuid").description("The UUID of the collection that this is a template of"),
                                fieldWithPath("name").description("The name of the collection that this is a template of"),
                                fieldWithPath("handle").description("The handle of the collection that this is a template of"),
                                fieldWithPath("metadata").description("The metadata of the collection that this is a template of"),
                                fieldWithPath("type").description("The type namely collection"))
                ))
        ;
    }

}
