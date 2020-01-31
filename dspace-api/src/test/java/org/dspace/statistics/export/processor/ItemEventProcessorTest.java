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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

import org.dspace.AbstractDSpaceTest;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * Test class for the ItemEventProcessor
 */
public class ItemEventProcessorTest extends AbstractDSpaceTest {

    @Mock
    private Item item = mock(Item.class);
    @Mock
    private ConfigurationService configurationService = mock(ConfigurationService.class);

    @InjectMocks
    ItemEventProcessor itemEventProcessor = mock(ItemEventProcessor.class, CALLS_REAL_METHODS);

    @Test
    /**
     * Test the method that adds data based on the object types
     */
    public void testAddObectSpecificData() throws UnsupportedEncodingException {
        itemEventProcessor.configurationService = configurationService;
        when(configurationService.getProperty(any(String.class))).thenReturn("demo.dspace.org");

        when(item.getHandle()).thenReturn("123456789/1");

        String result = itemEventProcessor.addObjectSpecificData("existing-string", item);

        assertThat(result,
                   is("existing-string&svc_dat=demo.dspace.org%2Fhandle%2F123456789%2F1&rft_dat=Investigation"));

    }


}
