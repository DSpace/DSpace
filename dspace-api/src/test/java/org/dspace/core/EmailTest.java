/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.io.IOException;

import jakarta.mail.MessagingException;
import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for email sender.
 *
 * @author mwood
 */
public class EmailTest
        extends AbstractDSpaceTest {
    private ConfigurationService config;

    @Before
    public void init_test() {
        config = kernelImpl.getConfigurationService();
    }

    @Test
    public void testNullParameter()
            throws MessagingException, IOException {
        // Ensure that no mail goes out
        config.setProperty("mail.server.disabled", "true");

        Email email = new Email();
        email.setContent("null test",
                "Testing: parameter value is /${params[0]}/.");
        email.addArgument(null);
        email.build();
        String message = email.getMessage();
        assertThat("Null message parameter should be transformed to empty",
                message, not(containsString("(null)")));
    }

    @Test
    public void testNotNullParameter()
            throws MessagingException, IOException {
        // Ensure that no mail goes out
        config.setProperty("mail.server.disabled", "true");

        Email email = new Email();
        email.setContent("not-null test",
                "Testing: parameter value is /${params[0]}/.");
        String testParam = "axolotl";
        email.addArgument(testParam);
        email.build();
        String message = email.getMessage();
        assertThat("Null message parameter should be transformed to empty",
                message, containsString(testParam));
    }
}
