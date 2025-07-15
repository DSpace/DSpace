/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deletionprocess;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Test;

/**
 * This class handles ITs for { @link DSpaceObjectDeletionProcess }.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class DSpaceObjectDeletionProcessIT extends AbstractControllerIntegrationTest {

    private Item item1;
    private Item item2;
    private Community community;
    private Collection collection;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context)
                                    .withName("My community")
                                    .build();
        collection = CollectionBuilder.createCollection(context, community)
                                      .withName("Publication collection")
                                      .withEntityType("Publication")
                                      .build();

        item1 = ItemBuilder.createItem(context, collection)
                           .withTitle("Publication item TEST 1")
                           .withType("TEST")
                           .build();

        item2 = ItemBuilder.createItem(context, collection)
                           .withTitle("Publication title test")
                           .withAuthor("Misha, Boychuk")
                           .withType("website_content")
                           .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void asyncDetetionOfItemTest() throws Exception {

    }

    @Test
    public void asyncDetetionOfCollectionTest() throws Exception {

    }

    @Test
    public void asyncDetetionOfCommunityTest() throws Exception {

    }

}
