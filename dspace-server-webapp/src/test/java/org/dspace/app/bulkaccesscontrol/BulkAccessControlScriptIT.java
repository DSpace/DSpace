/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.app.matcher.ResourcePolicyMatcher.matches;
import static org.dspace.authorize.ResourcePolicy.TYPE_CUSTOM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Basic integration testing for the bulk access Import feature via UI {@link BulkAccessControl}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class BulkAccessControlScriptIT extends AbstractEntityIntegrationTest {

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ProcessService processService;

    private final static String SCRIPTS_ENDPOINT = "/api/" + ScriptRest.CATEGORY + "/" + ScriptRest.PLURAL_NAME;
    private final static String CURATE_SCRIPT_ENDPOINT = SCRIPTS_ENDPOINT + "/bulk-access-control/" +
        ProcessRest.PLURAL_NAME;

    @After
    @Override
    public void destroy() throws Exception {
        List<Process> processes = processService.findAll(context);
        for (Process process : processes) {
            ProcessBuilder.deleteProcess(process.getID());
        }

        super.destroy();
    }

    @Test
    public void bulkAccessScriptWithAdminUserTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Community subCommunity = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                 .withName("Sub Community")
                                                 .build();

        Collection collection = CollectionBuilder.createCollection(context, subCommunity)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Public item")
                               .withSubject("ExtraEntry")
                               .build();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile bitstreamFile =
            new MockMultipartFile("file", "test.json", MediaType.TEXT_PLAIN_VALUE, inputStream);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-u", item.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-f", "test.json"));


        List<ParameterValueRest> list =
            parameters.stream()
                      .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                      .collect(Collectors.toList());

        context.restoreAuthSystemState();

        try {
            String token = getAuthToken(admin.getEmail(), password);

            getClient(token)
                .perform(
                    multipart(CURATE_SCRIPT_ENDPOINT)
                        .file(bitstreamFile)
                        .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void bulkAccessScriptWithAdminUserOfTargetCommunityTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withAdminGroup(eperson)
                                          .build();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile bitstreamFile =
            new MockMultipartFile("file", "test.json", MediaType.TEXT_PLAIN_VALUE, inputStream);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-u", parentCommunity.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-f", "test.json"));


        List<ParameterValueRest> list =
            parameters.stream()
                      .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                      .collect(Collectors.toList());

        context.restoreAuthSystemState();

        try {
            String token = getAuthToken(eperson.getEmail(), password);

            getClient(token)
                .perform(
                    multipart(CURATE_SCRIPT_ENDPOINT)
                        .file(bitstreamFile)
                        .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void bulkAccessScriptWithAdminUserOfTargetCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Community subCommunity = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                 .withName("Sub Community")
                                                 .build();

        Collection collection = CollectionBuilder.createCollection(context, subCommunity)
                                                 .withName("collection")
                                                 .withAdminGroup(eperson)
                                                 .build();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile bitstreamFile =
            new MockMultipartFile("file", "test.json", MediaType.TEXT_PLAIN_VALUE, inputStream);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-u", collection.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-f", "test.json"));


        List<ParameterValueRest> list =
            parameters.stream()
                      .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                      .collect(Collectors.toList());

        context.restoreAuthSystemState();

        try {
            String token = getAuthToken(eperson.getEmail(), password);

            getClient(token)
                .perform(
                    multipart(CURATE_SCRIPT_ENDPOINT)
                        .file(bitstreamFile)
                        .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void bulkAccessScriptWithAdminUserOfTargetItemTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Community subCommunity = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                 .withName("Sub Community")
                                                 .build();

        Collection collection = CollectionBuilder.createCollection(context, subCommunity)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Public item")
                               .withSubject("ExtraEntry")
                               .withAdminUser(eperson)
                               .build();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile bitstreamFile =
            new MockMultipartFile("file", "test.json", MediaType.TEXT_PLAIN_VALUE, inputStream);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-u", item.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-f", "test.json"));


        List<ParameterValueRest> list =
            parameters.stream()
                      .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                      .collect(Collectors.toList());

        context.restoreAuthSystemState();

        try {
            String token = getAuthToken(eperson.getEmail(), password);

            getClient(token)
                .perform(
                    multipart(CURATE_SCRIPT_ENDPOINT)
                        .file(bitstreamFile)
                        .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void bulkAccessScriptWithMultipleTargetUuidsWithAdminUserTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Community subCommunity = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                 .withName("Sub Community")
                                                 .build();

        Collection collection = CollectionBuilder.createCollection(context, subCommunity)
                                                 .withName("collection")
                                                 .build();

        Item itemOne = ItemBuilder.createItem(context, collection)
                               .withTitle("Public item one")
                               .build();

        Item itemTwo = ItemBuilder.createItem(context, collection)
                                  .withTitle("Public item two")
                                  .build();

        Item itemThree = ItemBuilder.createItem(context, collection)
                                    .withTitle("Public item three")
                                    .build();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile bitstreamFile =
            new MockMultipartFile("file", "test.json", MediaType.TEXT_PLAIN_VALUE, inputStream);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-u", itemOne.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-u", itemTwo.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-u", itemThree.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-f", "test.json"));


        List<ParameterValueRest> list =
            parameters.stream()
                      .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                      .collect(Collectors.toList());

        context.restoreAuthSystemState();

        try {
            String token = getAuthToken(admin.getEmail(), password);

            getClient(token)
                .perform(
                    multipart(CURATE_SCRIPT_ENDPOINT)
                        .file(bitstreamFile)
                        .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));

            itemOne = context.reloadEntity(itemOne);
            itemTwo = context.reloadEntity(itemTwo);
            itemThree = context.reloadEntity(itemThree);

            Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

            assertThat(itemOne.getResourcePolicies(), hasSize(1));
            assertThat(itemTwo.getResourcePolicies(), hasSize(1));
            assertThat(itemThree.getResourcePolicies(), hasSize(1));

            assertThat(itemOne.getResourcePolicies(), hasItem(
                matches(Constants.READ, anonymousGroup, "openaccess", TYPE_CUSTOM)
            ));

            assertThat(itemTwo.getResourcePolicies(), hasItem(
                matches(Constants.READ, anonymousGroup, "openaccess", TYPE_CUSTOM)
            ));

            assertThat(itemThree.getResourcePolicies(), hasItem(
                matches(Constants.READ, anonymousGroup, "openaccess", TYPE_CUSTOM)
            ));

        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void bulkAccessScriptWithoutTargetUUIDParameterTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .withAdminGroup(eperson)
                                          .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token)
            .perform(
                multipart(CURATE_SCRIPT_ENDPOINT)
                    .param("properties", new ObjectMapper().writeValueAsString(List.of()))
            )
            .andExpect(status().isInternalServerError())
            .andExpect(result -> assertTrue(result.getResolvedException()
                                                  .getMessage()
                                                  .contains("At least one target uuid must be provided")));
    }

    @Test
    public void bulkAccessScriptWithNormalUserTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile bitstreamFile =
            new MockMultipartFile("file", "test.json", MediaType.TEXT_PLAIN_VALUE, inputStream);

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-u", parentCommunity.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-f", "test.json"));


        List<ParameterValueRest> list =
            parameters.stream()
                      .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                      .collect(Collectors.toList());

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token)
            .perform(
                multipart(CURATE_SCRIPT_ENDPOINT)
                    .file(bitstreamFile)
                    .param("properties", new ObjectMapper().writeValueAsString(list)))
            .andExpect(status().isForbidden());
    }

}
