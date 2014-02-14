/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.integration.xoai;

import org.junit.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OAIContextTest extends AbstractDSpaceTest {
    public static final String ROOT_URL = "/";

    @Test
    public void requestToRootShouldGiveListOfContextsWithBadRequestError() throws Exception {
        againstTheDataProvider().perform(get(ROOT_URL))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeExists("contexts"));
    }

    @Test
    public void requestForUnknownContextShouldGiveListOfContextsWithBadRequestError() throws Exception {
        againstTheDataProvider().perform(get("/unexistentContext"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeExists("contexts"));
    }
}
