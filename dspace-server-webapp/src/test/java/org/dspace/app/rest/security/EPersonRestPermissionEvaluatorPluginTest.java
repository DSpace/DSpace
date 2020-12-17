/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.services.RequestService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * This class verifies that {@link EPersonRestPermissionEvaluatorPlugin} properly
 * evaluates Patch requests.
 */
@RunWith(MockitoJUnitRunner.class)
public class EPersonRestPermissionEvaluatorPluginTest {

    @InjectMocks
    private EPersonRestPermissionEvaluatorPlugin ePersonRestPermissionEvaluatorPlugin;

    private Authentication authentication;

    @Mock
    private RequestService requestService;

    @Before
    public void setUp() throws Exception {
        ePersonRestPermissionEvaluatorPlugin = spy(EPersonRestPermissionEvaluatorPlugin.class);
        authentication = mock(Authentication.class);
        DSpaceRestPermission restPermission = DSpaceRestPermission.convert("WRITE");
        when(ePersonRestPermissionEvaluatorPlugin
                 .hasDSpacePermission(authentication, null, null, restPermission)).thenReturn(true);
        ReflectionTestUtils.setField(ePersonRestPermissionEvaluatorPlugin, "requestService", requestService);
        when(requestService.getCurrentRequest()).thenReturn(null);
    }

    @Test
    public void testHasPatchPermissionAuthFails() throws Exception {

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation addOperation = new AddOperation("/password", "testpass");
        ops.add(addOperation);
        ReplaceOperation canLoginOperation = new ReplaceOperation("/canLogin", false);
        ops.add(canLoginOperation);
        Patch patch = new Patch(ops);
        assertFalse(ePersonRestPermissionEvaluatorPlugin
                        .hasPatchPermission(authentication, null, null, patch));

    }

    @Test
    public void testHasPatchPermissionAuthOk() throws Exception {

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation addOperation = new AddOperation("/password", "testpass");
        ops.add(addOperation);
        Patch patch = new Patch(ops);
        assertTrue(ePersonRestPermissionEvaluatorPlugin
                       .hasPatchPermission(authentication, null, null, patch));

    }

}
