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
    private Bitstream bitstream = mock(Bitstream.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();


    @InjectMocks
    BitstreamEventProcessor bitstreamEventProcessor = mock(BitstreamEventProcessor.class, CALLS_REAL_METHODS);

    private String encodedUrl;


    @Before
    public void setUp() {
        configurationService.setProperty("stats.tracker.enabled", true);

        String dspaceUrl = configurationService.getProperty("dspace.server.url");
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

        when(bitstream.getID()).thenReturn(UUID.fromString("455bd3cf-31d3-40db-b283-4106c47fc025"));


        String result = bitstreamEventProcessor.addObjectSpecificData("existing-string", bitstream);

        assertThat(result,
                   is("existing-string&svc_dat=" + encodedUrl + "%2Fapi%2Fcore%2Fbitstreams%2F455bd3cf-31d3-40db" +
                              "-b283-4106c47fc025%2Fcontent&rft_dat=Request"));

    }

}
