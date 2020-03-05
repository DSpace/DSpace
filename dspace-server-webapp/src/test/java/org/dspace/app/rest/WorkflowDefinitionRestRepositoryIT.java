/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

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
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.WorkflowDefinitionMatcher;
import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.repository.WorkflowDefinitionRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Workflow;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration tests for the {@link WorkflowDefinitionRestRepository}
 * and {@link WorkflowDefinitionCollectionsLinkRepository} controlled endpoints
 *
 * @author Maria Verdonck (Atmire) on 17/12/2019
 */
public class WorkflowDefinitionRestRepositoryIT extends AbstractControllerIntegrationTest {

    private XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();

    private static final String WORKFLOW_DEFINITIONS_ENDPOINT
        = "/api/" + WorkflowDefinitionRest.CATEGORY + "/" + WorkflowDefinitionRest.NAME_PLURAL;

    @Test
    public void getAllWorkflowDefinitionsEndpoint() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        List<Workflow> allConfiguredWorkflows = xmlWorkflowFactory.getAllConfiguredWorkflows();
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            //Number of total workflows is equals to number of configured workflows
            .andExpect(jsonPath("$.page.totalElements", is(allConfiguredWorkflows.size())))
            //There needs to be a self link to this endpoint
            .andExpect(jsonPath("$._links.self.href", containsString(WORKFLOW_DEFINITIONS_ENDPOINT)));
    }

    @Test
    public void getAllWorkflowDefinitionsEndpoint_Pagination_Size1() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        List<Workflow> allConfiguredWorkflows = xmlWorkflowFactory.getAllConfiguredWorkflows();
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT)
            .param("size", "1"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            //Number of total workflows is equals to number of configured workflows
            .andExpect(jsonPath("$.page.totalElements", is(allConfiguredWorkflows.size())))
            //Page size is 1
            .andExpect(jsonPath("$.page.size", is(1)))
            //Page nr is 1
            .andExpect(jsonPath("$.page.number", is(0)))
            //Contains only the first configured workflow
            .andExpect(jsonPath("$._embedded.workflowdefinitions", Matchers.contains(
                WorkflowDefinitionMatcher.matchWorkflowDefinitionEntry(allConfiguredWorkflows.get(0))
            )))
            //Doesn't contain the other workflows
            .andExpect(jsonPath("$._embedded.workflowdefinitions", Matchers.not(
                Matchers.contains(
                    WorkflowDefinitionMatcher.matchWorkflowDefinitionEntry(allConfiguredWorkflows.get(1))
                )
            )));
    }

    @Test
    public void getAllWorkflowDefinitionsEndpoint_Pagination_Size1_Page1() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        List<Workflow> allConfiguredWorkflows = xmlWorkflowFactory.getAllConfiguredWorkflows();
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT)
            .param("size", "1")
            .param("page", "1"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            //Number of total workflows is equals to number of configured workflows
            .andExpect(jsonPath("$.page.totalElements", is(allConfiguredWorkflows.size())))
            //Page size is 1
            .andExpect(jsonPath("$.page.size", is(1)))
            //Page nr is 2
            .andExpect(jsonPath("$.page.number", is(1)))
            //Contains only the second configured workflow
            .andExpect(jsonPath("$._embedded.workflowdefinitions", Matchers.contains(
                WorkflowDefinitionMatcher.matchWorkflowDefinitionEntry(allConfiguredWorkflows.get(1))
            )))
            //Doesn't contain 1st configured workflow
            .andExpect(jsonPath("$._embedded.workflowdefinitions", Matchers.not(
                Matchers.contains(
                    WorkflowDefinitionMatcher.matchWorkflowDefinitionEntry(allConfiguredWorkflows.get(0))
                )
            )));
    }

    @Test
    public void getAllWorkflowDefinitionsEndpoint_NonValidToken() throws Exception {
        String token = "NonValidToken";
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT))
            //We expect a 403 Forbidden status
            .andExpect(status().isForbidden());
    }

    @Test
    public void getAllWorkflowDefinitionsEndpoint_NoToken() throws Exception {
        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT))
            //We expect a 401 Unauthorized
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getWorkflowDefinitionByName_DefaultWorkflow() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        String workflowName = defaultWorkflow.getID();
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + workflowName))
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
        String token = getAuthToken(eperson.getEmail(), password);
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
            getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + firstNonDefaultWorkflowName))
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
        String token = getAuthToken(eperson.getEmail(), password);
        String workflowName = "TestNameNonExistentWorkflow9999";
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + workflowName))
            //We expect a 404 Not Found status
            .andExpect(status().isNotFound());
    }

    @Test
    public void getWorkflowDefinitionByName_DefaultWorkflow_NonValidToken() throws Exception {
        String token = "UnvalidToken";
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        String workflowName = defaultWorkflow.getID();
        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + workflowName))
            //We expect a 403 Forbidden status
            .andExpect(status().isForbidden());
    }

    @Test
    public void getWorkflowDefinitionByName_DefaultWorkflow_NoToken() throws Exception {
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        String workflowName = defaultWorkflow.getID();
        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + workflowName))
            //We expect a 401 Unauthorized
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getWorkflowDefinitionByCollectionId_ExistentCollection() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
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
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/search/findByCollection?uuid=" + col1.getID()))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            // its name is name of corresponding workflow
            .andExpect(jsonPath("$.name", equalToIgnoringCase(workflowForThisCollection.getID())));
    }

    @Test
    public void getWorkflowDefinitionByCollectionId_nonValidUUID() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String nonValidUUID = "TestNonValidUUID";

        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/search/findByCollection?uuid=" + nonValidUUID))
            //We expect a 400 Illegal Argument Exception (Bad Request) cannot convert UUID
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(containsString("Failed to convert " + nonValidUUID)));
    }

    @Test
    public void getWorkflowDefinitionByCollectionId_nonExistentCollection() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        UUID nonExistentCollectionUUID = UUID.randomUUID();

        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/search/findByCollection?uuid="
            + nonExistentCollectionUUID))
            //We expect a 404 Not Found status
            .andExpect(status().isNotFound());
    }

    @Test
    public void getCollectionsOfWorkflowByName_DefaultWorkflow_AllNonMappedCollections() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        List<Collection> allNonMappedCollections = xmlWorkflowFactory.getAllNonMappedCollectionsHandles(context);

        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + defaultWorkflow.getID()
            + "/collections"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            //Number of total workflows is equals to number of non-mapped collections
            .andExpect(jsonPath("$.page.totalElements", is(allNonMappedCollections.size())));
    }

    @Test
    public void getCollectionsOfWorkflowByName_DefaultWorkflow_AllNonMappedCollections_Paginated_Size1()
        throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 1")
            .build();
        Collection col2 = CollectionBuilder.createCollection(context, child1, "123456789/non-mapped-collection")
            .withName("Collection 2")
            .build();
        context.restoreAuthSystemState();

        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        List<Collection> allNonMappedCollections = xmlWorkflowFactory.getAllNonMappedCollectionsHandles(context);

        if (allNonMappedCollections.size() > 0) {
            Collection firstNonMappedCollection = allNonMappedCollections.get(0);

            //When we call this facets endpoint
            getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + defaultWorkflow.getID()
                + "/collections")
                .param("size", "1"))
                //We expect a 200 OK status
                .andExpect(status().isOk())
                //Number of total workflows is equals to number of configured workflows
                .andExpect(jsonPath("$.page.totalElements", is(allNonMappedCollections.size())))
                //Page size is 1
                .andExpect(jsonPath("$.page.size", is(1)))
                //Page nr is 1
                .andExpect(jsonPath("$.page.number", is(0)))
                //Contains only the first non-mapped collection
                .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                    WorkflowDefinitionMatcher.matchCollectionEntry(firstNonMappedCollection.getName(),
                        firstNonMappedCollection.getID(), firstNonMappedCollection.getHandle())
                )));
        }
    }

    @Test
    public void getCollectionsOfWorkflowByName_NonDefaultWorkflow() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
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
        // Collection with handle used in workflow.xml!
        Collection col1 = CollectionBuilder.createCollection(context, child1, "123456789/workflow-test-1")
            .withName("Collection 1")
            .build();
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
            List<Collection> mappedCollections
                = xmlWorkflowFactory.getCollectionHandlesMappedToWorklow(context, firstNonDefaultWorkflowName);
            //When we call this facets endpoint
            if (mappedCollections.size() > 0) {
                //returns array of collection jsons that are mapped to given workflow
                //When we call this facets endpoint
                Collection firstMappedCollection = mappedCollections.get(0);
                getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + firstNonDefaultWorkflowName
                    + "/collections")
                    .param("size", "1"))
                    //We expect a 200 OK status
                    .andExpect(status().isOk())
                    //Number of total workflows is equals to number of configured workflows
                    .andExpect(jsonPath("$.page.totalElements", is(mappedCollections.size())))
                    //Page size is 1
                    .andExpect(jsonPath("$.page.size", is(1)))
                    //Page nr is 1
                    .andExpect(jsonPath("$.page.number", is(0)))
                    //Contains only the first mapped collection
                    .andExpect(jsonPath("$._embedded.collections", Matchers.contains(
                        WorkflowDefinitionMatcher.matchCollectionEntry(firstMappedCollection.getName(),
                            firstMappedCollection.getID(), firstMappedCollection.getHandle())
                    )));
            } else {
                //no collections mapped to this workflow
                getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/"
                    + firstNonDefaultWorkflowName + "/collections"))
                    //We expect a 200 OK status
                    .andExpect(status().isOk())
                    //results in empty list
                    .andExpect(jsonPath("$._embedded.collections", empty()));
            }
        }
    }

    @Test
    public void getCollectionsOfWorkflowByName_NonExistentWorkflow() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String workflowName = "TestNameNonExistentWorkflow9999";

        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + workflowName + "/collections"))
            //We expect a 404 Not Found
            .andExpect(status().isNotFound());
    }

    @Test
    public void getCollectionsOfWorkflowByName_DefaultWorkflow_NoValidToken() throws Exception {
        String token = "NonValidToken";
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();

        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + defaultWorkflow.getID()
            + "/collections"))
            //We expect a 403 Forbidden status
            .andExpect(status().isForbidden());
    }

    @Test
    public void getCollectionsOfWorkflowByName_DefaultWorkflow_NoToken() throws Exception {
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        List<Collection> allNonMappedCollections = xmlWorkflowFactory.getAllNonMappedCollectionsHandles(context);

        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + defaultWorkflow.getID()
            + "/collections"))
            //We expect a 401 Unauthorized
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getStepsOfWorkflowByName_DefaultWorkflow() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();

        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + defaultWorkflow.getID()
            + "/steps")
            .param("projection", "full"))
            //We expect a 200 OK status
            .andExpect(status().isOk())
            //Number of total workflows is equals to number of non-mapped collections
            .andExpect(jsonPath("$.page.totalElements", is(defaultWorkflow.getSteps().size())));
    }

    @Test
    public void getStepsOfWorkflowByName_DefaultWorkflow_NoValidToken() throws Exception {
        String token = "NonValidToken";
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();

        //When we call this facets endpoint
        getClient(token).perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + defaultWorkflow.getID()
            + "/steps"))
            //We expect a 403 Forbidden status
            .andExpect(status().isForbidden());
    }

    @Test
    public void getStepsOfWorkflowByName_DefaultWorkflow_NoToken() throws Exception {
        Workflow defaultWorkflow = xmlWorkflowFactory.getDefaultWorkflow();
        List<Collection> allNonMappedCollections = xmlWorkflowFactory.getAllNonMappedCollectionsHandles(context);

        //When we call this facets endpoint
        getClient().perform(get(WORKFLOW_DEFINITIONS_ENDPOINT + "/" + defaultWorkflow.getID()
            + "/steps"))
            //We expect a 401 Unauthorized
            .andExpect(status().isUnauthorized());
    }
}
