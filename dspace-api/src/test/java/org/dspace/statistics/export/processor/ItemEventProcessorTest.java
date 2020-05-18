/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.processor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.codec.CharEncoding;
import org.dspace.AbstractDSpaceTest;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * Test class for the ItemEventProcessor
 */
public class ItemEventProcessorTest extends AbstractDSpaceTest {

    @Mock
    private Item item = mock(Item.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @InjectMocks
    ItemEventProcessor itemEventProcessor = mock(ItemEventProcessor.class, CALLS_REAL_METHODS);

    private String encodedUrl;

    @Before
    public void setUp() {
        configurationService.setProperty("stats.tracker.enabled", true);

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
        itemEventProcessor.configurationService = configurationService;

        when(item.getHandle()).thenReturn("123456789/1");

        String result = itemEventProcessor.addObjectSpecificData("existing-string", item);

        assertThat(result,
                   is("existing-string&svc_dat=" + encodedUrl + "%2Fhandle%2F123456789%2F1&rft_dat=Investigation"));

    }


}
