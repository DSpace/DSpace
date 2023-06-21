/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.model.HandleRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.handle.Handle;
import org.dspace.handle.service.HandleClarinService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test to test the /api/clarin/import/handle endpoint
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class ClarinHandleImportControllerIT extends AbstractControllerIntegrationTest {
    @Autowired
    private HandleClarinService handleService;

    @Test
    public void createHandleWithoutDSpaceObjectTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HandleRest handleRest = new HandleRest();
        handleRest.setHandle("123456789/1");
        handleRest.setResourceTypeID(2);
        Integer handleId = null;
        String adminToken = getAuthToken(admin.getEmail(), password);
        MvcResult mvcResult = getClient(adminToken).perform(post("/api/clarin/import/handle")
                        .content(mapper.writeValueAsBytes(handleRest))
                        .contentType(contentType))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        handleId = Integer.valueOf(String.valueOf(map.get("id")));
        //find created handle
        Handle handle = handleService.findByID(context, handleId);
        //control
        assertEquals(handle.getHandle(), "123456789/1");
        assertEquals(handle.getResourceTypeId(), (Integer)2);
        assertNull(handle.getUrl());
        assertNull(handle.getDSpaceObject());
        //clean all
        context.turnOffAuthorisationSystem();
        List<Handle> handles = handleService.findAll(context);
        for (Handle h: handles) {
            handleService.delete(context, h);
        }
        context.restoreAuthSystemState();
    }
}
