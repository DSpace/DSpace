/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.service.SystemConfigVariableService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link SystemConfigVariableServiceImpl}.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class SystemConfigVariableServiceImplTest extends AbstractUnitTest {

    private SystemConfigVariableService systemConfigVariableService;
    private AuthorizeService authorizeService;
    private Context context;

    @Before
    public void setUp() {
        systemConfigVariableService = new SystemConfigVariableServiceImpl();
        authorizeService = mock(AuthorizeService.class);
        ReflectionTestUtils.setField(systemConfigVariableService, "authorizeService", authorizeService);
        context = mock(Context.class);
        try {
            when(authorizeService.isAdmin(context)).thenReturn(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetConfigVariables() throws Exception {
        List<SystemConfigVariable> configVars = systemConfigVariableService.getConfigVariables(context);
        assertNotNull(configVars);
        assertFalse(configVars.isEmpty());

        for (SystemConfigVariable var : configVars) {
            assertNotNull(var.getKey());
            assertNotNull(var.getValue());
            assertEquals("${config.get('" + var.getKey() + "')}", var.getPlaceholder());
        }

        // Verify sorted by key
        for (int i = 0; i < configVars.size() - 1; i++) {
            assertTrue(configVars.get(i).getKey().compareTo(configVars.get(i + 1).getKey()) <= 0);
        }
    }

    @Test
    public void testGetConfigVariable() throws Exception {
        SystemConfigVariable dspaceNameVar = systemConfigVariableService.getConfigVariable(context, "dspace.name");
        assertNotNull(dspaceNameVar);
        assertEquals("dspace.name", dspaceNameVar.getKey());
        assertEquals("${config.get('dspace.name')}", dspaceNameVar.getPlaceholder());
    }

    @Test
    public void testGetConfigVariableNull() throws Exception {
        assertNull(systemConfigVariableService.getConfigVariable(context, null));
        assertNull(systemConfigVariableService.getConfigVariable(context, "non.existent.key.xyz"));
    }

    @Test
    public void testNonAdminAccessDenied() throws Exception {
        when(authorizeService.isAdmin(context)).thenReturn(false);
        assertThrows(AuthorizeException.class,
            () -> systemConfigVariableService.getConfigVariables(context));
        assertThrows(AuthorizeException.class,
            () -> systemConfigVariableService.getConfigVariable(context, "dspace.name"));
    }

    @Test
    public void testNullContextAccessDenied() {
        assertThrows(AuthorizeException.class,
            () -> systemConfigVariableService.getConfigVariables(null));
        assertThrows(AuthorizeException.class,
            () -> systemConfigVariableService.getConfigVariable(null, "dspace.name"));
    }
}
