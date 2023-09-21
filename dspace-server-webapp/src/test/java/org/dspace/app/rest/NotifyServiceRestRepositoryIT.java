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
import static org.hamcrest.Matchers.contains;
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
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
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
    public void createForbiddenTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        NotifyServiceRest notifyServiceRest = new NotifyServiceRest();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(post("/api/ldn/ldnservices")
                                .content(mapper.writeValueAsBytes(notifyServiceRest))
                                .contentType(contentType))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void createTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        NotifyServiceRest notifyServiceRest = new NotifyServiceRest();
        notifyServiceRest.setName("service name");
        notifyServiceRest.setDescription("service description");
        notifyServiceRest.setUrl("service url");
        notifyServiceRest.setLdnUrl("service ldn url");
        notifyServiceRest.setStatus(false);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(post("/api/ldn/ldnservices")
                                .content(mapper.writeValueAsBytes(notifyServiceRest))
                                .contentType(contentType))
                            .andExpect(status().isCreated())
                            .andExpect(jsonPath("$", matchNotifyService("service name", "service description",
                                "service url", "service ldn url", false)))
                            .andDo(result ->
                                idRef.set((read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/" + idRef.get()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                matchNotifyService(idRef.get(), "service name", "service description",
                    "service url", "service ldn url", false)));
    }

    @Test
    public void notifyServicePatchOperationForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation operation = new AddOperation("/description", "add service description");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    public void notifyServiceDescriptionAddOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation operation = new AddOperation("/description", "add service description");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void notifyServiceDescriptionAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .withStatus(false)
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation operation = new AddOperation("/description", "add service description");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "add service description", "service url", "service ldn url", false))
            );
    }

    @Test
    public void notifyServiceDescriptionReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/description", "service description replaced");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void notifyServiceDescriptionReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/description", "service description replaced");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description replaced", "service url", "service ldn url", true))
            );
    }

    @Test
    public void notifyServiceDescriptionRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .withStatus(false)
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation operation = new RemoveOperation("/description");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                null, "service url", "service ldn url", false))
            );
    }

    @Test
    public void notifyServiceUrlAddOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation operation = new AddOperation("/url", "add service url");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void notifyServiceUrlAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation operation = new AddOperation("/url", "add service url");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", "add service url", "service ldn url", true))
            );
    }

    @Test
    public void notifyServiceUrlReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/url", "service url replaced");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void notifyServiceUrlReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/url", "service url replaced");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", "service url replaced", "service ldn url", true))
            );
    }

    @Test
    public void notifyServiceUrlRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation operation = new RemoveOperation("/url");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", null, "service ldn url"))
            );
    }

    @Test
    public void notifyServiceNameReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/name", "service name replaced");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void notifyServiceNameReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/name", "service name replaced");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name replaced",
                "service description", "service url", "service ldn url"))
            );
    }

    @Test
    public void notifyServiceLdnUrlReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withUrl("service url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/ldnurl", "service ldn url replaced");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void notifyServiceLdnUrlReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/ldnurl", "service ldn url replaced");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", "service url", "service ldn url replaced"))
            );
    }

    @Test
    public void notifyServiceNameRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation operation = new RemoveOperation("/name");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void notifyServiceLdnUrlRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation operation = new RemoveOperation("/ldnurl");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnprocessableEntity());
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
    public void deleteForbiddenTest() throws Exception {
        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(delete("/api/ldn/ldnservices/" + RandomUtils.nextInt()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void deleteNotFoundTest() throws Exception {
        getClient(getAuthToken(admin.getEmail(), password))
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

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(delete("/api/ldn/ldnservices/" + notifyServiceEntity.getID()))
            .andExpect(status().isNoContent());

        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/" + notifyServiceEntity.getID()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void NotifyServiceInboundPatternsAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternsAddOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        // patch add operation but pattern is already existed
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceOutboundPatternsAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));
    }

    @Test
    public void NotifyServiceOutboundPatternsAddOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        // patch add operation but pattern is already existed
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceInboundPatternRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyservices_inbound_patterns[0]");
        ops.clear();
        ops.add(inboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(1)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", hasItem(
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternsRemoveOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperation = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        ops.add(inboundAddOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(1)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false)
                    ))
                )));

        // index out of the range
        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyservices_inbound_patterns[1]");
        ops.clear();
        ops.add(inboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceOutboundPatternRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        RemoveOperation outboundRemoveOperation = new RemoveOperation("notifyservices_outbound_patterns[0]");
        ops.clear();
        ops.add(outboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(1)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", hasItem(
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));
    }

    @Test
    public void NotifyServiceOutboundPatternsRemoveOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperation = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        ops.add(outboundAddOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(1)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", hasItem(
                        matchNotifyServicePattern("patternA", "itemFilterA")
                    ))
                )));

        // index out of the range
        RemoveOperation outboundRemoveOperation = new RemoveOperation("notifyservices_outbound_patterns[1]");
        ops.clear();
        ops.add(outboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceInboundPatternConstraintAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":null,\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", null, false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        AddOperation inboundAddOperation = new AddOperation("notifyservices_inbound_patterns[0]/constraint",
            "itemFilterA");
        ops.clear();
        ops.add(inboundAddOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternConstraintAddOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        AddOperation inboundAddOperation = new AddOperation("notifyservices_inbound_patterns[0]/constraint",
            "itemFilterA");
        ops.clear();
        ops.add(inboundAddOperation);
        patchBody = getPatchContent(ops);

        // constraint at index 0 already has value
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceInboundPatternConstraintReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns[0]/constraint",
            "itemFilterC");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterC", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternConstraintReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":null,\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", null, false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns[0]/constraint",
            "itemFilterA");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        // constraint at index 0 is null
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceInboundPatternConstraintRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyservices_inbound_patterns[1]/constraint");
        ops.clear();
        ops.add(inboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", null, true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternConstraintRemoveOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperation = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        ops.add(inboundAddOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(1)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false)
                    ))
                )));

        // index out of the range
        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyservices_inbound_patterns[1]/constraint");
        ops.clear();
        ops.add(inboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceOutboundPatternConstraintAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":null}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":null}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", null),
                        matchNotifyServicePattern("patternB", null)
                    ))
                )));

        AddOperation outboundAddOperation = new AddOperation("notifyservices_outbound_patterns[1]/constraint",
            "itemFilterB");
        ops.clear();
        ops.add(outboundAddOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", null),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));
    }

    @Test
    public void NotifyServiceOutboundPatternConstraintAddOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        AddOperation outboundAddOperation = new AddOperation("notifyservices_outbound_patterns[1]/constraint",
            "itemFilterB");
        ops.clear();
        ops.add(outboundAddOperation);
        patchBody = getPatchContent(ops);

        // constraint at index 1 already has value
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceOutboundPatternConstraintReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        ReplaceOperation outboundReplaceOperation = new ReplaceOperation(
            "notifyservices_outbound_patterns[1]/constraint", "itemFilterD");
        ops.clear();
        ops.add(outboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterD")
                    ))
                )));
    }

    @Test
    public void NotifyServiceOutboundPatternConstraintReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":null}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", null)
                    ))
                )));

        ReplaceOperation outboundReplaceOperation = new ReplaceOperation(
            "notifyservices_outbound_patterns[1]/constraint", "itemFilterB");
        ops.clear();
        ops.add(outboundReplaceOperation);
        patchBody = getPatchContent(ops);

        // constraint at index 1 is null
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceOutboundPatternConstraintRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        RemoveOperation outboundRemoveOperation = new RemoveOperation("notifyservices_outbound_patterns[0]/constraint");
        ops.clear();
        ops.add(outboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", null),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));
    }

    @Test
    public void NotifyServiceOutboundPatternConstraintRemoveOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperation = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        ops.add(outboundAddOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(1)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", hasItem(
                        matchNotifyServicePattern("patternA", "itemFilterA")
                    ))
                )));

        // index out of the range
        RemoveOperation outboundRemoveOperation = new RemoveOperation("notifyservices_outbound_patterns[1]/constraint");
        ops.clear();
        ops.add(outboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceInboundPatternPatternAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":null,\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern(null, "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        AddOperation inboundAddOperation = new AddOperation("notifyservices_inbound_patterns[0]/pattern",
            "patternA");
        ops.clear();
        ops.add(inboundAddOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternPatternAddOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        AddOperation inboundAddOperation = new AddOperation("notifyservices_inbound_patterns[0]/pattern",
            "patternA");
        ops.clear();
        ops.add(inboundAddOperation);
        patchBody = getPatchContent(ops);

        // pattern at index 0 already has value
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceInboundPatternPatternReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns[0]/pattern",
            "patternC");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternC", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternPatternReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":null,\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern(null, "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns[0]/pattern",
            "patternA");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        // pattern at index 0 is null
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceInboundPatternAutomaticReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns[0]/automatic",
            "true");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", true),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternAutomaticReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns[0]/automatic",
            "test");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        // patch not boolean value
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceOutboundPatternPatternAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":null,\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern(null, "itemFilterB")
                    ))
                )));

        AddOperation outboundAddOperation = new AddOperation("notifyservices_outbound_patterns[1]/pattern",
            "patternB");
        ops.clear();
        ops.add(outboundAddOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));
    }

    @Test
    public void NotifyServiceOutboundPatternPatternAddOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        AddOperation outboundAddOperation = new AddOperation("notifyservices_outbound_patterns[1]/pattern",
            "patternB");
        ops.clear();
        ops.add(outboundAddOperation);
        patchBody = getPatchContent(ops);

        // pattern at index 1 already has value
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceOutboundPatternPatternReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        ReplaceOperation outboundReplaceOperation = new ReplaceOperation("notifyservices_outbound_patterns[1]/pattern",
            "patternD");
        ops.clear();
        ops.add(outboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternD", "itemFilterB")
                    ))
                )));
    }

    @Test
    public void NotifyServiceOutboundPatternPatternReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":null,\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern(null, "itemFilterB")
                    ))
                )));

        ReplaceOperation outboundReplaceOperation = new ReplaceOperation("notifyservices_outbound_patterns[1]/pattern",
            "patternB");
        ops.clear();
        ops.add(outboundReplaceOperation);
        patchBody = getPatchContent(ops);

        // pattern at index 1 is null
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceInboundPatternsReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns",
            "[{\"pattern\":\"patternC\",\"constraint\":\"itemFilterC\",\"automatic\":\"true\"}," +
                "{\"pattern\":\"patternD\",\"constraint\":\"itemFilterD\",\"automatic\":\"true\"}]");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternC", "itemFilterC", true),
                        matchNotifyServicePattern("patternD", "itemFilterD", true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternsReplaceWithEmptyArrayOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        // empty array will only remove all old patterns
        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns", "[]");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()));
    }

    @Test
    public void NotifyServiceInboundPatternsReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        // value must be an array not object
        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceOutboundPatternsReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        ReplaceOperation outboundReplaceOperation = new ReplaceOperation("notifyservices_outbound_patterns",
            "[{\"pattern\":\"patternC\",\"constraint\":\"itemFilterC\"}," +
                "{\"pattern\":\"patternD\",\"constraint\":\"itemFilterD\"}]");
        ops.clear();
        ops.add(outboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternC", "itemFilterC"),
                        matchNotifyServicePattern("patternD", "itemFilterD")
                    ))
                )));
    }

    @Test
    public void NotifyServiceOutboundPatternsReplaceWithEmptyArrayOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        // empty array will only remove all old patterns
        ReplaceOperation outboundReplaceOperation = new ReplaceOperation("notifyservices_outbound_patterns", "[]");
        ops.clear();
        ops.add(outboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()));
    }

    @Test
    public void NotifyServiceOutboundPatternsReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        // value must be an array not object
        ReplaceOperation outboundReplaceOperation = new ReplaceOperation("notifyservices_outbound_patterns",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");
        ops.clear();
        ops.add(outboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void NotifyServiceInboundPatternsRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyservices_inbound_patterns");
        ops.clear();
        ops.add(inboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()));
    }

    @Test
    public void NotifyServiceOutboundPatternsRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        RemoveOperation outboundRemoveOperation = new RemoveOperation("notifyservices_outbound_patterns");
        ops.clear();
        ops.add(outboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()));
    }

    @Test
    public void NotifyServiceInboundPatternReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyservices_inbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\",\"automatic\":\"true\"}");

        ops.add(inboundAddOperationOne);
        ops.add(inboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyservices_inbound_patterns[1]",
            "{\"pattern\":\"patternC\",\"constraint\":\"itemFilterC\",\"automatic\":\"false\"}");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternC", "itemFilterC", false)
                    ))
                )));
    }

    @Test
    public void NotifyServiceOutboundPatternReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation outboundAddOperationOne = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\"}");

        AddOperation outboundAddOperationTwo = new AddOperation("notifyservices_outbound_patterns/-",
            "{\"pattern\":\"patternB\",\"constraint\":\"itemFilterB\"}");

        ops.add(outboundAddOperationOne);
        ops.add(outboundAddOperationTwo);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));

        ReplaceOperation outboundReplaceOperation = new ReplaceOperation("notifyservices_outbound_patterns[0]",
            "{\"pattern\":\"patternC\",\"constraint\":\"itemFilterC\"}");
        ops.clear();
        ops.add(outboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "service url", "service ldn url"),
                    hasJsonPath("$.notifyServiceOutboundPatterns", contains(
                        matchNotifyServicePattern("patternC", "itemFilterC"),
                        matchNotifyServicePattern("patternB", "itemFilterB")
                    ))
                )));
    }

    @Test
    public void NotifyServiceStatusReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .withStatus(true)
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("/status", "false");
        ops.add(inboundReplaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$.notifyServiceOutboundPatterns", empty()))
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", "service url", "service ldn url", false)));
    }

    @Test
    public void NotifyServiceStatusReplaceOperationTestBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context)
                                .withName("service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("/status", "test");
        ops.add(inboundReplaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        // patch not boolean value
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());
    }

}