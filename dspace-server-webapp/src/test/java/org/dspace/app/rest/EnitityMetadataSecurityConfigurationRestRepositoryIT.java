/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.repository.EnitityMetadataSecurityConfigurationRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration tests for {@link EnitityMetadataSecurityConfigurationRestRepository}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class EnitityMetadataSecurityConfigurationRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void testFindOne() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/core/securitysettings/Project"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is("Project")))
            .andExpect(jsonPath("$.type", is("securitysetting")))
            .andExpect(jsonPath("$.metadataSecurityDefault", contains(0, 1, 2)))
            .andExpect(jsonPath("$.metadataCustomSecurity", anEmptyMap()))
            .andExpect(jsonPath("$._links.self.href", stringContainsInOrder("/api/core/securitysettings/Project")));

        getClient(token).perform(get("/api/core/securitysettings/Person"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is("Person")))
            .andExpect(jsonPath("$.type", is("securitysetting")))
            .andExpect(jsonPath("$.metadataSecurityDefault", contains(0, 1)))
            .andExpect(jsonPath("$.metadataCustomSecurity", aMapWithSize(5)))
            .andExpect(jsonPath("$.metadataCustomSecurity['dc.description.provenance']", contains(0, 1, 2)))
            .andExpect(jsonPath("$.metadataCustomSecurity['creativework.datePublished']", contains(1, 2)))
            .andExpect(jsonPath("$.metadataCustomSecurity['cris.author.scopus-author-id']", contains(1, 2)))
            .andExpect(jsonPath("$.metadataCustomSecurity['cris.identifier.gscholar']", contains(0, 1, 2)))
            .andExpect(jsonPath("$.metadataCustomSecurity['cris.workflow.name']", contains(1)))
            .andExpect(jsonPath("$._links.self.href", stringContainsInOrder("/api/core/securitysettings/Person")));
    }

    @Test
    public void testFindOneWithAnonymousUser() throws Exception {

        getClient().perform(get("/api/core/securitysettings/Project"))
            .andExpect(status().isUnauthorized());

    }
}
