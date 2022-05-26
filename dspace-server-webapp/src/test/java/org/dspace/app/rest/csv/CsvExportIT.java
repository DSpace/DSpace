/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.csv;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CsvExportIT extends AbstractControllerIntegrationTest {


    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Test
    public void metadataExportTestWithoutFileParameterSucceeds() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item article = ItemBuilder.createItem(context, col1)
                                  .withTitle("Article")
                                  .withIssueDate("2017-10-17")
                                  .build();

        AtomicReference<Integer> idRef = new AtomicReference<>();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", col1.getHandle()));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        try {
            String token = getAuthToken(admin.getEmail(), password);

            getClient(token)
                .perform(multipart("/api/system/scripts/metadata-export/processes")
                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("metadata-export",
                                                String.valueOf(admin.getID()), parameters,
                                                ProcessStatus.COMPLETED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));
            String t = "";
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void metadataExportTestWithFileParameterFails() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withEntityType("Publication").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item article = ItemBuilder.createItem(context, col1)
                                  .withTitle("Article")
                                  .withIssueDate("2017-10-17")
                                  .build();

        AtomicReference<Integer> idRef = new AtomicReference<>();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-f", "test.csv"));
        parameters.add(new DSpaceCommandLineParameter("-i", col1.getHandle()));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        try {
            String token = getAuthToken(admin.getEmail(), password);

            getClient(token)
                .perform(multipart("/api/system/scripts/metadata-export/processes")
                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("metadata-export",
                                                String.valueOf(admin.getID()), parameters,
                                                ProcessStatus.FAILED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));
            String t = "";
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }
}
