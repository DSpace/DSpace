/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * Regression test for the issue where a dc.description.provenance value added via REST PATCH
 * returned HTTP 200 and was persisted, but was invisible in the default item projection.
 *
 * System provenance is written with language "en" (e.g. by InstallItem), while a PATCH stores the
 * new value with a null language. The per-field language filter used to drop the null-language
 * value when a locale-matching value existed for the field. Language-neutral (null-language) values
 * must remain visible alongside the localized values.
 */
public class ProvenancePatchIT extends AbstractControllerIntegrationTest {

    @Test
    public void patchAddProvenanceIsVisibleInDefaultProjection() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1")
                                            .build();
        // An installed item already carries a system provenance value with language "en".
        Item item = ItemBuilder.createItem(context, col1).withTitle("Public item 1").build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = List.of(
            new AddOperation("/metadata/dc.description.provenance/-", "User added provenance"));
        getClient(token).perform(patch("/api/core/items/" + item.getID())
                            .content(getPatchContent(ops))
                            .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());

        // The added (null-language) provenance must be visible to an admin in the default projection.
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.metadata",
                            matchMetadata("dc.description.provenance", "User added provenance")));
    }
}
