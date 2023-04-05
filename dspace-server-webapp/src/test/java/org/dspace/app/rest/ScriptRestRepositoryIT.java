/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.matcher.ScriptMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.content.service.BitstreamService;
import org.dspace.eperson.Group;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

public class ScriptRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ProcessService processService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private List<ScriptConfiguration> scriptConfigurations;

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Test
    public void findAllScriptsTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", containsInAnyOrder(
                            scriptConfigurations
                                .stream()
                                .map(scriptConfiguration -> ScriptMatcher.matchScript(
                                    scriptConfiguration.getName(),
                                    scriptConfiguration.getDescription()
                                ))
                                .collect(Collectors.toList())
                        )));
    }


    @Test
    public void findAllScriptsUnauthorizedTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts"))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void findAllScriptsPaginationTest() throws Exception {
        List<ScriptConfiguration> alphabeticScripts =
            scriptConfigurations.stream()
                                .sorted(Comparator.comparing(s -> s.getClass().getName()))
                                .collect(Collectors.toList());

        int totalPages = scriptConfigurations.size();
        int lastPage = totalPages - 1;

        String token = getAuthToken(admin.getEmail(), password);

        // NOTE: the scripts are always returned in alphabetical order by fully qualified class name.
        getClient(token).perform(get("/api/system/scripts").param("size", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", Matchers.not(Matchers.hasItem(
                            ScriptMatcher.matchScript(alphabeticScripts.get(1).getName(),
                                                      alphabeticScripts.get(1).getDescription())
                        ))))
                        .andExpect(jsonPath("$._embedded.scripts", hasItem(
                            ScriptMatcher.matchScript(alphabeticScripts.get(0).getName(),
                                                      alphabeticScripts.get(0).getDescription())
                        )))
                        .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=" + lastPage), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$.page.size", is(1)))
                        .andExpect(jsonPath("$.page.number", is(0)))
                        .andExpect(jsonPath("$.page.totalPages", is(totalPages)))
                        .andExpect(jsonPath("$.page.totalElements", is(totalPages)));


        getClient(token).perform(get("/api/system/scripts").param("size", "1").param("page", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", hasItem(
                            ScriptMatcher.matchScript(alphabeticScripts.get(1).getName(),
                                                      alphabeticScripts.get(1).getDescription())
                        )))
                        .andExpect(jsonPath("$._embedded.scripts", Matchers.not(hasItem(
                            ScriptMatcher.matchScript(alphabeticScripts.get(0).getName(),
                                                      alphabeticScripts.get(0).getDescription())
                        ))))
                        .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=2"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=" + lastPage), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$.page.size", is(1)))
                        .andExpect(jsonPath("$.page.number", is(1)))
                        .andExpect(jsonPath("$.page.totalPages", is(totalPages)))
                        .andExpect(jsonPath("$.page.totalElements", is(totalPages)));
    }

    @Test
    public void findOneScriptByNameTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts/mock-script"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ScriptMatcher
                            .matchMockScript(
                                scriptConfigurations
                                    .stream()
                                    .filter(scriptConfiguration
                                                -> scriptConfiguration.getName().equals("mock-script"))
                                    .findAny()
                                    .orElseThrow()
                                    .getOptions()
                            )
                        ));
    }

    @Test
    public void findOneScriptByNameTestAccessDenied() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts/mock-script"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void findOneScriptByInvalidNameBadRequestExceptionTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts/mock-script-invalid"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void postProcessNonAdminAuthorizeException() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(multipart("/api/system/scripts/mock-script/processes"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void postProcessAnonymousAuthorizeException() throws Exception {
        getClient().perform(multipart("/api/system/scripts/mock-script/processes"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void postProcessAdminWrongOptionsException() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()), new LinkedList<>(),
                                                        ProcessStatus.FAILED))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }


    }

    @Test
    public void postProcessAdminNoOptionsFailedStatus() throws Exception {

//        List<ParameterValueRest> list = new LinkedList<>();
//
//        ParameterValueRest parameterValueRest = new ParameterValueRest();
//        parameterValueRest.setName("-z");
//        parameterValueRest.setValue("test");
//        ParameterValueRest parameterValueRest1 = new ParameterValueRest();
//        parameterValueRest1.setName("-q");
//        list.add(parameterValueRest);
//        list.add(parameterValueRest1);

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-z", "test"));
        parameters.add(new DSpaceCommandLineParameter("-q", null));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(list)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()), parameters,
                                                        ProcessStatus.FAILED))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void postProcessNonExistingScriptNameException() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(multipart("/api/system/scripts/mock-script-invalid/processes"))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void postProcessAdminWithOptionsSuccess() throws Exception {
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(list)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()),
                                                        parameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void postProcessAndVerifyOutput() throws Exception {
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(list)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()),
                                                        parameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));


            Process process = processService.find(context, idRef.get());
            Bitstream bitstream = processService.getBitstream(context, process, Process.OUTPUT_TYPE);


            getClient(token).perform(get("/api/system/processes/" + idRef.get() + "/output"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$", BitstreamMatcher
                                    .matchBitstreamEntryWithoutEmbed(bitstream.getID(), bitstream.getSizeBytes())));


            MvcResult mvcResult = getClient(token)
                    .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content")).andReturn();
            String content = mvcResult.getResponse().getContentAsString();

            assertThat(content, CoreMatchers
                    .containsString("INFO mock-script - " + process.getID() + " @ The script has started"));
            assertThat(content,
                       CoreMatchers.containsString(
                               "INFO mock-script - " + process.getID() + " @ Logging INFO for Mock DSpace Script"));
            assertThat(content,
                       CoreMatchers.containsString(
                               "ERROR mock-script - " + process.getID() + " @ Logging ERROR for Mock DSpace Script"));
            assertThat(content,
                       CoreMatchers.containsString("WARNING mock-script - " + process
                               .getID() + " @ Logging WARNING for Mock DSpace Script"));
            assertThat(content, CoreMatchers
                    .containsString("INFO mock-script - " + process.getID() + " @ The script has completed"));




        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }


    @Test
    public void postProcessAdminWithWrongContentTypeBadRequestException() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/system/scripts/mock-script-invalid/processes"))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void postProcessAdminWithFileSuccess() throws Exception {
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));


        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "Hello, World!";
        MockMultipartFile bitstreamFile = new MockMultipartFile("file",
                                                                "helloProcessFile.txt", MediaType.TEXT_PLAIN_VALUE,
                                                                bitstreamContent.getBytes());
        parameters.add(new DSpaceCommandLineParameter("-f", "helloProcessFile.txt"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes")
                                 .file(bitstreamFile)
                                 .characterEncoding("UTF-8")
                                 .param("properties", new ObjectMapper().writeValueAsString(list)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()),
                                                        parameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void scriptTypeConversionTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts/type-conversion-test"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ScriptMatcher
                                .matchScript("type-conversion-test",
                                             "Test the type conversion different option types")))
                        .andExpect(jsonPath("$.parameters", containsInAnyOrder(
                                allOf(
                                        hasJsonPath("$.name", is("-b")),
                                        hasJsonPath("$.description", is("option set to the boolean class")),
                                        hasJsonPath("$.type", is("boolean")),
                                        hasJsonPath("$.mandatory", is(false)),
                                        hasJsonPath("$.nameLong", is("--boolean"))
                                ),
                                allOf(
                                        hasJsonPath("$.name", is("-s")),
                                        hasJsonPath("$.description", is("string option with an argument")),
                                        hasJsonPath("$.type", is("String")),
                                        hasJsonPath("$.mandatory", is(false)),
                                        hasJsonPath("$.nameLong", is("--string"))
                                ),
                                allOf(
                                        hasJsonPath("$.name", is("-n")),
                                        hasJsonPath("$.description", is("string option without an argument")),
                                        hasJsonPath("$.type", is("boolean")),
                                        hasJsonPath("$.mandatory", is(false)),
                                        hasJsonPath("$.nameLong", is("--noargument"))
                                ),
                                allOf(
                                        hasJsonPath("$.name", is("-f")),
                                        hasJsonPath("$.description", is("file option with an argument")),
                                        hasJsonPath("$.type", is("InputStream")),
                                        hasJsonPath("$.mandatory", is(false)),
                                        hasJsonPath("$.nameLong", is("--file"))
                                )
                        ) ));
    }

    @Test
    public void TrackSpecialGroupduringprocessSchedulingTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group specialGroup = GroupBuilder.createGroup(context)
            .withName("Special Group")
            .addMember(admin)
            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty("authentication-password.login.specialgroup", specialGroup.getName());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));

        List<ParameterValueRest> list = parameters.stream()
            .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
            .collect(Collectors.toList());



        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token).perform(post("/api/system/scripts/mock-script/processes")
                                         .contentType("multipart/form-data")
                                         .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(ProcessMatcher.matchProcess("mock-script",
                                                                        String.valueOf(admin.getID()),
                                                                        parameters, acceptableProcessStatuses))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));

            Process process = processService.find(context, idRef.get());
            List<Group> groups = process.getGroups();
            boolean isPresent = groups.stream().anyMatch(g -> g.getID().equals(specialGroup.getID()));
            assertTrue(isPresent);

        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }


    @After
    public void destroy() throws Exception {
        CollectionUtils.emptyIfNull(processService.findAll(context)).stream().forEach(process -> {
            try {
                processService.delete(context, process);
            } catch (SQLException | AuthorizeException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        super.destroy();
    }

}
