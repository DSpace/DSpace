/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.AbstractUnitTest;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for ShibAuthentication
 */
public class ShibAuthenticationTest extends AbstractUnitTest {

    private ShibAuthentication shibAuthentication;
    private EPersonService ePersonService;
    private ConfigurationService configurationService;

    @Before
    public void setup() {
        shibAuthentication = new ShibAuthentication();
        ePersonService = mock(EPersonService.class);
        shibAuthentication.ePersonService = ePersonService;
        configurationService = mock(ConfigurationService.class);
        shibAuthentication.configurationService = configurationService;
        when(configurationService.getProperty("authentication-shibboleth.netid-header")).thenReturn("SHIB-NETID");
        when(configurationService.getProperty("authentication-shibboleth.email-header")).thenReturn("SHIB-MAIL");
        when(configurationService.getArrayProperty("authentication-shibboleth.eperson.metadata"))
            .thenReturn(new String[]{"SHIB-telephone => eperson.phone"});
        when(configurationService.getBooleanProperty("authentication-shibboleth.eperson.metadata.autocreate", true))
            .thenReturn(true);
        MetadataFieldService metadataFieldService = mock(MetadataFieldService.class);
        shibAuthentication.metadataFieldService = metadataFieldService;

        try {
            when(metadataFieldService.findByElement(any(Context.class), any(String.class), any(String.class), any()))
                .thenReturn(mock(MetadataField.class));
        } catch (Exception e) {
            // ignore checked exceptions from mock
        }
    }

    @Test
    public void testPhoneMetadataUpdateOrder() throws Exception {
        Context context = mock(Context.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        EPerson eperson = mock(EPerson.class);
        when(request.getAttribute("SHIB-NETID")).thenReturn("test-user");
        when(request.getAttribute("SHIB-MAIL")).thenReturn("test@example.com");
        String phoneValue = "555-1234";
        when(request.getAttribute("SHIB-telephone")).thenReturn(phoneValue);
        shibAuthentication.initialize(context);
        assertNotNull("metadataHeaderMap should be initialized", shibAuthentication.metadataHeaderMap);
        assertTrue("metadataHeaderMap should contain SHIB-telephone", shibAuthentication.metadataHeaderMap
            .containsKey("SHIB-telephone"));
        shibAuthentication.updateEPerson(context, request, eperson);
        ArgumentCaptor<String> languageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(ePersonService, times(1)).setMetadataSingleValue(
            any(Context.class),
            eq(eperson),
            eq("eperson"),
            eq("phone"),
            isNull(),
            languageCaptor.capture(),
            valueCaptor.capture()
        );

        assertNull("The language argument should be NULL.", languageCaptor.getValue());
        assertEquals("The value argument should be the phone number.", phoneValue, valueCaptor.getValue());
    }

    @Test
    public void testInitializeLoadsMultipleMappings() throws Exception {
        Context context = mock(Context.class);
        when(configurationService.getArrayProperty("authentication-shibboleth.eperson.metadata"))
            .thenReturn(new String[]{
                "SHIB-telephone => eperson.phone",
                "SHIB-dept => eperson.department"
            });
        shibAuthentication.initialize(context);

        assertNotNull("metadataHeaderMap should be initialized", shibAuthentication.metadataHeaderMap);
        assertTrue("metadataHeaderMap should contain SHIB-telephone", shibAuthentication.metadataHeaderMap
            .containsKey("SHIB-telephone"));
        assertTrue("metadataHeaderMap should contain SHIB-dept", shibAuthentication.metadataHeaderMap
            .containsKey("SHIB-dept"));
    }

    @Test
    public void testNoMetadataMappingNoUpdate() throws Exception {
        Context context = mock(Context.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        EPerson eperson = mock(EPerson.class);
        when(configurationService.getArrayProperty("authentication-shibboleth.eperson.metadata"))
            .thenReturn(new String[0]);
        shibAuthentication.initialize(context);
        shibAuthentication.updateEPerson(context, request, eperson);

        verify(ePersonService, times(0)).setMetadataSingleValue(
            any(Context.class),
            any(EPerson.class),
            anyString(),
            anyString(),
            any(),
            any(),
            any()
        );
    }
}
