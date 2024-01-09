/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import static org.dspace.app.ldn.action.ActionStatus.ABORT;
import static org.dspace.app.ldn.action.ActionStatus.CONTINUE;
import static org.hamcrest.Matchers.any;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.factory.NotifyServiceFactory;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.NotifyServiceBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Integration Tests against {@link SendLDNMessageAction}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class SendLDNMessageActionIT extends AbstractIntegrationTestWithDatabase {

    private Collection collection;
    private EPerson submitter;

    private LDNMessageService ldnMessageService = NotifyServiceFactory.getInstance().getLDNMessageService();
    private WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
    private SendLDNMessageAction sendLDNMessageAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        sendLDNMessageAction = new SendLDNMessageAction();
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
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.ACCEPTED).build();
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity("https://notify-inbox.info/inbox/", any(Object.class), String.class)).thenReturn(response);
        ObjectMapper mapper = new ObjectMapper();

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("https://www.notify-inbox.info/")
                                .withLdnUrl("https://notify-inbox.info/inbox/")
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

        ldnMessage.getQueueStatus();

        Notification notification = mapper.readValue(ldnMessage.getMessage(), Notification.class);

        assertEquals(sendLDNMessageAction.execute(context, notification, item), CONTINUE);
    }

    @Test
    public void testLDNMessageConsumerRequestReviewGotRedirection() throws Exception {
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.ACCEPTED).build();
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity("https://notify-inbox.info/inbox", any(Object.class), String.class)).thenReturn(response);
        ObjectMapper mapper = new ObjectMapper();

        context.turnOffAuthorisationSystem();

        // ldnUrl should be https://notify-inbox.info/inbox/
        // but used https://notify-inbox.info/inbox for redirection
        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("https://www.notify-inbox.info/")
                                .withLdnUrl("https://notify-inbox.info/inbox")
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

        ldnMessage.getQueueStatus();

        Notification notification = mapper.readValue(ldnMessage.getMessage(), Notification.class);

        assertEquals(sendLDNMessageAction.execute(context, notification, item), CONTINUE);
    }

    @Test
    public void testLDNMessageConsumerRequestReviewWithInvalidLdnUrl() throws Exception {
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity("https://notify-inbox.info/inbox/", any(Object.class), String.class)).thenReturn(response);
        //sendLDNMessageAction.setRestTemplate(mockRestTemplate);
        ObjectMapper mapper = new ObjectMapper();

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("https://www.notify-inbox.info/")
                                .withLdnUrl("https://notify-inbox.info/invalidLdnUrl/")
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

        ldnMessage.getQueueStatus();

        Notification notification = mapper.readValue(ldnMessage.getMessage(), Notification.class);

        assertEquals(sendLDNMessageAction.execute(context, notification, item), ABORT);
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

