/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import static org.dspace.core.Constants.READ;
import static org.dspace.eperson.Group.ANONYMOUS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Provider;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
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
public class RequestItemEmailNotifierTest extends AbstractUnitTest {

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
    private static EPersonService ePersonService;

    public RequestItemEmailNotifierTest() {
        super();
    }

    @BeforeClass
    public static void setUpClass() {
        AbstractBuilder.init(); // AbstractUnitTest doesn't do this for us.

        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    }

    @AfterClass
    public static void tearDownClass() {
        AbstractBuilder.destroy(); // AbstractUnitTest doesn't do this for us.
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
        requestItemEmailNotifier.authorizeService = authorizeService;
        requestItemEmailNotifier.ePersonService = ePersonService;

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

        assertThat("Should contain the helpdesk name",
                (String)content, containsString(HELPDESK_NAME));

        assertThat("Should contain the test custom message",
                (String)content, containsString(TEST_MESSAGE));
    }

    /**
     * Test of sendResponse method with attached files, of class RequestItemEmailNotifier.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testSendResponseWithFiles() throws Exception {
        // Create some content to send.
        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("Top Community").build();
        Collection col = CollectionBuilder.createCollection(context, com).build();
        Item item = ItemBuilder.createItem(context, col).withTitle("Test Item").build();

        Group group = GroupBuilder.createGroup(context).withName("Group").build();
        Group anonymousGroup = EPersonServiceFactory.getInstance().getGroupService().findByName(context, ANONYMOUS);
        EPerson eperson = EPersonBuilder.createEPerson(context).withEmail("test@test.test").build();

        Bundle bundle = BundleBuilder.createBundle(context, item).withName("ORIGINAL").build();

        Bitstream bitstreamWithoutPolicies = createBitstream(bundle, "Bitstream without policies");
        authorizeService.removePoliciesActionFilter(context, bitstreamWithoutPolicies, READ);

        Bitstream bitstreamWithGroupPolicy = createBitstream(bundle, "Bitstream with group policy");
        authorizeService.removePoliciesActionFilter(context, bitstreamWithGroupPolicy, READ);
        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withAction(READ)
                             .withGroup(group)
                             .withDspaceObject(bitstreamWithGroupPolicy)
                             .build();

        Bitstream bitstreamWithEPersonPolicy = createBitstream(bundle, "Bitstream with eperson policy");
        authorizeService.removePoliciesActionFilter(context, bitstreamWithEPersonPolicy, READ);
        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withAction(READ)
                             .withUser(eperson)
                             .withDspaceObject(bitstreamWithEPersonPolicy)
                             .build();

        Bitstream bitstreamEmbargoed = createBitstream(bundle, "Bitstream embargoed");
        authorizeService.removePoliciesActionFilter(context, bitstreamEmbargoed, READ);
        ResourcePolicyBuilder.createResourcePolicy(context)
                             .withName("embargo")
                             .withAction(READ)
                             .withGroup(anonymousGroup)
                             .withDspaceObject(bitstreamEmbargoed)
                             .withStartDate(
                                 new GregorianCalendar(2200, Calendar.DECEMBER, 31).getTime()
                             )
                             .build();

        createBitstream(bundle, "Bitstream open access");

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
        requestItemEmailNotifier.authorizeService = authorizeService;
        requestItemEmailNotifier.ePersonService = ePersonService;

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

        assertThat("Body should be a multipart",
                   content, instanceOf(MimeMultipart.class));

        MimeMultipart multipartContent = (MimeMultipart) content;

        assertEquals("Body should have 5 parts (1 main message and 4 attachments)",
                     5, multipartContent.getCount());

        assertThat("Should contain the helpdesk name",
                   (String) multipartContent.getBodyPart(0).getDataHandler().getContent(),
                   containsString(HELPDESK_NAME));

        assertThat("Should contain the test custom message",
                   (String) multipartContent.getBodyPart(0).getDataHandler().getContent(),
                   containsString(TEST_MESSAGE));

        assertEquals("Should contain file without policies",
                     bitstreamWithoutPolicies.getName(),
                     multipartContent.getBodyPart(1).getFileName());

        assertEquals("Should contain file with group policy",
                     bitstreamWithGroupPolicy.getName(),
                     multipartContent.getBodyPart(2).getFileName());

        assertEquals("Should contain file with eperson policy",
                     bitstreamWithEPersonPolicy.getName(),
                     multipartContent.getBodyPart(3).getFileName());

        assertEquals("Should contain embargoed file",
                     bitstreamEmbargoed.getName(),
                     multipartContent.getBodyPart(4).getFileName());
    }

    private Bitstream createBitstream(Bundle bundle, String name) {
        try (InputStream is = IOUtils.toInputStream("Test file content", CharEncoding.UTF_8)) {
            return BitstreamBuilder.createBitstream(context, bundle, is)
                                   .withName(name)
                                   .withMimeType("text/plain")
                                   .build();
        } catch (IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
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
        requestItemEmailNotifier.authorizeService = authorizeService;
        requestItemEmailNotifier.ePersonService = ePersonService;

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
