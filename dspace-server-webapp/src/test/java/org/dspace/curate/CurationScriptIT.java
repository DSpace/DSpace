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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Strings;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
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
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
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

    @Autowired
    private ObjectMapper mapper;

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
                         .param("properties", mapper.writeValueAsString(list)))
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
                         .param("properties", mapper.writeValueAsString(list)))
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
                         .param("properties", mapper.writeValueAsString(list)))
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
                         .param("properties", mapper.writeValueAsString(list)))
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
                         .param("properties", mapper.writeValueAsString(list)))
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
                         .param("properties", mapper.writeValueAsString(list)))
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
                             .param("properties", mapper.writeValueAsString(list)))
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
                             .param("properties", mapper.writeValueAsString(list)))
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
                             .param("properties", mapper.writeValueAsString(list)))
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
                                 .param("properties", mapper.writeValueAsString(listCurateSite)))
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
                                 .param("properties", mapper.writeValueAsString(listCom)))
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
                                 .param("properties", mapper.writeValueAsString(listCol)))
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
                                 .param("properties", mapper.writeValueAsString(listItem)))
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
                                 .param("properties", mapper.writeValueAsString(listCurateSite)))
                    .andExpect(status().isForbidden());
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", mapper.writeValueAsString(listAnotherCom)))
                    .andExpect(status().isForbidden());
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", mapper.writeValueAsString(listAnotherCol)))
                    .andExpect(status().isForbidden());
            getClient(comAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", mapper.writeValueAsString(listAnotherItem)))
                    .andExpect(status().isForbidden());

            // check with the col admin
            getClient(colAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", mapper.writeValueAsString(listCol)))
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
                                 .param("properties", mapper.writeValueAsString(listItem)))
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
                             .param("properties", mapper.writeValueAsString(listCurateSite)))
                .andExpect(status().isForbidden());
            getClient(colAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", mapper.writeValueAsString(listCom)))
                .andExpect(status().isForbidden());
            getClient(colAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", mapper.writeValueAsString(listAnotherCol)))
                .andExpect(status().isForbidden());
            getClient(colAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", mapper.writeValueAsString(listAnotherItem)))
                    .andExpect(status().isForbidden());

            // check with the item admin
            getClient(itemAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", mapper.writeValueAsString(listItem)))
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
                             .param("properties", mapper.writeValueAsString(listCurateSite)))
                .andExpect(status().isForbidden());
            getClient(itemAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", mapper.writeValueAsString(listCom)))
                .andExpect(status().isForbidden());
            getClient(itemAdminToken)
                .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                             .param("properties", mapper.writeValueAsString(listCol)))
                .andExpect(status().isForbidden());
            getClient(itemAdminToken)
                    .perform(multipart("/api/system/scripts/" + curateScriptConfiguration.getName() + "/processes")
                                 .param("properties", mapper.writeValueAsString(listAnotherItem)))
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

    @Test
    public void testURLRedirectCurateTest() throws Exception {
        context.turnOffAuthorisationSystem();
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
                // Value not starting with http or https
                .withMetadata("dc", "identifier", "uri", "demo.dspace.org/home")
                // MetadataValueLinkChecker uri field with regular link
                .withMetadata("dc", "description", null, "https://google.com")
                // MetadataValueLinkChecker uri field with redirect link
                .withMetadata("dc", "description", "uri", "http://google.com")
                // MetadataValueLinkChecker uri field with non resolving link
                .withMetadata("dc", "description", "uri", "https://www.atmire.com/broken-link")
                .withSubject("ExtraEntry")
                .build();

        String[] args = new String[] {"curate", "-t", "checklinks", "-i", publicItem1.getHandle()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            script.initialize(args, handler, admin);
            script.run();
        }

        // field that should be ignored
        assertFalse(checkIfInfoTextLoggedByHandler(handler, "demo.dspace.org/home"));
        // redirect links in field that should not be ignored => expect OK (even though curl responds with 301)
        assertTrue(checkIfInfoTextLoggedByHandler(handler, "http://google.com = 200 - OK"));
        // regular link in field that should not be ignored => expect OK
        assertTrue(checkIfInfoTextLoggedByHandler(handler, "https://google.com = 200 - OK"));
        // nonexistent link in field that should not be ignored => expect 404
        assertTrue(checkIfInfoTextLoggedByHandler(handler, "https://www.atmire.com/broken-link = 404 - FAILED"));
    }

    boolean checkIfInfoTextLoggedByHandler(TestDSpaceRunnableHandler handler, String text) {
        for (String message: handler.getInfoMessages()) {
            if (Strings.CI.contains(message, text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test DBTaskQueue basic functionality including enqueue, dequeue and release operations.
     *
     * @author Stefano Maffei (stefano.maffei at 4science.com)
     * @throws Exception if test fails
     */
    @Test
    public void testDBTaskQueue() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create a test item for curation
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Test Community")
                .build();
        Collection testCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Test Collection")
                .build();
        Item testItem = ItemBuilder.createItem(context, testCollection)
                .withTitle("Test Item for DBTaskQueue")
                .withIssueDate("2026-02-10")
                .withAuthor("Test, Author")
                .build();

        // Create DBTaskQueue instance
        DBTaskQueue dbTaskQueue = new DBTaskQueue();

        String queueName = "testQueue";
        long ticket = System.currentTimeMillis();

        // Create test TaskQueueEntry using the correct constructor
        TaskQueueEntry testEntry = new TaskQueueEntry(
                admin.getID().toString(),
                System.currentTimeMillis(),
                Arrays.asList("noop"),
                testItem.getHandle()
        );

        context.restoreAuthSystemState();

        try {
            // Test 1: Check initial queue is empty
            String[] initialQueues = dbTaskQueue.queueNames();
            boolean queueExists = false;
            for (String queue : initialQueues) {
                if (queueName.equals(queue)) {
                    queueExists = true;
                    break;
                }
            }
            assertFalse("Queue should not exist initially", queueExists);

            // Test 2: Enqueue a task
            dbTaskQueue.enqueue(queueName, testEntry);

            // Verify queue exists after enqueue
            String[] queuesAfterEnqueue = dbTaskQueue.queueNames();
            boolean foundQueue = false;
            for (String queue : queuesAfterEnqueue) {
                if (queueName.equals(queue)) {
                    foundQueue = true;
                    break;
                }
            }
            assertTrue("Queue should exist after enqueue", foundQueue);

            // Test 3: Dequeue tasks
            java.util.Set<TaskQueueEntry> dequeuedTasks = dbTaskQueue.dequeue(queueName, ticket);
            assertFalse("Dequeued tasks should not be empty", dequeuedTasks.isEmpty());
            org.junit.Assert.assertEquals("Should have exactly one task", 1, dequeuedTasks.size());

            // Verify the dequeued task matches what we enqueued
            TaskQueueEntry dequeuedTask = dequeuedTasks.iterator().next();
            org.junit.Assert.assertEquals("EPersonID should match", testEntry.getEpersonId(),
                    dequeuedTask.getEpersonId());
            org.junit.Assert.assertEquals("ObjectID should match", testEntry.getObjectId(),
                    dequeuedTask.getObjectId());

            // Test 4: Release with remove=true
            dbTaskQueue.release(queueName, ticket, true);

            // Test 5: Try to dequeue after release (should be empty as tasks were removed)
            long differentTicket = ticket + 1000;
            java.util.Set<TaskQueueEntry> afterRelease = dbTaskQueue.dequeue(queueName, differentTicket);
            assertTrue("Queue should be empty after release with remove=true", afterRelease.isEmpty());

            // Clean up - release any remaining lock
            dbTaskQueue.release(queueName, differentTicket, false);

        } catch (Exception e) {
            // Clean up in case of error
            try {
                dbTaskQueue.release(queueName, ticket, true);
            } catch (Exception ignored) {
                // Ignore cleanup errors
            }
            throw e;
        }
    }

    /**
     * Test curate script execution with -q option to process curation queues.
     * This test verifies the integration between DBTaskQueue and the curation script.
     *
     * @author Stefano Maffei (stefano.maffei at 4science.com)
     * @throws Exception if test fails
     */
    @Test
    public void testCurateScriptWithQueueOption() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create a test item for curation
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Test Community for Queue")
                .build();
        Collection testCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Test Collection for Queue")
                .build();
        Item testItem = ItemBuilder.createItem(context, testCollection)
                .withTitle("Test Item for Queue Processing")
                .withIssueDate("2026-02-10")
                .withAuthor("Queue, Test")
                .build();

        String queueName = "testQueueScript";

        context.restoreAuthSystemState();

        try {
            // Step 1: Populate the queue using DBTaskQueue
            DBTaskQueue dbTaskQueue = new DBTaskQueue();
            TaskQueueEntry testEntry = new TaskQueueEntry(
                    admin.getID().toString(),
                    System.currentTimeMillis(),
                    Arrays.asList("noop"),
                    testItem.getHandle()
            );

            dbTaskQueue.enqueue(queueName, testEntry);

            // Verify the queue has been populated
            String[] queueNames = dbTaskQueue.queueNames();
            boolean queueFound = false;
            for (String queue : queueNames) {
                if (queueName.equals(queue)) {
                    queueFound = true;
                    break;
                }
            }
            assertTrue("Queue should exist before processing", queueFound);

            // Step 2: Execute curate script with -q option using TestDSpaceRunnableHandler
            String[] args = new String[] {"curate", "-q", queueName};
            TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

            ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
            ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

            DSpaceRunnable script = null;
            if (scriptConfiguration != null) {
                script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
            }
            if (script != null) {
                script.initialize(args, testDSpaceRunnableHandler, admin);
                script.run();
            }

            // Step 3: Verify the queue has been processed (should be empty after processing)
            // Note: The queue should be empty after successful processing as tasks are consumed
            String[] queuesAfterProcessing = dbTaskQueue.queueNames();
            assertTrue("Queue should be empty after processing", ArrayUtils.isEmpty(queuesAfterProcessing));

        } catch (Exception e) {
            // Clean up in case of error
            try {
                DBTaskQueue dbTaskQueue = new DBTaskQueue();
                dbTaskQueue.release(queueName, System.currentTimeMillis(), true);
            } catch (Exception ignored) {
                // Ignore cleanup errors
            }
            throw e;
        }
    }

}
