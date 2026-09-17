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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.velocity.exception.ParseErrorException;
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
public class EmailTest extends AbstractDSpaceTest {

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

    /**
     * Test that verifies the ability to create headers with dashes using $global.put()
     * without creating a temporary variable.
     * Syntax: $!global.put("X-Custom-Header", "value")
     * The $! (silent reference) calls the method without printing anything to the body.
     */
    @Test
    public void testVelocityHeaderWithDashUsingGlobalPutWithoutTempVariable() throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "true");
        config.setProperty("mail.message.headers", "X-Custom-Header, subject");

        var body = "Email body with custom header.";
        String template = "$!global.put('X-Custom-Header', 'test-value-123')" + body;

        Email email = new Email();
        email.setContent("test-header-dash", template);
        email.addRecipient("test@example.com");
        email.build();

        MimeMessage message = email.message;
        String[] headerValues = message.getHeader("X-Custom-Header");
        String messageBody = message.getContent().toString();

        assertThat("Header should contain the correct value", headerValues[0], is("test-value-123"));
        assertThat("Body should contain only the body text", messageBody, containsString(body));
        assertThat("Body should NOT contain references to $global.put", messageBody, not(containsString("$global")));
        assertThat("Body should NOT contain the header name", messageBody, not(containsString("X-Custom-Header")));
        assertThat("Body should NOT contain the header value", messageBody, not(containsString("test-value-123")));
    }

    /**
     * Test with alternative syntax using #if()
     * to force evaluation of the expression without creating temporary variables.
     * Syntax: #if($global.put("X-Header", "value"))#end
     * The #if evaluates the expression (executing .put()), and since .put() returns null,
     * the condition is always false, but the method is still executed.
     */
    @Test
    public void testVelocityHeaderWithDashUsingIfConstruct() throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "true");
        config.setProperty("mail.message.headers", "X-TEST-TEST,subject");

        var body = "Email body with test header.";
        String template = "#if($global.put('X-TEST-TEST', $config.get('dspace.name')))#end\n" + body;

        Email email = new Email();
        email.setContent("test-if-construct", template);
        email.addRecipient("test@example.com");
        email.build();

        MimeMessage message = email.message;
        String[] headerValues = message.getHeader("X-TEST-TEST");
        String messageBody = message.getContent().toString();

        assertThat("Header X-TEST-TEST should exist", headerValues, notNullValue());
        assertThat("Header should contain the DSpace name", headerValues[0], equalTo("DSpace at My University"));
        assertThat("Body should contain only the body text", messageBody, containsString(body));
        assertThat("Body should NOT contain references to $global.put", messageBody, not(containsString("$global")));
        assertThat("Body should NOT contain the header name", messageBody, not(containsString("X-TEST-TEST")));
        assertThat("Body should NOT contain the header value", messageBody,
                not(containsString("DSpace at My University")));
    }

    /**
     * Test of the current syntax (with temporary variable) for comparison.
     * Syntax: #set($temp = $global.put("X-Header", "value"))
     * This works but creates an unnecessary temporary variable with null value.
     * The variable will be rendered as "$test" (literal) if not handled with $!
     */
    @Test
    public void testVelocityHeaderWithDashUsingTempVariable() throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "true");
        config.setProperty("mail.message.headers", "X-Test-ID,subject");

        // Using $!test instead of $test to avoid printing the literal string
        String template = "#set($test = $global.put('X-Test-ID', 'test-001'))\n" +
                "Email body. Temp variable value:$!test";

        Email email = new Email();
        email.setContent("test-temp-var", template);
        email.addRecipient("test@example.com");
        email.build();

        MimeMessage message = email.message;
        String[] headerValues = message.getHeader("X-Test-ID");
        String messageBody = message.getContent().toString();

        assertThat("Header X-Test-ID should exist", headerValues, is(not((String[]) null)));
        assertThat("Header should contain the ID", headerValues[0], is("test-001"));
        // $!test prints nothing when null, so the body should not contain "test"
        assertThat("Temp variable should render as empty string", messageBody,
                equalTo("Email body. Temp variable value:"));
        assertThat("Temp variable should not print literal $test", messageBody, not(containsString("$test")));
    }

    /**
     * Test with multiple headers containing dashes using different syntaxes.
     * Demonstrates that all three approaches work together in the same template.
     */
    @Test
    public void testVelocityMultipleHeadersWithDash() throws MessagingException, IOException {
        config.setProperty("mail.server.disabled", "true");
        config.setProperty("mail.message.headers", "X-Header-One,X-Header-Two,X-Header-Three,subject");

        String template = "$!global.put('X-Header-One', 'value1')\n" +
                "#if($global.put('X-Header-Two', 'value2'))#end\n" +
                "#set($temp = $global.put('X-Header-Three', 'value3'))\n" +
                "Email with multiple custom headers.";

        Email email = new Email();
        email.setContent("test-multiple-headers", template);
        email.addRecipient("test@example.com");
        email.build();

        MimeMessage message = email.message;

        assertThat("X-Header-One should exist", message.getHeader("X-Header-One")[0], is("value1"));
        assertThat("X-Header-Two should exist", message.getHeader("X-Header-Two")[0], is("value2"));
        assertThat("X-Header-Three should exist", message.getHeader("X-Header-Three")[0], is("value3"));
    }

    /**
     * Test that verifies headers with dashes do not work with standard syntax
     * (without using $global.put()).
     * This demonstrates the problem that necessitates the $global.put() workaround.
     *
     * The standard Velocity syntax {@code #set($X-Header = 'value')} causes a
     * {@link ParseErrorException} when the template is loaded via {@link Email#setContent},
     * because Velocity interprets the dash as a subtraction operator (e.g., $X minus Header).
     *
     * This test expects the {@link ParseErrorException} to be thrown during template compilation,
     * proving that the workaround with $global.put() is necessary for headers containing dashes.
     */
    @Test(expected = ParseErrorException.class)
    public void testVelocityHeaderWithDashStandardSyntaxFails() throws IOException {
        config.setProperty("mail.server.disabled", "true");
        config.setProperty("mail.message.headers", "X-Will-Not-Work,subject");

        // This syntax does NOT work due to the dash - it causes a parse error at template compilation
        String template = "#set($X-Will-Not-Work = 'this-will-fail')Email body.";

        Email email = new Email();
        // This call will throw ParseErrorException because the template cannot be compiled
        email.setContent("test-standard-syntax", template);
    }

}