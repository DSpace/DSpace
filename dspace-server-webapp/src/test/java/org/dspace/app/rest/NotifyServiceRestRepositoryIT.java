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
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.rest.model.NotifyServiceInboundPatternRest;
import org.dspace.app.rest.model.NotifyServiceRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.NotifyServiceRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.NotifyServiceBuilder;
import org.dspace.builder.NotifyServiceInboundPatternBuilder;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Integration test class for {@link NotifyServiceRestRepository}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private NotifyService notifyService;

    @Test
    public void findAllUnAuthorizedTest() throws Exception {
        getClient().perform(get("/api/ldn/ldnservices"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntityOne =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name one")
                                .withDescription("service description one")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        NotifyServiceEntity notifyServiceEntityTwo =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name two")
                                .withDescription("service description two")
                                .withUrl("https://service2.ldn.org/about")
                                .withLdnUrl("https://service2.ldn.org/inbox")
                                .build();

        NotifyServiceEntity notifyServiceEntityThree =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name three")
                                .withDescription("service description three")
                                .withUrl("https://service3.ldn.org/about")
                                .withLdnUrl("https://service3.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/ldnservices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.ldnservices", containsInAnyOrder(
                matchNotifyService(notifyServiceEntityOne.getID(), "service name one", "service description one",
                    "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                matchNotifyService(notifyServiceEntityTwo.getID(), "service name two", "service description two",
                    "https://service2.ldn.org/about", "https://service2.ldn.org/inbox"),
                matchNotifyService(notifyServiceEntityThree.getID(), "service name three", "service description three",
                    "https://service3.ldn.org/about", "https://service3.ldn.org/inbox")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/" + notifyServiceEntity.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                    "https://service.ldn.org/about", "https://service.ldn.org/inbox")));
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
    public void createTestScoreFail() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        NotifyServiceInboundPatternRest inboundPatternRestOne = new NotifyServiceInboundPatternRest();
        inboundPatternRestOne.setPattern("patternA");
        inboundPatternRestOne.setConstraint("itemFilterA");
        inboundPatternRestOne.setAutomatic(true);

        NotifyServiceInboundPatternRest inboundPatternRestTwo = new NotifyServiceInboundPatternRest();
        inboundPatternRestTwo.setPattern("patternB");
        inboundPatternRestTwo.setAutomatic(false);

        NotifyServiceRest notifyServiceRest = new NotifyServiceRest();
        notifyServiceRest.setName("service name");
        notifyServiceRest.setDescription("service description");
        notifyServiceRest.setUrl("service url");
        notifyServiceRest.setLdnUrl("service ldn url");
        notifyServiceRest.setScore(BigDecimal.TEN);
        notifyServiceRest.setNotifyServiceInboundPatterns(List.of(inboundPatternRestOne, inboundPatternRestTwo));
        notifyServiceRest.setEnabled(false);

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(post("/api/ldn/ldnservices")
            .content(mapper.writeValueAsBytes(notifyServiceRest))
            .contentType(contentType))
        .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        NotifyServiceInboundPatternRest inboundPatternRestOne = new NotifyServiceInboundPatternRest();
        inboundPatternRestOne.setPattern("patternA");
        inboundPatternRestOne.setConstraint("itemFilterA");
        inboundPatternRestOne.setAutomatic(true);

        NotifyServiceInboundPatternRest inboundPatternRestTwo = new NotifyServiceInboundPatternRest();
        inboundPatternRestTwo.setPattern("patternB");
        inboundPatternRestTwo.setAutomatic(false);

        NotifyServiceRest notifyServiceRest = new NotifyServiceRest();
        notifyServiceRest.setName("service name");
        notifyServiceRest.setDescription("service description");
        notifyServiceRest.setUrl("https://service.ldn.org/about");
        notifyServiceRest.setLdnUrl("https://service.ldn.org/inbox");
        notifyServiceRest.setNotifyServiceInboundPatterns(List.of(inboundPatternRestOne, inboundPatternRestTwo));
        notifyServiceRest.setEnabled(false);
        notifyServiceRest.setLowerIp("192.168.0.1");
        notifyServiceRest.setUpperIp("192.168.0.5");

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(post("/api/ldn/ldnservices")
                                .content(mapper.writeValueAsBytes(notifyServiceRest))
                                .contentType(contentType))
                            .andExpect(status().isCreated())
                            .andExpect(jsonPath("$", matchNotifyService("service name", "service description",
                                "https://service.ldn.org/about", "https://service.ldn.org/inbox", false,
                                "192.168.0.1", "192.168.0.5")))
                            .andDo(result ->
                                idRef.set((read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/" + idRef.get()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$", allOf(
                matchNotifyService(idRef.get(), "service name", "service description",
                    "https://service.ldn.org/about", "https://service.ldn.org/inbox", false,
                    "192.168.0.1", "192.168.0.5"),
                hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                    matchNotifyServicePattern("patternA", "itemFilterA", true),
                    matchNotifyServicePattern("patternB", null, false)
                )))
            ));

        // Delete the created service
        NotifyServiceBuilder.deleteNotifyService(idRef.get());
    }

    @Test
    public void notifyServicePatchOperationForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .isEnabled(false)
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
                "add service description", "https://service.ldn.org/about", "https://service.ldn.org/inbox", false))
            );
    }

    @Test
    public void notifyServiceDescriptionReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
                "service description replaced", "https://service.ldn.org/about",
                "https://service.ldn.org/inbox", false))
            );
    }

    @Test
    public void notifyServiceDescriptionRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .isEnabled(false)
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
                null, "https://service.ldn.org/about", "https://service.ldn.org/inbox", false))
            );
    }

    @Test
    public void notifyServiceUrlAddOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
                "service description", "add service url", "https://service.ldn.org/inbox", false))
            );
    }

    @Test
    public void notifyServiceUrlReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .isEnabled(true)
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
                "service description", "service url replaced", "https://service.ldn.org/inbox", true))
            );
    }

    @Test
    public void notifyServiceUrlRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
                "service description", null, "https://service.ldn.org/inbox"))
            );
    }

    // TODO: Test is currently ignored as the code in this test is IDENTICAL to the code in
    // "notifyServiceNameReplaceOperationTest". It's unclear WHY this should throw a 400 exception?
    @Test
    @Ignore
    public void notifyServiceNameReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
                "service description", "https://service.ldn.org/about", "https://service.ldn.org/inbox"))
            );
    }

    @Test
    public void notifyServiceLdnUrlReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withUrl("https://service.ldn.org/about")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
                "service description", "https://service.ldn.org/about", "service ldn url replaced"))
            );
    }

    @Test
    public void notifyServiceNameRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
    @Ignore
    /*
     * frabacche senseless because it's a mandatory+unique
     * entity field and also table column!
     */
    public void notifyServiceLdnUrlRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name one")
                                .withDescription("service description one")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

      NotifyServiceEntity notifyServiceEntityTwo =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name two")
                                .withDescription("service description two")
                                .withUrl("https://service2.ldn.org/about")
                                .withLdnUrl("https://service2.ldn.org/inbox")
                                .build();

      NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name three")
                          .withDescription("service description three")
                          .withUrl("https://service3.ldn.org/about")
                          .withLdnUrl("https://service3.ldn.org/inbox")
                          .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/search/byLdnUrl")
                .param("ldnUrl", notifyServiceEntityOne.getLdnUrl()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntityOne.getID(),
                "service name one", "service description one",
                "https://service.ldn.org/about", "https://service.ldn.org/inbox")));
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
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
    public void NotifyServiceInboundPatternRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyServiceInboundPatterns[0]");
        ops.clear();
        ops.add(inboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(1)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", hasItem(
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));
    }

    @Test
    public void NotifyServiceInboundPatternsRemoveOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperation = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false)
                    ))
                )));

        // index out of the range
        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyServiceInboundPatterns[1]");
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
    public void NotifyServiceInboundPatternConstraintAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":null,\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", null, false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        AddOperation inboundAddOperation = new AddOperation("notifyServiceInboundPatterns[0]/constraint",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        AddOperation inboundAddOperation = new AddOperation("notifyServiceInboundPatterns[0]/constraint",
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns[0]/constraint",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":null,\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", null, false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns[0]/constraint",
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyServiceInboundPatterns[1]/constraint");
        ops.clear();
        ops.add(inboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", hasSize(2)))
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperation = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false)
                    ))
                )));

        // index out of the range
        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyServiceInboundPatterns[1]/constraint");
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
    public void NotifyServiceInboundPatternPatternAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":null,\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern(null, "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        AddOperation inboundAddOperation = new AddOperation("notifyServiceInboundPatterns[0]/pattern",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        AddOperation inboundAddOperation = new AddOperation("notifyServiceInboundPatterns[0]/pattern",
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns[0]/pattern",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":null,\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern(null, "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns[0]/pattern",
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns[0]/automatic",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns[0]/automatic",
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
    public void NotifyServiceInboundPatternsReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
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
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        // empty array will only remove all old patterns
        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns", "[]");
        ops.clear();
        ops.add(inboundReplaceOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()));
    }

    @Test
    public void NotifyServiceInboundPatternsReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        // value must be an array not object
        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns",
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
    public void NotifyServiceInboundPatternsRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", containsInAnyOrder(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        RemoveOperation inboundRemoveOperation = new RemoveOperation("notifyServiceInboundPatterns");
        ops.clear();
        ops.add(inboundRemoveOperation);
        patchBody = getPatchContent(ops);

        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()));
    }

    @Test
    public void NotifyServiceInboundPatternReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation inboundAddOperationOne = new AddOperation("notifyServiceInboundPatterns/-",
            "{\"pattern\":\"patternA\",\"constraint\":\"itemFilterA\",\"automatic\":\"false\"}");

        AddOperation inboundAddOperationTwo = new AddOperation("notifyServiceInboundPatterns/-",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternB", "itemFilterB", true)
                    ))
                )));

        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("notifyServiceInboundPatterns[1]",
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
            .andExpect(jsonPath("$",
                allOf(
                    matchNotifyService(notifyServiceEntity.getID(), "service name", "service description",
                        "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                    hasJsonPath("$.notifyServiceInboundPatterns", contains(
                        matchNotifyServicePattern("patternA", "itemFilterA", false),
                        matchNotifyServicePattern("patternC", "itemFilterC", false)
                    ))
                )));
    }

    @Test
    public void findManualServicesByInboundPatternUnAuthorizedTest() throws Exception {
        getClient().perform(get("/api/ldn/ldnservices/search/byInboundPattern")
                       .param("pattern", "pattern"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findManualServicesByInboundPatternBadRequestTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/search/byInboundPattern"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void findManualServicesByInboundPatternTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntityOne =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name one")
                                .withDescription("service description one")
                                .withUrl("https://service.ldn.org/about")
                                .withStatus(true)
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();

        NotifyServiceEntity notifyServiceEntityTwo =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name two")
                                .withDescription("service description two")
                                .withUrl("https://service2.ldn.org/about")
                                .withStatus(true)
                                .withLdnUrl("https://service2.ldn.org/inbox")
                                .build();

        NotifyServiceEntity notifyServiceEntityThree =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name three")
                                .withDescription("service description")
                                .withUrl("https://service3.ldn.org/about")
                                .withStatus(true)
                                .withLdnUrl("https://service3.ldn.org/inbox")
                                .build();

        NotifyServiceInboundPatternBuilder.createNotifyServiceInboundPatternBuilder(context, notifyServiceEntityOne)
                                          .withPattern("review")
                                          .withConstraint("itemFilterA")
                                          .isAutomatic(false)
                                          .build();

        NotifyServiceInboundPatternBuilder.createNotifyServiceInboundPatternBuilder(context, notifyServiceEntityOne)
                                          .withPattern("review")
                                          .withConstraint("itemFilterB")
                                          .isAutomatic(true)
                                          .build();

        NotifyServiceInboundPatternBuilder.createNotifyServiceInboundPatternBuilder(context, notifyServiceEntityTwo)
                                          .withPattern("review")
                                          .withConstraint("itemFilterA")
                                          .isAutomatic(false)
                                          .build();

        NotifyServiceInboundPatternBuilder.createNotifyServiceInboundPatternBuilder(context, notifyServiceEntityTwo)
                                          .withPattern("review")
                                          .withConstraint("itemFilterB")
                                          .isAutomatic(true)
                                          .build();

        NotifyServiceInboundPatternBuilder.createNotifyServiceInboundPatternBuilder(context, notifyServiceEntityThree)
                                          .withPattern("review")
                                          .withConstraint("itemFilterB")
                                          .isAutomatic(true)
                                          .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/ldnservices/search/byInboundPattern")
                .param("pattern", "review"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(2)))
            .andExpect(jsonPath("$._embedded.ldnservices", containsInAnyOrder(
                matchNotifyService(notifyServiceEntityOne.getID(), "service name one", "service description one",
                "https://service.ldn.org/about", "https://service.ldn.org/inbox"),
                matchNotifyService(notifyServiceEntityTwo.getID(), "service name two", "service description two",
                    "https://service2.ldn.org/about", "https://service2.ldn.org/inbox")
                )));
    }

    @Test
    public void NotifyServiceStatusReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .isEnabled(true)
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("/enabled", "false");
        ops.add(inboundReplaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyServiceInboundPatterns", empty()))
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", "https://service.ldn.org/about", "https://service.ldn.org/inbox", false)));
    }

    @Test
    public void NotifyServiceScoreReplaceOperationTest() throws Exception {
        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withScore(BigDecimal.ZERO)
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("/score", "0.522");
        ops.add(inboundReplaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", "https://service.ldn.org/about", "https://service.ldn.org/inbox", false)))
            .andExpect(jsonPath("$.score", notNullValue()))
            .andExpect(jsonPath("$.score", closeTo(0.522d, 0.001d)));
    }

    @Test
    public void NotifyServiceScoreReplaceOperationTestUnprocessableTest() throws Exception {

        context.turnOffAuthorisationSystem();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .withScore(BigDecimal.ZERO)
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation inboundReplaceOperation = new ReplaceOperation("/score", "10");
        ops.add(inboundReplaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnprocessableEntity());
    }


    @Test
    public void notifyServiceScoreAddOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("service ldn url")
                                .isEnabled(false)
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation operation = new AddOperation("/score", "1");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", "service url", "service ldn url", false)))
            .andExpect(jsonPath("$.score", notNullValue()))
            .andExpect(jsonPath("$.score", closeTo(1d, 0.001d)))
        ;
    }

    @Override
    @After
    public void destroy() throws Exception {
        List<NotifyServiceEntity> notifyServiceEntities = notifyService.findAll(context);
        if (CollectionUtils.isNotEmpty(notifyServiceEntities)) {
            notifyServiceEntities.forEach(notifyServiceEntity -> {
                try {
                    notifyService.delete(context, notifyServiceEntity);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        super.destroy();
    }

    @Test
    public void notifyServiceLowerIpReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withUrl("https://service.ldn.org/about")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/lowerIp", "192.168.0.1");
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
    public void notifyServiceLowerIpReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .withLowerIp("192.168.0.1")
                                .withUpperIp("192.168.0.5")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/lowerIp", "192.168.0.2");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", "https://service.ldn.org/about", "https://service.ldn.org/inbox",
                false, "192.168.0.2", "192.168.0.5"))
            );
    }

    @Test
    public void notifyServiceLowerIpRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .withLowerIp("192.168.0.1")
                                .withUpperIp("192.168.0.5")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation operation = new RemoveOperation("/lowerIp");
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
    public void notifyServiceUpperIpReplaceOperationBadRequestTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withUrl("https://service.ldn.org/about")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/lowerIp", "192.168.0.8");
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
    public void notifyServiceUpperIpReplaceOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .withLowerIp("192.168.0.1")
                                .withUpperIp("192.168.0.5")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation operation = new ReplaceOperation("/upperIp", "192.168.0.8");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", matchNotifyService(notifyServiceEntity.getID(), "service name",
                "service description", "https://service.ldn.org/about", "https://service.ldn.org/inbox",
                false, "192.168.0.1", "192.168.0.8"))
            );
    }

    @Test
    public void notifyServiceUpperIpRemoveOperationTest() throws Exception {

        context.turnOffAuthorisationSystem();

        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://service.ldn.org/about")
                                .withLdnUrl("https://service.ldn.org/inbox")
                                .withLowerIp("192.168.0.1")
                                .withUpperIp("192.168.0.5")
                                .build();
        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation operation = new RemoveOperation("/upperIp");
        ops.add(operation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/ldnservices/" + notifyServiceEntity.getID())
                .content(patchBody)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnprocessableEntity());
    }

}