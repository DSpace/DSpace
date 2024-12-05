/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.ProcessMatcher.matchProcess;
import static org.dspace.content.ProcessStatus.SCHEDULED;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.ProcessFileTypesMatcher;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.ProcessStatus;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessLogLevel;
import org.dspace.scripts.service.ProcessService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProcessRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ProcessService processService;

    Process process;

    LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();


    @Before
    public void setup() throws SQLException {
        CollectionUtils.emptyIfNull(processService.findAll(context)).stream().forEach(process -> {
            try {
                processService.delete(context, process);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));

        process = ProcessBuilder.createProcess(context, admin, "mock-script", parameters).build();
    }

    @Test
    public void getProcessAdmin() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + process.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.is(
                            ProcessMatcher.matchProcess(process.getName(), String.valueOf(process.getEPerson().getID()),
                                                        process.getID(), parameters, ProcessStatus.SCHEDULED)))
        );
    }

    @Test
    public void getProcessAdminEmptyParam() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        Process process = ProcessBuilder.createProcess(context, admin, "mock-script", new LinkedList<>()).build();

        getClient(token).perform(get("/api/system/processes/" + process.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.is(
                            ProcessMatcher.matchProcess(process.getName(), String.valueOf(process.getEPerson().getID()),
                                                        process.getID(), new LinkedList<>(), ProcessStatus.SCHEDULED)))
        );
    }

    @Test
    public void getProcessAnonymousUnauthorizedException() throws Exception {

        getClient().perform(get("/api/system/processes/" + process.getID()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getProcessForStartedUser() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + newProcess.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.is(
                            ProcessMatcher.matchProcess(newProcess.getName(),
                                                        String.valueOf(newProcess.getEPerson().getID()),
                                                        newProcess.getID(),
                                                        new LinkedList<>(),
                                                        ProcessStatus.SCHEDULED))));

    }

    @Test
    public void getProcessForDifferentUserForbiddenException() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + process.getID()))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void getProcessNotExisting() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + process.getID() * 23 + 17))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void getAllProcessesTestAnonymous() throws Exception {


        getClient().perform(get("/api/system/processes/"))
                   .andExpect(status().isUnauthorized());
    }


    @Test
    public void getAllProcessesTestAdmin() throws Exception {

        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess4 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess5 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess6 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess7 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters).build();
        Process newProcess8 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters).build();
        Process newProcess9 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters).build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/"))
                        .andExpect(status().isOk())
                        // Expect all processes to be returned, newest to oldest
                        .andExpect(jsonPath("$._embedded.processes", contains(
                            ProcessMatcher.matchProcess(newProcess9.getName(),
                                                        String.valueOf(newProcess9.getEPerson().getID()),
                                                        newProcess9.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess8.getName(),
                                                        String.valueOf(newProcess8.getEPerson().getID()),
                                                        newProcess8.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess7.getName(),
                                                        String.valueOf(newProcess7.getEPerson().getID()),
                                                        newProcess7.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess6.getName(),
                                                        String.valueOf(newProcess6.getEPerson().getID()),
                                                        newProcess6.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess5.getName(),
                                                        String.valueOf(newProcess5.getEPerson().getID()),
                                                        newProcess5.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess4.getName(),
                                                        String.valueOf(newProcess4.getEPerson().getID()),
                                                        newProcess4.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess3.getName(),
                                                        String.valueOf(newProcess3.getEPerson().getID()),
                                                        newProcess3.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess2.getName(),
                                                        String.valueOf(newProcess2.getEPerson().getID()),
                                                        newProcess2.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess1.getName(),
                                                        String.valueOf(newProcess1.getEPerson().getID()),
                                                        newProcess1.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess.getName(),
                                                        String.valueOf(newProcess.getEPerson().getID()),
                                                        newProcess.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(process.getName(), String.valueOf(process.getEPerson().getID()),
                                                        process.getID(), parameters, ProcessStatus.SCHEDULED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 11))));

    }

    @Test
    public void getAllProcessesTestStartingUser() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();
        Process newProcess4 = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();
        Process newProcess5 = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();
        Process newProcess6 = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();
        Process newProcess7 = ProcessBuilder.createProcess(context, admin, "mock-script", new LinkedList<>()).build();
        Process newProcess8 = ProcessBuilder.createProcess(context, admin, "mock-script", new LinkedList<>()).build();
        Process newProcess9 = ProcessBuilder.createProcess(context, admin, "mock-script", new LinkedList<>()).build();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void getProcessFiles() throws Exception {
        context.setCurrentUser(eperson);
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();
        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendFile(context, newProcess, is, "inputfile", "test.csv");
        }
        Bitstream bitstream = processService.getBitstream(context, newProcess, "inputfile");

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + newProcess.getID() + "/files"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.files[0].name", is("test.csv")))
                        .andExpect(jsonPath("$._embedded.files[0].uuid", is(bitstream.getID().toString())))
                        .andExpect(jsonPath("$._embedded.files[0].metadata['dspace.process.filetype']" +
                                                "[0].value", is("inputfile")));
        getClient(token).perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                        .andExpect(status().isOk());
        // also the user that triggered the process should be able to access the process' files
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken)
                        .perform(get("/api/system/processes/" + newProcess.getID() + "/files"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.files[0].name", is("test.csv")))
                        .andExpect(jsonPath("$._embedded.files[0].uuid", is(bitstream.getID().toString())))
                        .andExpect(jsonPath("$._embedded.files[0].metadata['dspace.process.filetype']" +
                                                "[0].value", is("inputfile")));
        getClient(epersonToken)
                        .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content"))
                        .andExpect(status().isOk());
    }

    @Test
    public void getProcessFilesByFileType() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();

        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendFile(context, newProcess, is, "inputfile", "test.csv");
        }
        Bitstream bitstream = processService.getBitstream(context, newProcess, "inputfile");

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + newProcess.getID() + "/files/inputfile"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.bitstreams[0].name", is("test.csv")))
                        .andExpect(jsonPath("$._embedded.bitstreams[0].uuid", is(bitstream.getID().toString())))
                        .andExpect(jsonPath("$._embedded.bitstreams[0].metadata['dspace.process.filetype']" +
                                                "[0].value", is("inputfile")));
        // also the user that triggered the process should be able to access the process' files
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken)
                        .perform(get("/api/system/processes/" + newProcess.getID() + "/files/inputfile"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.bitstreams[0].name", is("test.csv")))
                        .andExpect(jsonPath("$._embedded.bitstreams[0].uuid", is(bitstream.getID().toString())))
                        .andExpect(jsonPath("$._embedded.bitstreams[0].metadata['dspace.process.filetype']" +
                                                "[0].value", is("inputfile")));
    }

    @Test
    public void getProcessFilesTypes() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();
        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendFile(context, newProcess, is, "inputfile", "test.csv");
        }

        List<String> fileTypesToCheck = new LinkedList<>();
        fileTypesToCheck.add("inputfile");

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + newProcess.getID() + "/filetypes"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ProcessFileTypesMatcher
                            .matchProcessFileTypes("filetypes-" + newProcess.getID(), fileTypesToCheck)));

        // also the user that triggered the process should be able to access the process' files
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken)
                        .perform(get("/api/system/processes/" + newProcess.getID() + "/filetypes"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ProcessFileTypesMatcher
                            .matchProcessFileTypes("filetypes-" + newProcess.getID(), fileTypesToCheck)));
    }

    @Test
    public void getProcessFilesTypesForbidden() throws Exception {
        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendFile(context, process, is, "inputfile", "test.csv");
        }

        List<String> fileTypesToCheck = new LinkedList<>();
        fileTypesToCheck.add("inputfile");

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + process.getID() + "/filetypes"))
                        .andExpect(status().isForbidden());


    }

    @Test
    public void getProcessFilesTypesUnAuthorized() throws Exception {
        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendFile(context, process, is, "inputfile", "test.csv");
        }

        List<String> fileTypesToCheck = new LinkedList<>();
        fileTypesToCheck.add("inputfile");

        getClient().perform(get("/api/system/processes/" + process.getID() + "/filetypes"))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void getProcessFilesTypesRandomProcessId() throws Exception {
        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendFile(context, process, is, "inputfile", "test.csv");
        }

        List<String> fileTypesToCheck = new LinkedList<>();
        fileTypesToCheck.add("inputfile");

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + new Random() + "/filetypes"))
                        .andExpect(status().isNotFound());


    }

    @Test
    public void searchProcessTestForbidden() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void searchProcessTestUnauthorized() throws Exception {

        getClient().perform(get("/api/system/processes/search/byProperty"))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    public void searchProcessTestByUser() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess4 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess5 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess6 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess7 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters).build();
        Process newProcess8 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters).build();
        Process newProcess9 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters).build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                .param("userId", admin.getID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", containsInAnyOrder(
                            ProcessMatcher.matchProcess(process.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        process.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess7.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess7.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess8.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess8.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess9.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess9.getID(), parameters, ProcessStatus.SCHEDULED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 4))));

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("userId", eperson.getID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", containsInAnyOrder(
                            ProcessMatcher.matchProcess(newProcess.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess1.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess1.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess2.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess2.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess3.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess3.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess4.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess4.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess5.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess5.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess6.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess6.getID(), parameters, ProcessStatus.SCHEDULED)

                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 7))));
    }

    @Test
    public void searchProcessTestByProcessStatus() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess4 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess5 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess6 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess7 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();
        Process newProcess8 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters).build();
        Process newProcess9 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("processStatus", "FAILED"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", containsInAnyOrder(
                            ProcessMatcher.matchProcess(newProcess7.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess7.getID(), parameters, ProcessStatus.FAILED),
                            ProcessMatcher.matchProcess(newProcess9.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess9.getID(), parameters, ProcessStatus.FAILED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2))));
    }

    @Test
    public void searchProcessTestByScriptName() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess4 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess5 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess6 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess7 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();
        Process newProcess8 = ProcessBuilder.createProcess(context, admin, "another-mock-script", parameters).build();
        Process newProcess9 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("scriptName", "another-mock-script"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", containsInAnyOrder(
                            ProcessMatcher.matchProcess(newProcess8.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess8.getID(), parameters, ProcessStatus.SCHEDULED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1))));
    }

    @Test
    public void searchProcessTestByScriptNameAndUserId() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess4 = ProcessBuilder.createProcess(context, eperson, "another-mock-script", parameters).build();
        Process newProcess5 = ProcessBuilder.createProcess(context, eperson, "another-mock-script", parameters).build();
        Process newProcess6 = ProcessBuilder.createProcess(context, eperson, "another-mock-script", parameters).build();
        Process newProcess7 = ProcessBuilder.createProcess(context, admin, "another-mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();
        Process newProcess8 = ProcessBuilder.createProcess(context, admin, "another-mock-script", parameters).build();
        Process newProcess9 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("scriptName", "another-mock-script")
                                     .param("userId", admin.getID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", containsInAnyOrder(
                            ProcessMatcher.matchProcess(newProcess7.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess7.getID(), parameters, ProcessStatus.FAILED),
                            ProcessMatcher.matchProcess(newProcess8.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess8.getID(), parameters, ProcessStatus.SCHEDULED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2))));
    }

    @Test
    public void searchProcessTestByUserIdAndProcessStatus() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();
        Process newProcess4 = ProcessBuilder.createProcess(context, eperson, "another-mock-script", parameters).build();
        Process newProcess5 = ProcessBuilder.createProcess(context, eperson, "another-mock-script", parameters).build();
        Process newProcess6 = ProcessBuilder.createProcess(context, eperson, "another-mock-script", parameters).build();
        Process newProcess7 = ProcessBuilder.createProcess(context, admin, "another-mock-script", parameters).build();
        Process newProcess8 = ProcessBuilder.createProcess(context, admin, "another-mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();
        Process newProcess9 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("processStatus", "FAILED")
                                     .param("userId", admin.getID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", containsInAnyOrder(
                            ProcessMatcher.matchProcess(newProcess9.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess9.getID(), parameters, ProcessStatus.FAILED),
                            ProcessMatcher.matchProcess(newProcess8.getName(),
                                                        String.valueOf(admin.getID().toString()),
                                                        newProcess8.getID(), parameters, ProcessStatus.FAILED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2))));
    }

    @Test
    public void searchProcessTestByUserIdAndProcessStatusAndScriptName() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();
        Process newProcess4 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters).build();
        Process newProcess5 = ProcessBuilder.createProcess(context, eperson, "another-mock-script", parameters).build();
        Process newProcess6 = ProcessBuilder.createProcess(context, eperson, "another-mock-script", parameters).build();
        Process newProcess7 = ProcessBuilder.createProcess(context, admin, "another-mock-script", parameters).build();
        Process newProcess8 = ProcessBuilder.createProcess(context, admin, "another-mock-script", parameters)
                                            .withProcessStatus(ProcessStatus.FAILED).build();
        Process newProcess9 = ProcessBuilder.createProcess(context, admin, "mock-script", parameters).build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("processStatus", "FAILED")
                                     .param("userId", eperson.getID().toString())
                                     .param("scriptName", "mock-script"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", containsInAnyOrder(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess1.getID(), parameters, ProcessStatus.FAILED),
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess3.getID(), parameters, ProcessStatus.FAILED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2))));
    }

    @Test
    public void searchProcessTestNoParametersBadRequest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty"))
                        .andExpect(status().isBadRequest());
    }


    @Test
    public void searchProcessTestUnparseableProcessStatusParamBadRequest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("processStatus", "not-a-valid-status"))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void searchProcessTestInvalidEPersonUuid() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("userId", UUID.randomUUID().toString()))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void searchProcessTestByUserSortedOnStartTimeAsc() throws Exception {
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("10/01/1990", "20/01/1990").build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("11/01/1990", "19/01/1990").build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("12/01/1990", "18/01/1990").build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("userId", eperson.getID().toString())
                                    .param("sort", "startTime,asc"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", contains(
                            ProcessMatcher.matchProcess(newProcess1.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess1.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess2.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess2.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess3.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess3.getID(), parameters, ProcessStatus.SCHEDULED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3))));
    }

    @Test
    public void searchProcessTestByUserSortedOnStartTimeDesc() throws Exception {
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("10/01/1990", "20/01/1990").build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("11/01/1990", "19/01/1990").build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("12/01/1990", "18/01/1990").build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("userId", eperson.getID().toString())
                                     .param("sort", "startTime,desc"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", contains(
                            ProcessMatcher.matchProcess(newProcess3.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess3.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess2.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess2.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess1.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess1.getID(), parameters, ProcessStatus.SCHEDULED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3))));
    }

    @Test
    public void searchProcessTestByUserSortedOnEndTimeAsc() throws Exception {
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("10/01/1990", "20/01/1990").build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("11/01/1990", "19/01/1990").build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("12/01/1990", "18/01/1990").build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("userId", eperson.getID().toString())
                                     .param("sort", "endTime,asc"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", contains(
                            ProcessMatcher.matchProcess(newProcess3.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess3.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess2.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess2.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess1.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess1.getID(), parameters, ProcessStatus.SCHEDULED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3))));
    }

    @Test
    public void searchProcessTestByUserSortedOnEndTimeDesc() throws Exception {
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("10/01/1990", "20/01/1990").build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("11/01/1990", "19/01/1990").build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("12/01/1990", "18/01/1990").build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("userId", eperson.getID().toString())
                                     .param("sort", "endTime,desc"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", contains(
                            ProcessMatcher.matchProcess(newProcess1.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess1.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess2.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess2.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess3.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess3.getID(), parameters, ProcessStatus.SCHEDULED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3))));
    }

    @Test
    public void searchProcessTestByUserSortedOnMultipleBadRequest() throws Exception {
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("10/01/1990", "20/01/1990").build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("11/01/1990", "19/01/1990").build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("12/01/1990", "18/01/1990").build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("userId", eperson.getID().toString())
                                     .param("sort", "endTime,desc")
                                     .param("sort", "startTime,desc"))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void searchProcessTestByUserSortedOnDefault() throws Exception {
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("10/01/1990", "20/01/1990").build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("11/01/1990", "19/01/1990").build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("12/01/1990", "18/01/1990").build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("userId", eperson.getID().toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.processes", contains(
                            ProcessMatcher.matchProcess(newProcess3.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess3.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess2.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess2.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess1.getName(),
                                                        String.valueOf(eperson.getID().toString()),
                                                        newProcess1.getID(), parameters, ProcessStatus.SCHEDULED)
                        )))
                        .andExpect(jsonPath("$.page", is(
                            PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3))));
    }

    @Test
    public void searchProcessTestByUserSortedOnNonExistingIsSortedAsDefault() throws Exception {
        Process newProcess1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("10/01/1990", "20/01/1990").build();
        Process newProcess2 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("11/01/1990", "19/01/1990").build();
        Process newProcess3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                                            .withStartAndEndTime("12/01/1990", "18/01/1990").build();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/byProperty")
                                     .param("userId", eperson.getID().toString())
                                     .param("sort", "eaz,desc"))
                        .andExpect(status().isBadRequest());
    }

    @Test
    public void testFindByCurrentUser() throws Exception {

        Process process1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
            .withStartAndEndTime("10/01/1990", "20/01/1990")
            .build();
        ProcessBuilder.createProcess(context, admin, "mock-script", parameters)
            .withStartAndEndTime("11/01/1990", "19/01/1990")
            .build();
        Process process3 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
            .withStartAndEndTime("12/01/1990", "18/01/1990")
            .build();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/search/own"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.processes", contains(
                matchProcess(process3.getName(), eperson.getID().toString(), process3.getID(), parameters, SCHEDULED),
                matchProcess(process1.getName(), eperson.getID().toString(), process1.getID(), parameters, SCHEDULED))))
            .andExpect(jsonPath("$.page", is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2))));

    }

    @Test
    public void getProcessOutput() throws Exception {
        context.setCurrentUser(eperson);
        Process process1 = ProcessBuilder.createProcess(context, eperson, "mock-script", parameters)
                .withStartAndEndTime("10/01/1990", "20/01/1990")
                .build();

        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendLog(process1.getID(), process1.getName(), "testlog", ProcessLogLevel.INFO);
        }
        processService.createLogBitstream(context, process1);
        List<String> fileTypesToCheck = new LinkedList<>();
        fileTypesToCheck.add("inputfile");

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + process1.getID() + "/output"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name",
                                            is(process1.getID() + "-" + process1.getName() + ".log")))
                        .andExpect(jsonPath("$.type", is("bitstream")))
                        .andExpect(jsonPath("$.metadata['dc.title'][0].value",
                                            is(process1.getID() + "-" + process1.getName() + ".log")))
                        .andExpect(jsonPath("$.metadata['dspace.process.filetype'][0].value",
                                            is("script_output")));

        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken)
                        .perform(get("/api/system/processes/" + process1.getID() + "/output"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name",
                                            is(process1.getID() + "-" + process1.getName() + ".log")))
                        .andExpect(jsonPath("$.type", is("bitstream")))
                        .andExpect(jsonPath("$.metadata['dc.title'][0].value",
                                            is(process1.getID() + "-" + process1.getName() + ".log")))
                        .andExpect(jsonPath("$.metadata['dspace.process.filetype'][0].value",
                                            is("script_output")));

    }
}
