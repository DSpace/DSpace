/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.WorkflowStepMatcher;
import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.app.rest.repository.WorkflowStepRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration tests for the {@link WorkflowStepRestRepository} controlled endpoints
 *
 * @author Maria Verdonck (Atmire) on 13/01/2020
 */
public class WorkflowStepRestRepositoryIT extends AbstractControllerIntegrationTest {

    private XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();

    private static final String WORKFLOW_ACTIONS_ENDPOINT
        = "/api/" + WorkflowStepRest.CATEGORY + "/" + WorkflowStepRest.NAME_PLURAL;

    @Test
    public void getAllWorkflowSteps_NonImplementedEndpoint() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT))
            //We expect a 405 Method not allowed status
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void getAllWorkflowSteps_NonImplementedEndpoint_NonValidToken() throws Exception {
        String token = "NonValidToken";
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT))
            //We expect a 403 Forbidden status
            .andExpect(status().isForbidden());
    }

    @Test
    public void getAllWorkflowSteps_NonImplementedEndpoint_NoToken() throws Exception {
        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_ACTIONS_ENDPOINT))
            //We expect a 401 Unauthorized
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getWorkflowStepByName_NonExistentWorkflowStep() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String nameNonExistentWorkflowActionName = "TestNameNonExistentWorkflowStep9999";
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT + "/" + nameNonExistentWorkflowActionName))
            //We expect a 404 Not Found status
            .andExpect(status().isNotFound());
    }

    @Test
    public void getWorkflowStepByName_ExistentStep_reviewstep() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String nameStep = "reviewstep";
        Step existentStep = xmlWorkflowFactory.getStepByName(nameStep);
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT + "/" + nameStep)
            .param("projection", "full"))
            //We expect a 200 is ok status
            .andExpect(status().isOk())
            //Matches expected step
            .andExpect(jsonPath("$", Matchers.is(
                WorkflowStepMatcher.matchWorkflowStepEntry(existentStep)
            )));
    }
}
