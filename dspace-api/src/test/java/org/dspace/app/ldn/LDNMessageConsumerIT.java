/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static org.dspace.app.ldn.LDNMessageEntity.QUEUE_STATUS_QUEUED;
import static org.dspace.matcher.NotifyServiceEntityMatcher.matchesNotifyServiceEntity;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.ldn.factory.NotifyServiceFactory;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.NotifyServiceBuilder;
import org.dspace.builder.NotifyServiceInboundPatternBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration Tests against {@link LDNMessageConsumer}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LDNMessageConsumerIT extends AbstractIntegrationTestWithDatabase {

    private Collection collection;
    private EPerson submitter;

    private LDNMessageService ldnMessageService = NotifyServiceFactory.getInstance().getLDNMessageService();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. create a normal user to use as submitter
        submitter = EPersonBuilder.createEPerson(context)
                                          .withEmail("submitter@example.com")
                                          .withPassword(password)
                                          .build();

        //2. A community with one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection 1")
                                                 .withSubmitterGroup(submitter)
                                                 .build();
        context.setCurrentUser(submitter);

        context.restoreAuthSystemState();
    }

    @Test
    public void testLDNMessageConsumerRequestReview() throws Exception {
        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        //3. a workspace item ready to go
        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("Submission Item")
                                .withIssueDate("2023-11-20")
                                .withCOARNotifyService(notifyService, "request-review")
                                .withFulltext("test.txt", "test", InputStream.nullInputStream())
                                .grantLicense()
                                .build();

        WorkflowItem workflowItem = workflowService.start(context, workspaceItem);
        Item item = workflowItem.getItem();
        context.dispatchEvents();
        context.restoreAuthSystemState();

        LDNMessageEntity ldnMessage =
            ldnMessageService.findAll(context).stream().findFirst().orElse(null);


        assertThat(notifyService, matchesNotifyServiceEntity(ldnMessage.getTarget()));
        assertEquals(workflowItem.getItem().getID(), ldnMessage.getObject().getID());
        assertEquals(QUEUE_STATUS_QUEUED, ldnMessage.getQueueStatus());
        assertNull(ldnMessage.getOrigin());
        assertNotNull(ldnMessage.getMessage());

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(ldnMessage.getMessage(), Notification.class);

        // check id
        assertThat(notification.getId(), containsString("urn:uuid:"));

        // check object
        assertEquals(notification.getObject().getId(),
            configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle());
        assertEquals(notification.getObject().getIetfCiteAs(),
            itemService.getMetadataByMetadataString(item, "dc.identifier.uri").get(0).getValue());
        assertEquals(notification.getObject().getUrl().getId(),
            configurationService.getProperty("dspace.ui.url") + "/bitstreams/" +
                item.getBundles(Constants.CONTENT_BUNDLE_NAME).get(0).getBitstreams().get(0).getID() + "/download");

        // check target
        assertEquals(notification.getTarget().getId(), notifyService.getUrl());
        assertEquals(notification.getTarget().getInbox(), notifyService.getLdnUrl());
        assertEquals(notification.getTarget().getType(), Set.of("Service"));

        // check origin
        assertEquals(notification.getOrigin().getId(), configurationService.getProperty("dspace.ui.url"));
        assertEquals(notification.getOrigin().getInbox(), configurationService.getProperty("ldn.notify.inbox"));
        assertEquals(notification.getOrigin().getType(), Set.of("Service"));

        // check actor
        assertEquals(notification.getActor().getId(), configurationService.getProperty("dspace.ui.url"));
        assertEquals(notification.getActor().getName(), configurationService.getProperty("dspace.name"));
        assertEquals(notification.getOrigin().getType(), Set.of("Service"));

        // check types
        assertEquals(notification.getType(), Set.of("coar-notify:ReviewAction", "Offer"));

    }

    @Test
    public void testLDNMessageConsumerRequestReviewAutomatic() throws Exception {
        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        NotifyServiceInboundPatternBuilder.createNotifyServiceInboundPatternBuilder(context, notifyService)
                                          .withPattern("request-review")
                                          .withConstraint("simple-demo_filter")
                                          .isAutomatic(true)
                                          .build();

        //3. a workspace item ready to go
        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("demo Item")
                                .withIssueDate("2023-11-20")
                                .withFulltext("test.txt", "test", InputStream.nullInputStream())
                                .grantLicense()
                                .build();

        WorkflowItem workflowItem = workflowService.start(context, workspaceItem);
        Item item = workflowItem.getItem();
        context.dispatchEvents();
        context.restoreAuthSystemState();

        LDNMessageEntity ldnMessage =
            ldnMessageService.findAll(context).stream().findFirst().orElse(null);


        assertThat(notifyService, matchesNotifyServiceEntity(ldnMessage.getTarget()));
        assertEquals(workflowItem.getItem().getID(), ldnMessage.getObject().getID());
        assertEquals(QUEUE_STATUS_QUEUED, ldnMessage.getQueueStatus());
        assertNull(ldnMessage.getOrigin());
        assertNotNull(ldnMessage.getMessage());

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(ldnMessage.getMessage(), Notification.class);

        // check id
        assertThat(notification.getId(), containsString("urn:uuid:"));

        // check object
        assertEquals(notification.getObject().getId(),
            configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle());
        assertEquals(notification.getObject().getIetfCiteAs(),
            itemService.getMetadataByMetadataString(item, "dc.identifier.uri").get(0).getValue());
        assertEquals(notification.getObject().getUrl().getId(),
            configurationService.getProperty("dspace.ui.url") + "/bitstreams/" +
                item.getBundles(Constants.CONTENT_BUNDLE_NAME).get(0).getBitstreams().get(0).getID() + "/download");

        // check target
        assertEquals(notification.getTarget().getId(), notifyService.getUrl());
        assertEquals(notification.getTarget().getInbox(), notifyService.getLdnUrl());
        assertEquals(notification.getTarget().getType(), Set.of("Service"));

        // check origin
        assertEquals(notification.getOrigin().getId(), configurationService.getProperty("dspace.ui.url"));
        assertEquals(notification.getOrigin().getInbox(), configurationService.getProperty("ldn.notify.inbox"));
        assertEquals(notification.getOrigin().getType(), Set.of("Service"));

        // check actor
        assertEquals(notification.getActor().getId(), configurationService.getProperty("dspace.ui.url"));
        assertEquals(notification.getActor().getName(), configurationService.getProperty("dspace.name"));
        assertEquals(notification.getOrigin().getType(), Set.of("Service"));

        // check types
        assertEquals(notification.getType(), Set.of("coar-notify:ReviewAction", "Offer"));

    }

    @Test
    public void testLDNMessageConsumerRequestEndorsement() throws Exception {
        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        //3. a workspace item ready to go
        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("Submission Item")
                                .withIssueDate("2023-11-20")
                                .withCOARNotifyService(notifyService, "request-endorsement")
                                .withFulltext("test.txt", "test", InputStream.nullInputStream())
                                .grantLicense()
                                .build();

        WorkflowItem workflowItem = workflowService.start(context, workspaceItem);
        Item item = workflowItem.getItem();
        context.dispatchEvents();
        context.restoreAuthSystemState();

        LDNMessageEntity ldnMessage =
            ldnMessageService.findAll(context).stream().findFirst().orElse(null);


        assertThat(notifyService, matchesNotifyServiceEntity(ldnMessage.getTarget()));
        assertEquals(workflowItem.getItem().getID(), ldnMessage.getObject().getID());
        assertEquals(QUEUE_STATUS_QUEUED, ldnMessage.getQueueStatus());
        assertNull(ldnMessage.getOrigin());
        assertNotNull(ldnMessage.getMessage());

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(ldnMessage.getMessage(), Notification.class);

        // check id
        assertThat(notification.getId(), containsString("urn:uuid:"));

        // check object
        assertEquals(notification.getObject().getId(),
            configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle());
        assertEquals(notification.getObject().getIetfCiteAs(),
            itemService.getMetadataByMetadataString(item, "dc.identifier.uri").get(0).getValue());
        assertEquals(notification.getObject().getUrl().getId(),
            configurationService.getProperty("dspace.ui.url") + "/bitstreams/" +
                item.getBundles(Constants.CONTENT_BUNDLE_NAME).get(0).getBitstreams().get(0).getID() + "/download");

        // check target
        assertEquals(notification.getTarget().getId(), notifyService.getUrl());
        assertEquals(notification.getTarget().getInbox(), notifyService.getLdnUrl());
        assertEquals(notification.getTarget().getType(), Set.of("Service"));

        // check origin
        assertEquals(notification.getOrigin().getId(), configurationService.getProperty("dspace.ui.url"));
        assertEquals(notification.getOrigin().getInbox(), configurationService.getProperty("ldn.notify.inbox"));
        assertEquals(notification.getOrigin().getType(), Set.of("Service"));

        // check actor
        assertEquals(notification.getActor().getId(), configurationService.getProperty("dspace.ui.url"));
        assertEquals(notification.getActor().getName(), configurationService.getProperty("dspace.name"));
        assertEquals(notification.getOrigin().getType(), Set.of("Service"));

        // check types
        assertEquals(notification.getType(), Set.of("coar-notify:EndorsementAction", "Offer"));

    }

    @Test
    public void testLDNMessageConsumerRequestIngest() throws Exception {
        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        //3. a workspace item ready to go
        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("Submission Item")
                                .withIssueDate("2023-11-20")
                                .withCOARNotifyService(notifyService, "request-ingest")
                                .withFulltext("test.txt", "test", InputStream.nullInputStream())
                                .grantLicense()
                                .build();

        WorkflowItem workflowItem = workflowService.start(context, workspaceItem);
        Item item = workflowItem.getItem();
        context.dispatchEvents();
        context.restoreAuthSystemState();

        LDNMessageEntity ldnMessage =
            ldnMessageService.findAll(context).stream().findFirst().orElse(null);


        assertThat(notifyService, matchesNotifyServiceEntity(ldnMessage.getTarget()));
        assertEquals(workflowItem.getItem().getID(), ldnMessage.getObject().getID());
        assertEquals(QUEUE_STATUS_QUEUED, ldnMessage.getQueueStatus());
        assertNull(ldnMessage.getOrigin());
        assertNotNull(ldnMessage.getMessage());

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(ldnMessage.getMessage(), Notification.class);

        // check id
        assertThat(notification.getId(), containsString("urn:uuid:"));

        // check object
        assertEquals(notification.getObject().getId(),
            configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle());
        assertEquals(notification.getObject().getIetfCiteAs(),
            itemService.getMetadataByMetadataString(item, "dc.identifier.uri").get(0).getValue());
        assertEquals(notification.getObject().getUrl().getId(),
            configurationService.getProperty("dspace.ui.url") + "/bitstreams/" +
                item.getBundles(Constants.CONTENT_BUNDLE_NAME).get(0).getBitstreams().get(0).getID() + "/download");

        // check target
        assertEquals(notification.getTarget().getId(), notifyService.getUrl());
        assertEquals(notification.getTarget().getInbox(), notifyService.getLdnUrl());
        assertEquals(notification.getTarget().getType(), Set.of("Service"));

        // check origin
        assertEquals(notification.getOrigin().getId(), configurationService.getProperty("dspace.ui.url"));
        assertEquals(notification.getOrigin().getInbox(), configurationService.getProperty("ldn.notify.inbox"));
        assertEquals(notification.getOrigin().getType(), Set.of("Service"));

        // check actor
        assertEquals(notification.getActor().getId(), configurationService.getProperty("dspace.ui.url"));
        assertEquals(notification.getActor().getName(), configurationService.getProperty("dspace.name"));
        assertEquals(notification.getOrigin().getType(), Set.of("Service"));

        // check types
        assertEquals(notification.getType(), Set.of("coar-notify:IngestAction", "Offer"));

    }

    @Test
    public void testLDNMessageConsumerRequestFake() throws Exception {
        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        //3. a workspace item ready to go
        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("Submission Item")
                                .withIssueDate("2023-11-20")
                                .withCOARNotifyService(notifyService, "request-fake")
                                .withFulltext("test.txt", "test", InputStream.nullInputStream())
                                .grantLicense()
                                .build();

        workflowService.start(context, workspaceItem);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        LDNMessageEntity ldnMessage =
            ldnMessageService.findAll(context).stream().findFirst().orElse(null);

        assertNull(ldnMessage);

    }

    @Test
    public void testLDNMessageConsumerNoRequests() throws Exception {
        context.turnOffAuthorisationSystem();

        //3. a workspace item ready to go
        WorkspaceItem workspaceItem =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("Submission Item")
                                .withIssueDate("2023-11-20")
                                .grantLicense()
                                .build();

        workflowService.start(context, workspaceItem);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        LDNMessageEntity ldnMessage =
            ldnMessageService.findAll(context).stream().findFirst().orElse(null);

        assertNull(ldnMessage);
    }

    @Override
    @After
    public void destroy() throws Exception {
        List<LDNMessageEntity> ldnMessageEntities = ldnMessageService.findAll(context);
        if (CollectionUtils.isNotEmpty(ldnMessageEntities)) {
            ldnMessageEntities.forEach(ldnMessage -> {
                try {
                    ldnMessageService.delete(context, ldnMessage);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        super.destroy();
    }
}

