/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.model.StagedUploadRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.StagedUploadService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Collection;
import org.dspace.content.service.BitstreamService;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;

/** Exercise staging with real REST authentication and the JSON script invocation that consumes it. */
@Import(StagedUploadsControllerIT.UploadConfiguration.class)
public class StagedUploadsControllerIT extends AbstractControllerIntegrationTest {
    private static final String ENDPOINT = "/api/core/uploads";
    private static final String IMPORT = "/api/system/scripts/import/processes";
    @ClassRule
    public static TemporaryFolder temporary = new TemporaryFolder();
    @Autowired
    private StagedUploadService uploads;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ProcessService processes;
    @Autowired
    private BitstreamService bitstreams;
    @Autowired
    private ConfigurationService configuration;

    @Test
    public void creationValidatesSizeAndIsDiscoverableFromTheApiRoot() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api"))
            .andExpect(jsonPath("$._links.uploads.href", endsWith(ENDPOINT)));
        getClient(token).perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"too-large.zip\",\"size\":1048577}"))
            .andExpect(status().isPayloadTooLarge());
        getClient(token).perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"../unsafe.zip\",\"size\":4}"))
            .andExpect(status().isBadRequest());
        String receipt = getClient(token).perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"batch.zip\",\"size\":4}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.receivedBytes").value(0))
            .andExpect(jsonPath("$.chunkSize").value(1024))
            .andExpect(jsonPath("$.state").value("UPLOADING"))
            .andExpect(jsonPath("$._links.chunks.templated").value(true))
            .andExpect(jsonPath("$._links.result").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        String id = read(receipt, "$.id");
        getClient(token).perform(delete(ENDPOINT + "/" + id)).andExpect(status().isNoContent());
        getClient(token).perform(get(ENDPOINT + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    public void everyOperationRequiresAuthentication() throws Exception {
        String path = ENDPOINT + "/" + UUID.randomUUID();
        getClient().perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"batch.zip\",\"size\":4}")).andExpect(status().isUnauthorized());
        getClient().perform(get(path)).andExpect(status().isUnauthorized());
        getClient().perform(put(path + "/chunks/0").contentType(MediaType.APPLICATION_OCTET_STREAM)
            .content(new byte[] {1, 2, 3, 4})).andExpect(status().isUnauthorized());
        getClient().perform(delete(path)).andExpect(status().isUnauthorized());
        getClient().perform(post(IMPORT).contentType(MediaType.APPLICATION_JSON)
            .content("{\"properties\":[],\"uploads\":[\"" + UUID.randomUUID() + "\"]}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void ownershipIsEnforcedEvenForAnAdministrator() throws Exception {
        StagedUploadRest upload = uploads.create(eperson.getID(), "private.zip", 4);
        String path = ENDPOINT + "/" + upload.id();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get(path)).andExpect(status().isNotFound());
        getClient(token).perform(put(path + "/chunks/0").contentType(MediaType.APPLICATION_OCTET_STREAM)
            .content(new byte[] {1, 2, 3, 4})).andExpect(status().isNotFound());
        getClient(token).perform(delete(path)).andExpect(status().isNotFound());
        getClient(token).perform(post(IMPORT).contentType(MediaType.APPLICATION_JSON)
            .content("{\"properties\":[{\"name\":\"--add\"}],\"uploads\":[\"" + upload.id() + "\"]}"))
            .andExpect(status().isNotFound());
        uploads.delete(eperson.getID(), upload.id());
    }

    @Test
    public void incompleteAndOversizedChunksCannotAdvanceOrBeConsumed() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        StagedUploadRest upload = uploads.create(admin.getID(), "batch.zip", 4);
        String path = ENDPOINT + "/" + upload.id();
        getClient(token).perform(put(path + "/chunks/0").contentType(MediaType.APPLICATION_OCTET_STREAM)
            .content(new byte[] {1, 2})).andExpect(status().isBadRequest());
        getClient(token).perform(put(path + "/chunks/0").contentType(MediaType.APPLICATION_OCTET_STREAM)
            .content(new byte[] {1, 2, 3, 4, 5})).andExpect(status().isPayloadTooLarge());
        getClient(token).perform(get(path)).andExpect(jsonPath("$.receivedBytes").value(0));
        getClient(token).perform(post(IMPORT).contentType(MediaType.APPLICATION_JSON)
            .content("{\"properties\":[{\"name\":\"--add\"}],\"uploads\":[\"" + upload.id() + "\"]}"))
            .andExpect(status().isConflict());
        uploads.delete(admin.getID(), upload.id());
    }

    @Test
    public void aUserWhoMayNotRunTheScriptKeepsTheUploadResumable() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        StagedUploadRest upload = uploads.create(eperson.getID(), "batch.zip", 4);
        String path = ENDPOINT + "/" + upload.id();
        getClient(token).perform(put(path + "/chunks/0").contentType(MediaType.APPLICATION_OCTET_STREAM)
            .content(new byte[] {1, 2, 3, 4})).andExpect(status().isOk());
        getClient(token).perform(post(IMPORT).contentType(MediaType.APPLICATION_JSON)
            .content("{\"properties\":[{\"name\":\"--add\"}],\"uploads\":[\"" + upload.id() + "\"]}"))
            .andExpect(status().isForbidden());
        getClient(token).perform(get(path))
            .andExpect(jsonPath("$.state").value("UPLOADING"))
            .andExpect(jsonPath("$.receivedBytes").value(4));
        uploads.delete(eperson.getID(), upload.id());
    }

    @Test
    public void stagedFileBecomesOneNormalProcessWithItsInputFile() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("SAF community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("SAF collection").build();
        context.restoreAuthSystemState();
        Files.createDirectories(Path.of(configuration.getProperty("org.dspace.app.batchitemimport.work.dir")));
        byte[] file;
        try (InputStream input = getClass().getResourceAsStream("/org/dspace/app/itemimport/saf-bitstreams.zip")) {
            file = input.readAllBytes();
        }
        String token = getAuthToken(admin.getEmail(), password);
        String receipt = getClient(token).perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("name", "batch.zip", "size", file.length))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = read(receipt, "$.id");
        String path = ENDPOINT + "/" + id;
        for (int offset = 0; offset < file.length; offset += 1024) {
            byte[] chunk = Arrays.copyOfRange(file, offset, Math.min(offset + 1024, file.length));
            // Repeat every PUT to simulate a lost acknowledgement.
            for (int attempt = 0; attempt < 2; attempt++) {
                getClient(token).perform(put(path + "/chunks/" + offset)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).content(chunk))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.receivedBytes").value(offset + chunk.length));
            }
        }
        String body = mapper.writeValueAsString(Map.of(
            "properties", List.of(Map.of("name", "--add"), Map.of("name", "--zip", "value", "batch.zip"),
                Map.of("name", "-v"), Map.of("name", "--collection", "value", collection.getID().toString())),
            "uploads", List.of(id)));
        Integer processId = null;
        try {
            String response = getClient(token).perform(post(IMPORT)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
            processId = read(response, "$.processId");
            getClient(token).perform(post(IMPORT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.processId").value(processId));
            getClient(token).perform(get("/api/system/processes/" + processId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.processStatus").value("COMPLETED"));
            getClient(token).perform(get(path))
                .andExpect(jsonPath("$.state").value("CONSUMED"))
                .andExpect(jsonPath("$.receivedBytes").value(file.length))
                .andExpect(jsonPath("$._links.result.href", endsWith("/api/system/processes/" + processId)));
            Process process = processes.find(context, processId);
            // Process input files are readable by their creator; the test context user is a plain eperson.
            context.turnOffAuthorisationSystem();
            try (InputStream input = bitstreams.retrieve(context,
                processes.getBitstreamByName(context, process, "batch.zip"))) {
                assertArrayEquals(file, input.readAllBytes());
            } finally {
                context.restoreAuthSystemState();
            }
        } finally {
            if (processId != null) {
                ProcessBuilder.deleteProcess(processId);
            }
        }
    }

    @Test
    public void jsonInvocationWithoutUploadsStartsAProcess() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        String body = mapper.writeValueAsString(Map.of(
            "properties", List.of(Map.of("name", "-r", "value", "test"), Map.of("name", "-i")),
            "uploads", List.of()));
        Integer processId = null;
        try {
            String response = getClient(token).perform(post("/api/system/scripts/mock-script/processes")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.scriptName").value("mock-script"))
                .andReturn().getResponse().getContentAsString();
            processId = read(response, "$.processId");
        } finally {
            if (processId != null) {
                ProcessBuilder.deleteProcess(processId);
            }
        }
    }

    /** Isolated staging, with deliberately small chunks to exercise the complete HTTP flow. */
    @TestConfiguration
    public static class UploadConfiguration {
        /** Keep test staging outside the normal test installation's configuration. */
        @Bean
        @Primary
        public StagedUploadService testStagedUploads() {
            return new StagedUploadService(temporary.getRoot().toPath(), 1048576, 1024, 10,
                                           Duration.ofHours(1), Clock.systemUTC());
        }
    }
}
