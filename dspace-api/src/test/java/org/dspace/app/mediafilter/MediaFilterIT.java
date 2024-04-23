/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of {@link MediaFilterScript}.
 *
 * @author Andrea Bollini <andrea.bollini at 4science.com>
 */
public class MediaFilterIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected Community topComm1;
    protected Community topComm2;
    protected Community childComm1_1;
    protected Community childComm1_2;
    protected Collection col1_1;
    protected Collection col1_2;
    protected Collection col1_1_1;
    protected Collection col1_1_2;
    protected Collection col1_2_1;
    protected Collection col1_2_2;
    protected Collection col2_1;
    protected Item item1_1_a;
    protected Item item1_1_b;
    protected Item item1_2_a;
    protected Item item1_2_b;
    protected Item item1_1_1_a;
    protected Item item1_1_1_b;
    protected Item item1_1_2_a;
    protected Item item1_1_2_b;
    protected Item item1_2_1_a;
    protected Item item1_2_1_b;
    protected Item item1_2_2_a;
    protected Item item1_2_2_b;
    protected Item item2_1_a;
    protected Item item2_1_b;

    @Before
    public void setup() throws IOException, SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        topComm1 = CommunityBuilder.createCommunity(context).withName("Parent Community1").build();
        topComm2 = CommunityBuilder.createCommunity(context).withName("Parent Community2").build();
        childComm1_1 = CommunityBuilder.createCommunity(context).withName("Child Community1_1")
                .addParentCommunity(context, topComm1).build();
        childComm1_2 = CommunityBuilder.createCommunity(context).withName("Child Community1_2")
                .addParentCommunity(context, topComm1).build();
        col1_1 = CollectionBuilder.createCollection(context, topComm1).withName("Collection 1_1").build();
        col1_2 = CollectionBuilder.createCollection(context, topComm1).withName("Collection 1_2").build();
        col1_1_1 = CollectionBuilder.createCollection(context, childComm1_1).withName("Collection 1_1_1").build();
        col1_1_2 = CollectionBuilder.createCollection(context, childComm1_1).withName("Collection 1_1_2").build();
        col1_2_1 = CollectionBuilder.createCollection(context, childComm1_2).withName("Collection 1_1_1").build();
        col1_2_2 = CollectionBuilder.createCollection(context, childComm1_2).withName("Collection 1_2").build();
        col2_1 = CollectionBuilder.createCollection(context, topComm2).withName("Collection 2_1").build();

        // Create two items in each collection, one with the test.csv file and one with the test.txt file
        item1_1_a = ItemBuilder.createItem(context, col1_1).withTitle("Item 1_1_a").withIssueDate("2017-10-17").build();
        item1_1_b = ItemBuilder.createItem(context, col1_1).withTitle("Item 1_1_b").withIssueDate("2017-10-17").build();
        item1_1_1_a = ItemBuilder.createItem(context, col1_1_1).withTitle("Item 1_1_1_a").withIssueDate("2017-10-17")
                .build();
        item1_1_1_b = ItemBuilder.createItem(context, col1_1_1).withTitle("Item 1_1_1_b").withIssueDate("2017-10-17")
                .build();
        item1_1_2_a = ItemBuilder.createItem(context, col1_1_2).withTitle("Item 1_1_2_a").withIssueDate("2017-10-17")
                .build();
        item1_1_2_b = ItemBuilder.createItem(context, col1_1_2).withTitle("Item 1_1_2_b").withIssueDate("2017-10-17")
                .build();
        item1_2_a = ItemBuilder.createItem(context, col1_2).withTitle("Item 1_2_a").withIssueDate("2017-10-17").build();
        item1_2_b = ItemBuilder.createItem(context, col1_2).withTitle("Item 1_2_b").withIssueDate("2017-10-17").build();
        item1_2_1_a = ItemBuilder.createItem(context, col1_2_1).withTitle("Item 1_2_1_a").withIssueDate("2017-10-17")
                .build();
        item1_2_1_b = ItemBuilder.createItem(context, col1_2_1).withTitle("Item 1_2_1_b").withIssueDate("2017-10-17")
                .build();
        item1_2_2_a = ItemBuilder.createItem(context, col1_2_2).withTitle("Item 1_2_2_a").withIssueDate("2017-10-17")
                .build();
        item1_2_2_b = ItemBuilder.createItem(context, col1_2_2).withTitle("Item 1_2_2_b").withIssueDate("2017-10-17")
                .build();
        item2_1_a = ItemBuilder.createItem(context, col2_1).withTitle("Item 2_1_a").withIssueDate("2017-10-17").build();
        item2_1_b = ItemBuilder.createItem(context, col2_1).withTitle("Item 2_1_b").withIssueDate("2017-10-17").build();
        addBitstream(item1_1_a, "test.csv");
        addBitstream(item1_1_b, "test.txt");
        addBitstream(item1_2_a, "test.csv");
        addBitstream(item1_2_b, "test.txt");
        addBitstream(item1_1_1_a, "test.csv");
        addBitstream(item1_1_1_b, "test.txt");
        addBitstream(item1_1_2_a, "test.csv");
        addBitstream(item1_1_2_b, "test.txt");
        addBitstream(item1_2_1_a, "test.csv");
        addBitstream(item1_2_1_b, "test.txt");
        addBitstream(item1_2_2_a, "test.csv");
        addBitstream(item1_2_2_b, "test.txt");
        addBitstream(item2_1_a, "test.csv");
        addBitstream(item2_1_b, "test.txt");
        context.restoreAuthSystemState();
    }

    private void addBitstream(Item item, String filename) throws SQLException, AuthorizeException, IOException {
        BitstreamBuilder.createBitstream(context, item, getClass().getResourceAsStream(filename)).withName(filename)
                .guessFormat().build();
    }

    @Test
    public void mediaFilterScriptAllItemsTest() throws Exception {
        performMediaFilterScript(null);
        Iterator<Item> items = itemService.findAll(context);
        while (items.hasNext()) {
            Item item = items.next();
            checkItemHasBeenProcessed(item);
        }
    }

    @Test
    public void mediaFilterScriptIdentifiersTest() throws Exception {
        // process the item 1_1_a and verify that no other items has been processed using the "closer" one
        performMediaFilterScript(item1_1_a);
        checkItemHasBeenProcessed(item1_1_a);
        checkItemHasBeenNotProcessed(item1_1_b);
        // process the collection 1_1_1 and verify that items in another collection has not been processed
        performMediaFilterScript(col1_1_1);
        checkItemHasBeenProcessed(item1_1_1_a);
        checkItemHasBeenProcessed(item1_1_1_b);
        checkItemHasBeenNotProcessed(item1_1_2_a);
        checkItemHasBeenNotProcessed(item1_1_2_b);
        // process a top community with only collections
        performMediaFilterScript(topComm2);
        checkItemHasBeenProcessed(item2_1_a);
        checkItemHasBeenProcessed(item2_1_b);
        // verify that the other items have not been processed yet
        checkItemHasBeenNotProcessed(item1_1_b);
        checkItemHasBeenNotProcessed(item1_2_a);
        checkItemHasBeenNotProcessed(item1_2_b);
        checkItemHasBeenNotProcessed(item1_1_2_a);
        checkItemHasBeenNotProcessed(item1_1_2_b);
        checkItemHasBeenNotProcessed(item1_2_1_a);
        checkItemHasBeenNotProcessed(item1_2_1_b);
        checkItemHasBeenNotProcessed(item1_2_2_a);
        checkItemHasBeenNotProcessed(item1_2_2_b);
        // process a more structured community and verify that all the items at all levels are processed
        performMediaFilterScript(topComm1);
        // items that were already processed should stay processed
        checkItemHasBeenProcessed(item1_1_a);
        checkItemHasBeenProcessed(item1_1_1_a);
        checkItemHasBeenProcessed(item1_1_1_b);
        // residual items should have been processed as well now
        checkItemHasBeenProcessed(item1_1_b);
        checkItemHasBeenProcessed(item1_2_a);
        checkItemHasBeenProcessed(item1_2_b);
        checkItemHasBeenProcessed(item1_1_2_a);
        checkItemHasBeenProcessed(item1_1_2_b);
        checkItemHasBeenProcessed(item1_2_1_a);
        checkItemHasBeenProcessed(item1_2_1_b);
        checkItemHasBeenProcessed(item1_2_2_a);
        checkItemHasBeenProcessed(item1_2_2_b);
    }

    private void checkItemHasBeenNotProcessed(Item item) throws IOException, SQLException, AuthorizeException {
        List<Bundle> textBundles = item.getBundles("TEXT");
        assertTrue("The item " + item.getName() + " should NOT have the TEXT bundle", textBundles.size() == 0);
    }

    private void checkItemHasBeenProcessed(Item item) throws IOException, SQLException, AuthorizeException {
        String expectedFileName = StringUtils.endsWith(item.getName(), "_a") ? "test.csv.txt" : "test.txt.txt";
        String expectedContent = StringUtils.endsWith(item.getName(), "_a") ? "data3,3" : "quick brown fox";
        List<Bundle> textBundles = item.getBundles("TEXT");
        assertTrue("The item " + item.getName() + " has NOT the TEXT bundle", textBundles.size() == 1);
        List<Bitstream> bitstreams = textBundles.get(0).getBitstreams();
        assertTrue("The item " + item.getName() + " has NOT exactly 1 bitstream in the TEXT bundle",
                bitstreams.size() == 1);
        assertTrue("The text bistream in the " + item.getName() + " is NOT named properly [" + expectedFileName + "]",
                StringUtils.equals(bitstreams.get(0).getName(), expectedFileName));
        assertTrue("The text bistream in the " + item.getName() + " doesn't contain the proper content ["
                + expectedContent + "]", StringUtils.contains(getContent(bitstreams.get(0)), expectedContent));
    }

    private CharSequence getContent(Bitstream bitstream) throws IOException, SQLException, AuthorizeException {
        try (InputStream input = bitstreamService.retrieve(context, bitstream)) {
            return IOUtils.toString(input, "UTF-8");
        }
    }

    private void performMediaFilterScript(DSpaceObject dso) throws Exception {
        if (dso != null) {
            runDSpaceScript("filter-media", "-i", dso.getHandle());
        } else {
            runDSpaceScript("filter-media");
        }
        // reload our items to see the changes
        item1_1_a = context.reloadEntity(item1_1_a);
        item1_1_b = context.reloadEntity(item1_1_b);
        item1_2_a = context.reloadEntity(item1_2_a);
        item1_2_b = context.reloadEntity(item1_2_b);
        item1_1_1_a = context.reloadEntity(item1_1_1_a);
        item1_1_1_b = context.reloadEntity(item1_1_1_b);
        item1_1_2_a = context.reloadEntity(item1_1_2_a);
        item1_1_2_b = context.reloadEntity(item1_1_2_b);
        item1_2_1_a = context.reloadEntity(item1_2_1_a);
        item1_2_1_b = context.reloadEntity(item1_2_1_b);
        item1_2_2_a = context.reloadEntity(item1_2_2_a);
        item1_2_2_b = context.reloadEntity(item1_2_2_b);
        item2_1_a = context.reloadEntity(item2_1_a);
        item2_1_b = context.reloadEntity(item2_1_b);

    }
}
