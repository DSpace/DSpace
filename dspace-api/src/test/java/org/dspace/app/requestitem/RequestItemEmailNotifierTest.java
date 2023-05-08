/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Provider;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;

import org.dspace.AbstractUnitTest;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for {@link RequestItemEmailNotifier}.
 *
 * @author mwood
 */
public class RequestItemEmailNotifierTest
        extends AbstractUnitTest {

    public static final String TRANSPORT_CLASS_KEY = "mail.smtp.class";

    public RequestItemEmailNotifierTest() {
        super();
    }

    @BeforeClass
    public static void setUpClass() {
        AbstractBuilder.init(); // AbstractUnitTest doesn't do this for us.
    }

    /**
     * Test of sendRequest method, of class RequestItemEmailNotifier.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testSendRequest() throws Exception {
    }

    /**
     * Test of sendResponse method, of class RequestItemEmailNotifier.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testSendResponse() throws Exception {
        final String REQUESTOR_ADDRESS = "mhwood@wood.net";
        final String REQUESTOR_NAME = "Mark Wood";
        final String HELPDESK_ADDRESS = "help@example.com";
        final String HELPDESK_NAME = "Help Desk";
        final String TEST_MESSAGE = "Message";
        final String DUMMY_PROTO = "dummy";

        // Create some content to send.
        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context)
                .withName("Top Community")
                .build();
        Collection col = CollectionBuilder.createCollection(context, com)
                .build();
        Item item = ItemBuilder.createItem(context, col)
                .withTitle("Test Item")
                .build();
        context.restoreAuthSystemState();

        // Create a request to which we can respond.
        RequestItem ri = new RequestItem();
        ri.setAccept_request(true);
        ri.setItem(item);
        ri.setAllfiles(true);
        ri.setReqEmail(REQUESTOR_ADDRESS);
        ri.setReqName(REQUESTOR_NAME);

        // Ensure that mail is "sent".
        final ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        configurationService.setProperty("mail.server.disabled", "false");

        // Install a fake transport for RFC2822 email addresses.
        Session session = DSpaceServicesFactory.getInstance().getEmailService().getSession();
        Provider transportProvider = new Provider(Provider.Type.TRANSPORT,
                DUMMY_PROTO, JavaMailTestTransport.class.getCanonicalName(),
                "DSpace", "1.0");
        session.addProvider(transportProvider);
        session.setProvider(transportProvider);
        session.setProtocolForAddress("rfc822", DUMMY_PROTO);

        // Instantiate and initialize the unit, using the "help desk" strategy.
        RequestItemEmailNotifier requestItemEmailNotifier
                = new RequestItemEmailNotifier();
        requestItemEmailNotifier.bitstreamService
                = ContentServiceFactory.getInstance().getBitstreamService();
        requestItemEmailNotifier.configurationService = configurationService;
        requestItemEmailNotifier.handleService
                = HandleServiceFactory.getInstance().getHandleService();
        requestItemEmailNotifier.requestItemService
                = RequestItemServiceFactory.getInstance().getRequestItemService();
        requestItemEmailNotifier.requestItemAuthorExtractor
                =  DSpaceServicesFactory.getInstance()
                    .getServiceManager()
                        .getServiceByName("HelpdeskStrategy", RequestItemAuthorExtractor.class);

        // Configure the help desk strategy.
        configurationService.setProperty("mail.helpdesk", HELPDESK_ADDRESS);
        configurationService.setProperty("mail.helpdesk.name", HELPDESK_NAME);
        configurationService.setProperty("request.item.helpdesk.override", "true");

        // Test the unit.  Template supplies the Subject: value
        requestItemEmailNotifier.sendResponse(context, ri, null, TEST_MESSAGE);

        // Evaluate the test results.

        // Check the To: address.
        Address[] myAddresses = JavaMailTestTransport.getAddresses();
        assertEquals("Should have one To: address.",
                myAddresses.length, 1);
        assertThat("To: should be an Internet address",
                myAddresses[0], instanceOf(InternetAddress.class));
        String address = ((InternetAddress)myAddresses[0]).getAddress();
        assertEquals("To: address should match requestor.",
                ri.getReqEmail(), address);

        // Check the message body.
        Message myMessage = JavaMailTestTransport.getMessage();

        Object content = myMessage.getContent();
        assertThat("Body should be a single text bodypart",
                content, instanceOf(String.class));

        assertThat("Should contain the helpdesk address",
                (String)content, containsString(HELPDESK_ADDRESS));

        assertThat("Should contain the helpdesk name",
                (String)content, containsString(HELPDESK_NAME));

        assertThat("Should contain the test custom message",
                (String)content, containsString(TEST_MESSAGE));
    }

    /**
     * Test of requestOpenAccess method, of class RequestItemEmailNotifier.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testRequestOpenAccess() throws Exception {
    }
}
