/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.Collections.singletonList;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.junit.Test;

/**
 * Tests to validate that in case a patch requests contains an authority and interested field is
 * populated from a closed controlled vocabulary, authority can be stored
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class PatchWithAuthorityIT extends AbstractControllerIntegrationTest {

    private WorkspaceItem workspaceItem;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("parent community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("collection")
            .withEntityType("Publication")
            .build();

        workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection).build();

        context.restoreAuthSystemState();
    }

    @Test
    public void addValueFromControlledVocabularyHasAuthorityStored() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);

        MetadataValueRest value = new MetadataValueRest("dataset");
        value.setAuthority("c_ddb1");
        value.setConfidence(600);
        List<Operation> operations =
            singletonList(new AddOperation("/sections/publication/dc.type",
                singletonList(value)));

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(getPatchContent(operations)))
            .andExpect(status().isOk());

        Item item = context.reloadEntity(workspaceItem).getItem();

        assertThat(item.getMetadata(), hasItem(with("dc.type", "dataset", null,
            "c_ddb1", 0, 600)));
    }

    @Test
    public void addValueWithoutControlledVocabularyHasNotAuthorityStored() throws Exception {

        String authToken = getAuthToken(admin.getEmail(), password);

        MetadataValueRest value = new MetadataValueRest("title test");
        value.setAuthority("fake");
        value.setConfidence(600);
        List<Operation> operations =
            singletonList(new AddOperation("/sections/publication/dc.title",
                singletonList(value)));

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + workspaceItem.getID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(getPatchContent(operations)))
            .andExpect(status().isOk());

        Item item = context.reloadEntity(workspaceItem).getItem();

        assertThat(item.getMetadata(), hasItem(with("dc.title", "title test", null,
            null, 0, -1)));
    }

}
