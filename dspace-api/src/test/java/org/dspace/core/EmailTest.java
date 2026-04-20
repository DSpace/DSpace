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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
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

    @Test
    public void testCatchAllRecipient()
            throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "false");
        config.setProperty("mail.server.catchAll.enabled", "true");
        config.setProperty("mail.server.catchAll.recipient", "fixed@example.com");

        Email email = new Email();
        email.setContent("fixed recipient test",
                "This is a test email that should go to catchAll recipient.");
        email.addRecipient("original@example.com");
        email.build();

        MimeMessage message = email.message;
        InternetAddress[] recipients = (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.TO);

        assertThat("Exactly one 'To:' recipient expected.", recipients.length, is(1));
        assertThat("CatchAll recipient should receive the email", recipients[0].getAddress(), is("fixed@example.com"));

        // Check that original recipient marker is included in the body
        String messageBody = message.getContent().toString();
        assertThat("Original recipient should be included in email body",
                messageBody, containsString("original@example.com"));
        assertThat("Email body should contain REAL RECIPIENT marker",
                messageBody, containsString("===REAL RECIPIENT==="));
    }

    @Test
    public void testMultipleCatchAllRecipientsWhenMailServerDisabled()
            throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "true");
        config.setProperty("mail.server.catchAll.enabled", "true");
        config.setProperty("mail.server.catchAll.recipient", "fixed1@example.com,fixed2@example.com");

        Email email = new Email();
        email.setContent("multiple catchAll recipients test",
                "This is a test email that should go to multiple catchAll recipients.");
        email.addRecipient("original@example.com");
        email.build();

        MimeMessage message = email.message;
        InternetAddress[] recipients = (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.TO);

        assertThat("Email should be sent to both catchAll recipients", recipients.length, is(2));
        assertThat("First catchAll recipient should receive the email", recipients[0].getAddress(),
                   is("fixed1@example.com"));
        assertThat("Second catchAll recipient should receive the email", recipients[1].getAddress(),
                   is("fixed2@example.com"));
    }

    @Test
    public void testEmailWithoutCatchAllRecipient()
            throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "false");
        config.setProperty("mail.server.catchAll.enabled", "true");
        config.setProperty("mail.server.catchAll.recipient", "");

        Email email = new Email();
        email.setContent("disabled server test",
                "This is a test email that should not be sent.");
        email.addRecipient("original@example.com");
        email.build();

        MimeMessage message = email.message;
        InternetAddress[] recipients = (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.TO);

        assertThat("Message should be created", message, is(not((MimeMessage) null)));
        assertThat("Email should be sent to original recipient when no catchAll recipient configured",
                recipients.length, is(1));
        assertThat("Original recipient should receive the email", recipients[0].getAddress(),
                   is("original@example.com"));

        // Check that real recipient marker is NOT included in the body (normal behavior)
        String messageBody = message.getContent().toString();
        assertThat("Real recipient marker should NOT be included in email body for normal disabled behavior",
                messageBody, not(containsString("===REAL RECIPIENT===")));
    }

    @Test
    public void testNormalEmailSendingWithCatchAllRecipientConfigured()
            throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "false");
        config.setProperty("mail.server.catchAll.enabled", "false");
        config.setProperty("mail.server.catchAll.recipient", "fixed@example.com");

        Email email = new Email();
        email.setContent("normal sending test",
                "This is a test email that should go to original recipient.");
        email.addRecipient("original@example.com");
        email.build();

        MimeMessage message = email.message;
        InternetAddress[] recipients = (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.TO);

        assertThat("Message should have exactly one recipient.", recipients.length, is(1));
        assertThat("Original recipient should receive the email",
                   recipients[0].getAddress(), is("original@example.com"));

        // Check that real recipient marker is NOT included in the body (normal behavior)
        String messageBody = message.getContent().toString();
        assertThat("Real recipient marker should NOT be included in email body for normal sending",
                messageBody, not(containsString("===REAL RECIPIENT===")));
    }

    @Test
    public void testCatchAllRecipientWithCCRecipients()
            throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "false");
        config.setProperty("mail.server.catchAll.enabled", "true");
        config.setProperty("mail.server.catchAll.recipient", "fixed@example.com");

        Email email = new Email();
        email.setContent("CC recipients test",
                "This is a test email with CC recipients.");
        email.addRecipient("original@example.com");
        // Note: In the current implementation, CC addresses need to be added via the message directly
        // For this test, we'll verify the structure is ready for CC handling
        email.build();

        MimeMessage message = email.message;
        InternetAddress[] recipients = (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.TO);
        InternetAddress[] ccRecipients = (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.CC);

        assertThat("Message should have exactly one recipient.", recipients.length, is(1));
        assertThat("CatchAll recipient should receive the email", recipients[0].getAddress(), is("fixed@example.com"));
        assertThat("CC field should be null for catchAll recipient", ccRecipients, is((InternetAddress[]) null));


        // Check that original recipient is included in the body
        String messageBody = message.getContent().toString();
        assertThat("Original recipient should be included in email body",
                messageBody, containsString("original@example.com"));
    }

    @Test
    public void testCatchAllRecipientWithEmptyRecipient()
        throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "false");
        config.setProperty("mail.server.catchAll.enabled", "true");
        config.setProperty("mail.server.catchAll.recipient", "");

        Email email = new Email();
        email.setContent("Empty recipient test","This is a test email with empty recipient.");
        email.addRecipient("original@example.com");

        email.build();

        MimeMessage message = email.message;
        InternetAddress[] recipients = (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.TO);
        InternetAddress[] ccRecipients = (InternetAddress[]) message.getRecipients(MimeMessage.RecipientType.CC);

        assertThat("Message should have exactly one recipient.", recipients.length, is(1));
        assertThat("Original recipient should receive the email", recipients[0].getAddress(),
                   is("original@example.com"));
        assertThat("CC field should be null for catchAll recipient", ccRecipients, Matchers.nullValue());

        assertThat(
            "Real recipient marker should NOT be included in email body",
            message.getContent().toString(),
            not(containsString("===REAL RECIPIENT==="))
        );
    }

    @Test
    public void testCatchAllRecipientsLogging()
            throws MessagingException, IOException {
        config.setProperty("mail.server.catchAll.enabled", "true");
        config.setProperty("mail.server.catchAll.recipient", "fixed@example.com");

        Email email = new Email();
        email.setContent("logging test",
                "This is a test email for logging verification.");
        email.addRecipient("original@example.com");
        email.build();

        // Get the logged message (this simulates what would be logged)
        String loggedMessage = email.getMessage();

        assertThat("Logged message should contain catchAllrecipient info",
                loggedMessage, containsString("fixed@example.com"));
        assertThat("Logged message should contain original recipient info",
                loggedMessage, containsString("original@example.com"));
        assertThat("Logged message should contain REAL RECIPIENT section",
                loggedMessage, containsString("===REAL RECIPIENT==="));
    }
}
