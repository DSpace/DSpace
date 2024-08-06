/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.dspace.app.rest.matcher.SubmissionCOARNotifyMatcher;
import org.dspace.app.rest.repository.SubmissionCoarNotifyRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.coarnotify.NotifyPattern;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration test class for {@link SubmissionCoarNotifyRestRepository}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class SubmissionCOARNotifyRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllTestUnAuthorized() throws Exception {
        getClient().perform(get("/api/config/submissioncoarnotifyconfigs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncoarnotifyconfigs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.submissioncoarnotifyconfigs", Matchers.containsInAnyOrder(
                        SubmissionCOARNotifyMatcher.matchCOARNotifyEntry("coarnotify", List.of(
                            new NotifyPattern("request-review", true),
                            new NotifyPattern("request-endorsement", true),
                            new NotifyPattern("request-ingest", false)))
                )));
    }

    @Test
    public void findOneTestUnAuthorized() throws Exception {
        getClient().perform(get("/api/config/submissioncoarnotifyconfigs/coarnotify"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneTestNonExistingCOARNotify() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncoarnotifyconfigs/non-existing-coar"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncoarnotifyconfigs/coarnotify"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        SubmissionCOARNotifyMatcher.matchCOARNotifyEntry("coarnotify", List.of(
                            new NotifyPattern("request-review", true),
                            new NotifyPattern("request-endorsement", true),
                            new NotifyPattern("request-ingest", false)))
                )));
    }

}
