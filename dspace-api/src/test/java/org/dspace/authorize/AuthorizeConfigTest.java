/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.dspace.AbstractIntegrationTest;
import org.junit.Test;

/**
 * This integration test verify that the {@link AuthorizeConfiguration} works
 * properly with the configuration reloading
 * 
 * FIXME Due to a not optimal definition of UnitTest and IntegrationTest in
 * DSpace see https://github.com/DSpace/DSpace/issues/3055 all the integration
 * test that inherit from org.dspace.AbstractUnitTest without denying its init()
 * method behavior MUST BE named as Unit Test
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class AuthorizeConfigTest extends AbstractIntegrationTest {

    @Test
    public void testReloadConfiguration() {
        cleanExtraConfigurations();
        // the default configuration is to allow delegation of all feature, just check a selection of three permissions
        assertTrue(AuthorizeConfiguration.canCommunityAdminPerformGroupCreation());
        assertTrue(AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion());
        assertTrue(AuthorizeConfiguration.canCommunityAdminPerformSubelementCreation());

        // in our extra configuration file for test, disable some feature
        appendToLocalConfiguration(
            "core.authorization.community-admin.group = false\n" +
            "core.authorization.community-admin.delete-subelement = false\n");
        // verify that the two changed one are reflected in the AuthorizationConfiguration
        assertFalse(AuthorizeConfiguration.canCommunityAdminPerformGroupCreation());
        assertFalse(AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion());

        // verify that the third still retain the original value
        assertTrue(AuthorizeConfiguration.canCommunityAdminPerformSubelementCreation());

        // add other configuration to switch off also the third
        appendToLocalConfiguration(
                "core.authorization.community-admin.create-subelement = false\n");

        assertFalse(AuthorizeConfiguration.canCommunityAdminPerformSubelementCreation());
        assertFalse(AuthorizeConfiguration.canCommunityAdminPerformGroupCreation());
        assertFalse(AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion());

        // empty our extra configuration
        cleanExtraConfigurations();

        // now the default should be returned again
        assertTrue(AuthorizeConfiguration.canCommunityAdminPerformGroupCreation());
        assertTrue(AuthorizeConfiguration.canCommunityAdminPerformSubelementCreation());
        assertTrue(AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion());
    }

}
