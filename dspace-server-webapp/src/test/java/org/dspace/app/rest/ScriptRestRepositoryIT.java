/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.matcher.ScriptMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.ProcessStatus;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.service.ProcessService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ScriptRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ProcessService processService;

    @Autowired
    private List<DSpaceRunnable> dSpaceRunnableList;

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Test
    public void findAllScriptsTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", containsInAnyOrder(
                            ScriptMatcher.matchScript(dSpaceRunnableList.get(0).getName(),
                                                      dSpaceRunnableList.get(0).getDescription()),
                            ScriptMatcher.matchScript(dSpaceRunnableList.get(1).getName(),
                                                      dSpaceRunnableList.get(1).getDescription())
                        )));

    }


    @Test
    public void findAllScriptsUnauthorizedTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page",
                                            is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0))));

    }

    @Test
    public void findAllScriptsPaginationTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts").param("size", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", hasItem(
                            ScriptMatcher.matchScript(dSpaceRunnableList.get(0).getName(),
                                                      dSpaceRunnableList.get(0).getDescription())
                        )))
                        .andExpect(jsonPath("$._embedded.scripts", Matchers.not(hasItem(
                            ScriptMatcher.matchScript(dSpaceRunnableList.get(1).getName(),
                                                      dSpaceRunnableList.get(1).getDescription())
                        ))))
                        .andExpect(jsonPath("$.page",
                                            is(PageMatcher.pageEntry(0, 1))));


        getClient(token).perform(get("/api/system/scripts").param("size", "1").param("page", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", Matchers.not(hasItem(
                            ScriptMatcher.matchScript(dSpaceRunnableList.get(0).getName(),
                                                      dSpaceRunnableList.get(0).getDescription())
                        ))))
                        .andExpect(jsonPath("$._embedded.scripts", hasItem(
                            ScriptMatcher.matchScript(dSpaceRunnableList.get(1).getName(),
                                                      dSpaceRunnableList.get(1).getDescription())
                        )))
                        .andExpect(jsonPath("$.page",
                                            is(PageMatcher.pageEntry(1, 1))));
    }

    @Test
    public void findOneScriptByNameTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts/mock-script"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ScriptMatcher
                            .matchMockScript(dSpaceRunnableList.get(1).getOptions())));
    }

    @Test
    public void findOneScriptByNameTestAccessDenied() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts/mock-script"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void findOneScriptByInvalidNameBadRequestExceptionTest() throws Exception {
        getClient().perform(get("/api/system/scripts/mock-script-invalid"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void postProcessNonAdminAuthorizeException() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/system/scripts/mock-script/processes").contentType("multipart/form-data"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void postProcessAnonymousAuthorizeException() throws Exception {
        getClient().perform(post("/api/system/scripts/mock-script/processes").contentType("multipart/form-data"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void postProcessAdminWrongOptionsException() throws Exception {


        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/system/scripts/mock-script/processes").contentType("multipart/form-data"))
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()), new LinkedList<>(),
                                                        ProcessStatus.FAILED))));
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
                        .convert(dSpaceCommandLineParameter, Projection.DEFAULT)).collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/system/scripts/mock-script/processes").contentType("multipart/form-data")
                                                                                  .param("properties",
                                                                                         new Gson().toJson(list)))
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()), parameters,
                                                        ProcessStatus.FAILED))));
    }

    @Test
    public void postProcessNonExistingScriptNameException() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/system/scripts/mock-script-invalid/processes")
                                     .contentType("multipart/form-data"))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void postProcessAdminWithOptionsSuccess() throws Exception {
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));

        List<ParameterValueRest> list = parameters.stream()
                .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                        .convert(dSpaceCommandLineParameter, Projection.DEFAULT)).collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        getClient(token).perform(post("/api/system/scripts/mock-script/processes").contentType("multipart/form-data")
                                                                                  .param("properties",
                                                                                         new Gson().toJson(list)))
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()),
                                                        parameters,
                                                        acceptableProcessStatuses))));

    }

    @Test
    public void postProcessAdminWithWrongContentTypeBadRequestException() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/system/scripts/mock-script-invalid/processes"))
                        .andExpect(status().isBadRequest());
    }

    @After
    public void destroy() throws Exception {
        CollectionUtils.emptyIfNull(processService.findAll(context)).stream().forEach(process -> {
            try {
                processService.delete(context, process);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        super.destroy();
    }

}
