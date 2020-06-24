package org.dspace.curate;

import static org.hamcrest.Matchers.is;
import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.ProcessBuilder;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Maria Verdonck (Atmire) on 24/06/2020
 */
public class CurationScriptIT extends AbstractControllerIntegrationTest {

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

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

        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-t", "invalidTaskOption"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        // Request with -t <invalidTaskOption>
        getClient(token)
            .perform(post(CURATE_SCRIPT_ENDPOINT).contentType("multipart/form-data")
                                                 .param("properties",
                                                     new Gson().toJson(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest())
            // Contains the valid options
            .andExpect(status().reason(containsString(Arrays.toString(CurationClientOptions.getTaskOptions()))));
    }

    @Test
    public void curateScript_MissingEperson() throws Exception {
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
        parameters.add(new DSpaceCommandLineParameter("-t", CurationClientOptions.getTaskOptions()[0]));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        // Request with missing required -e <email>
        getClient(token)
            .perform(post(CURATE_SCRIPT_ENDPOINT).contentType("multipart/form-data")
                                                 .param("properties",
                                                     new Gson().toJson(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest())
            // Contains -e (the missing required cl option
            .andExpect(status().reason(containsString("-e")));
    }

    @Test
    public void curateScript_NonExistentEPerson() throws Exception {
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

        parameters.add(new DSpaceCommandLineParameter("-e", "nonExistentEmail@test.com"));
        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-t", CurationClientOptions.getTaskOptions()[0]));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        // Request with -e <nonExistingEPersonEmail>
        getClient(token)
            .perform(post(CURATE_SCRIPT_ENDPOINT).contentType("multipart/form-data")
                                                 .param("properties",
                                                     new Gson().toJson(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest())
            // Contains email
            .andExpect(status().reason(containsString("email")));
    }

    @Test
    public void curateScript_MissingHandle() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-t", CurationClientOptions.getTaskOptions()[0]));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        // Request with missing required -i <handle>
        getClient(token)
            .perform(post(CURATE_SCRIPT_ENDPOINT).contentType("multipart/form-data")
                                                 .param("properties",
                                                     new Gson().toJson(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest())
            // Contains handle
            .andExpect(status().reason(containsString("handle")));
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

        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        // Request without -t <task> or -T <taskFile> (and no -q <queue>)
        getClient(token)
            .perform(post(CURATE_SCRIPT_ENDPOINT).contentType("multipart/form-data")
                                                 .param("properties",
                                                     new Gson().toJson(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest())
            // Contains task
            .andExpect(status().reason(containsString("task")));
    }

    @Test
    public void curateScript_InvalidScope() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-i", "all"));
        parameters.add(new DSpaceCommandLineParameter("-s", "invalidScope"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        // Request with invalid -s <scope>; must be object, curation or open
        getClient(token)
            .perform(post(CURATE_SCRIPT_ENDPOINT).contentType("multipart/form-data")
                                                 .param("properties",
                                                     new Gson().toJson(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest());
    }

    @Test
    public void curateScript_InvalidTaskFile() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-i", "all"));
        parameters.add(new DSpaceCommandLineParameter("-T", "invalidTaskFile"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        // Request with invalid -s <scope>; must be object, curation or open
        getClient(token)
            .perform(post(CURATE_SCRIPT_ENDPOINT).contentType("multipart/form-data")
                                                 .param("properties",
                                                     new Gson().toJson(list)))
            // Illegal Argument Exception
            .andExpect(status().isBadRequest())
            // Contains taskFile
            .andExpect(status().reason(containsString("taskFile")));
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

        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-t", CurationClientOptions.getTaskOptions()[0]));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        try {
            getClient(token)
                .perform(post(CURATE_SCRIPT_ENDPOINT).contentType("multipart/form-data")
                                                     .param("properties",
                                                         new Gson().toJson(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("curate",
                        String.valueOf(admin.getID()), parameters,
                        ProcessStatus.SCHEDULED))))
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
        parameters.add(new DSpaceCommandLineParameter("-e", admin.getEmail()));
        parameters.add(new DSpaceCommandLineParameter("-i", publicItem1.getHandle()));
        parameters.add(new DSpaceCommandLineParameter("-T", taskFile.getAbsolutePath()));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        context.restoreAuthSystemState();

        try {
            getClient(token)
                .perform(post(CURATE_SCRIPT_ENDPOINT).contentType("multipart/form-data")
                                                     .param("properties",
                                                         new Gson().toJson(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("curate",
                        String.valueOf(admin.getID()), parameters,
                        ProcessStatus.SCHEDULED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }


}
