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
import java.util.UUID;

import org.apache.commons.codec.CharEncoding;
import org.dspace.AbstractDSpaceTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * Test class for the BitstreamEventProcessor
 */
public class BitstreamEventProcessorTest extends AbstractDSpaceTest {

    @Mock
    private Item item = mock(Item.class);

    @Mock
    private Bitstream bitstream = mock(Bitstream.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();


    @InjectMocks
    BitstreamEventProcessor bitstreamEventProcessor = mock(BitstreamEventProcessor.class, CALLS_REAL_METHODS);

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
        bitstreamEventProcessor.configurationService = configurationService;

        when(item.getHandle()).thenReturn("123456789/1");

        String result = bitstreamEventProcessor.addObjectSpecificData("existing-string", item, bitstream);

        assertThat(result,
                   is("existing-string&svc_dat=" + encodedUrl + "%2Fbitstream%2Fhandle%2F123456789%2F1%2F%3Fsequence" +
                              "%3D0" +
                              "&rft_dat=Request"));

    }

    @Test
    /**
     * Test the method that adds data based on the object types when no handle can be found for the item
     */
    public void testAddObectSpecificDataWhenNoHandle() throws UnsupportedEncodingException {
        bitstreamEventProcessor.configurationService = configurationService;

        when(item.getHandle()).thenReturn(null);
        when(item.getID()).thenReturn(UUID.fromString("d84c8fa8-50e2-4267-98f4-00954ea89c94"));

        String result = bitstreamEventProcessor.addObjectSpecificData("existing-string", item, bitstream);

        assertThat(result,
                   is("existing-string&svc_dat=" + encodedUrl + "%2Fbitstream%2Fitem%2Fd84c8fa8-50e2-4267-98f4" +
                              "-00954ea89c94%2F%3Fsequence%3D0" +
                              "&rft_dat=Request"));

    }

    @Test
    /**
     * Test the method that adds data based on the object types when no item is present
     */
    public void testAddObectSpecificDataWhenNoItem() throws UnsupportedEncodingException {
        bitstreamEventProcessor.configurationService = configurationService;

        when(bitstream.getID()).thenReturn(UUID.fromString("1a0c4e40-969d-467b-8f01-9b7edab8fd1a"));
        String result = bitstreamEventProcessor.addObjectSpecificData("existing-string", null, bitstream);

        assertThat(result, is("existing-string&svc_dat=" + encodedUrl + "%2Fbitstream%2Fid%2F" +
                                      "1a0c4e40-969d-467b-8f01-9b7edab8fd1a%2F%3Fsequence%3D0&rft_dat=Request"));

    }


}
