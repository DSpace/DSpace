/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.codec.CharEncoding;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the ItemEventProcessor
 */
public class ItemEventProcessorIT extends AbstractIntegrationTestWithDatabase {


    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private String encodedUrl;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        configurationService.setProperty("irus.statistics.tracker.enabled", true);

        String dspaceUrl = configurationService.getProperty("dspace.ui.url");
        try {
            encodedUrl = URLEncoder.encode(dspaceUrl, CharEncoding.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("Error occurred in setup()", e);
        }

    }

    @Test
    /**
     * Test the method that adds data based on the object types
     */
    public void testAddObectSpecificData() throws UnsupportedEncodingException {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        String encodedHandle = URLEncoder.encode(item.getHandle(), CharEncoding.UTF_8);

        ItemEventProcessor itemEventProcessor = new ItemEventProcessor(context, null, item);
        String result = itemEventProcessor.addObjectSpecificData("existing-string", item);

        assertThat(result,
                   is("existing-string&svc_dat=" + encodedUrl + "%2Fhandle%2F" + encodedHandle +
                              "&rft_dat=Investigation"));

    }


}
