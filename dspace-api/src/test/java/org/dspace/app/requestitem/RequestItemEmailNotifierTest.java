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

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Provider;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import org.dspace.AbstractUnitTest;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.AfterClass;
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

    private static final String REQUESTOR_ADDRESS = "mhwood@wood.net";
    private static final String REQUESTOR_NAME = "Mark Wood";
    private static final String HELPDESK_ADDRESS = "help@example.com";
    private static final String HELPDESK_NAME = "Help Desk";
    private static final String TEST_MESSAGE = "Message";
    private static final String DUMMY_PROTO = "dummy";

    private static ConfigurationService configurationService;
    private static BitstreamService bitstreamService;
    private static HandleService handleService;
    private static RequestItemService requestItemService;

    public RequestItemEmailNotifierTest() {
        super();
    }

    @BeforeClass
    public static void setUpClass() {
        AbstractBuilder.init(); // AbstractUnitTest doesn't do this for us.

        configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        bitstreamService
                = ContentServiceFactory.getInstance().getBitstreamService();
        handleService
                = HandleServiceFactory.getInstance().getHandleService();
        requestItemService
                = RequestItemServiceFactory.getInstance().getRequestItemService();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // AbstractUnitTest doesn't do this for us.
        AbstractBuilder.cleanupObjects();
        AbstractBuilder.destroy();
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

        // Install a fake transport for RFC2822 email addresses.
        Session session = DSpaceServicesFactory.getInstance().getEmailService().getSession();
        Provider transportProvider = new Provider(Provider.Type.TRANSPORT,
                DUMMY_PROTO, JavaMailTestTransport.class.getCanonicalName(),
                "DSpace", "1.0");
        session.addProvider(transportProvider);
        session.setProvider(transportProvider);
        session.setProtocolForAddress("rfc822", DUMMY_PROTO);

        // Configure the help desk strategy.
        configurationService.setProperty("mail.helpdesk", HELPDESK_ADDRESS);
        configurationService.setProperty("mail.helpdesk.name", HELPDESK_NAME);
        configurationService.setProperty("request.item.helpdesk.override", "true");

        // Ensure that mail is "sent".
        configurationService.setProperty("mail.server.disabled", "false");

        // Instantiate and initialize the unit, using the "help desk" strategy.
        RequestItemEmailNotifier requestItemEmailNotifier
                = new RequestItemEmailNotifier(
                        DSpaceServicesFactory.getInstance()
                                .getServiceManager()
                                .getServiceByName(RequestItemHelpdeskStrategy.class.getName(),
                                        RequestItemAuthorExtractor.class));
        requestItemEmailNotifier.bitstreamService = bitstreamService;
        requestItemEmailNotifier.configurationService = configurationService;
        requestItemEmailNotifier.handleService = handleService;
        requestItemEmailNotifier.requestItemService = requestItemService;

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
        assertEquals("To: address should match requester.",
                ri.getReqEmail(), address);

        // Check the message body.
        Message myMessage = JavaMailTestTransport.getMessage();

        Object content = myMessage.getContent();
        assertThat("Body should be a single text bodypart",
                content, instanceOf(String.class));

        assertThat("Should contain the helpdesk name",
                (String)content, containsString(HELPDESK_NAME));

        assertThat("Should contain the test custom message",
                (String)content, containsString(TEST_MESSAGE));
    }

    /**
     * Test of sendResponse method -- rejection case.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testSendRejection()
        throws Exception {
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
        ri.setAccept_request(false);
        ri.setItem(item);
        ri.setAllfiles(true);
        ri.setReqEmail(REQUESTOR_ADDRESS);
        ri.setReqName(REQUESTOR_NAME);

        // Install a fake transport for RFC2822 email addresses.
        Session session = DSpaceServicesFactory.getInstance().getEmailService().getSession();
        Provider transportProvider = new Provider(Provider.Type.TRANSPORT,
                DUMMY_PROTO, JavaMailTestTransport.class.getCanonicalName(),
                "DSpace", "1.0");
        session.addProvider(transportProvider);
        session.setProvider(transportProvider);
        session.setProtocolForAddress("rfc822", DUMMY_PROTO);

        // Configure the help desk strategy.
        configurationService.setProperty("mail.helpdesk", HELPDESK_ADDRESS);
        configurationService.setProperty("mail.helpdesk.name", HELPDESK_NAME);
        configurationService.setProperty("request.item.helpdesk.override", "true");

        // Ensure that mail is "sent".
        configurationService.setProperty("mail.server.disabled", "false");

        // Instantiate and initialize the unit, using the "help desk" strategy.
        RequestItemEmailNotifier requestItemEmailNotifier
                = new RequestItemEmailNotifier(
                        DSpaceServicesFactory.getInstance()
                                .getServiceManager()
                                .getServiceByName(RequestItemHelpdeskStrategy.class.getName(),
                                        RequestItemAuthorExtractor.class));
        requestItemEmailNotifier.bitstreamService = bitstreamService;
        requestItemEmailNotifier.configurationService = configurationService;
        requestItemEmailNotifier.handleService = handleService;
        requestItemEmailNotifier.requestItemService = requestItemService;

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
        assertEquals("To: address should match requester.",
                ri.getReqEmail(), address);

        // Check the message body.
        Message myMessage = JavaMailTestTransport.getMessage();

        Object content = myMessage.getContent();
        assertThat("Body should be a single text bodypart",
                content, instanceOf(String.class));

        assertThat("Should contain the helpdesk name",
                (String)content, containsString(HELPDESK_NAME));

        assertThat("Should contain the test custom message",
                (String)content, containsString(TEST_MESSAGE));

        // FIXME Note that this depends on the content of the rejection template!
        assertThat("Should contain the word 'denied'.",
                (String)content, containsString("denied"));
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
