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

import java.sql.SQLException;
import java.util.LinkedList;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.builder.ProcessBuilder;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
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
