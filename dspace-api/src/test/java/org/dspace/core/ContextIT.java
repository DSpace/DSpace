/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CommunityBuilder;
import org.junit.Test;

public class ContextIT extends AbstractIntegrationTestWithDatabase {

    AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    @Test
    public void testGetPoliciesNewCommunityAfterReadOnlyModeChange() throws Exception {

        context.turnOffAuthorisationSystem();

        // First disable the index consumer. The indexing process calls the authorizeService
        // function used in this test and may affect the test
        context.setDispatcher("noindex");

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        context.restoreAuthSystemState();

        context.setMode(Context.Mode.READ_ONLY);

        List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, parentCommunity,
            Constants.READ);

        assertEquals("Should return the default anonymous group read policy", 1, policies.size());
    }

}
