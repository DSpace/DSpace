/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.MockSolrSearchCore;
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.email.EmailServiceImpl;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Integration tests for {@link SubscriptionEmailNotificationService}.
 */
public class SubscriptionEmailNotificationServiceIT extends AbstractIntegrationTestWithDatabase {

    SubscriptionEmailNotificationService subscriptionEmailNotificationService =
            DSpaceServicesFactory.getInstance()
                                 .getServiceManager()
                                 .getServiceByName(SubscriptionEmailNotificationServiceImpl.class.getName(),
                                                   SubscriptionEmailNotificationServiceImpl.class);
    SubscribeService subscribeService = ContentServiceFactory.getInstance()
                                                             .getSubscribeService();
    ItemService itemService = ContentServiceFactory.getInstance()
                                                   .getItemService();
    MockSolrSearchCore searchService = DSpaceServicesFactory.getInstance()
                                                            .getServiceManager()
                                                            .getServiceByName(null, MockSolrSearchCore.class);
    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    private Collection col;
    private Community com;
    private final ZonedDateTime yesterday = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1).truncatedTo(SECONDS);
    private final ZonedDateTime twoDaysAgo = ZonedDateTime.now(ZoneOffset.UTC).minusDays(2).truncatedTo(SECONDS);
    private final String yesterdayString = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    private final String twoDaysAgoString = twoDaysAgo.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    private String originalMailServer;
    private String originalMailServerPort;
    private String originalMailFromAddress;
    private Boolean originalMailServerDisabled;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        com = CommunityBuilder.createCommunity(context)
                              .withName("Test Community")
                              .build();
        col = CollectionBuilder.createCollection(context, com)
                             .withName("Test Collection")
                             .build();

        // Store original mail configuration
        originalMailServer = configurationService.getProperty("mail.server");
        originalMailServerPort = configurationService.getProperty("mail.server.port");
        originalMailFromAddress = configurationService.getProperty("mail.from.address");
        originalMailServerDisabled = configurationService.getBooleanProperty("mail.server.disabled", false);

        configurationService.setProperty("mail.server", ServerSetupTest.SMTP.getBindAddress());
        configurationService.setProperty("mail.server.port", String.valueOf(ServerSetupTest.SMTP.getPort()));
        configurationService.setProperty("mail.from.address", "dspace@example.com");
        configurationService.setProperty("mail.server.disabled", false);

        // Reset the email service to apply the new configuration for the test
        ((EmailServiceImpl)DSpaceServicesFactory.getInstance().getEmailService()).reset();

        context.restoreAuthSystemState();
    }

    @After
    public void restoreMailConfig() {
        configurationService.setProperty("mail.server", originalMailServer);
        configurationService.setProperty("mail.server.port", originalMailServerPort);
        configurationService.setProperty("mail.from.address", originalMailFromAddress);
        configurationService.setProperty("mail.server.disabled", originalMailServerDisabled);
        // Reset the email service to apply the restored configuration
        ((EmailServiceImpl)DSpaceServicesFactory.getInstance().getEmailService()).reset();
    }

    @Test
    public void testSubscriptionToCollectionWithNewItem() throws Exception {
        context.turnOffAuthorisationSystem();
        subscribeTo(col);

        ItemBuilder.createItem(context, col)
                   .withTitle("Test Item")
                   .withDateAccessioned(yesterdayString)
                   .withLastModified(yesterday.toInstant())
                   .build();
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        assertEquals(eperson.getEmail(), message.getRecipients(Message.RecipientType.TO)[0].toString());
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("New items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection:"));
        assertTrue(body.contains("New Items (1):"));
        assertTrue(body.contains("Title: Test Item"));
    }

    @Test
    public void testSubscriptionToCollectionWithMultipleNewItems() throws Exception {
        context.turnOffAuthorisationSystem();
        subscribeTo(col);

        ItemBuilder.createItem(context, col)
                   .withTitle("Test Item 1")
                   .withDateAccessioned(yesterdayString)
                   .withLastModified(yesterday.toInstant())
                   .build();
        ItemBuilder.createItem(context, col)
                   .withTitle("Test Item 2")
                   .withDateAccessioned(yesterdayString)
                   .withLastModified(yesterday.toInstant())
                   .build();
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        assertEquals(eperson.getEmail(), message.getRecipients(Message.RecipientType.TO)[0].toString());
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("New items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection:"));
        assertTrue(body.contains("New Items (2):"));
        assertTrue(body.contains("Title: Test Item 1"));
        assertTrue(body.contains("Title: Test Item 2"));
    }


    @Test
    public void testSubscriptionToCollectionWithModifiedItem() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, col)
                               .withTitle("Original Title")
                               .withDateAccessioned(twoDaysAgoString)
                               .withLastModified(twoDaysAgo.toInstant())
                               .build();

        subscribeTo(col);
        context.restoreAuthSystemState();

        context.turnOffAuthorisationSystem();
        item.setLastModified(yesterday.toInstant());
        itemService.addMetadata(context, item, "dc", "title", null, null, "Modified Title");
        setFakeLastModifiedOnItemSolrDocument(context, item.getID(), yesterday);
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        assertEquals(eperson.getEmail(), message.getRecipients(Message.RecipientType.TO)[0].toString());
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("Modified items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection:"));
        assertTrue(body.contains("Modified Items (1):"));
        assertTrue(body.contains("Title: Original Title, Modified Title"));
    }

    @Test
    public void testSubscriptionToCollectionWithMultipleModifiedItems() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, col)
                                .withTitle("Original Title 1")
                                .withDateAccessioned(twoDaysAgoString)
                                .withLastModified(twoDaysAgo.toInstant())
                                .build();
        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Original Title 2")
                                .withDateAccessioned(twoDaysAgoString)
                                .withLastModified(twoDaysAgo.toInstant())
                                .build();

        subscribeTo(col);

        item1.setLastModified(yesterday.toInstant());
        itemService.addMetadata(context, item1, "dc", "title", null, null, "Modified Title 1");
        setFakeLastModifiedOnItemSolrDocument(context, item1.getID(), yesterday);

        item2.setLastModified(yesterday.toInstant());
        itemService.addMetadata(context, item2, "dc", "title", null, null, "Modified Title 2");
        setFakeLastModifiedOnItemSolrDocument(context, item2.getID(), yesterday);
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        assertEquals(eperson.getEmail(), message.getRecipients(Message.RecipientType.TO)[0].toString());
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("Modified items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection:"));
        assertTrue(body.contains("Modified Items (2):"));
        assertTrue(body.contains("Title: Original Title 1, Modified Title 1"));
        assertTrue(body.contains("Title: Original Title 2, Modified Title 2"));
    }

    @Test
    public void testSubscriptionToCollectionWithNewAndModifiedItems() throws Exception {
        context.turnOffAuthorisationSystem();
        // new item
        ItemBuilder.createItem(context, col)
                   .withTitle("New Item")
                   .withDateAccessioned(yesterdayString)
                   .withLastModified(yesterday.toInstant())
                   .build();
        // modified item
        Item modifiedItem = ItemBuilder.createItem(context, col)
                                       .withTitle("Original Title")
                                       .withDateAccessioned(twoDaysAgoString)
                                       .withLastModified(twoDaysAgo.toInstant())
                                       .build();

        subscribeTo(col);

        itemService.addMetadata(context, modifiedItem, "dc", "title", null, null, "Modified Title");
        modifiedItem.setLastModified(yesterday.toInstant());
        setFakeLastModifiedOnItemSolrDocument(context, modifiedItem.getID(), yesterday);
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("New and modified items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection:"));
        assertTrue(body.contains("New Items (1):"));
        assertTrue(body.contains("Title: New Item"));
        assertTrue(body.contains("Modified Items (1):"));
        assertTrue(body.contains("Title: Original Title, Modified Title"));
    }

    @Test
    public void testSubscriptionToCommunityWithNewItem() throws Exception {
        context.turnOffAuthorisationSystem();
        subscribeTo(com);

        ItemBuilder.createItem(context, col)
                   .withTitle("Test Item")
                   .withDateAccessioned(yesterdayString)
                   .withLastModified(yesterday.toInstant())
                   .build();
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        assertEquals(eperson.getEmail(), message.getRecipients(Message.RecipientType.TO)[0].toString());
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("New items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection (via community subscription to \"Test Community\"):"));
        assertTrue(body.contains("New Items (1):"));
        assertTrue(body.contains("Title: Test Item"));
    }

    @Test
    public void testSubscriptionToCommunityWithMultipleNewItems() throws Exception {
        context.turnOffAuthorisationSystem();
        subscribeTo(com);

        ItemBuilder.createItem(context, col)
                   .withTitle("Test Item 1")
                   .withDateAccessioned(yesterdayString)
                   .withLastModified(yesterday.toInstant())
                   .build();
        ItemBuilder.createItem(context, col)
                   .withTitle("Test Item 2")
                   .withDateAccessioned(yesterdayString)
                   .withLastModified(yesterday.toInstant())
                   .build();
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        assertEquals(eperson.getEmail(), message.getRecipients(Message.RecipientType.TO)[0].toString());
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("New items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection (via community subscription to \"Test Community\"):"));
        assertTrue(body.contains("New Items (2):"));
        assertTrue(body.contains("Title: Test Item 1"));
        assertTrue(body.contains("Title: Test Item 2"));
    }

    @Test
    public void testSubscriptionToCommunityWithModifiedItem() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, col)
                               .withTitle("Original Title")
                               .withDateAccessioned(twoDaysAgoString)
                               .withLastModified(twoDaysAgo.toInstant())
                               .build();

        subscribeTo(com);
        context.restoreAuthSystemState();

        context.turnOffAuthorisationSystem();
        item.setLastModified(yesterday.toInstant());
        itemService.addMetadata(context, item, "dc", "title", null, null, "Modified Title");
        setFakeLastModifiedOnItemSolrDocument(context, item.getID(), yesterday);
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        assertEquals(eperson.getEmail(), message.getRecipients(Message.RecipientType.TO)[0].toString());
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("Modified items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection (via community subscription to \"Test Community\"):"));
        assertTrue(body.contains("Modified Items (1):"));
        assertTrue(body.contains("Title: Original Title, Modified Title"));
    }

    @Test
    public void testSubscriptionToCommunityWithMultipleModifiedItems() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, col)
                                .withTitle("Original Title 1")
                                .withDateAccessioned(twoDaysAgoString)
                                .withLastModified(twoDaysAgo.toInstant())
                                .build();
        Item item2 = ItemBuilder.createItem(context, col)
                                .withTitle("Original Title 2")
                                .withDateAccessioned(twoDaysAgoString)
                                .withLastModified(twoDaysAgo.toInstant())
                                .build();

        subscribeTo(com);
        context.restoreAuthSystemState();

        context.turnOffAuthorisationSystem();
        item1.setLastModified(yesterday.toInstant());
        itemService.addMetadata(context, item1, "dc", "title", null, null, "Modified Title 1");
        setFakeLastModifiedOnItemSolrDocument(context, item1.getID(), yesterday);

        item2.setLastModified(yesterday.toInstant());
        itemService.addMetadata(context, item2, "dc", "title", null, null, "Modified Title 2");
        setFakeLastModifiedOnItemSolrDocument(context, item2.getID(), yesterday);
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        assertEquals(eperson.getEmail(), message.getRecipients(Message.RecipientType.TO)[0].toString());
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("Modified items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection (via community subscription to \"Test Community\"):"));
        assertTrue(body.contains("Modified Items (2):"));
        assertTrue(body.contains("Title: Original Title 1, Modified Title 1"));
        assertTrue(body.contains("Title: Original Title 2, Modified Title 2"));
    }

    @Test
    public void testSubscriptionToCommunityWithNewAndModifiedItems() throws Exception {
        context.turnOffAuthorisationSystem();
        // new item
        ItemBuilder.createItem(context, col)
                   .withTitle("New Item")
                   .withDateAccessioned(yesterdayString)
                   .withLastModified(yesterday.toInstant())
                   .build();
        // modified item
        Item modifiedItem = ItemBuilder.createItem(context, col)
                                       .withTitle("Original Title")
                                       .withDateAccessioned(twoDaysAgoString)
                                       .withLastModified(twoDaysAgo.toInstant())
                                       .build();

        subscribeTo(com);
        context.restoreAuthSystemState();

        context.turnOffAuthorisationSystem();
        modifiedItem.setLastModified(yesterday.toInstant());
        itemService.addMetadata(context, modifiedItem, "dc", "title", null, null, "Modified Title");
        setFakeLastModifiedOnItemSolrDocument(context, modifiedItem.getID(), yesterday);
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage message = receivedMessages[0];
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("New and modified items are available in the collections you have subscribed to:"));
        assertTrue(body.contains("Test Collection (via community subscription to \"Test Community\"):"));
        assertTrue(body.contains("New Items (1):"));
        assertTrue(body.contains("Title: New Item"));
        assertTrue(body.contains("Modified Items (1):"));
        assertTrue(body.contains("Title: Original Title, Modified Title"));
    }

    @Test
    public void testSubscriptionToCommunityAndCollectionWithNewAndModifiedItems() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection col2 = CollectionBuilder.createCollection(context, com).withName("Another Collection").build();

        subscribeTo(com);
        subscribeTo(col2);

        // In 'col' (under 'com' subscription)
        ItemBuilder.createItem(context, col).withTitle("Community New Item")
                   .withDateAccessioned(yesterdayString).withLastModified(yesterday.toInstant()).build();
        Item commModified = ItemBuilder.createItem(context, col).withTitle("Community Original Title")
                                       .withDateAccessioned(twoDaysAgoString)
                                       .withLastModified(twoDaysAgo.toInstant()).build();

        // In 'col2' (collection subscription)
        ItemBuilder.createItem(context, col2).withTitle("Collection New Item")
                   .withDateAccessioned(yesterdayString).withLastModified(yesterday.toInstant()).build();
        Item collModified = ItemBuilder.createItem(context, col2).withTitle("Collection Original Title")
                                       .withDateAccessioned(twoDaysAgoString)
                                       .withLastModified(twoDaysAgo.toInstant()).build();

        context.restoreAuthSystemState();

        context.turnOffAuthorisationSystem();
        commModified.setLastModified(yesterday.toInstant());
        itemService.addMetadata(context, commModified, "dc", "title", null, null, "Community Modified Title");
        setFakeLastModifiedOnItemSolrDocument(context, commModified.getID(), yesterday);

        collModified.setLastModified(yesterday.toInstant());
        itemService.addMetadata(context, collModified, "dc", "title", null, null, "Collection Modified Title");
        setFakeLastModifiedOnItemSolrDocument(context, collModified.getID(), yesterday);
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);
        String body = GreenMailUtil.getBody(receivedMessages[0]);

        assertTrue(body.contains("New and modified items are available in the collections you have subscribed to:"));

        assertTrue(body.contains("Test Collection (via community subscription to \"Test Community\"):"));
        assertTrue(body.contains("New Items (1):"));
        assertTrue(body.contains("Title: Community New Item"));
        assertTrue(body.contains("Modified Items (1):"));
        assertTrue(body.contains("Title: Community Original Title, Community Modified Title"));

        assertTrue(body.contains("Another Collection (via community subscription to \"Test Community\"):"));
        assertTrue(body.contains("Title: Collection New Item"));
        assertTrue(body.contains("Title: Collection Original Title, Collection Modified Title"));
    }


    @Test
    public void testNoEmailSentWhenNoUpdates() throws Exception {
        context.turnOffAuthorisationSystem();
        subscribeTo(col);
        context.restoreAuthSystemState();

        subscriptionEmailNotificationService.perform(context, null, "content", "D");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(0, receivedMessages.length);
    }

    private void subscribeTo(Collection collection) throws Exception {
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        subscribeService.subscribe(context, eperson, collection, subscriptionParameterList, "content");
    }

    private void subscribeTo(Community community) throws Exception {
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        subscribeService.subscribe(context, eperson, community, subscriptionParameterList, "content");
    }

    private void setFakeLastModifiedOnItemSolrDocument(Context c, UUID itemId, ZonedDateTime lastModified)
            throws SolrServerException, IOException {
        SolrDocument d = searchService.getSolr()
                                      .query(new SolrQuery("search.resourceid:" + itemId.toString()))
                                      .getResults()
                                      .get(0);
        SolrInputDocument inputdoc = new SolrInputDocument();
        for (String name : d.getFieldNames()) {
            inputdoc.setField(name, d.getFieldValue(name));
        }
        inputdoc.setField("lastModified", lastModified.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        // remove, as it will be added again with the new value
        inputdoc.removeField("lastModified_dt");
        searchService.getSolr().add(inputdoc);
        searchService.getSolr().commit();
    }
}
