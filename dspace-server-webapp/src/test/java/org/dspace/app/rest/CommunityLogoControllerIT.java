/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CommunityBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class CommunityLogoControllerIT extends AbstractControllerIntegrationTest {

    private ObjectMapper mapper;
    private String adminAuthToken;
    private String bitstreamContent;
    private MockMultipartFile bitstreamFile;

    @Before
    public void createStructure() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        adminAuthToken = getAuthToken(admin.getEmail(), password);
        bitstreamContent = "Hello, World!";
        bitstreamFile = new MockMultipartFile("file",
                "hello.txt", MediaType.TEXT_PLAIN_VALUE,
                bitstreamContent.getBytes());
        mapper = new ObjectMapper();
    }

    private String createLogoInternal() throws Exception {
        MvcResult mvcPostResult = getClient(adminAuthToken).perform(
                MockMvcRequestBuilders.multipart(getLogoUrlTemplate(parentCommunity.getID().toString()))
                        .file(bitstreamFile))
                .andExpect(status().isCreated())
                .andReturn();

        String postContent = mvcPostResult.getResponse().getContentAsString();
        Map<String, Object> mapPostResult = mapper.readValue(postContent, Map.class);
        return String.valueOf(mapPostResult.get("uuid"));
    }

    @Test
    public void createLogoNotLoggedIn() throws Exception {
        getClient().perform(
                MockMvcRequestBuilders.multipart(getLogoUrlTemplate(parentCommunity.getID().toString()))
                .file(bitstreamFile))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createLogo() throws Exception {
        String postUuid = createLogoInternal();
        assert (postUuid != null);

        MvcResult mvcGetResult = getClient().perform(get(getLogoUrlTemplate(parentCommunity.getID().toString())))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String getContent = mvcGetResult.getResponse().getContentAsString();
        Map<String, Object> mapGetResult = mapper.readValue(getContent, Map.class);
        String getUuid = String.valueOf(mapGetResult.get("uuid"));
        assert (postUuid.equals(getUuid));
    }

    @Test
    public void createLogoNoRights() throws Exception {
        String userToken = getAuthToken(eperson.getEmail(), password);
        getClient(userToken).perform(
                MockMvcRequestBuilders.multipart(getLogoUrlTemplate(parentCommunity.getID().toString()))
                        .file(bitstreamFile))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createDuplicateLogo() throws Exception {
        getClient(adminAuthToken).perform(
                MockMvcRequestBuilders.multipart(getLogoUrlTemplate(parentCommunity.getID().toString()))
                        .file(bitstreamFile))
                .andExpect(status().isCreated());

        getClient(adminAuthToken).perform(
                MockMvcRequestBuilders.multipart(getLogoUrlTemplate(parentCommunity.getID().toString()))
                        .file(bitstreamFile))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createLogoForNonexisting() throws Exception {
        getClient(adminAuthToken).perform(
                MockMvcRequestBuilders.multipart(getLogoUrlTemplate("16a4b65b-3b3f-4ef5-8058-ef6f5a653ef9"))
                        .file(bitstreamFile))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteLogoNotLoggedIn() throws Exception {
        String postUuid = createLogoInternal();

        getClient().perform(delete(getBitstreamUrlTemplate(postUuid)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteLogo() throws Exception {
        String postUuid = createLogoInternal();

        getClient(adminAuthToken).perform(delete(getBitstreamUrlTemplate(postUuid)))
                .andExpect(status().isNoContent());

        getClient(adminAuthToken).perform(get(getLogoUrlTemplate(parentCommunity.getID().toString())))
                .andExpect(status().isNoContent());
    }

    @Test
    public void deleteLogoNoRights() throws Exception {
        String postUuid = createLogoInternal();

        String userToken = getAuthToken(eperson.getEmail(), password);
        getClient(userToken).perform(delete(getBitstreamUrlTemplate(postUuid)))
                .andExpect(status().isForbidden());
    }

    private String getLogoUrlTemplate(String uuid) {
        return "/api/core/communities/" + uuid + "/logo";
    }

    private String getBitstreamUrlTemplate(String uuid) {
        return "/api/core/bitstreams/" + uuid;
    }
}
