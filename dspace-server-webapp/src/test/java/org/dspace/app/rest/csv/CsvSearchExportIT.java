/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.csv;

import static com.jayway.jsonpath.JsonPath.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.ProcessStatus;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessQueryParameterContainer;
import org.dspace.scripts.Process_;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CsvSearchExportIT extends AbstractControllerIntegrationTest {

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;
    private ProcessService processService = ScriptServiceFactory.getInstance().getProcessService();

    @Test
    public void exportSearchQueryTest() throws Exception {
        AtomicReference<Integer> idRef = new AtomicReference<>();
        List<DSpaceCommandLineParameter> parameterList = new ArrayList<>();
        parameterList.add(new DSpaceCommandLineParameter("-q", "subject:subject1" ));
        List<ParameterValueRest> restparams = parameterList.stream()
            .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter.convert(dSpaceCommandLineParameter,
                Projection.DEFAULT)).collect(
                Collectors.toList());

        try {
            String token = getAuthToken(admin.getEmail(), password);
            getClient(token).perform(multipart("/api/system/scripts/metadata-export-search/processes")
                    .param("properties", new ObjectMapper().writeValueAsString(restparams)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$",
                    ProcessMatcher.matchProcess("metadata-export-search", admin.getID().toString(), parameterList,
                        ProcessStatus.COMPLETED)))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void exportSearchUnauthorizedTest() throws Exception {
        List<DSpaceCommandLineParameter> parameterList = new ArrayList<>();
        parameterList.add(new DSpaceCommandLineParameter("-q", "subject:subject1"));
        List<ParameterValueRest> restparams = parameterList.stream()
            .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter.convert(dSpaceCommandLineParameter,
                Projection.DEFAULT)).collect(
                Collectors.toList());

        getClient().perform(multipart("/api/system/scripts/metadata-export-search/processes")
                .param("properties", new ObjectMapper().writeValueAsString(restparams)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void exportSearchForbiddenTest() throws Exception {
        List<DSpaceCommandLineParameter> parameterList = new ArrayList<>();
        parameterList.add(new DSpaceCommandLineParameter("-q", "subject:subject1"));
        List<ParameterValueRest> restparams = parameterList.stream()
            .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter.convert(dSpaceCommandLineParameter,
                Projection.DEFAULT)).collect(
                Collectors.toList());

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(multipart("/api/system/scripts/metadata-export-search/processes")
                .param("properties", new ObjectMapper().writeValueAsString(restparams)))
            .andExpect(status().isForbidden());
    }

    @Test
    public void exportSearchInvalidQuery() throws Exception {
        AtomicReference<Integer> idRef = new AtomicReference<>();
        List<DSpaceCommandLineParameter> parameterList = new ArrayList<>();
        parameterList.add(new DSpaceCommandLineParameter("-q", "blabla"));
        List<ParameterValueRest> restparams = parameterList.stream()
            .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter.convert(dSpaceCommandLineParameter,
                Projection.DEFAULT)).collect(
                Collectors.toList());


        String token = getAuthToken(admin.getEmail(), password);
        try {
            getClient(token).perform(multipart("/api/system/scripts/metadata-export-search/processes")
                                         .param("properties", new ObjectMapper().writeValueAsString(restparams)))
                            .andExpect(status().isAccepted())
                            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void exportSearchInvalidDiscoveryFacets() throws Exception {
        List<DSpaceCommandLineParameter> parameterList = new ArrayList<>();
        parameterList.add(new DSpaceCommandLineParameter("-f", "nonExisting,equals=bla"));
        List<ParameterValueRest> restparams = parameterList.stream()
            .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter.convert(dSpaceCommandLineParameter,
                Projection.DEFAULT)).collect(
                Collectors.toList());


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(multipart("/api/system/scripts/metadata-export-search/processes")
                .param("properties", new ObjectMapper().writeValueAsString(restparams)))
            .andExpect(status().isInternalServerError());

        // NOTE: While the above call returns 500 (from Discovery), it DOES create a Process.
        // We must find that process by its name and eperson
        ProcessQueryParameterContainer processQueryParameterContainer = new ProcessQueryParameterContainer();
        processQueryParameterContainer.addToQueryParameterMap(Process_.NAME, "metadata-export-search");
        processQueryParameterContainer.addToQueryParameterMap(Process_.E_PERSON, admin);
        // Find all processes which match & clean them all up.
        List<Process> processes = processService.search(context, processQueryParameterContainer, -1, -1);
        for (Process process : processes) {
            ProcessBuilder.deleteProcess(process.getID());
        }
    }
}
