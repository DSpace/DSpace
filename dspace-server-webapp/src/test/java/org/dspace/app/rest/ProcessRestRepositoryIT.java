/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

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
import org.dspace.scripts.service.ProcessService;
import org.hamcrest.Matchers;
import org.junit.After;
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
                        .andExpect(jsonPath("$._embedded.processes", containsInAnyOrder(
                            ProcessMatcher.matchProcess(process.getName(), String.valueOf(process.getEPerson().getID()),
                                                        process.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess.getName(),
                                                        String.valueOf(newProcess.getEPerson().getID()),
                                                        newProcess.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess1.getName(),
                                                        String.valueOf(newProcess1.getEPerson().getID()),
                                                        newProcess1.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess2.getName(),
                                                        String.valueOf(newProcess2.getEPerson().getID()),
                                                        newProcess2.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess3.getName(),
                                                        String.valueOf(newProcess3.getEPerson().getID()),
                                                        newProcess3.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess4.getName(),
                                                        String.valueOf(newProcess4.getEPerson().getID()),
                                                        newProcess4.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess5.getName(),
                                                        String.valueOf(newProcess5.getEPerson().getID()),
                                                        newProcess5.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess6.getName(),
                                                        String.valueOf(newProcess6.getEPerson().getID()),
                                                        newProcess6.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess7.getName(),
                                                        String.valueOf(newProcess7.getEPerson().getID()),
                                                        newProcess7.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess8.getName(),
                                                        String.valueOf(newProcess8.getEPerson().getID()),
                                                        newProcess8.getID(), parameters, ProcessStatus.SCHEDULED),
                            ProcessMatcher.matchProcess(newProcess9.getName(),
                                                        String.valueOf(newProcess9.getEPerson().getID()),
                                                        newProcess9.getID(), parameters, ProcessStatus.SCHEDULED)
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
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();

        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendFile(context, process, is, "inputfile", "test.csv");
        }
        Bitstream bitstream = processService.getBitstream(context, process, "inputfile");

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + process.getID() + "/files"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.files[0].name", is("test.csv")))
                        .andExpect(jsonPath("$._embedded.files[0].uuid", is(bitstream.getID().toString())))
                        .andExpect(jsonPath("$._embedded.files[0].metadata['dspace.process.filetype']" +
                                                "[0].value", is("inputfile")));

    }

    @Test
    public void getProcessFilesByFileType() throws Exception {
        Process newProcess = ProcessBuilder.createProcess(context, eperson, "mock-script", new LinkedList<>()).build();

        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendFile(context, process, is, "inputfile", "test.csv");
        }
        Bitstream bitstream = processService.getBitstream(context, process, "inputfile");

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + process.getID() + "/files/inputfile"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.bitstreams[0].name", is("test.csv")))
                        .andExpect(jsonPath("$._embedded.bitstreams[0].uuid", is(bitstream.getID().toString())))
                        .andExpect(jsonPath("$._embedded.bitstreams[0].metadata['dspace.process.filetype']" +
                                                "[0].value", is("inputfile")));

    }

    @Test
    public void getProcessFilesTypes() throws Exception {
        try (InputStream is = IOUtils.toInputStream("Test File For Process", CharEncoding.UTF_8)) {
            processService.appendFile(context, process, is, "inputfile", "test.csv");
        }

        List<String> fileTypesToCheck = new LinkedList<>();
        fileTypesToCheck.add("inputfile");

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/processes/" + process.getID() + "/filetypes"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ProcessFileTypesMatcher
                            .matchProcessFileTypes("filetypes-" + process.getID(), fileTypesToCheck)));


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

    @After
    @Override
    public void destroy() throws Exception {
        super.destroy();
    }
}
