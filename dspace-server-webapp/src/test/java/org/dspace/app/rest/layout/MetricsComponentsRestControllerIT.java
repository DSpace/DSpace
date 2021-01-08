/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.layout;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutMetric2BoxBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.content.EntityType;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * This test class verify the REST Services for the Layout Metrics Component functionality
 * (endpoint /api/layout/boxmetricsconfiguration/<:string>)
 * 
 * @author Alessandro Martelli (alessandro dot martelli at 4science dot it)
 *
 */
public class MetricsComponentsRestControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void getMetricsComponent() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create entity type Publication
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        // Create boxes
        CrisLayoutBoxBuilder.createBuilder(context, eType, CrisLayoutBoxTypes.METRICS.name(), true, true)
                .withShortname("box-shortname-one")
                .build();
        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType,
                CrisLayoutBoxTypes.METRICS.name(), true, true)
                .withShortname("box-shortname-two")
                .withMaxColumns(2)
                .build();
        // Add metrics
        CrisLayoutMetric2BoxBuilder.create(context, box, "metric1", 0).build();
        CrisLayoutMetric2BoxBuilder.create(context, box, "metric2", 1).build();

        CrisLayoutBoxBuilder.createBuilder(context, eType, CrisLayoutBoxTypes.METRICS.name(), true, true)
                .withShortname("box-shortname-three")
                .build();
        context.restoreAuthSystemState();
        // Test WS endpoint
        getClient().perform(get("/api/layout/boxmetricsconfigurations/" + box.getID()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.id", Matchers.is(box.getID())))
            .andExpect(jsonPath("$.maxColumns", Matchers.is(2)))
            .andExpect(jsonPath("$.metrics.length()", Matchers.is(2)))
            .andExpect(jsonPath("$.metrics[0]", Matchers.is("metric1")))
            .andExpect(jsonPath("$.metrics[1]", Matchers.is("metric2")))
            ;
    }

    @Test
    public void patchAddMetricsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, eType,
                CrisLayoutBoxTypes.METRICS.name(), true, true)
                .withShortname("box-shortname-test")
                .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        // 1 Add metrics

        List<Operation> operations = new ArrayList<Operation>();
        List<String> metrics = new ArrayList<>();
        metrics.add("metric1");
        metrics.add("metric2");
        operations.add(new AddOperation("/metrics/", metrics));

        String patchBody = getPatchContent(operations);
        getClient(authToken).perform(patch("/api/layout/boxmetricsconfigurations/" + box.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.length()", is(2)))
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.id", is(box.getID())), // now the configuration id is a number (box id)
                        hasJsonPath("$.type", is("boxmetricsconfiguration")),
                        hasJsonPath("$.metrics[0]", is("metric1")),
                        hasJsonPath("$.metrics[1]", is("metric2"))
                       )));

        // 2 Overwrite metrics

        operations = new ArrayList<Operation>();
        metrics = new ArrayList<>();
        metrics.add("metric2");
        operations.add(new AddOperation("/metrics/", metrics));

        patchBody = getPatchContent(operations);
        getClient(authToken).perform(patch("/api/layout/boxmetricsconfigurations/" + box.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.length()", is(1)))
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.id", is(box.getID())), // now the configuration id is a number (box id)
                        hasJsonPath("$.type", is("boxmetricsconfiguration")),
                        hasJsonPath("$.metrics[0]", is("metric2"))
                       )));

        // 3 Append metrics

        operations = new ArrayList<Operation>();
        metrics = new ArrayList<>();
        metrics.add("metric4");
        operations.add(new AddOperation("/metrics/-", metrics));

        patchBody = getPatchContent(operations);
        getClient(authToken).perform(patch("/api/layout/boxmetricsconfigurations/" + box.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.length()", is(2)))
                .andExpect(jsonPath("$", Matchers.allOf(
                        hasJsonPath("$.id", is(box.getID())), // now the configuration id is a number (box id)
                        hasJsonPath("$.type", is("boxmetricsconfiguration")),
                        hasJsonPath("$.metrics[0]", is("metric2")),
                        hasJsonPath("$.metrics[1]", is("metric4"))
                       )));

    }


}