/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.csv;

import static com.jayway.jsonpath.JsonPath.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.gson.Gson;
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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CsvSearchExportIT extends AbstractControllerIntegrationTest {

    private String subject1 = "subject 1";
    private String subject2 = "subject 2";
    private int numberItemsSubject1 = 30;
    private int numberItemsSubject2 = 2;
    private Item[] itemsSubject1 = new Item[numberItemsSubject1];
    private Item[] itemsSubject2 = new Item[numberItemsSubject2];

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();

        for (int i = 0; i < numberItemsSubject1; i++) {
            itemsSubject1[i] = ItemBuilder.createItem(context, collection)
                .withTitle(String.format("%s item %d", subject1, i))
                .withSubject(subject1)
                .build();
        }

        for (int i = 0; i < numberItemsSubject2; i++) {
            itemsSubject2[i] = ItemBuilder.createItem(context, collection)
                .withTitle(String.format("%s item %d", subject2, i))
                .withSubject(subject2)
                .build();
        }
        context.restoreAuthSystemState();
    }

    @Test
    public void exportSearchQueryTest() throws Exception {
        AtomicReference<Integer> idRef = new AtomicReference<>();
        List<DSpaceCommandLineParameter> parameterList = new ArrayList<>();
        parameterList.add(new DSpaceCommandLineParameter("-q", "subject:" + subject1));
        List<ParameterValueRest> restparams = parameterList.stream()
            .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter.convert(dSpaceCommandLineParameter,
                Projection.DEFAULT)).collect(
                Collectors.toList());

        try {
            String token = getAuthToken(admin.getEmail(), password);
            getClient(token).perform(fileUpload("/api/system/scripts/metadata-export-search/processes")
                    .param("properties", new Gson().toJson(restparams)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$",
                    ProcessMatcher.matchProcess("metadata-export-search", admin.getID().toString(), parameterList,
                        ProcessStatus.COMPLETED)))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));
        }
        finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }
}
