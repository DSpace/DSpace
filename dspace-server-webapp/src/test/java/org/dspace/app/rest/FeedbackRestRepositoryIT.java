/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.model.FeedbackRest;
import org.dspace.app.rest.repository.FeedbackRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.FeedbackServiceImpl;
import org.dspace.content.service.FeedbackService;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class for the feedback endpoint
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class FeedbackRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private FeedbackRestRepository feedbackRestRepository;

    @Test
    public void findAllTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/tools/feedbacks"))
                            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/tools/feedbacks/1"))
                            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void sendFeedbackTest() throws Exception {
        configurationService.setProperty("feedback.recipient", "recipient.email@test.com");
        FeedbackService originFeedbackService = feedbackRestRepository.getFeedbackService();
        try {
            FeedbackService feedbackServiceMock = mock (FeedbackServiceImpl.class);
            feedbackRestRepository.setFeedbackService(feedbackServiceMock);

            ObjectMapper mapper = new ObjectMapper();
            FeedbackRest feedbackRest = new FeedbackRest();

            feedbackRest.setEmail("misha.boychuk@test.com");
            feedbackRest.setMessage("My feedback!");

            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(post("/api/tools/feedbacks")
                                .content(mapper.writeValueAsBytes(feedbackRest))
                                .contentType(contentType))
                                .andExpect(status().isCreated());

            verify(feedbackServiceMock).sendEmail(isNotNull(), isNotNull(), eq("recipient.email@test.com"),
                    eq("misha.boychuk@test.com"), eq("My feedback!"), isNull());

            verifyNoMoreInteractions(feedbackServiceMock);
        } finally {
            feedbackRestRepository.setFeedbackService(originFeedbackService);
        }

    }

    @Test
    public void sendFeedbackWithRecipientEmailNotConfiguredTest() throws Exception {
        configurationService.setProperty("feedback.recipient", null);
        FeedbackService originFeedbackService = feedbackRestRepository.getFeedbackService();
        try {
            FeedbackService feedbackServiceMock = mock (FeedbackServiceImpl.class);
            feedbackRestRepository.setFeedbackService(feedbackServiceMock);
            ObjectMapper mapper = new ObjectMapper();
            FeedbackRest feedbackRest = new FeedbackRest();

            feedbackRest.setEmail("misha.boychuk@test.com");
            feedbackRest.setMessage("My feedback!");

            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(post("/api/tools/feedbacks")
                                .content(mapper.writeValueAsBytes(feedbackRest))
                                .contentType(contentType))
                                .andExpect(status().isNotFound());

            verifyNoMoreInteractions(feedbackServiceMock);
        } finally {
            feedbackRestRepository.setFeedbackService(originFeedbackService);
        }
    }

    @Test
    public void sendFeedbackBadRequestTest() throws Exception {
        FeedbackService originFeedbackService = feedbackRestRepository.getFeedbackService();
        try {
            FeedbackService feedbackServiceMock = mock (FeedbackServiceImpl.class);
            feedbackRestRepository.setFeedbackService(feedbackServiceMock);
            ObjectMapper mapper = new ObjectMapper();
            FeedbackRest feedbackRest = new FeedbackRest();

            feedbackRest.setMessage("My feedback!");

            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(post("/api/tools/feedbacks")
                                .content(mapper.writeValueAsBytes(feedbackRest))
                                .contentType(contentType))
                                .andExpect(status().isBadRequest());

            verifyNoMoreInteractions(feedbackServiceMock);
        } finally {
            feedbackRestRepository.setFeedbackService(originFeedbackService);
        }
    }

}