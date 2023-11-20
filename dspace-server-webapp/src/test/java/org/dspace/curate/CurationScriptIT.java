/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ScriptService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * IT for {@link Curation}
 *
 * @author Maria Verdonck (Atmire) on 24/06/2020
 */
public class CurationScriptIT extends AbstractControllerIntegrationTest {

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Autowired
    private ScriptService scriptService;

    private final static String SCRIPTS_ENDPOINT = "/api/" + ScriptRest.CATEGORY + "/" + ScriptRest.PLURAL_NAME;
    private final static String CURATE_SCRIPT_ENDPOINT = SCRIPTS_ENDPOINT + "/curate/" + ProcessRest.PLURAL_NAME;

    @Test
    public void curateScript_invalidTaskOption() throws Exception {
        context.turnOffAuthorisationSystem();

        String token = getAuthToken(admin.getEmail(), password);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-t", "invalidTaskOption"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        // Request with -t <invalidTaskOption>
        getClient(token)
            .perform(multipart(CURATE_SCRIPT_ENDPOINT)
                         .param("properties", new ObjectMapper().writeValueAsString(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest());
    }

    @Test
    public void curateScript_MissingHandle() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-t", CurationClientOptions.getTaskOptions().get(0)));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        // Request with missing required -i <handle>
        getClient(token)
            .perform(multipart(CURATE_SCRIPT_ENDPOINT)
                         .param("properties", new ObjectMapper().writeValueAsString(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest());
    }

    @Test
    public void curateScript_invalidHandle() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-i", "invalidhandle"));
        parameters.add(new DSpaceCommandLineParameter("-t", CurationClientOptions.getTaskOptions().get(0)));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        // Request with missing required -i <handle>
        getClient(token)
            .perform(multipart(CURATE_SCRIPT_ENDPOINT)
                         .param("properties", new ObjectMapper().writeValueAsString(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest());
    }

    @Test
    public void curateScript_MissingTaskOrTaskFile() throws Exception {
        context.turnOffAuthorisationSystem();

        String token = getAuthToken(admin.getEmail(), password);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        // Request without -t <task> or -T <taskFile> (and no -q <queue>)
        getClient(token)
            .perform(multipart(CURATE_SCRIPT_ENDPOINT)
                         .param("properties", new ObjectMapper().writeValueAsString(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest());
    }

    @Test
    public void curateScript_InvalidScope() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-i", "all"));
        parameters.add(new DSpaceCommandLineParameter("-s", "invalidScope"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        // Request with invalid -s <scope>; must be object, curation or open
        getClient(token)
            .perform(multipart(CURATE_SCRIPT_ENDPOINT)
                         .param("properties", new ObjectMapper().writeValueAsString(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest());
    }

    @Test
    public void curateScript_InvalidTaskFile() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-i", "all"));
        parameters.add(new DSpaceCommandLineParameter("-T", "invalidTaskFile"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        // Request with invalid -s <scope>; must be object, curation or open
        getClient(token)
            .perform(multipart(CURATE_SCRIPT_ENDPOINT)
                         .param("properties", new ObjectMapper().writeValueAsString(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest());
    }

    @Test
    public void curateScript_validRequest_Task() throws Exception {
        context.turnOffAuthorisationSystem();

        String token = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-t", CurationClientOptions.getTaskOptions().get(0)));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        try {
            getClient(token)
                .perform(multipart(CURATE_SCRIPT_ENDPOINT)
                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("curate",
                        String.valueOf(admin.getID()), parameters,
                        ProcessStatus.COMPLETED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void curateScript_validRequest_TaskFile() throws Exception {
        context.turnOffAuthorisationSystem();

        String token = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        File taskFile = new File(testProps.get("test.curateTaskFile").toString());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-T", taskFile.getAbsolutePath()));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        try {
            getClient(token)
                .perform(multipart(CURATE_SCRIPT_ENDPOINT)
                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("curate",
                        String.valueOf(admin.getID()), parameters,
                        ProcessStatus.COMPLETED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void curateScript_EPersonInParametersFails() throws Exception {
        context.turnOffAuthorisationSystem();

        String token = getAuthToken(admin.getEmail(), password);

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-e", eperson.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-t", CurationClientOptions.getTaskOptions().get(0)));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());
        AtomicReference<Integer> idRef = new AtomicReference<>();

        context.restoreAuthSystemState();
        try {

            getClient(token)
                .perform(multipart(CURATE_SCRIPT_ENDPOINT)
                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("curate",
                                                String.valueOf(admin.getID()), parameters,
                                                ProcessStatus.FAILED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    /**
     * This test will create a basic structure of communities, collections and items with some local admins at each
     * level and verify that the local admins can only run the curate script on their own objects
     */
    @Test
    public void securityCurateTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson comAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("comAdmin@example.com")
                .withPassword(password).build();
        EPerson colAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("colAdmin@example.com")
                .withPassword(password).build();
        EPerson itemAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("itemAdmin@example.com")
                .withPassword(password).build();
        Community community = CommunityBuilder.createCommunity(context)
                                          .withName("Community")
                                          .withAdminGroup(comAdmin)
                                          .build();
        Community anotherCommunity = CommunityBuilder.createCommunity(context)
                .withName("Another Community")
                .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                .withName("Collection")
                                                .withAdminGroup(colAdmin)
                                                .build();
        Collection anotherCollection = CollectionBuilder.createCollection(context, anotherCommunity)
                                                .withName("AnotherCollection")
                                                .build();
        Item item = ItemBuilder.createItem(context, collection).withAdminUser(itemAdmin)
                                .withTitle("Test item to curate").build();
        Item anotherItem = ItemBuilder.createItem(context, anotherCollection)
                                .withTitle("Another Test item to curate").build();
        Site site = ContentServiceFactory.getInstance().getSiteService().findSite(context);
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> siteParameters = new LinkedList<>();
        siteParameters.add(new DSpaceCommandLineParameter("-i", site.getHandle()));
        siteParameters.add(new DSpaceCommandLineParameter("-t", "noop"));
        LinkedList<DSpaceCommandLineParameter> comParameters = new LinkedList<>();
        comParameters.add(new DSpaceCommandLineParameter("-i", community.getHandle()));
        comParameters.add(new DSpaceCommandLineParameter("-t", "noop"));
        LinkedList<DSpaceCommandLineParameter> anotherComParameters = new LinkedList<>();
        anotherComParameters.add(new DSpaceCommandLineParameter("-i", anotherCommunity.getHandle()));
        anotherComParameters.add(new DSpaceCommandLineParameter("-t", "noop"));
        LinkedList<DSpaceCommandLineParameter> colParameters = new LinkedList<>();
        colParameters.add(new DSpaceCommandLineParameter("-i", collection.getHandle()));
        colParameters.add(new DSpaceCommandLineParameter("-t", "noop"));
        LinkedList<DSpaceCommandLineParameter> anotherColParameters = new LinkedList<>();
        anotherColParameters.add(new DSpaceCommandLineParameter("-i", anotherCollection.getHandle()));
        anotherColParameters.add(new DSpaceCommandLineParameter("-t", "noop"));
        LinkedList<DSpaceCommandLineParameter> itemParameters = new LinkedList<>();
        itemParameters.add(new DSpaceCommandLineParameter("-i", item.getHandle()));
        itemParameters.add(new DSpaceCommandLineParameter("-t", "noop"));
        LinkedList<DSpaceCommandLineParameter> anotherItemParameters = new LinkedList<>();
        anotherItemParameters.add(new DSpaceCommandLineParameter("-i", anotherItem.getHandle()));
        anotherItemParameters.add(new DSpaceCommandLineParameter("-t", "noop"));

        String comAdminToken = getAuthToken(comAdmin.getEmail(), password);
        String colAdminToken = getAuthToken(colAdmin.getEmail(), password);
        String itemAdminToken = getAuthToken(itemAdmin.getEmail(), password);

        List<ParameterValueRest> listCurateSite = siteParameters.stream()
                .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                        .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                .collect(Collectors.toList());
        List<ParameterValueRest> listCom = comParameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());
        List<ParameterValueRest> listAnotherCom = anotherComParameters.stream()
                .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                        .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                .collect(Collectors.toList());
        List<ParameterValueRest> listCol = colParameters.stream()
                                                .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                        .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                .collect(Collectors.toList());
        List<ParameterValueRest> listAnotherCol = anotherColParameters.stream()
                .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                        .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                .collect(Collectors.toList());
        List<ParameterValueRest> listItem = itemParameters.stream()
                                                .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                        .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                .collect(Collectors.toList());
        List<ParameterValueRest> listAnotherItem = anotherItemParameters.stream()
                .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                        .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                .collect(Collectors.toList());
        String adminToken = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        AtomicReference<Integer> idSiteRef = new AtomicReference<>();
        AtomicReference<Integer> idComRef = new AtomicReference<>();
        AtomicReference<Integer> idComColRef = new AtomicReference<>();
        AtomicReference<Integer> idComItemRef = new AtomicReference<>();
        AtomicReference<Integer> idColRef = new AtomicReference<>();
        AtomicReference<Integer> idColItemRef = new AtomicReference<>();
        AtomicReference<Integer> idItemRef = new AtomicReference<>();

        ScriptConfiguration curateScriptConfiguration = scriptService.getScriptConfiguration("curate");
        // we should be able to start the curate script with all our admins on the respective dso
        try {
            // start a process as general admin
            getClient(adminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listCurateSite)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("curate",
                                                        String.valueOf(admin.getID()),
                                                        siteParameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idSiteRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));

            // check with the com admin
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listCom)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("curate",
                                                        String.valueOf(comAdmin.getID()),
                                                        comParameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idComRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
            // the com admin should be able to run the curate also over the children collection and item
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listCol)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("curate",
                                                        String.valueOf(comAdmin.getID()),
                                                        colParameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idComColRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listItem)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("curate",
                                                        String.valueOf(comAdmin.getID()),
                                                        itemParameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idComItemRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
            // the com admin should be NOT able to run the curate over other com, col or items
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listCurateSite)))
                    .andExpect(status().isForbidden());
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listAnotherCom)))
                    .andExpect(status().isForbidden());
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listAnotherCol)))
                    .andExpect(status().isForbidden());
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listAnotherItem)))
                    .andExpect(status().isForbidden());

            // check with the col admin
            getClient(colAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listCol)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("curate",
                                                        String.valueOf(colAdmin.getID()),
                                                        colParameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idColRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
            // the col admin should be able to run the curate also over the owned item
            getClient(colAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listItem)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("curate",
                                                        String.valueOf(colAdmin.getID()),
                                                        itemParameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idColItemRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));

            // the col admin should be NOT able to run the curate over the community nor another collection nor
            // on a not owned item
            getClient(colAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", new ObjectMapper().writeValueAsString(listCurateSite)))
                .andExpect(status().isForbidden());
            getClient(colAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", new ObjectMapper().writeValueAsString(listCom)))
                .andExpect(status().isForbidden());
            getClient(colAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", new ObjectMapper().writeValueAsString(listAnotherCol)))
                .andExpect(status().isForbidden());
            getClient(colAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listAnotherItem)))
                    .andExpect(status().isForbidden());

            // check with the item admin
            getClient(itemAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listItem)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("curate",
                                                        String.valueOf(itemAdmin.getID()),
                                                        itemParameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idItemRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
            // the item admin should be NOT able to run the curate over the community nor the collection nor
            // on a not owned item
            getClient(itemAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", new ObjectMapper().writeValueAsString(listCurateSite)))
                .andExpect(status().isForbidden());
            getClient(itemAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", new ObjectMapper().writeValueAsString(listCom)))
                .andExpect(status().isForbidden());
            getClient(itemAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", new ObjectMapper().writeValueAsString(listCol)))
                .andExpect(status().isForbidden());
            getClient(itemAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(listAnotherItem)))
                    .andExpect(status().isForbidden());

        } finally {
            ProcessBuilder.deleteProcess(idSiteRef.get());
            ProcessBuilder.deleteProcess(idComRef.get());
            ProcessBuilder.deleteProcess(idComColRef.get());
            ProcessBuilder.deleteProcess(idComItemRef.get());
            ProcessBuilder.deleteProcess(idColRef.get());
            ProcessBuilder.deleteProcess(idColItemRef.get());
            ProcessBuilder.deleteProcess(idItemRef.get());
        }
    }
}
