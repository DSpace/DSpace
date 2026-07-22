/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.app.rest.matcher.ItemAuthorityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.authority.OpenAIREProjectAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test via REST for {@link OpenAIREProjectAuthority} class.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OpenAIREProjectAuthorityIT extends AbstractControllerIntegrationTest {

    private static MockedStatic<DSpaceHttpClientFactory> mockHttpClientFactory;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @BeforeClass
    public static void init() {
        mockHttpClientFactory = Mockito.mockStatic(DSpaceHttpClientFactory.class);
    }

    @AfterClass
    public static void close() {
        mockHttpClientFactory.close();
    }

    @Before
    public void setup() throws Exception {
        DSpaceHttpClientFactory mockFactory = Mockito.mock(DSpaceHttpClientFactory.class);
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = Mockito.mock(CloseableHttpResponse.class);
        StatusLine mockStatusLine = Mockito.mock(StatusLine.class);
        HttpEntity mockEntity = Mockito.mock(HttpEntity.class);

        String xml = IOUtils.resourceToString(
            "/org/dspace/external/openaire-projects.xml", StandardCharsets.UTF_8);
        InputStream xmlStream = IOUtils.toInputStream(xml, StandardCharsets.UTF_8);

        Mockito.when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        Mockito.when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        Mockito.when(mockResponse.getEntity()).thenReturn(mockEntity);
        Mockito.when(mockEntity.getContent()).thenReturn(xmlStream);
        Mockito.when(mockClient.execute(Mockito.any())).thenReturn(mockResponse);
        Mockito.when(mockFactory.buildWithRequestConfig(Mockito.any(RequestConfig.class)))
            .thenReturn(mockClient);

        mockHttpClientFactory.when(DSpaceHttpClientFactory::getInstance).thenReturn(mockFactory);

        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            "org.dspace.content.authority.OpenAIREProjectAuthority = OpenAIREProjectAuthority");

        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
    }

    @After
    @Override
    public void destroy() throws Exception {
        super.destroy();
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void openAIREAuthorityTest() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/OpenAIREProjectAuthority/entries")
            .param("metadata", "dc.contributor.author")
            .param("filter", "mushroom"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", Matchers.containsInAnyOrder(
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::103679",
                    "Mushroom Robo-Pic - Development of an autonomous robotic mushroom picking system",
                    "Mushroom Robo-Pic - Development of an autonomous robotic mushroom picking system(103679)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::752540",
                    "Exending Shelf Life of Mushroom Growing Kits",
                    "Exending Shelf Life of Mushroom Growing Kits(752540)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::LP0220040",
                    "Use of Organic Residues in Edible Mushroom Production",
                    "Use of Organic Residues in Edible Mushroom Production(LP0220040)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::133611",
                    "The development of a mushroom harvesting machine to increase yield and "
                        + "production while reducing waste and labour shortage risk",
                    "The development of a mushroom harvesting machine to increase yield and "
                        + "production while reducing waste and labour shortage risk(133611)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::820352",
                    "Smart MAnagement of spent mushRoom subsTrate to lead the MUSHROOM "
                        + "sector towards a circular economy",
                    "Smart MAnagement of spent mushRoom subsTrate to lead the MUSHROOM "
                        + "sector towards a circular economy(820352)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::2300148817",
                    "Production of therapeutic proteins in mushroom",
                    "Production of therapeutic proteins in mushroom(2300148817)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::2300148209",
                    "Control of Verticillium fungicola on mushroom",
                    "Control of Verticillium fungicola on mushroom(2300148209)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::2300147728",
                    "Master switches of initiation of mushroom formation",
                    "Master switches of initiation of mushroom formation(2300147728)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::2300164658",
                    "Push the white button; controlling mushroom formation",
                    "Push the white button; controlling mushroom formation(2300164658)",
                    "vocabularyEntry"),
                ItemAuthorityMatcher.matchItemAuthorityProperties(
                    "will be generated::OPENAIRE-PROJECT-ID::6112554",
                    "Respiratory Mechanisms in Cultivated Mushroom",
                    "Respiratory Mechanisms in Cultivated Mushroom(6112554)",
                    "vocabularyEntry"))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(10)));

    }

}
