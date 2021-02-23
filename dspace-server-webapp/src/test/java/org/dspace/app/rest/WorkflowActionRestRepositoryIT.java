/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.WorkflowActionMatcher;
import org.dspace.app.rest.model.WorkflowActionRest;
import org.dspace.app.rest.repository.WorkflowActionRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration tests for the {@link WorkflowActionRestRepository} controlled endpoints
 *
 * @author Maria Verdonck (Atmire) on 06/01/2020
 */
public class WorkflowActionRestRepositoryIT extends AbstractControllerIntegrationTest {

    private XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();

    private static final String WORKFLOW_ACTIONS_ENDPOINT
        = "/api/" + WorkflowActionRest.CATEGORY + "/" + WorkflowActionRest.NAME_PLURAL;

    @Test
    public void getAllWorkflowActions_NonImplementedEndpoint() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT))
            //We expect a 405 Method not allowed status
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void getAllWorkflowActions_NonImplementedEndpoint_NonValidToken() throws Exception {
        String token = "nonValidToken";
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT))
            //We expect a 403 Forbidden status
            .andExpect(status().isForbidden());
    }

    @Test
    public void getAllWorkflowActions_NonImplementedEndpoint_NoToken() throws Exception {
        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_ACTIONS_ENDPOINT))
            //We expect a 401 Unauthorized
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getWorkflowActionByName_NonExistentWorkflowAction() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String nameNonExistentWorkflowActionName = "TestNameNonExistentWorkflowAction9999";
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT + "/" + nameNonExistentWorkflowActionName))
            //We expect a 404 Not Found status
            .andExpect(status().isNotFound());
    }

    @Test
    public void getWorkflowActionByName_ExistentWithOptions_editaction() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String nameActionWithOptions = "editaction";
        WorkflowActionConfig existentWorkflow = xmlWorkflowFactory.getActionByName(nameActionWithOptions);
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT + "/" + nameActionWithOptions))
            //We expect a 200 is ok status
            .andExpect(status().isOk())
            // has options
            .andExpect(jsonPath("$.options", not(empty())))
            //Matches expected corresponding rest action values
            .andExpect(jsonPath("$", Matchers.is(
                WorkflowActionMatcher.matchWorkflowActionEntry(existentWorkflow)
            )));
    }

    @Test
    public void getWorkflowActionByName_ExistentWithoutOptions_claimaction() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String nameActionWithoutOptions = "claimaction";
        WorkflowActionConfig existentWorkflowNoOptions = xmlWorkflowFactory.getActionByName(nameActionWithoutOptions);
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT + "/" + nameActionWithoutOptions))
            //We expect a 200 is ok status
            .andExpect(status().isOk())
            // has no options
            .andExpect(jsonPath("$.options", empty()))
            //Matches expected corresponding rest action values
            .andExpect(jsonPath("$", Matchers.is(
                WorkflowActionMatcher.matchWorkflowActionEntry(existentWorkflowNoOptions)
            )));
    }

    @Test
    public void getWorkflowActionByName_ExistentWithOptions_NonValidToken() throws Exception {
        String token = "nonValidToken";
        String nameActionWithOptions = "editaction";
        WorkflowActionConfig existentWorkflow = xmlWorkflowFactory.getActionByName(nameActionWithOptions);
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_ACTIONS_ENDPOINT + "/" + nameActionWithOptions))
            //We expect a 403 Forbidden status
            .andExpect(status().isForbidden());
    }

    @Test
    public void getWorkflowActionByName_ExistentWithOptions_NoToken() throws Exception {
        String nameActionWithOptions = "editaction";
        WorkflowActionConfig existentWorkflow = xmlWorkflowFactory.getActionByName(nameActionWithOptions);
        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_ACTIONS_ENDPOINT + "/" + nameActionWithOptions))
            //We expect a 401 Unauthorized
            .andExpect(status().isUnauthorized());
    }
}
