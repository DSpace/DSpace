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
import org.dspace.content.service.CollectionService;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.relaxedLinks;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Documentation test for the Owning Collection link in items
 */
public class OwningCollectionControllerDocumentation extends AbstractControllerIntegrationTest {

    protected String getRestCategory() {
        return RestModel.CORE;
    }


    @Test
    public void findOneItemOwningCollection() throws Exception {
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



        //When we call the root endpoint
        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/owningCollection"))
                //The status has to be 200 OK
                .andExpect(status().isOk())

                .andDo(document("owningcollection",

                        relaxedLinks(
                                linkWithRel("c:logo").description("Link to the logo of this collection"),
                                linkWithRel("self").description("Link to <<owningCollection.adoc#collection,this>> page"),
                                linkWithRel("curies").description("Curies for documentation")
                        ),

                        relaxedResponseFields(
                                fieldWithPath("_links").description("The links that are displayed on this page namely logo, self and curies"),
                                fieldWithPath("uuid").description("The UUID of the selected collection"),
                                fieldWithPath("name").description("The name of the selected collection"),
                                fieldWithPath("handle").description("The handle of the selected collection"),
                                fieldWithPath("metadata").description("The metadata of the selected collection"),
                                fieldWithPath("type").description("The type namely collection"))
                ))
        ;
    }

}
