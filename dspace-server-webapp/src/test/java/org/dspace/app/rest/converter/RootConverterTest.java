/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.dspace.app.rest.model.RootRest;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This class' purpose is to test the RootConvertor class.
 */
@RunWith(MockitoJUnitRunner.class)
public class RootConverterTest {

    @InjectMocks
    private RootConverter rootConverter;

    @Mock
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        when(configurationService.getProperty("dspace.ui.url")).thenReturn("dspaceurl");
        when(configurationService.getProperty("dspace.name")).thenReturn("dspacename");
        when(configurationService.getProperty("dspace.server.url")).thenReturn("rest");
    }

    @Test
    public void testReturnCorrectClass() throws Exception {
        assertEquals(rootConverter.convert().getClass(), RootRest.class);
    }

    @Test
    public void testCorrectPropertiesSetFromConfigurationService() throws Exception {
        String restUrl = "rest";
        RootRest rootRest = rootConverter.convert();
        assertEquals("dspaceurl", rootRest.getDspaceURL());
        assertEquals("dspacename", rootRest.getDspaceName());
        assertEquals(restUrl, rootRest.getDspaceRest());
    }

    @Test
    public void testReturnNotNull() throws Exception {
        assertNotNull(rootConverter.convert());
    }
}
