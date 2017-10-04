package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.RootRest;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Created by raf on 26/09/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class RootConverterTest {

    @InjectMocks
    private RootConverter rootConverter;

    @Mock
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception{
        when(configurationService.getProperty("dspace.url")).thenReturn("dspaceurl");
        when(configurationService.getProperty("dspace.name")).thenReturn("dspacename");
    }

    @Test
    public void testReturnCorrectClass() throws Exception{
        assertEquals(rootConverter.convert().getClass(), RootRest.class);
    }

    @Test
    public void testCorrectPropertiesSetFromConfigurationService() throws Exception{
        RootRest rootRest = rootConverter.convert();
        assertEquals("dspaceurl", rootRest.getDspaceURL());
        assertEquals("dspacename", rootRest.getDspaceName());
    }

    @Test
    public void testReturnNotNull() throws Exception{
        assertNotNull(rootConverter.convert());
    }
}
