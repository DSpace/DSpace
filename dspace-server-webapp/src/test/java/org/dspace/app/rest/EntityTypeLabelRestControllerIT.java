/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.content.EntityType;
import org.dspace.content.service.EntityTypeService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the {@link org.dspace.app.rest.EntityTypeLabelRestController} controlled endpoints
 *
 * @author Maria Verdonck (Atmire) on 16/12/2019
 */
public class EntityTypeLabelRestControllerIT extends AbstractEntityIntegrationTest {

    @Autowired
    private EntityTypeService entityTypeService;

    @Test
    public void testGetEntityTypeByLabel_ExistingLabel() throws Exception {
        String testLabel = "Person";
        EntityType entityType = entityTypeService.findByEntityType(context, testLabel);
        getClient().perform(get("/api/core/entitytypes/label/" + testLabel))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(entityType.getID())))
                .andExpect(jsonPath("$.label", containsString(testLabel)));
    }

    @Test
    public void testGetEntityTypeByLabel_NonExistentLabel() throws Exception {
        String testLabel = "Person2";
        getClient().perform(get("/api/core/entitytypes/label" + testLabel))
                .andExpect(status().isNotFound());
    }
}
