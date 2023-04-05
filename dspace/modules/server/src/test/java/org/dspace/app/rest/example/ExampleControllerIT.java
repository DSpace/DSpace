/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.example;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * This IT serves as an example of how & where to add integration tests for local customizations to the DSpace REST API.
 * See {@link ExampleController} for the Controller of which the functionality is tested.
 */
public class ExampleControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void testTest() throws Exception {

        getClient()
                .perform(get("/example"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello world"));
    }
}
