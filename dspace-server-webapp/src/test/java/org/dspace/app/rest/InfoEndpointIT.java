/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for info actuator.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class InfoEndpointIT extends AbstractControllerIntegrationTest {

    private static final String INFO_PATH = "/actuator/info";

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void testWithAnonymousUser() throws Exception {

        getClient().perform(get(INFO_PATH))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void testWithNotAdminUser() throws Exception {

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get(INFO_PATH))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testWithAdminUser() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get(INFO_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.app.name", matchProperty("dspace.name")))
            .andExpect(jsonPath("$.app.dir", matchProperty("dspace.dir")))
            .andExpect(jsonPath("$.app.url", matchProperty("dspace.server.url")))
            .andExpect(jsonPath("$.app.db", matchProperty("db.url")))
            .andExpect(jsonPath("$.app.solr.server", matchProperty("solr.server")))
            .andExpect(jsonPath("$.app.solr.prefix", matchProperty("solr.multicorePrefix")))
            .andExpect(jsonPath("$.app.mail.server", matchProperty("mail.server")))
            .andExpect(jsonPath("$.app.mail.from-address", matchProperty("mail.from.address")))
            .andExpect(jsonPath("$.app.mail.feedback-recipient", matchProperty("feedback.recipient")))
            .andExpect(jsonPath("$.app.mail.mail-admin", matchProperty("mail.admin")))
            .andExpect(jsonPath("$.app.mail.mail-helpdesk", matchProperty("mail.helpdesk")))
            .andExpect(jsonPath("$.app.mail.alert-recipient", matchProperty("alert.recipient")))
            .andExpect(jsonPath("$.app.cors.allowed-origins", matchProperty("rest.cors.allowed-origins")))
            .andExpect(jsonPath("$.app.ui.url", matchProperty("dspace.ui.url")))
            .andExpect(jsonPath("$.java").exists());

    }

    private Matcher<?> matchProperty(String name) {
        return is(configurationService.getProperty(name));
    }


}
