/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.repository.WorkflowDefinitionRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Workflow;
import org.json.JSONArray;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for the {@link WorkflowDefinitionRestRepository} and {@link WorkflowDefinitionController} controlled endpoints
 *
 * @author Maria Verdonck (Atmire) on 17/12/2019
 */
public class WorkflowDefinitionRestRepositoryIT extends AbstractControllerIntegrationTest {

    private static final Logger log
            = org.apache.logging.log4j.LogManager.getLogger(WorkflowDefinitionRestRepositoryIT.class);

    private XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();

    private static final String WORKFLOW_DEFINITIONS_ENDPOINT
            = "/api/" + WorkflowDefinitionRest.CATEGORY + "/" + WorkflowDefinitionRest.NAME_PLURAL;

    @Test
    public void getAllWorkflowDefinitionsEndpoint() throws Exception {
        List<Workflow> allConfiguredWorkflows = xmlWorkflowFactory.getAllConfiguredWorkflows();
        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT))
                //We expect a 200 OK status
                .andExpect(status().isOk())
                //Number of total workflows is equals to number of configured workflows
                .andExpect(jsonPath("$.page.totalElements", is(allConfiguredWorkflows.size())))
                //There needs to be a self link to this endpoint
                .andExpect(jsonPath("$._links.self.href", containsString(WORKFLOW_DEFINITIONS_ENDPOINT)));
    }

    @Test
    public void getWorkflowDefinitionByName_DefaultWorkflow() throws Exception {
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        String workflowName = defaultWorkflow.getID();
        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + workflowName))
                //We expect a 200 OK status
                .andExpect(status().isOk())
                //There needs to be a self link to this endpoint
                .andExpect(jsonPath("$._links.self.href", containsString(WORKFLOW_DEFINITIONS_ENDPOINT)))
                // its name is default
                .andExpect(jsonPath("$.name", equalToIgnoringCase(workflowName)))
                // is default
                .andExpect(jsonPath("$.isDefault", is(true)));
    }

    @Test
    public void getWorkflowDefinitionByName_NonDefaultWorkflow() throws Exception {
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        List<Workflow> allConfiguredWorkflows = xmlWorkflowFactory.getAllConfiguredWorkflows();
        String firstNonDefaultWorkflowName = "";
        for (Workflow workflow : allConfiguredWorkflows) {
            if (!workflow.getID().equalsIgnoreCase(defaultWorkflow.getID())) {
                firstNonDefaultWorkflowName = workflow.getID();
            }
        }
        if (StringUtils.isNotBlank(firstNonDefaultWorkflowName)) {
            //When we call this facets endpoint
            getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + firstNonDefaultWorkflowName))
                    //We expect a 200 OK status
                    .andExpect(status().isOk())
                    //There needs to be a self link to this endpoint
                    .andExpect(jsonPath("$._links.self.href", containsString(WORKFLOW_DEFINITIONS_ENDPOINT)))
                    // its name is name of non-default workflow
                    .andExpect(jsonPath("$.name", equalToIgnoringCase(firstNonDefaultWorkflowName)))
                    // is not default
                    .andExpect(jsonPath("$.isDefault", is(false)));
        }
    }

    @Test
    public void getWorkflowDefinitionByName_NonExistentWorkflow() throws Exception {
        String workflowName = "TestNameNonExistentWorkflow9999";
        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + workflowName))
                //We expect a 404 Not Found status
                .andExpect(status().isNotFound());
    }

    @Test
    public void getWorkflowDefinitionByCollectionId_ExistentCollection() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        context.restoreAuthSystemState();

        Workflow workflowForThisCollection = xmlWorkflowFactory.getWorkflow(col1);

        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/search/findByCollection?uuid=" + col1.getID()))
                //We expect a 200 OK status
                .andExpect(status().isOk())
                // its name is name of corresponding workflow
                .andExpect(jsonPath("$.name", equalToIgnoringCase(workflowForThisCollection.getID())));
    }

    @Test
    public void getWorkflowDefinitionByCollectionId_nonValidUUID() throws Exception {
        String nonValidUUID = "TestNonValidUUID";

        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/search/findByCollection?uuid=" + nonValidUUID))
                //We expect a 422 Unprocessable Entity status
                .andExpect(status().is(422));
    }

    @Test
    public void getWorkflowDefinitionByCollectionId_nonExistentCollection() throws Exception {
        UUID nonExistentCollectionUUID = UUID.randomUUID();

        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/search/findByCollection?uuid="
                + nonExistentCollectionUUID))
                //We expect a 404 Not Found status
                .andExpect(status().isNotFound());
    }

    @Test
    public void getCollectionsOfWorkflowByName_DefaultWorkflow_EmptyList() throws Exception {
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();

        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + defaultWorkflow.getID()
                + "/collections"))
                //We expect a 200 OK status
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    public void getCollectionsOfWorkflowByName_NonDefaultWorkflow() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1, "123456789/4").withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1, "123456789/5").withName("Collection 1").build();
        // until handle 123456789/5 used in example in workflow.xml (if uncommented)
        context.restoreAuthSystemState();

        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        List<Workflow> allConfiguredWorkflows = xmlWorkflowFactory.getAllConfiguredWorkflows();
        String firstNonDefaultWorkflowName = "";
        for (Workflow workflow : allConfiguredWorkflows) {
            if (!workflow.getID().equalsIgnoreCase(defaultWorkflow.getID())) {
                firstNonDefaultWorkflowName = workflow.getID();
            }
        }

        if (StringUtils.isNotBlank(firstNonDefaultWorkflowName)) {
            List<String> handlesOfMappedCollections
                    = xmlWorkflowFactory.getCollectionHandlesMappedToWorklow(firstNonDefaultWorkflowName);
            //When we call this facets endpoint
            if (handlesOfMappedCollections.size() > 0) {
                //returns array of collection jsons that are mapped to given workflow
                MvcResult result = getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/"
                        + firstNonDefaultWorkflowName + "/collections")).andReturn();
                String response = result.getResponse().getContentAsString();
                JSONArray collectionsResult = new JSONArray(response);
                assertEquals(collectionsResult.length(), handlesOfMappedCollections.size());
            } else {
                //no collections mapped to this workflow
                getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/"
                        + firstNonDefaultWorkflowName + "/collections"))
                        //We expect a 200 OK status
                        .andExpect(status().isOk())
                        //results in empty list
                        .andExpect(jsonPath("$", empty()));
            }
        }
    }

    @Test
    public void getCollectionsOfWorkflowByName_NonExistentWorkflow() throws Exception {
        String workflowName = "TestNameNonExistentWorkflow9999";

        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + workflowName + "/collections"))
                //We expect a 404 Not Found status
                .andExpect(status().isNotFound());
    }
}
