/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * This class test the REST controller that returns information about the client user.
 * E.g. the client's IP address.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinUserInfoControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void getUserIPAddress() throws Exception {
        getClient().perform(get("/api/userinfo/ipaddress")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ipAddress\":\"127.0.0.1\"}"));
    }
}
