/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.ldn;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

public class LDNInboxControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void methodNotAllowedTest() throws Exception {
        // HEAD not allowed
        getClient()
            .perform(head("/ldn/inbox"))
            .andExpect(status().is(405));
        // GET not allowed
        getClient()
            .perform(get("/ldn/inbox"))
            .andExpect(status().is(405));
        // PUT not allowed
        getClient()
            .perform(put("/ldn/inbox"))
            .andExpect(status().is(405));
        // PATCH not allowed
        getClient()
            .perform(patch("/ldn/inbox"))
            .andExpect(status().is(405));
        // DELETE not allowed
        getClient()
            .perform(delete("/ldn/inbox"))
            .andExpect(status().is(405));
    }

    @Test
    public void optionsTest() throws Exception {
        getClient()
            .perform(options("/ldn/inbox"))
            .andExpect(status().is(200))
            .andExpect(header().string("Accept-Post", "application/ld+json"))
            .andExpect(header().string("Allow", "OPTIONS,POST"));
    }

}
