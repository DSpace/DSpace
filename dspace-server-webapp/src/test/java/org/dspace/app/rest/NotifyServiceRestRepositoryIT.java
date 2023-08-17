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
import static org.dspace.app.rest.matcher.NotifyServiceMatcher.matchNotifyService;
import static org.dspace.app.rest.matcher.NotifyServiceMatcher.matchNotifyServicePattern;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomUtils;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.rest.model.NotifyServiceRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.NotifyServiceRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.NotifyServiceBuilder;
import org.junit.Test;

/**
 * Integration test class for {@link NotifyServiceRestRepository}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllUnAuthorizedTest() throws Exception {
        getClient().perform(get("/api/ldn/ldnservices"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntityOne =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name one")
                                .withDescription("service description one")
                                .withUrl("service url one")
                                .withLdnUrl("service ldn url one")
                                .build();

        NotifyServiceEntity notifyServiceEntityTwo =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name two")
                                .withDescription("service description two")
                                .withUrl("service url two")
                                .withLdnUrl("service ldn url two")
                                .build();

        NotifyServiceEntity notifyServiceEntityThree =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name three")
                                .withDescription("service description three")
                                .withUrl("service url three")
                                .withLdnUrl("service ldn url three")
                                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/ldnservices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.ldnservices", containsInAnyOrder(
                matchNotifyService(notifyServiceEntityOne.getID(), "service name one", "service description one",
                    "service url one", "service ldn url one"),
                matchNotifyService(notifyServiceEntityTwo.getID(), "service name two", "service description two",
                    "service url two", "service ldn url two"),
                matchNotifyService(notifyServiceEntityThree.getID(), "service name three", "service description three",
                    "service url three", "service ldn url three")
            )));
    }

    @Test
    public void findOneUnAuthorizedTest() throws Exception {
        getClient().perform(get("/api/ldn/ldnservices/1"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {

        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(get("/api/ldn/ldnservices/" + RandomUtils.nextInt()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/" + notifyServiceEntity.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                    "service url", "service ldn url")));
    }

    @Test
    public void createTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        NotifyServiceRest notifyServiceRest = new NotifyServiceRest();
        notifyServiceRest.setName("service name");
        notifyServiceRest.setDescription("service description");
        notifyServiceRest.setUrl("service url");
        notifyServiceRest.setLdnUrl("service ldn url");

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(post("/api/ldn/ldnservices")
                                .content(mapper.writeValueAsBytes(notifyServiceRest))
                                .contentType(contentType))
                            .andExpect(status().isCreated())
                            .andExpect(jsonPath("$", matchNotifyService("service name", "service description",
                                "service url", "service ldn url")))
                            .andDo(result ->
                                idRef.set((read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/" + idRef.get()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                matchNotifyService(idRef.get(), "service name", "service description",
                    "service url", "service ldn url")));
    }

    @Test
    public void patchNewPatternsTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntityOne =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name one")
                                .withDescription("service description one")
                                .withUrl("service url one")
                                .withLdnUrl("service ldn url one")
                                .build();

        NotifyServiceEntity notifyServiceEntityTwo =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name two")
                                .withDescription("service description two")
                                .withUrl("service url two")
                                .withLdnUrl("service ldn url two")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation inboundReplaceOperationOne = new ReplaceOperation("notifyservices_inbound_patterns",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        ReplaceOperation inboundReplaceOperationTwo = new ReplaceOperation("notifyservices_inbound_patterns",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"false\"}");

        ReplaceOperation outboundReplaceOperation = new ReplaceOperation("notifyservices_outbound_patterns",
            "{\"pattern\":\"patternC\",\"constraint\":\"itemFilterC\"}");
        ops.add(inboundReplaceOperationOne);
        ops.add(inboundReplaceOperationTwo);
        ops.add(outboundReplaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntityOne.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(1)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntityOne.getID(), "service name one", "service description one",
                        "service url one", "service ldn url one"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", false)
                    )),
                    hasJsonPath("$.notifyServiceOutboundPatterns",
                        hasItem(matchNotifyServicePattern("patternC", "itemFilterC")))
                )));

        patchBody = getPatchContent(List.of(ops.get(0)));
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntityTwo.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(1)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntityTwo.getID(), "service name two", "service description two",
                        "service url two", "service ldn url two"),
                    hasJsonPath("$.notifyServiceInboundPatterns", hasItem(
                        matchNotifyServicePattern("patternA", "itemFilterA", false)
                    )),
                    hasJsonPath("$.notifyServiceOutboundPatterns", empty())
                )));
    }

    @Test
    public void findByLdnUrlUnAuthorizedTest() throws Exception {
        getClient().perform(get("/api/ldn/ldnservices/search/byLdnUrl")
                       .param("ldnUrl", "test"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByLdnUrlBadRequestTest() throws Exception {
        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(get("/api/ldn/ldnservices/search/byLdnUrl"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void findByLdnUrlTest() throws Exception {
        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntityOne =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name one")
                                .withDescription("service description one")
                                .withUrl("service url one")
                                .withLdnUrl("service ldn url one")
                                .build();

      NotifyServiceEntity notifyServiceEntityTwo =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name two")
                                .withDescription("service description two")
                                .withUrl("service url two")
                                .withLdnUrl("service ldn url two")
                                .build();

      NotifyServiceBuilder.createNotifyServiceBuilder(context)
                          .withName("service name three")
                          .withDescription("service description three")
                          .withUrl("service url three")
                          .withLdnUrl("service ldn url three")
                          .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/search/byLdnUrl")
                .param("ldnUrl", notifyServiceEntityOne.getLdnUrl()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntityOne.getID(),
                "service name one", "service description one",
                "service url one", "service ldn url one")));
    }

    @Test
    public void deleteUnAuthorizedTest() throws Exception {
        getClient().perform(delete("/api/ldn/ldnservices/" + RandomUtils.nextInt()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteNotFoundTest() throws Exception {
        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(delete("/api/ldn/ldnservices/" + RandomUtils.nextInt()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteTest() throws Exception {
        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldnUrl")
                                .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(delete("/api/ldn/ldnservices/" + notifyServiceEntity.getID()))
            .andExpect(status().isNoContent());

        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/" + notifyServiceEntity.getID()))
            .andExpect(status().isNotFound());
    }


}