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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;

/**
 * This class verifies that {@link EPersonRestPermissionEvaluatorPlugin} properly
 * evaluates Patch requests.
 */
public class EPersonRestPermissionEvaluatorPluginTest {

    private EPersonRestPermissionEvaluatorPlugin ePersonRestPermissionEvaluatorPlugin;

    private Authentication authentication;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        ePersonRestPermissionEvaluatorPlugin = spy(EPersonRestPermissionEvaluatorPlugin.class);
        authentication = mock(Authentication.class);
        DSpaceRestPermission restPermission = DSpaceRestPermission.convert("WRITE");
        when(ePersonRestPermissionEvaluatorPlugin
                .hasDSpacePermission(authentication, null, null, restPermission)).thenReturn(true);
    }

    @Test
    public void testHasPatchPermissionAuthFails() throws Exception {

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation passwordOperation = new ReplaceOperation("/password", "testpass");
        ops.add(passwordOperation);
        ReplaceOperation canLoginOperation = new ReplaceOperation("/canLogin", false);
        ops.add(canLoginOperation);
        JsonNode jsonNode = objectMapper.valueToTree(ops);
        assertFalse(ePersonRestPermissionEvaluatorPlugin
                .hasPatchPermission(authentication, null, null, jsonNode));

    }

    @Test
    public void testHasPatchPermissionAuthOk() throws Exception {

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation passwordOperation = new ReplaceOperation("/password", "testpass");
        ops.add(passwordOperation);
        JsonNode jsonNode = objectMapper.valueToTree(ops);
        assertTrue(ePersonRestPermissionEvaluatorPlugin
                .hasPatchPermission(authentication, null, null, jsonNode));

    }

}
