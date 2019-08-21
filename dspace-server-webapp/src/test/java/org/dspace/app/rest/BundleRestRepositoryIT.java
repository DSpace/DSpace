/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.BundleBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.BundleMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class BundleRestRepositoryIT extends AbstractControllerIntegrationTest {

    private Collection collection;
    private Item item;
    private Bundle bundle1;
    private Bundle bundle2;
    private Bitstream bitstream1;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        System.out.println("test");
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        item = ItemBuilder.createItem(context, collection)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald").withAuthor("Doe, John")
            .withSubject("ExtraEntry")
            .build();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                .withName("Bitstream")
                .withMimeType("text/plain")
                .build();
        }

        bundle1 = BundleBuilder.createBundle(context, item)
            .withName("testname")
            .withBitstream(bitstream1)
            .build();
        bundle2 = BundleBuilder.createBundle(context, item).withName("test2").build();

        context.restoreAuthSystemState();
    }

    @Test
    public void GetSingleBundle() throws Exception {
        getClient().perform(get("/api/core/bundles/" + bundle1.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", BundleMatcher.matchBundle(bundle1)))
            .andExpect(jsonPath("$._embedded.bitstreams._embedded.bitstreams", containsInAnyOrder(
                BitstreamMatcher.matchBitstreamEntry(bitstream1.getID(), bitstream1.getSizeBytes())))
            )
        ;
    }


    @Test
    public void getItemBundles() throws Exception {
        getClient().perform(get("/api/core/items/" + item.getID() + "/bundles"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.bundles", Matchers.hasItems(
                    BundleMatcher.matchBundle(bundle1),
                    BundleMatcher.matchBundle(bundle2)
                )))
            .andExpect(jsonPath("$._links.self.href", endsWith("/api/core/items/" + item.getID() + "/bundles")))
            ;
    }




}
